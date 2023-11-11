/*
 * Copyright (C) 2017-2023 Dominic Heutelbeck (dominic@heutelbeck.com)
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
package io.sapl.server.ce.security.apikey;

import io.sapl.server.ce.model.clients.AuthType;
import io.sapl.server.ce.model.clients.ClientCredentialsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApiKeyFinderService {
    private final PasswordEncoder             passwordEncoder;
    private final ClientCredentialsRepository clientCredentialsRepository;

    public boolean isApiKeyAssociatedWithClientCredentials(String apiKey) throws AuthenticationException {
        // extract key from apiKey
        var apiKeyComponents = apiKey.split("\\.");
        if (apiKeyComponents.length == 2) {
            var key = apiKeyComponents[0];
            var c   = clientCredentialsRepository.findByKey(key)
                    .orElseThrow(() -> new UsernameNotFoundException("Provided apiKey client credentials not found"));
            return c.getAuthType().equals(AuthType.ApiKey) && passwordEncoder.matches(apiKey, c.getEncodedSecret());
        } else {
            throw new AuthenticationServiceException("Invalid apiKey");
        }
    }
}
