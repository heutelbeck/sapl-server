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

import java.util.List;

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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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

    private final ApiKeaderHeaderAuthFilterService apiKeyAuthenticationFilterService;

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

        setLoginView(http, LoginView.class);
    }

}
