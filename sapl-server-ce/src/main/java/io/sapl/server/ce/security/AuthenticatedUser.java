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

import com.vaadin.flow.server.VaadinServletRequest;
import io.sapl.server.ce.model.setup.condition.SetupFinishedCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import io.sapl.server.ce.security.oauth2.OAuth2UserDetailsAdapter;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.RequestEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@Conditional(SetupFinishedCondition.class)
public class AuthenticatedUser {

    @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri:#{null}}")
    private String keycloakIssuerUri;

    private static final Logger logger = LoggerFactory.getLogger(AuthenticatedUser.class);

    public Optional<UserDetails> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if the user need an OAuth2 implementation
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            return Optional.of(new OAuth2UserDetailsAdapter(oauth2User));
        } else if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            return Optional.of((UserDetails) authentication.getPrincipal());
        }
        return Optional.empty();
    }

    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            if (authentication.getPrincipal() instanceof OidcUser user) {
                String endSessionEndpoint = keycloakIssuerUri + "/protocol/openid-connect/logout";

                UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endSessionEndpoint)
                        .queryParam("id_token_hint", user.getIdToken().getTokenValue());

                RestTemplate restTemplate = new RestTemplate();
                try {
                    RequestEntity<Void> requestEntity = RequestEntity.get(builder.build().toUri()).build();
                    restTemplate.exchange(requestEntity, Void.class);
                } catch (Exception e) {
                    logger.error("Fehler beim Abmelden der Keycloak-Session");
                }
            }
        }
        // Invalidate the session in Vaadin
        VaadinServletRequest.getCurrent().getHttpServletRequest().getSession().invalidate();
    }

}
