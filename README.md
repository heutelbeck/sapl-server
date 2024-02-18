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

SAPL adheres to the secure by default principle. SAPL Server CE comes with a pre-configured basic setup that includes the essential security controls to ensure compliance with Germany's Federal Office for Information Security (BSI) [BSI Baseline Protection Compendium Edition 2022](https://www.bsi.bund.de/SharedDocs/Downloads/EN/BSI/Grundschutz/International/bsi_it_gs_comp_2022.pdf).

For SAPL Server CE, the binary software packages (OCI Container Images and Java applications delivered as a JAR) come with a strict configuration. Additional configuration is required for authentication and TLS to run the server. This documentation explains these configuration steps in detail.

Application security is a top priority in the development of SAPL. The SAPL project also embraces radical transparency and accountability. Security issues are reported and shared using the GitHub advisory feature. For more details, see the [SAPL Security Policy](https://github.com/heutelbeck/sapl-policy-engine/blob/master/SECURITY.md).

## System requirements

Requirements for local installation.

- Java Development Kit 17 or a newer version
- Operating system that is compatible with java
- Maven 3.9 or a newer version

## Prerequisites and download

SAPL Server CE comes in two forms: an executable Java JAR file for OpenJDK 17 (or later) and an OCI container. The server's full source code is also available for building, running from source, and auditing.

### Java OpenJDK

#### Prerequisites

Before running SAPL Server CE on your system, make sure that you have installed [OpenJDK 17](https://openjdk.org/projects/jdk/17/) or a newer version. [Eclipse Temurin](https://adoptium.net/de/temurin/releases/) is one of the available distributions that provides binaries for different platforms.

Ensure that the Java executables are added to the system path.

#### Download

To be done: setup download of release and snapshot via GitHub packages.

### Running from Source

#### Prerequisites

To build SAPL Server CE from source, first ensure that [OpenJDK 17](https://openjdk.org/projects/jdk/17/) or newer is installed. There are several distributions available, such as [Eclipse Temurin](https://adoptium.net/de/temurin/releases/) supplying binaries for different platforms.

SAPL uses Apache Maven as its build system, which must be installed and can be downloaded from the [Apache Maven Download Page](https://maven.apache.org/download.cgi).

Ensure the Java and Maven executables are on the system path.

#### Download

The source of the policy engine is found on the public [GitHub](https://github.com/) repository: <https://github.com/heutelbeck/sapl-server>.

You can either download the source as a ZIP file and unzip the archive, or clone the repository using git:

```
git clone https://github.com/heutelbeck/sapl-server.git
```

#### Build the Engine and Server locally

To build the server application go to the `sapl-server-ce` folder and execute the following command:

```
mvn install
```

After a few minutes the complete engine and server should be built. There are two options to run the server after the build concluded.

## Configuration

A basic configuration is required to operate the SAPL Server CE securely. This includes configuring [client application authentication](#managing-client-authentications) and [TLS](#tls-configuration). SAPL Server CE is implemented using [Spring Boot](https://spring.io/projects/spring-boot/), which offers flexible tools for application configuration. The Spring Boot documentation for [Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config) provides helpful guidelines to follow. It is important to note the order in which configurations are loaded and can overwrite each other.

In summary, the application's configuration is controlled by key-value pairs known as properties. These properties are provided to the application through `.properties` files, `.yml` files, or environment variables. The method of providing these values depends on the specific target environment.

To start the SAPL Server LT for development, there is a minimal basic configuration `application.yml` file in the folder `sapl-server-ce/config` within the folder from where the server is started.

**Note:** This example configuration is not intended for production. It contains secrets and certificates which are publicly known. Whenever you run a SAPL Server CE with this configuration **you** **accept the resulting risks** making the API publicly accessible via the provided credentials and that the server and its decisions cannot be properly authenticated by client applications because of the use of a publicly known self-signed TLS certificate.

To create a configuration for productive use, it is strongly recommended to create an individual configuration using the setup wizard.

### Using Setup-Wizard

#### Prerequisites

##### Certificate

Generate a Certificate using Certbot (Let's Encrypt client). Prerequisites: Server with Internetaccess, DNS Entry with Domain pointing to the IP addess of the Server, installed openssl (Example using Ubuntu 20)

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

When using a MariaDB, the url, user name and password must be known for successful configuration in the setup wizard.



!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
#################### alt
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

### Deploy via Docker Image 

This Docker Image uses a mounted directory to store access the configuration files as well as the Database if it is used as a standalone. In this Example a Windows directory 'C:\devkit\data\docker-sapl'. Copy the application.yml from sapl-server-ce\target\classes to the mounted directory. 

```shell
docker run "-p80:8080" "-v/c/devkit/data/docker-sapl:/sapl:rw" ghcr.io/heutelbeck/sapl-server-ce:3.0.0-snapshot
```

Open a browser and visit http://localhost/ . The username and password are both demo, but can be changed in the application.yml. Use https://bcrypt-generator.com/ to create a bcrypt encoded password. 

### Running the Server from Source

Clone the git Repository. Prerequisits: JDK 17 and Maven 3.9

open a commandline tool / Windows power shell. Change the directory to \path\to\repository\sapl-server-ce. Build and run the Project with

```shell
mvn
```

Open a browser and visit https://localhost:8443/ . The credentials are demo/demo and can be changed in \path\to\repository\sapl-server-ce\config\application.yml . The Database will be created in C:\users\username\sapl

### Certificate 

Generate a Certificate using Certbot (Let's Encrypt client). Prerequisites: Server with Internetaccess, DNS Entry with Domain pointing to the IP addess of the Server, installed openssl (Example using Ubuntu 20)  

Pull Certbot https://github.com/certbot/certbot

Generate a certificate for your domain (e.g. example.com)

```shell
./certbot-auto certonly -a standalone -d example.com -d www.example.com
cd /etc/letsencrypt/live/example.com
openssl pkcs12 -export -in fullchain.pem -inkey privkey.pem -out keystore.p12 -name tomcat -CAfile chain.pem -caname root
```
The file keystore.p12 must then be copied to the directory with the config files. Add the following entry to the application.yml or replace the previous Entry:

```shell
server:
  port: ${PORT:8443}
  ssl:
    enabled: true
    key-store-type: PKCS12
    key-store: file:config/keystore.p12
    key-store-password: <your-password>
    key-alias: tomcat
```
Then restart the application.

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
