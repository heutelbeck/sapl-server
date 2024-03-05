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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import io.sapl.server.ce.model.setup.ApplicationConfigService;
import io.sapl.server.ce.model.setup.condition.SetupNotFinishedCondition;
import io.sapl.server.ce.ui.utils.ConfirmUtils;
import io.sapl.server.ce.ui.views.SetupLayout;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;

import java.io.IOException;

@AnonymousAllowed
@PageTitle("API Authentication Setup")
@Route(value = ApiAuthenticationSetupView.ROUTE, layout = SetupLayout.class)
@Conditional(SetupNotFinishedCondition.class)
public class ApiAuthenticationSetupView extends VerticalLayout {
    public static final String ROUTE = "/setup/apiauthentication";

    private transient ApplicationConfigService applicationConfigService;

    private final Checkbox     allowBasicAuth     = new Checkbox("Basic Auth");
    private final Checkbox     allowApiKeyAuth    = new Checkbox("API Key Auth");
    private final TextField    apiKeyHeader       = new TextField("API Key Header");
    private final Checkbox     allowApiKeyCaching = new Checkbox("API Key Caching");
    private final IntegerField apiKeyCacheExpires = new IntegerField("Cache expires (seconds)");
    private final IntegerField apiKeyCacheMaxSize = new IntegerField("Max Size");

    private final Button saveConfig = new Button("Save API Authentication Settings");

    public ApiAuthenticationSetupView(@Autowired ApplicationConfigService applicationConfigService){
        this.applicationConfigService = applicationConfigService;
    }
    @PostConstruct
    private void init() {
        add(getLayout());
    }

    private Component getLayout() {
        saveConfig.setEnabled(applicationConfigService.getApiAuthenticationConfig().isValidConfig());
        saveConfig.addClickListener(e -> persistApiAuthenticationConfig());

        allowBasicAuth.setValue(applicationConfigService.getApiAuthenticationConfig().isBasicAuthEnabled());

        allowApiKeyAuth.setValue(applicationConfigService.getApiAuthenticationConfig().isApiKeyAuthEnabled());
        allowApiKeyAuth.getStyle().setAlignItems(Style.AlignItems.START);

        apiKeyHeader.setValueChangeMode(ValueChangeMode.EAGER);
        apiKeyHeader.setRequiredIndicatorVisible(true);
        apiKeyHeader.setRequired(true);
        apiKeyHeader.setValue(applicationConfigService.getApiAuthenticationConfig().getApiKeyHeaderName());
        apiKeyHeader.getStyle().setAlignItems(Style.AlignItems.START);

        allowApiKeyCaching.setValue(applicationConfigService.getApiAuthenticationConfig().isApiKeyCachingEnabled());

        apiKeyCacheExpires.setValueChangeMode(ValueChangeMode.EAGER);
        apiKeyCacheExpires.setRequiredIndicatorVisible(true);
        apiKeyCacheExpires.setRequired(true);
        apiKeyCacheExpires.setMin(1);
        apiKeyCacheExpires.setHelperText("Larger than 0");
        apiKeyCacheExpires.setValue(applicationConfigService.getApiAuthenticationConfig().getApiKeyCachingExpires());

        apiKeyCacheMaxSize.setValueChangeMode(ValueChangeMode.EAGER);
        apiKeyCacheMaxSize.setRequiredIndicatorVisible(true);
        apiKeyCacheMaxSize.setRequired(true);
        apiKeyCacheMaxSize.setMin(1);
        apiKeyCacheMaxSize.setHelperText("Larger than 0");
        apiKeyCacheMaxSize.setValue(applicationConfigService.getApiAuthenticationConfig().getApiKeyCachingMaxSize());

        allowBasicAuth.addValueChangeListener(e -> updateApiAuthenticationConfig());
        allowApiKeyAuth.addValueChangeListener(e -> updateApiAuthenticationConfig());
        apiKeyHeader.addValueChangeListener(e -> updateApiAuthenticationConfig());
        allowApiKeyCaching.addValueChangeListener(e -> updateApiAuthenticationConfig());
        apiKeyCacheExpires.addValueChangeListener(e -> updateApiAuthenticationConfig());
        apiKeyCacheMaxSize.addValueChangeListener(e -> updateApiAuthenticationConfig());

        var apiAuthenticationLayout = new FormLayout(allowBasicAuth, allowApiKeyAuth, apiKeyHeader, allowApiKeyCaching,
                apiKeyCacheExpires, apiKeyCacheMaxSize, saveConfig);

        apiAuthenticationLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                new FormLayout.ResponsiveStep("490px", 2, FormLayout.ResponsiveStep.LabelsPosition.TOP));
        apiAuthenticationLayout.setColspan(allowBasicAuth, 2);
        apiAuthenticationLayout.setColspan(apiKeyHeader, 2);
        apiAuthenticationLayout.setColspan(allowApiKeyCaching, 2);
        apiAuthenticationLayout.setColspan(saveConfig, 2);
        setVisibility();

        return apiAuthenticationLayout;

    }

    private void setVisibility() {
        apiKeyHeader.setVisible(applicationConfigService.getApiAuthenticationConfig().isApiKeyAuthEnabled());
        allowApiKeyCaching.setVisible(applicationConfigService.getApiAuthenticationConfig().isApiKeyAuthEnabled());
        apiKeyCacheExpires.setVisible(applicationConfigService.getApiAuthenticationConfig().isApiKeyAuthEnabled()
                && applicationConfigService.getApiAuthenticationConfig().isApiKeyCachingEnabled());
        apiKeyCacheMaxSize.setVisible(applicationConfigService.getApiAuthenticationConfig().isApiKeyAuthEnabled()
                && applicationConfigService.getApiAuthenticationConfig().isApiKeyCachingEnabled());
    }

    private void persistApiAuthenticationConfig() {
        try {
            applicationConfigService.persistApiAuthenticationConfig();
            ConfirmUtils.inform("saved", "API Authentication setup successfully saved");
        } catch (IOException ioe) {
            ConfirmUtils.inform("IO-Error",
                    "Error while writing application.yml-File. Please make sure that the file is not in use and can be written. Otherwise configure the application.yml-file manually. Error: "
                            + ioe.getLocalizedMessage());
        }
    }

    private void updateApiAuthenticationConfig() {
        applicationConfigService.getApiAuthenticationConfig().setBasicAuthEnabled(allowBasicAuth.getValue());
        applicationConfigService.getApiAuthenticationConfig().setApiKeyAuthEnabled(allowApiKeyAuth.getValue());
        applicationConfigService.getApiAuthenticationConfig().setApiKeyHeaderName(apiKeyHeader.getValue());
        applicationConfigService.getApiAuthenticationConfig().setApiKeyCachingEnabled(allowApiKeyCaching.getValue());
        if (apiKeyCacheExpires.getValue() == null) {
            applicationConfigService.getApiAuthenticationConfig().setApiKeyCachingExpires(0);
        } else {
            applicationConfigService.getApiAuthenticationConfig()
                    .setApiKeyCachingExpires(apiKeyCacheExpires.getValue());
        }
        if (apiKeyCacheMaxSize.getValue() == null) {
            applicationConfigService.getApiAuthenticationConfig().setApiKeyCachingMaxSize(0);
        } else {
            applicationConfigService.getApiAuthenticationConfig()
                    .setApiKeyCachingMaxSize(apiKeyCacheMaxSize.getValue());
        }

        setVisibility();
        saveConfig.setEnabled(applicationConfigService.getApiAuthenticationConfig().isValidConfig());
    }

}
