[![GitHub license](https://img.shields.io/github/license/indigo-dc/orchestrator.svg?maxAge=2592000&style=flat-square)](https://github.com/indigo-dc/orchestrator/blob/master/LICENSE)
[![GitHub release](https://img.shields.io/github/release/indigo-dc/orchestrator.svg?maxAge=2592000&style=flat-square)](https://github.com/indigo-dc/orchestrator/releases/latest)
[![Jenkins](https://img.shields.io/jenkins/s/https/ci.cloud.reply.eu/job/INDIGO/orchestrator-unittest-master.svg?maxAge=2592000&style=flat-square)](https://ci.cloud.reply.eu/job/INDIGO/job/orchestrator-unittest-master/)
[![Jenkins tests](https://img.shields.io/jenkins/t/https/ci.cloud.reply.eu/job/INDIGO/orchestrator-unittest-master.svg?maxAge=2592000&style=flat-square)](https://ci.cloud.reply.eu/job/INDIGO/job/orchestrator-unittest-master/)
[![Jenkins coverage](https://img.shields.io/jenkins/c/https/ci.cloud.reply.eu/job/INDIGO/orchestrator-coverage-master.svg?maxAge=2592000&style=flat-square)](https://ci.cloud.reply.eu/job/INDIGO/job/orchestrator-coverage-master/)


INDIGO Orchestrator
============================

This is the Orchestrator of the PaaS layer, a core component of the INDIGO project. It receives high-level deployment requests and coordinates the deployment process over the IaaS platforms or Mesos.

You can find the REST APIs docs [orchestrator-rest-doc] (http://indigo-dc.github.io/orchestrator/restdocs/).


1. INSTALLATION
===============

1.1 REQUISITES
--------------

This project has been created with maven 3.3.3 and Java 1.8. Maven will take care of downloading the extra dependencies needed for the project but this project dependes on [im-java-api](https://github.com/indigo-dc/im-java-api) and [workflow-manager](https://github.com/ConceptReplyIT/workflow-manager) too.
To run the Orchestrator you need [Docker](https://www.docker.com) and a MySQL Server instance (which may be local, remote, or in a docker container). See next section to have details.

1.1.1 MySQL
-----------

The MySQL server needs the following customized settings (use `my.cnf` file to edit those settings):
- `max_allowed_packet = 256M`

1.2 INSTALLING
--------------

First you have to customize:
- the IM endpoint in `/orchestrator/src/main/resources/im-config/im-java-api.properties`;
- the authorization file in `/orchestrator/src/main/resources/im-config/auth.dat`.

### Compile the code
To compile the project you need to be in the same folder as the `pom.xml` file and type:
```
mvn clean install
```
This command compiles the code and creates a war package, `orchestrator.war`, which will be put inside the `docker` folder.
If you want run only unit-test type:
```
mvn test
```
otherwise if you want run integration test type (or use surefire.skip=true property to skip unit tests)
```
mvn integration-test
```
### Build the Docker image

You can build the docker image with the command
```
docker build -t indigodatacloud/orchestrator /path/to/the/docker/folder
```

1.3 RUNNING
--------------
### With MySQL dockerized on the same host
The Orchestrator can be run in 3 steps:

1. Run the MySQL deployments database with the command:

    ```
    sudo docker run --name databaseOrchestrator -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=orchestrator -d mysql:5.7
    ```

2. Run the MySQL workflow database with the command:

    ```
    sudo docker run --name databaseWorkflow -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=workflow -d mysql:5.7
    ```

3. Run the Orchestrator with the command:

	**`IMPORTANT`**: Remember to replace `ORCHESTRATOR_URL` with the base URL which the Orchestrator is available to (it MUST be accessible for the ElasticCluster callbacks!).

    ```
    sudo docker run --name orchestrator --link databaseWorkflow:databaseWorkflow --link databaseOrchestrator:databaseOrchestrator \
    -p 80:8080 -e ORCHESTRATOR_URL="<public_orchestrator_url, like http://localhost:80>" -d indigodatacloud/orchestrator
    ```

### With external databases

The Orchestrator can also be run using already deployed DBs; you just need to start it with the command
```
sudo docker run --name orchestrator1 -h orchestrator1 -e ORCHESTRATOR_DB_ENDPOINT=DOMAIN_NAME:PORT \
  -e ORCHESTRATOR_DB_NAME=SCHEMA_NAME -e ORCHESTRATOR_DB_USER=DB_USER -e ORCHESTRATOR_DB_PWD=DB_USER_PASSWORD  \
  -e WORKFLOW_DB_ENDPOINT=DOMAIN_NAME:PORT -e WORKFLOW_DB_NAME=SCHEMA_NAME -e WORKFLOW_DB_USER=DB_USER \
  -e WORKFLOW_DB_PWD=DB_USER_PASSWORD -p 80:8080 -d indigodatacloud/orchestrator
```
using as parameters (`DOMAIN_NAME`, `PORT`, `SCHEMA_NAME`, `DB_USER`, `DB_USER_PASSWORD`) the correct values.

1.4 CONFIGURING
--------------
Besides those used to link the Orchestrator to the database, there are other environment variables that can be set in order to configure the Orchestrator behaviour.

### Configure security
 1. `SECURITY_ENABLE`: if set to `true` enable AAI OAuth2 authentication and authorization
 2. `OIDC_ISSUERS`: String containing a list of comma separated values of whitelisted AAI token issuers
 3. `OIDC_CLIENT_ID`: The OAuth2 client ID
 4. `OIDC_CLIENT_SECRET` The OAuth2 client secret
 
### Configure Chronos
 1. `CHRONOS_AUTH_FILE_PATH`: the path to the property file containing credentials for Chronos and OneData service space for Chronos.
You can also edit the file `chronos/chronos.properties` directly in the deployment folder.
