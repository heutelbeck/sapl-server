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

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.rsocket.api.PayloadExchange;
import org.springframework.security.rsocket.authentication.PayloadExchangeAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import io.netty.buffer.ByteBuf;
import io.rsocket.metadata.CompositeMetadata;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ApiKeyPayloadExchangeAuthenticationConverterService implements PayloadExchangeAuthenticationConverter {
    @Value("${io.sapl.server.accesscontrol.apiKeyHeaderName:API_KEY}")
    private String apiKeyHeaderName = "API_KEY";

    private String                    apiKeyMimeTypeValue = String
            .valueOf(MimeType.valueOf("messaging/" + apiKeyHeaderName));
    private final ApiKeyFinderService apiKeyFinderService;

    @Override
    public Mono<Authentication> convert(PayloadExchange exchange) {
        ByteBuf           metadata          = exchange.getPayload().metadata();
        CompositeMetadata compositeMetadata = new CompositeMetadata(metadata, false);
        for (CompositeMetadata.Entry entry : compositeMetadata) {
            if (apiKeyMimeTypeValue.equals(entry.getMimeType())) {
                String apikey = entry.getContent().toString(StandardCharsets.UTF_8);
                if (apiKeyFinderService.isApiKeyAssociatedWithClientCredentials(apikey)) {
                    return Mono.just(new ApiKeyAuthenticationToken());
                } else {
                    return Mono.error(() -> new ApiKeyAuthenticationException("ApiKey not authorized"));
                }
            }
        }
        return Mono.empty();
    }
}
