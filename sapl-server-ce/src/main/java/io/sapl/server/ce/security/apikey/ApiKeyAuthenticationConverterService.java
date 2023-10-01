/*
 * Copyright © 2023 Dominic Heutelbeck (dominic@heutelbeck.com)
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

import io.sapl.server.ce.security.ClientDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationConverterService implements ServerAuthenticationConverter {

	@Value("${io.sapl.server.accesscontrol.apiKeyHeaderName:API_KEY}")
	private String	apiKeyHeaderName;
	private final ClientDetailsService clientDetailsService;


	@Override
	public Mono<Authentication> convert(ServerWebExchange exchange) {
		return Mono.justOrEmpty(exchange)
				.flatMap(serverWebExchange -> Mono.justOrEmpty(
						serverWebExchange.getRequest().getHeaders().get(apiKeyHeaderName)))
				.filter(headerValues -> !headerValues.isEmpty()).flatMap(headerValues -> lookup(headerValues.get(0)));
	}

	/**
	 * Lookup authentication token in cache.
	 *
	 * @param apiKey api key
	 */
	private Mono<ApiKeyAuthenticationToken> lookup(final String apiKey) {
		if (clientDetailsService.isApiKeyAllowedToConnect(apiKey)) {
			return Mono.just(new ApiKeyAuthenticationToken(apiKey, "apikey"));
		} else {
			return Mono.error(() -> new ApiKeyAuthenticationException("ApiKey not authorized"));
		}
	}

}