package io.sapl.server.ce.security;

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.vaadin.flow.spring.security.AuthenticationContext;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthenticatedUser {

	private final AuthenticationContext authenticationContext;

	public Optional<UserDetails> get() {
		return authenticationContext.getAuthenticatedUser(UserDetails.class);
	}

	public void logout() {
		authenticationContext.logout();
	}

}
