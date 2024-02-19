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
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import io.sapl.server.ce.setup.ApplicationYmlHandler;
import io.sapl.server.ce.setup.condition.SetupNotFinishedCondition;
import io.sapl.server.ce.ui.utils.ConfirmUtils;
import io.sapl.server.ce.ui.utils.ErrorNotificationUtils;
import io.sapl.server.ce.ui.views.SetupLayout;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@AnonymousAllowed
@RequiredArgsConstructor
@PageTitle("DBMS Setup")
@Route(value = DbmsSetupView.ROUTE, layout = SetupLayout.class)
@Conditional(SetupNotFinishedCondition.class)
public class DbmsSetupView extends VerticalLayout {

    public static final String ROUTE = "/setup/dbms";

    @Autowired
    private ApplicationYmlHandler          applicationYmlHandler;
    private static final String            H2_DRIVER_CLASS_NAME      = "org.h2.Driver";
    private static final String            MARIADB_DRIVER_CLASS_NAME = "org.mariadb.jdbc.Driver";
    private static String                  url                       = "jdbc:h2:file:~/sapl/db";
    private static String                  user                      = "";
    private static String                  pwd                       = "";
    private static String                  dbmsType                  = "H2";
    private static boolean                 enableSaveConfigBtn;
    private final RadioButtonGroup<String> dbms                      = new RadioButtonGroup<>("DBMS");
    private final TextField                dbmsURL                   = new TextField("DBMS URL");
    private final TextField                dbmsUsername              = new TextField("DBMS Username");
    private final PasswordField            dbmsPwd                   = new PasswordField("DBMS Password");
    private final Button                   dbmsTest                  = new Button("Test connection");
    private final Button                   dbmsSaveConfig            = new Button("Save DBMS-Configuration");

    @PostConstruct
    private void init() {
        add(getLayout());
    }

    public Component getLayout() {
        dbms.setItems("H2", "MariaDB");
        dbms.setValue(dbmsType);
        dbms.addValueChangeListener(e -> {
            setDbmsConnStringDefault();
            dbmsType = e.getValue();
        });
        dbmsURL.setValue(url);
        dbmsURL.setRequiredIndicatorVisible(true);
        dbmsURL.setClearButtonVisible(true);
        dbmsURL.setValueChangeMode(ValueChangeMode.EAGER);
        dbmsURL.addValueChangeListener(e -> {
            setSaveButtonDisable();
            url = e.getValue();
        });
        dbmsUsername.setRequiredIndicatorVisible(true);
        dbmsUsername.setClearButtonVisible(true);
        dbmsUsername.setValue(user);
        dbmsUsername.setValueChangeMode(ValueChangeMode.EAGER);
        dbmsUsername.addValueChangeListener(e -> {
            setSaveButtonDisable();
            user = e.getValue();
        });
        dbmsPwd.setRequiredIndicatorVisible(true);
        dbmsPwd.setClearButtonVisible(true);
        dbmsPwd.setValue(pwd);
        dbmsPwd.setValueChangeMode(ValueChangeMode.EAGER);
        dbmsPwd.addValueChangeListener(e -> {
            setSaveButtonDisable();
            pwd = e.getValue();
        });
        dbmsTest.setVisible(true);
        dbmsTest.addClickListener(e -> dbmsConnectionTest());
        dbmsSaveConfig.setVisible(true);
        dbmsSaveConfig.setEnabled(enableSaveConfigBtn);
        dbmsSaveConfig.addClickListener(e -> writeDbmsConfigToApplicationYml());

        FormLayout dbmsLayout = new FormLayout(dbms, dbmsURL, dbmsUsername, dbmsPwd, dbmsTest, dbmsSaveConfig);
        dbmsLayout.setColspan(dbms, 2);
        dbmsLayout.setColspan(dbmsURL, 2);
        dbmsLayout.setColspan(dbmsSaveConfig, 2);
        dbmsLayout.setColspan(dbmsTest, 2);

        return dbmsLayout;
    }

    private void setSaveButtonDisable() {
        enableSaveConfigBtn = false;
        dbmsSaveConfig.setEnabled(false);
    }

    private void setDbmsConnStringDefault() {
        switch (dbms.getValue()) {
        case "H2":
            dbmsURL.setValue("jdbc:h2:file:~/sapl/db");
            break;
        case "MariaDB":
            dbmsURL.setValue("jdbc:mariadb://127.17.0.2:3306/saplserver");
            break;
        default:
        }
    }

    private void writeDbmsConfigToApplicationYml() {
        String driverClassName = "";
        switch (dbms.getValue()) {
        case "H2":
            driverClassName = H2_DRIVER_CLASS_NAME;
            break;
        case "MariaDB":
            driverClassName = MARIADB_DRIVER_CLASS_NAME;
            break;
        default:
        }

        applicationYmlHandler.setAt("spring/datasource/driverClassName", driverClassName);
        applicationYmlHandler.setAt("spring/datasource/url", dbmsURL.getValue());
        applicationYmlHandler.setAt("spring/datasource/username", dbmsUsername.getValue());
        applicationYmlHandler.setAt("spring/datasource/password", dbmsPwd.getValue());
        try {
            applicationYmlHandler.saveYmlFiles();
            ConfirmUtils.inform("saved", "DBMS setup successfully saved");
        } catch (IOException ioe) {
            ConfirmUtils.inform("IO-Error",
                    "Error while writing application.yml-File. Please make sure that the file is not in use and can be written. Otherwise configure the application.yml-file manually. Error: "
                            + ioe.getMessage());
        }
    }

    private void dbmsConnectionTest() {
        try {
            SqlConnection.test(dbmsURL.getValue(), dbmsUsername.getValue(), dbmsPwd.getValue());
            enableSaveConfigBtn = true;
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