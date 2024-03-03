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
package io.sapl.server.ce.model.setup;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiAuthenticationConfig {
    static final String BASICAUTH_PATH            = "io.sapl.server.allowBasicAuth";
    static final String APIKEYAUTH_PATH           = "io.sapl.server.allowApiKeyAuth";
    static final String APIKEYHEADERNAME_PATH     = "io.sapl.server.apiKeyHeaderName";
    static final String APIKEYCACHINGENABLED_PATH = "io.sapl.server.apiKeyCaching.enabled";
    static final String APIKEYCACHINGEXPIRE_PATH  = "io.sapl.server.apiKeyCaching.expire";
    static final String APIKEYCACHINGMAXSIZE_PATH = "io.sapl.server.apiKeyCaching.maxSize";

    private boolean basicAuthEnabled     = false;
    private boolean apiKeyAuthEnabled    = false;
    private String  apiKeyHeaderName     = "";
    private boolean apiKeyCachingEnabled = false;
    private int     apiKeyCachingExpires = 300;
    private int     apiKeyCachingMaxSize = 10000;
    private boolean saved                = false;

    public boolean isValidConfig() {
        if (apiKeyAuthEnabled) {
            return !apiKeyHeaderName.isEmpty()
                    && (!apiKeyCachingEnabled || (apiKeyCachingExpires > 0 && apiKeyCachingMaxSize > 0));
        }
        return true;
    }

}
