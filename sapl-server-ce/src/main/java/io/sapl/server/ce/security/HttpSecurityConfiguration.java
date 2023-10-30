package io.sapl.server.ce.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import io.sapl.server.ce.security.apikey.ApiKeaderHeaderAuthFilterService;
import io.sapl.server.ce.ui.views.login.LoginView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class HttpSecurityConfiguration extends VaadinWebSecurity {
	@Value("${io.sapl.server.allowApiKeyAuth:#{true}}")
	private boolean  allowApiKeyAuth;

	@Value("${io.sapl.server.allowOauth2Auth:#{false}}")
	private boolean allowOauth2Auth;

	@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:#{null}}")
	private String jwtIssuerURI;

	private final PasswordEncoder passwordEncoder;
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
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(jwt -> List.of(new SimpleGrantedAuthority(ClientDetailsService.CLIENT)));
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

		if (allowOauth2Auth) {
			log.info("configuring Oauth2 authentication with jwtIssuerURI: " + jwtIssuerURI);
			http = http.oauth2ResourceServer(oauth2 -> oauth2.jwt(
					jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter())
			));
		}

		http.httpBasic(withDefaults()) // offer basic authentication
			// all requests to this end point require the CLIENT role
			.authorizeHttpRequests(authz -> authz.anyRequest().hasAnyAuthority(ClientDetailsService.CLIENT));

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
