/*
 * Copyright (C) 2017-2024 Dominic Heutelbeck (dominic@heutelbeck.com)
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sapl.server.ce.security;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.*;
import java.util.stream.Collectors;

import io.sapl.server.ce.model.setup.condition.SetupFinishedCondition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.vaadin.flow.spring.security.VaadinWebSecurity;

import io.sapl.server.ce.security.apikey.ApiKeaderHeaderAuthFilterService;
import io.sapl.server.ce.ui.views.login.LoginView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Conditional(SetupFinishedCondition.class)
public class HttpSecurityConfiguration extends VaadinWebSecurity {
    @Value("${io.sapl.server.allowBasicAuth:#{false}}")
    private boolean allowBasicAuth;

    @Value("${io.sapl.server.allowApiKeyAuth:#{true}}")
    private boolean allowApiKeyAuth;

    @Value("${io.sapl.server.allowOauth2Auth:#{false}}")
    private boolean allowOauth2Auth;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:#{null}}")
    private String jwtIssuerURI;

    @Value("${io.sapl.server.allowKeycloakLogin:#{true}}")
    private boolean allowKeycloakLogin;

    private final ApiKeaderHeaderAuthFilterService apiKeyAuthenticationFilterService;

    private static final String GROUPS             = "groups";
    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM        = "roles";

    /**
     * Decodes JSON Web Token (JWT) according to the configuration that was
     * initialized by the OpenID Provider specified in the jwtIssuerURI.
     */
    @Bean
    JwtDecoder jwtDecoder() {
        if (allowOauth2Auth) {
            return JwtDecoders.fromIssuerLocation(jwtIssuerURI);
        } else {
            return null;
        }
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                jwt -> List.of(new SimpleGrantedAuthority(ClientDetailsService.CLIENT)));
        return converter;
    }

    /**
     * This filter chain is offering Basic Authn for the API.
     *
     * @param http the HttpSecurity.
     * @return configured HttpSecurity
     * @throws Exception if error occurs during HTTP security configuration
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 5)
    SecurityFilterChain apiAuthnFilterChain(HttpSecurity http) throws Exception {
        // @formatter:off
		http.securityMatcher("/api/**") // API path
		    .csrf(AbstractHttpConfigurer::disable)    // api is not to be browser, disable CSRF token
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)); // no session required

		if (allowApiKeyAuth) {
			log.info("configuring ApiKey for Http authentication");
			http = http.addFilterAt(apiKeyAuthenticationFilterService, UsernamePasswordAuthenticationFilter.class);

		}

        // fix sporadic spring-security issue 9175: https://github.com/spring-projects/spring-security/issues/9175#issuecomment-661879599
        http.headers(headers -> headers
                .withObjectPostProcessor(new ObjectPostProcessor<HeaderWriterFilter>() {
                    @Override
                    public <O extends HeaderWriterFilter> O postProcess(O headerWriterFilter) {
                        headerWriterFilter.setShouldWriteHeadersEagerly(true);
                        return headerWriterFilter;
                    }
                })
        );

		if (allowOauth2Auth) {
			log.info("configuring Oauth2 authentication with jwtIssuerURI: " + jwtIssuerURI);
			http.oauth2ResourceServer(oauth2 -> oauth2.jwt(
					jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter())
			));
		}

        if (allowBasicAuth){
            http.httpBasic(withDefaults()); // offer basic authentication
        }

        // Enable OAuth2 Login with default setting and change the session creation policy to always for a proper login handling
        if (allowKeycloakLogin){
            http
                    .oauth2Login(withDefaults())
                    .logout(logout -> logout.logoutSuccessUrl("/oauth2?logout=true"))
                    .authorizeHttpRequests(authorize -> authorize.requestMatchers("/unauthenticated", "/oauth2/**", "/login/**", "/VAADIN/push/**").permitAll());
        }

        // all requests to this end point require the CLIENT role
        http.authorizeHttpRequests(authz -> authz.anyRequest().hasAnyAuthority(ClientDetailsService.CLIENT));

		// @formatter:on
        return http.build();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(
                requests -> requests.requestMatchers(new AntPathRequestMatcher("/images/*.png")).permitAll());

        // Icons from the line-awesome addon
        http.authorizeHttpRequests(
                requests -> requests.requestMatchers(new AntPathRequestMatcher("/line-awesome/**/*.svg")).permitAll());

        // Xtext services
        http.csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/xtext-service/**")));

        super.configure(http);

        // Set another LoginPage if OAuth2 is enabled
        if (allowKeycloakLogin) {
            setOAuth2LoginPage(http, "/oauth2");
        } else {
            setLoginView(http, LoginView.class);
        }
    }

    // Bean for the administration of OAuth2 clients. Needed if OAuth2 is activated
    /*
     * @Bean public OAuth2AuthorizedClientService authorizedClientService(
     * ClientRegistrationRepository clientRegistrationRepository) { return new
     * InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository); }
     *
     * @Bean public OAuth2AuthorizedClientRepository authorizedClientRepository(
     * OAuth2AuthorizedClientService authorizedClientService) { return new
     * AuthenticatedPrincipalOAuth2AuthorizedClientRepository(
     * authorizedClientService); }
     */

    // Important to extract the OAuth2 roles so that the Role admin is identified
    // right by SpringBoot
    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapperForKeycloak() {
        return authorities -> {
            Set     mappedAuthorities = new HashSet<>();
            var     authority         = authorities.iterator().next();
            boolean isOidc            = authority instanceof OidcUserAuthority;

            if (isOidc) {
                var oidcUserAuthority = (OidcUserAuthority) authority;
                var userInfo          = oidcUserAuthority.getUserInfo();

                // Check if the roles are contained in the REALM_ACCESS_CLAIM or the GROUPS
                // claim
                if (userInfo.hasClaim(REALM_ACCESS_CLAIM)) {
                    // Get the role from the REALM_ACCESS_CLAIM
                    var realmAccess = userInfo.getClaimAsMap(REALM_ACCESS_CLAIM);
                    var roles       = (Collection) realmAccess.get(ROLES_CLAIM);

                    // Add the roles to SpringSecurity
                    mappedAuthorities.addAll(generateAuthoritiesFromClaim(roles));

                } else if (userInfo.hasClaim(GROUPS)) {
                    // Get the role from the GROUPS claim
                    Collection roles = (Collection) userInfo.getClaim(GROUPS);

                    // Add the roles to SpringSecurity
                    mappedAuthorities.addAll(generateAuthoritiesFromClaim(roles));
                }
            } else {
                var                 oauth2UserAuthority = (OAuth2UserAuthority) authority;
                Map<String, Object> userAttributes      = oauth2UserAuthority.getAttributes();

                if (userAttributes.containsKey(REALM_ACCESS_CLAIM)) {
                    Map<String, Object> realmAccess = (Map<String, Object>) userAttributes.get(REALM_ACCESS_CLAIM);
                    Collection          roles       = (Collection) realmAccess.get(ROLES_CLAIM);
                    mappedAuthorities.addAll(generateAuthoritiesFromClaim(roles));
                }
            }
            return mappedAuthorities;
        };
    }

    Collection<SimpleGrantedAuthority> generateAuthoritiesFromClaim(Collection<String> roles) {
        // Returns the roles from OAuth2 and add the prefix ROLE_
        return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).collect(Collectors.toList());
    }
}
