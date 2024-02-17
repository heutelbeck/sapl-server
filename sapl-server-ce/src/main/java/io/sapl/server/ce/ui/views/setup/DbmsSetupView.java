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
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import io.sapl.server.SaplServerCeApplication;
import io.sapl.server.ce.setup.condition.SetupNotFinishedCondition;
import io.sapl.server.ce.setup.ApplicationYamlHandler;
import io.sapl.server.ce.ui.utils.ConfirmUtils;
import io.sapl.server.ce.ui.utils.ErrorNotificationUtils;
import io.sapl.server.ce.ui.views.SetupLayout;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

//TODO: Add MariaDB support.

@AnonymousAllowed
@RequiredArgsConstructor
@PageTitle("DBMS Setup")
@Route(value = DbmsSetupView.ROUTE, layout = SetupLayout.class)
@Conditional(SetupNotFinishedCondition.class)
public class DbmsSetupView extends VerticalLayout {

    public static final String  ROUTE     = "/setup/dbms";
    private static final String H2_DRIVER = "org.h2.Driver";

    private ApplicationYamlHandler         applicationYamlHandler;
    @Getter
    private final static Icon              finishedIcon   = VaadinIcon.CHECK_CIRCLE.create();
    private final RadioButtonGroup<String> dbms           = new RadioButtonGroup<>("DBMS");
    private final TextField                dbmsURL        = new TextField("DBMS URL");
    private final TextField                dbmsUsername   = new TextField("DBMS Username");
    private final PasswordField            dbmsPwd        = new PasswordField("DBMS Password");
    private final Button                   dbmsTest       = new Button("Test connection");
    private final Button                   dbmsSaveConfig = new Button("Save DBMS-Configuration");
    private final Button                   restart        = new Button("Restart Server CE");

    @Autowired
    public DbmsSetupView(ApplicationYamlHandler appYH) {
        this.applicationYamlHandler = appYH;
    }

    @PostConstruct
    private void init() {
        add(getLayout());

    }

    public Component getLayout() {
        dbms.setItems("H2", "MariaDB");
        dbms.addValueChangeListener(e -> setDbmsConnStringDefault());
        dbmsURL.setRequiredIndicatorVisible(true);
        dbmsURL.setClearButtonVisible(true);
        dbmsURL.setVisible(false);
        dbmsURL.setValueChangeMode(ValueChangeMode.EAGER);
        dbmsURL.addValueChangeListener(e -> setSaveButtonDisable());
        dbmsUsername.setRequiredIndicatorVisible(true);
        dbmsUsername.setClearButtonVisible(true);
        dbmsUsername.setVisible(false);
        dbmsUsername.setValueChangeMode(ValueChangeMode.EAGER);
        dbmsUsername.addValueChangeListener(e -> setSaveButtonDisable());
        dbmsPwd.setRequiredIndicatorVisible(true);
        dbmsPwd.setClearButtonVisible(true);
        dbmsPwd.setVisible(false);
        dbmsPwd.setValueChangeMode(ValueChangeMode.EAGER);
        dbmsPwd.addValueChangeListener(e -> setSaveButtonDisable());
        dbmsTest.setVisible(true);
        dbmsTest.addClickListener(e -> dbmsConnectionTest());
        dbmsSaveConfig.setVisible(true);
        dbmsSaveConfig.setEnabled(false);
        dbmsSaveConfig.addClickListener(e -> {
            writeDbmsConfigToApplicationYml();
            if (!applicationYamlHandler.getAt("spring/datasource/url", "").toString().isEmpty()) {
                restart.setEnabled(true);
            }
        });
        restart.addClickListener(e -> SaplServerCeApplication.restart());
        if (applicationYamlHandler.getAt("spring/datasource/url", "").toString().isEmpty()) {
            restart.setEnabled(false);
        }

        FormLayout dbmsLayout = new FormLayout(dbms, dbmsURL, dbmsUsername, dbmsPwd, dbmsTest, dbmsSaveConfig, restart);
        dbmsLayout.setColspan(dbms, 2);
        dbmsLayout.setColspan(dbmsURL, 2);
        dbmsLayout.setColspan(dbmsSaveConfig, 2);
        dbmsLayout.setColspan(dbmsTest, 2);
        dbmsLayout.setColspan(restart, 2);

        return dbmsLayout;
    }

    private void setSaveButtonDisable() {
        dbmsSaveConfig.setEnabled(false);
    }

    private void setDbmsConnStringDefault() {
        switch (dbms.getValue()) {
        case "H2":
            dbmsURL.setValue("jdbc:h2:file:~/sapl/db");
            break;
        case "MariaDB":
            dbmsURL.setValue("localhost:3306/saplserver");
            break;
        default:
        }
        dbmsURL.setVisible(true);
        dbmsUsername.setVisible(true);
        dbmsPwd.setVisible(true);
        dbmsSaveConfig.setEnabled(false);
    }

    private void writeDbmsConfigToApplicationYml() {
        String driverClassName = "";
        switch (dbms.getValue()) {
        case "H2":
            driverClassName = H2_DRIVER;
            break;
        case "MariaDB":
            break;
        default:
        }

        applicationYamlHandler.setAt("spring/datasource/driverClassName", driverClassName);
        applicationYamlHandler.setAt("spring/datasource/url", dbmsURL.getValue());
        applicationYamlHandler.setAt("spring/datasource/username", dbmsUsername.getValue());
        applicationYamlHandler.setAt("spring/datasource/password", dbmsPwd.getValue());
        try {
            applicationYamlHandler.saveYamlFiles();
            ConfirmUtils.inform("saved", "DBMS setup successfully saved");
        } catch (IOException ioe) {
            ConfirmUtils.inform("IO-Error",
                    "Error while writing application.yml-File. Please make sure that the file is not in use and can be written. Otherwise configure the application.yml-file manually. Error: "
                            + ioe.getMessage());
        }
    }

    private void dbmsConnectionTest() {
        try {
            System.out.println("URL: " + dbmsURL.getValue());
            System.out.println("User: " + dbmsUsername.getValue());
            System.out.println("Password: " + dbmsPwd.getValue());
            SqlConnection.test(dbmsURL.getValue(), dbmsUsername.getValue(), dbmsPwd.getValue());
            dbmsSaveConfig.setEnabled(true);
            ConfirmUtils.inform("Success", "Connection test sucessfull");
        } catch (SQLException e) {
            dbmsSaveConfig.setEnabled(false);
            ErrorNotificationUtils.show("Connection to the database not possible. " + e.getMessage());
        }
    }

}

class SqlConnection {
    public static void test(String jdbcURL, String username, String pwd) throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcURL, username, pwd);
        connection.close();
    }
}
