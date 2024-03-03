# SAPL ServerCE Setup Wizard

## Description:
The Setup Wizard is a component designed to streamline the setup and initial configuration process of the SAPL ServerCE. It serves as a user-friendly interface that guides administrators through the necessary steps to properly set up and configure the SAPL ServerCE for their specific environment.

## Key Features:
1. Configure the DBMS connection, either with an existing H2 or MariaDB database or a newly created H2 database.
2. Set up the username and password for the administrator user for the SAPL ServerCE.
3. Configure the HTTP and RSocket endpoints of the SAPL ServerCE.

## Usage:
- Run the SAPL Server CE with the production profile as described in the readme.
- If the application cannot find a URL for the connection to the database in the configuration, the Setup Wizard will be started and shown in the browser. The default ports are 8080 and 8443.
- Now you can set up all the parameters according to your environment.
- After you have saved all settings, use the "Restart SAPL ServerCE" button to restart the application.
- Once restarted, the new configuration parameters will take effect.

## Limitations:
- If you use the spring-dev-tools by activating them in the pom.xml, the restart functionality works correctly only in the production profile. If you use spring-dev-tool with a non-production profile, you have to restart the application yourself.

## Good to know:
- The configuration properties are stored in the Spring application.yml files.
- It can work with multiple application-*.yml files, for example, when using different profiles or importing a second application-*.yml file into the main one.
- The Setup Wizard attempts to overwrite existing properties in the file with the highest priority.
- If a property is not available, the Setup Wizard will add it to the file with the highest priority.
- If no application.yml file outside the classpath is found, the Setup Wizard will create one and all necessary folders located in <working-dir>/config/

## If you don't want to use it
If you don't want to use it, just have a look at the documentation, set up the application.yml files as you prefer and start the SAPL ServerCE