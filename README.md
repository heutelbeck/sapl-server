# SAPL Server CE - Lightweight Authorization Server  

This server is a lightweight Policy Decision Point (PDP) that uses the Streaming Attribute Policy Language (SAPL) and provides authorization services through an HTTP and RSocket API. SAPL is further explained on the [SAPL home page](https://sapl.io/).

The SAPL Server CE uses a database for PDP settings and SAPL documents. Via WebUI the administrator is allowed for runtime updating of policies that are reflected in decisions made for ongoing authorization subscriptions.

## Introduction

### Aim of the README

This README aims to describe the commissioning and correct operation of SAPL Server CE, providing an easy introduction to its use. Additionally, it presents important information security aspects of SAPL Server CE in a transparent manner.

### Target group

This documentation focuses on the operator and the knowledge required for operating. However, technical aspects of interest for further development of the software are also covered.

### Secure by Design

SAPL Server CE is a software product that follows secure information technology practices and multiple layers of defense based on Secure By Design Principles (also see [CISA Secure By Design](https://www.cisa.gov/sites/default/files/2023-10/SecureByDesign_1025_508c.pdf)).

It is a tool for users to implement Secure by Design systems by incorporating attribute-based access control (ABAC) into their own products and environments.

SAPL adheres to the secure by default principle. SAPL Server CE comes with a basic example configuration that includes the essential security controls to ensure compliance with Germany's Federal Office for Information Security (BSI) [BSI Baseline Protection Compendium Edition 2022](https://www.bsi.bund.de/SharedDocs/Downloads/EN/BSI/Grundschutz/International/bsi_it_gs_comp_2022.pdf).

Application security is a top priority in the development of SAPL. The SAPL project also embraces radical transparency and accountability. Security issues are reported and shared using the GitHub advisory feature. For more details, see the [SAPL Security Policy](https://github.com/heutelbeck/sapl-policy-engine/blob/master/SECURITY.md).

## System requirements

Requirements for local installation.

- Java Development Kit 17 or a newer version
- Operating system that is compatible with java
- Maven 3.9 or a newer version

## Prerequisites and download

SAPL Server CE comes in two forms: as an OCI container or as full source code for building, running from source, and auditing.

### Prerequisites

Before running SAPL Server CE on your system, make sure that you have installed [OpenJDK 17](https://openjdk.org/projects/jdk/17/) or a newer version. [Eclipse Temurin](https://adoptium.net/de/temurin/releases/) is one of the available distributions that provides binaries for different platforms.

Ensure that the Java executables are added to the system path.

### Download and running from source
The source of the policy engine is found on the public GitHub repository: [https://github.com/heutelbeck/sapl-server](https://github.com/heutelbeck/sapl-server).

You can either download the source as a ZIP file and unzip the archive, or clone the repository using git:

```shell
git clone https://github.com/heutelbeck/sapl-server.git
```

## Configuration

A basic configuration is required to operate the SAPL Server CE securely. This includes configuring client application authentication and TLS. SAPL Server CE is implemented using [Spring Boot](https://spring.io/projects/spring-boot/), which offers flexible tools for application configuration. The Spring Boot documentation for [Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config) provides helpful guidelines to follow. It is important to note the order in which configurations are loaded and can overwrite each other.

In summary, the application's configuration is controlled by key-value pairs known as properties. These properties are provided to the application through `.properties` files, `.yml` files, or environment variables. The method of providing these values depends on the specific target environment.

To start the SAPL Server CE for development, there is a minimal basic configuration `application.yml` file in the folder `sapl-server-ce/config` within the folder from where the server is started.

**Note:** This example configuration is not intended for production. It contains secrets and certificates which are publicly known. Whenever you run a SAPL Server CE with this configuration **you** **accept the resulting risks** making the API publicly accessible via the provided credentials and that the server and its decisions cannot be properly authenticated by client applications because of the use of a publicly known self-signed TLS certificate.

To create a configuration for productive, we recommend using the setup wizard to create a basic configuration for the admin user, database, network endpoints and API authentication.
From this point, the user can customize further settings in the application.yml.

**Note:** The Setup Wizard is designed to work with yml-Files. .properties-files are not supported. 

### Manually set username and password
The admin username and password can be set in application.yml. The password has to be encoded with Argon2.
The Argon2 parameters are:
- salt length of 16 bytes
- a hash length of 32 bytes
- parallelism of 1
- memory cost of 1 << 14
- 2 iterations

Alternatively you can use the setup wizard create the encoded password.

### Using Setup-Wizard

#### Prerequisites

##### Certificate

Generate a Certificate using Certbot (Let's Encrypt client). Prerequisites: Server with Internetaccess, DNS Entry with Domain pointing to the IP address of the Server, installed openssl (Example using Ubuntu 20)

Pull Certbot https://github.com/certbot/certbot

Generate a certificate for your domain (e.g. example.com)

```shell
./certbot-auto certonly -a standalone -d example.com -d www.example.com
cd /etc/letsencrypt/live/example.com
openssl pkcs12 -export -in fullchain.pem -inkey privkey.pem -out keystore.p12 -name tomcat -CAfile chain.pem -caname root
```
The keystore.p12 file must then be saved in a directory to which the SAPL Server CE application has access. The storage location must be entered later in the setup wizard.

##### Database

The SAPL Server CE requires a database. It is possible to use an H2 or MariaDB.
When using an H2 database, this can be configured and created by the setup wizard.

When using a MariaDB, the url, username and password must be known to pass the connection test and successfully complete the database configuration.

#### How to start the wizard

**Warning:** When you run a SAPL Server CE Wizard over an unencrypted connection **you accept the risks** that someone could potentially intercept the entered parameters such as usernames and passwords. It is recommended to either configure a secure TLS connection for the application in the application.yml file or to run the wizard locally and later transfer the generated application.yml file to the target system via a secure connection.

The options for starting the application are described below in the chapter `Running the Server CE`.
When the application is started, the parameters in application.yml are checked. If the entry
```
spring.datasource.url
```
or
```
io.sapl.server.accesscontrol.admin-username
```
not present here, the application starts with the setup wizard. The wizard is then accessible depending on the settings for `server.port` and `server.address` in the application.yml. If as an example, you delete the `application.yml` file under `sapl-server-ce/config`, the settings of the `application.yml` under `sapl-server-ce/src/main/resources take` effect and the application can be accessed under the url configured in the application.yml, typically
```
http://localhost:8080/
```

The start page is the welcome page. Here you will find all the information you need to successfully create a configuration with the wizard.

### Logging Configuration

The SAPL Server CE uses the Simple Logging Facade for Java (SLF4J) and the logging framework Logback for logging. The annotation types for SLF4J are imported via Lombok.

Configuration of logging can be done in the `application.yml` file. Additional logging configurations can be found in the [Spring documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging).

#### Logging Level

To configure the logging level for the desired classes, use the `logging.level` property and select one of the following levels:

- `TRACE`:  Detailed information for troubleshooting and debugging during development.
- `DEBUG`: Logs useful information for development and debugging purposes.
- `INFO`: These messages provide updates on the application's progress and significant events.
- `WARN`: Records issues that do not result in errors.
- `ERROR`: The application's errors or exceptions that occurred during execution are logged.

The logging level for the `io.sapl` and `org.springframework` classes is set to `INFO` by default.

To enhance the performance of the SAPL Server CE, consider adjusting the logging level to `WARN`. This will reduce the number of log messages and improve latency. However, it is important to note that decisions made by the PDP will still be logged with the `INFO` level.

#### Logging specific to SAPL

Configuration options for logging PDP decisions are set under `io.sapl.pdp.embedded`. Boolean values determine which log formats are activated or deactivated for PDP decisions.

- `print-trace`: The PDP documents each individual calculation step in a fine-grained manner. The trace is provided in JSON format, which may become very large. This should only be considered as a final option for resolving issues.
- `print-json-report`: This JSON report summarizes the applied algorithms and results of each evaluated policy (set) in the decision-making process. It includes lists of all errors and values of policy information point attributes encountered during the evaluation of each policy (set).
- `print-text-report`: This will log a human-readable textual report based on the same data as the 'print-json-report' option generates.
- `pretty-print-reports`: This option can enable formatting of JSON data while printing JSON during reporting and tracing.

The following code example shows the format in which the information is logged when the `print-text-report` property is activated.

```
--- The PDP made a decision ---
Subscription: {"subject":"bs@simpsons.com","action":"read","resource":"file://example/med/record/patient/BartSimpson"}
Decision    : {"decision":"DENY"}
Timestamp   : 2024-01-12T09:48:19.668342200Z
Algorithm   : "DENY_OVERRIDES"
Matches     : ["policy 1"]
Policy Evaluation Result ===================
Name        : "policy 1"
Entitlement : "DENY"
Decision    : {"decision":"DENY"}
Target      : true
Where       : true
```

#### Log to a file

If you need to write the logs to a file, refer to the [Spring documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging.file-output) for the procedure.

## Running the Server CE

### Running from Source

**Note:** Running from source is not intended for production environments. If doing so, **you accept the risk** of running with publicly known credentials and TLS certificates if you accidentally use the development configuration of the server.

It is likely that you only need to run the server from the source if you are a contributor to the policy engine project.

The source code includes a small development configuration of the server which contains some pre-configured credentials and a self-signed certificate for setting up TLS.

#### Running the Server from source using Maven

Change the current directory to `sapl-server-ce/` and execute the following command:

```shell
mvn -Pproduction
```
Now you can open a browser and visit and visit https://localhost:8443/ .
If started from this folder, the application will start with the demonstration configuration located in  `sapl-server-ce/config` which sets up TLS with a self-signed certificate.
The credentials are demo/demo and the database will be created in ~\sapl


#### Running the Server as a JAR

After the build concludes an executable JAR will be available in the folder `sapl-server-ce/target`.

**Note:** If the JAR is executed from within the folder `sapl-server-ce/` using the command `java -jar target/sapl-server-ce-3.0.0-SNAPSHOT.jar` the server will pick up the same demonstration configuration as described above.

## Deploy via Docker Image

The following Docker Image uses a mounted directory to store access the configuration files as well as the Database if it is used as a standalone. In this example a Windows directory 'C:\devkit\data\docker-sapl'. Copy the application.yml file to the mounted directory. 

```shell
docker run "-p80:8080" "-v/c/devkit/data/docker-sapl:/sapl:rw" ghcr.io/heutelbeck/sapl-server-ce:3.0.0-snapshot
```

Open a browser and visit http://localhost/ . The username and password are both demo.

## Kubernetes

This Section deals with the Deployment via Containerorchestration 

### Prerequisites

Installed Kubernetes v1.28+ 

### Deployment

create a persistent Volume with the Tool of your Choice and the 'storageClassName: saplcepv' it should match the Size specified in the pvclaim.yaml.
Upload the application.yaml and keystore.p12 from \path\to\repository\sapl-server-ce\config\ to the persistent volume.

Create the Namespace

```shell
kubectl create namespace sapl-server-ce
```

Apply the Kubernetes-Deployment.yaml

```shell
kubectl apply -f https://raw.githubusercontent.com/heutelbeck/sapl-server/main/sapl-server-ce/Kubernetes/Kubernetes-Deployment.yaml -n sapl-server-ce
```

For testing Purposes the Service can be changed to Nodeport. 

```shell
kubectl patch svc sapl-server-ce-comp --type='json' -p '[{"op":"replace","path":"/spec/type","value":"NodePort"}]' -n sapl-server-ce
```

Identify the Port 

```shell
kubectl get services -n sapl-server-ce 
```

the output should look like:
  
sapl-server-ce NodePort 10.107.25.241 <none> 8443:30773/TCP 

The service should be reachable in this Example under <https://localhost:30773/> or if you access it from another pc <https://server-ip-adress:30773/>.

DON'T forget to change the type back to ClusterIP 
```shell
kubectl patch svc sapl-server-ce-comp --type='json' -p '[{"op":"replace","path":"/spec/type","value":"ClusterIP"}]' -n sapl-server-ce
```

### Deployment with MariaDB
You can run the SAPL Server CE with MariaDB. To achieve that, use the following steps BEFORE applying the Kubernetes-Deployment.yaml or restart the pod after finishing the steps.

```
kubectl create namespace sapl-server-ce
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install sapl-ce-mariadb --set auth.rootPassword=Q8g7SvwDso5svlebNLQO,auth.username=saplce,auth.password=cvm72OadXaOGgbQ5F9ao,primary.persistence.storageClass=saplcedb bitnami/mariadb -n sapl-server-ce
```

Log into the pod and create Database with Latin-1
```
kubectl exec --stdin --tty -n sapl-server-ce sapl-ce-mariadb-0 -- /bin/bash
mysql -u root -p
CREATE DATABASE saplce CHARACTER SET = 'latin1' COLLATE = 'latin1_swedish_ci';
GRANT ALL PRIVILEGES ON saplce.* TO `saplce`@`%`;
FLUSH Privileges;
```

Copy the following text into the application.yaml 
```
spring:
  datasource:
    driver-class-name: org.mariadb.jdbc.Driver
    url: jdbc:mariadb://sapl-ce-mariadb.sapl-server-ce:3306/saplce
    username: saplce
    password: cvm72OadXaOGgbQ5F9ao
```

#### Kubernetes Deployment with Let's Encrypt Certificate

This section assumes that the Kubernetes is installed on a Linux OS f.e. Ubuntu with exposed ports 80 and 443 to the internet and matching DNS entries.

Install NGINX Ingress Controller according to https://kubernetes.github.io/ingress-nginx/deploy/

```shell
helm upgrade --install ingress-nginx ingress-nginx --repo https://kubernetes.github.io/ingress-nginx --namespace ingress-nginx --create-namespace --set controller.hostNetwork=true,controller.kind=DaemonSet
```

Install Cert-Manager according to https://cert-manager.io/docs/installation/kubernetes/ 

```shell
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.7.2/cert-manager.yaml
```

Change the Email address in the Clusterissuer.yaml (Line email: user@email.com)

```shell
wget https://raw.githubusercontent.com/heutelbeck/sapl-server/main/sapl-server-ce/Kubernetes/clusterissuer.yml
kubectl apply -f clusterissuer.yml 
```

Apply the Persistent Volume yaml (or create persistent volumes with the storageclassnames detailed in the yaml file according to your preferred Method)

```shell
kubectl apply -f https://raw.githubusercontent.com/heutelbeck/sapl-server/main/sapl-server-ce/Kubernetes/sapl-server-ce-pv.yml -n sapl-server-ce
```

Download the Config Files from the Kubernetes/config folder and copy them to the config directory specified in the config-section of sapl-server-ce-pv.yml `/data/sapl-server-ce/conf`


Then download the TLS yaml file 

```shell
wget https://raw.githubusercontent.com/heutelbeck/sapl-server/main/sapl-server-ce/Kubernetes/sapl-server-ce-ingress-sample.yml
```

change the URL in the Ingress section 

```
  tls:
    - hosts:
        - saplce.exampleurl.com
      secretName: saplce.lt.local-tls
  rules:
    - host: saplce.exampleurl.com
```

then apply the yaml file

```shell
kubectl apply -f sapl-server-ce-ingress-sample.yml -n sapl-server-ce
```

## Configuring Keycloak

The SAPL Server CE can also be used with its own Keycloak realm. This means that existing users can be used for login.
Keycloak still needs to be set up so that Keycloak can be used for the SAPL Server CE. The following steps show the necessary configurations for using OAuth2 with Keycloak.

In most cases, a Keycloak realm in which the SAPL client is to be created will already exist. If not, then the realm must first be created in Keycloak via 'Create Realm'.

The first step is to create the role 'ADMIN' so that the users have access to the server CE's pages. The following steps are necessary to create the role:

1. Realm roles
2. Create roles
    1. **Role name**: ADMIN
3. Save

The next step is to create the OAuth2 client so that the SAPL Server CE can connect to Keycloak. For that you have to do the following steps:

1. Clients
2. Create client
    1. **Client type**: OpenID Connect
    2. **Client ID**: sapl-client
    3. **Name**: sapl
    4. **Always display in UI**: Off
3. Next
    1. **Client authentication**: On
    2. **Authorization**: On
    3. **Authentication flow**: Standard flow, Direct access grants
4. Next
    1. **Root URL**:
    2. **Home URL**:
    3. **Valid redirect URIs**: https://\<YOUR CE Server Domain>:8443/*
    4. **Valid post logout redirect URIs**: https://\<YOUR CE Server Domain>:8443/*
    5. **Web origins**: https://\<YOUR CE Server Domain>:8443
5. Save
6. Click on **Client scopes** inside the freshly created client and check if **microprofile-jwt** and **roles** are set to **Default**.
7. Under **Credentials** the client secret can be found, which will be needed later.

The last step is to assign the new role to the client scope **roles**. For that you have to do the following steps:

1. Client scopes (side menu)
2. Roles
3. Scope
4. Assign role
    1. ADMIN
5. Assign

If you are not using LDAP or another user base, you can also simply create a user in Keycloak. Please note that the user requires a valid and non-temporary password and is assigned to the 'ADMIN' role.

To configure the SAPL Server CE correctly, we recommend opening the following overview:

1. Realm settings
2. Endpoints **OpenID Endpoint Configuration**

You should see an overview page in JSON format with various parameters. The next step is to configure the **application.yml**. The following configuration can be adopted here:

```shell
spring.security.oauth2.client:
  registration.keycloak:
    client-id: <Your SAPL client id e.g. sapl-client>
    client-secret: <Your SAPL client secret>
    client-authentication-method: client_secret_basic
    authorization-grant-type: authorization_code
    redirect-uri: "{baseUrl}/login/oauth2/code/keycloak"
    scope: openid, profile, email, roles
    provider: keycloak
  provider.keycloak:
    issuer-uri: <Issuer URI under issuer:>
    user-name-attribute: preferred_username
    jwk-set-uri: <JWK Set URI under jwks_uri:>
    authorization-uri: <Authorization URI under authorization_endpoint:>
    token-uri: <Token URI under token_endpoint:>
    user-info-uri: <User Info URI under userinfo_endpoint:>
```

The last necessary step is to set the parameter `allowOAuth2Login: True` in the application.yml. Now your SAPL Server CE will show an OAuth2 Login page.
=======
### Configure Bucket4j

To configure `Bucket4j` in your application, you'll need to adjust some properties in your configuration file.

Firstly, set `bucket4j.enabled=true` to activate Bucket4j's auto-configuration feature.

Then set `bucket4j.cache-to-use=jcache`.

Use `bucket4j.filters.url` to define the path expression where the rate limit should be applied. The property `bucket4j.filters.strategy=first` ensures that the rate limit stops at the first matching configuration encountered.
et
To retrieve the key, use Spring Expression Language (SpEL) with `bucket4j.filters.rate-limits.cache-key`. Then, determine whher to execute the rate limit using SpEL with `bucket4j.filters.rate-limits.execute-condition`.

Lastly, define the rate limit parameters using `bucket4j.filters.rate-limits.bandwidths` to set up the Bucket4j rate limit. Adjust these settings according to your application's requirements.
