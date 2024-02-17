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

package io.sapl.server.ce.ui.views.setup;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import io.sapl.server.ce.condition.SetupNotFinishedCondition;
import io.sapl.server.ce.config.ApplicationYamlHandler;
import io.sapl.server.ce.ui.views.SetupLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;

@AnonymousAllowed
@PageTitle("RSocket Endpoint Setup")
@Route(value = RSocketEndpointSetupView.ROUTE, layout = SetupLayout.class)
@Conditional(SetupNotFinishedCondition.class)
public class RSocketEndpointSetupView extends EndpointSetupView {
    public static final String  ROUTE       = "/setup/rsocket";
    private static final String PATH_PREFIX = "spring/rsocket/server/";

    @Autowired
    public RSocketEndpointSetupView(ApplicationYamlHandler appYH) {
        super(appYH);
    }

    @Override
    String getPathPrefix() {
        return PATH_PREFIX;
    }

    @Override
    void writeTlsConfigToApplicationYml() {
        super.writeTlsConfigToApplicationYml();
        applicationYamlHandler.setAt(getPathPrefix() + "transport", "tcp");
    }
}
