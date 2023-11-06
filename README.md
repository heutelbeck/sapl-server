# SAPL Server CE

This is a lightweight PDP server storing policies in a MariaDB and offering a simple WebUI for policy and PDP administration.

The server can be run locally via maven. 
Alternatively, a container image and configurations for deployment 
on Docker and/or Kubernetes is available.

## Local Execution

The Descriptions in this Passage are for Demo Purposes and require changes in the application.yaml. Details about SSL Certificats are provided in a latter Abschnitt.

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
kubectl apply -f https://raw.githubusercontent.com/heutelbeck/sapl-server/main/sapl-server-ce/kubernetes/Kubernetes-Deployment.yaml -n sapl-server-ce
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
wget https://raw.githubusercontent.com/heutelbeck/sapl-server/main/sapl-server-ce/kubernetes/clusterissuer.yml
kubectl apply -f clusterissuer.yml 
```

Apply the Persistent Volume yaml (or create persistent volumes with the storageclassnames detailed in the yaml file according to your preferred Method)

```shell
kubectl apply -f https://raw.githubusercontent.com/heutelbeck/sapl-server/main/sapl-server-ce/kubernetes/sapl-server-ce-pv.yml -n sapl-server-ce
```

Download the Config Files from the Kubernetes/config folder and copy them to the config directory specified in the config-section of sapl-server-ce-pv.yml `/data/sapl-server-ce/conf`

```shell
wget https://raw.githubusercontent.com/heutelbeck/sapl-server/main/sapl-server-ce/kubernetes/sapl-server-ce-TLS.tar
tar -xf sapl-server-ce-TLS.tar -C /data/sapl-server-ce/conf
```

Then download the TLS yaml file 

```shell
wget https://raw.githubusercontent.com/heutelbeck/sapl-server/main/sapl-server-ce/kubernetes/sapl-server-ce-tls.yml
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
kubectl apply -f ssapl-server-ce-ingress-sample.yml -n sapl-server-ce
```
