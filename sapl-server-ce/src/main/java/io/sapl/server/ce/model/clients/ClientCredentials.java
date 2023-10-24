/*
 * Copyright © 2017-2021 Dominic Heutelbeck (dominic@heutelbeck.com)
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
package io.sapl.server.ce.model.clients;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Model for a set of credentials of a single client.
 */
@Getter
@Setter
@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ClientCredentials")
public class ClientCredentials {
	/**
	 * The unique identifier.
	 */
	@Id
	@GeneratedValue
	@Column(name = "Id", nullable = false)
	private Long id;

	/**
	 * The key (user)
	 */
	@Column(length = 250, name = "clientKey", unique = true) // MariaDB / MySQL do not like a column with the name "key"
	private String key;

	/**
	 * The encoded secret (hashed / salted password).
	 */
	@Column(length = 250, name = "clientEncodedSecret")
	private String encodedSecret;

	/**
	 * The encoded secret (hashed / salted password).
	 */
	@Column(length = 512, name = "clientApiKey")
	private String apiKey;

	public enum Authtype {
		BASIC, APIKEY;
	}

	@Enumerated(EnumType.STRING)
	private ClientCredentials.Authtype status = Authtype.BASIC;

	// TODO: basic oder API Key authentication

	public ClientCredentials(String key, String encodedSecret, String apiKey) {
		this.key           = key;
		this.encodedSecret = encodedSecret;
		this.apiKey = apiKey;
	}
}
