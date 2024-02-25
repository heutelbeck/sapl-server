package io.sapl.server.ce.setup;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Getter
@NoArgsConstructor
public class DBMSConfig {
    static final String DRIVERCLASSNAME_PATH = "spring/datasource/driverClassName";
    static final String URL_PATH             = "spring/datasource/url";
    static final String USERNAME_PATH        = "spring/datasource/username";
    static final String PASSWORD_PATH        = "spring/datasource/password";

    static final String DRIVERCLASSNAME_H2      = "org.h2.Driver";
    static final String DRIVERCLASSNAME_MARIADB = "org.mariadb.jdbc.Driver";

    private SupportedDatasourceTypes dbms;
    private String                   url;
    private String                   username;
    private String                   password;
    private boolean                  validConfig = false;
    @Setter
    private boolean                  saved       = false;

    public void setDbms(SupportedDatasourceTypes dbms) {
        this.dbms        = dbms;
        this.validConfig = false;
    }

    public void setUrl(String url) {
        this.url         = url;
        this.validConfig = false;
    }

    public void setUsername(String username) {
        this.username    = username;
        this.validConfig = false;
    }

    public void setPassword(String password) {
        this.password    = password;
        this.validConfig = false;
    }

    public String getDriverClassName() {
        return switch (dbms) {
        case H2 -> DRIVERCLASSNAME_H2;
        case MARIADB -> DRIVERCLASSNAME_MARIADB;
        };
    }

    public void setToDbmsDefaults(SupportedDatasourceTypes dbms) {
        this.setDbms(dbms);
        switch (dbms) {
        case H2:
            this.url = "jdbc:h2:file:~/sapl/db";
            break;
        case MARIADB:
            this.url = "jdbc:mariadb://127.17.0.2:3306/saplserver";
            break;
        }
    }

    public void testConnection() throws SQLException {
        this.validConfig = false;
        Connection connection = DriverManager.getConnection(this.url, this.username, this.password);
        connection.close();
        this.validConfig = true;
    }

    public static SupportedDatasourceTypes getDatasourceTypeFromDriverClassName(String driverClassName) {
        return switch (driverClassName) {
        case DRIVERCLASSNAME_H2 -> SupportedDatasourceTypes.H2;
        case DRIVERCLASSNAME_MARIADB -> SupportedDatasourceTypes.MARIADB;
        default -> null;
        };
    }

}
