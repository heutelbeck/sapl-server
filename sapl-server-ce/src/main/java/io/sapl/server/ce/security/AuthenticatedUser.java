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

import java.util.Optional;

import com.vaadin.flow.component.UI;
import io.sapl.server.ce.model.setup.condition.SetupFinishedCondition;
import io.sapl.server.ce.security.OAuth2.OAuth2UserDetailsAdapter;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import com.vaadin.flow.spring.security.AuthenticationContext;

import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class AuthenticatedUser {

    private final AuthenticationContext authenticationContext;

    // If a user is from an OAuth2 provider then set it to true
    private boolean isOauth2User = false;

    public Optional<UserDetails> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if the user need an OAuth2 implementation
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            isOauth2User = true;
            return Optional.of(new OAuth2UserDetailsAdapter(oauth2User));
        } else if (authentication.getPrincipal() instanceof UserDetails) {
            return Optional.of((UserDetails) authentication.getPrincipal());
        }
        return Optional.empty();
    }

    public void logout() {
        // If OAuth2 is enabled, perform also a logout at the OAuth2 endpoint
        if (isOauth2User) {
            Authentication       authentication     = SecurityContextHolder.getContext().getAuthentication();
            OidcUser             user               = ((OidcUser) authentication.getPrincipal());
            String               endSessionEndpoint = "http://localhost:9000/realms/SAPL/protocol/openid-connect/logout";
            UriComponentsBuilder builder            = UriComponentsBuilder.fromUriString(endSessionEndpoint)
                    .queryParam("id_token_hint", user.getIdToken().getTokenValue());

            RestTemplate restTemplate = new RestTemplate();
            // ResponseEntity<String> response =
            // restTemplate.getForEntity(builder.toUriString(), String.class);
        }
        authenticationContext.logout();
        UI.getCurrent().getSession().close();
    }
}
