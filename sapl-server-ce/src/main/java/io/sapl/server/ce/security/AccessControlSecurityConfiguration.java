package io.sapl.server.ce.security;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.vaadin.flow.spring.security.VaadinWebSecurity;

import io.sapl.server.ce.ui.views.login.LoginView;

@Configuration
@EnableWebSecurity
public class AccessControlSecurityConfiguration extends VaadinWebSecurity {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
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
	SecurityFilterChain tokenAuthnFilterChain(HttpSecurity http) throws Exception {
		// @formatter:off
		http.securityMatcher("/api/**") // API path
		    .csrf(AbstractHttpConfigurer::disable)    // not to be used by a browser disable CSRF token
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // no session required
			.httpBasic(withDefaults()) // offer basic authentication
			// all requests to this end point require the CLIENT role
			.authorizeHttpRequests(authz -> authz.anyRequest().hasAnyRole(ClientDetailsService.CLIENT)); 
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
