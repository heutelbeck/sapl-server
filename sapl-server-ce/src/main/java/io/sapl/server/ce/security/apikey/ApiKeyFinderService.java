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

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.sapl.server.ce.model.clients.ClientCredentials;
import io.sapl.server.ce.model.clients.ClientCredentialsRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApiKeyFinderService {
    private final PasswordEncoder             passwordEncoder;
    private final ClientCredentialsRepository clientCredentialsRepository;

    public boolean isApiKeyAssociatedWithClientCredentials(String apiKey) {
        for (ClientCredentials c : clientCredentialsRepository.getApiKeyCredentials()) {
            if (passwordEncoder.matches(apiKey, c.getEncodedSecret())) {
                return true;
            }
        }
        return false;
    }
}