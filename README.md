[![GitHub license](https://img.shields.io/github/license/indigo-dc/orchestrator.svg?style=flat-square)](https://github.com/indigo-dc/orchestrator/blob/master/LICENSE)
[![GitHub release](https://img.shields.io/github/release/indigo-dc/orchestrator.svg?style=flat-square)](https://github.com/indigo-dc/orchestrator/releases/latest)

INDIGO Orchestrator
============================

This is the orchestrator of the PaaS layer, a core component of the INDIGO project. It receives high-level deployment requests and coordinates the deployment process over the IaaS platforms or Mesos.

You can find the REST APIs at [INDIGO OpenProject] (https://project.indigo-datacloud.eu/projects/wp5/wiki/Orchestrator_REST_APIs).


1. INSTALLATION
===============

1.1 REQUISITES
--------------

This project has been created with maven 3.3.3 and Java 1.8. Maven will take care of downloading the extra dependencies needed for the project but this project dependes on [im-java-api](https://github.com/indigo-dc/im-java-api) and [workflow-manager](https://github.com/ConceptReplyIT/workflow-manager) too.
To run the Orchestrator you need [Docker](https://www.docker.com) and a MySQL Server instance (which may be local, remote, or in a docker container). See next section to have details.

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

### Build the Docker image

You can build the docker image with the command
```
docker build -t indigodatacloud/orchestrator /path/to/the/docker/folder
```

1.3 RUNNING
--------------
### With MySQL dockerized on the same host
The orchestrator can be run in 4 steps:

1. Create a docker bridge network (called `orchestrator_net`) with the command

    ```
    sudo docker network create --driver bridge orchestrator_net
    ```

2. Run the MySQL deployments database with the command

    ```
    sudo docker run --net orchestrator_net --name databaseOrchestrator -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=orchestrator -d mysql
    ```

3. Run the MySQL workflow database with the command

    ```
    sudo docker run --net orchestrator_net --name databaseWorkflow -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=workflow -d mysql
    ```

4. Run the orchestrator with the command

    ```
    sudo docker run --net orchestrator_net --name orchestrator1 -h orchestrator1 -p 80:8080 -d indigodatacloud/orchestrator
    ```

Thanks to the first step, the orchestrator will be able to communicate with the MySQL instances using as domain name their container names (`databaseOrchestrator` and `databaseWorkflow`)

### With external databases

The orchestrator can also be run using already deployed DBs; you just need to start it with the command
```
sudo docker run --name orchestrator1 -h orchestrator1 -e ORCHESTRATOR_DB_ENDPOINT=DOMAIN_NAME:PORT \
  -e ORCHESTRATOR_DB_NAME=SCHEMA_NAME -e ORCHESTRATOR_DB_USER=DB_USER -e ORCHESTRATOR_DB_PWD=DB_USER_PASSWORD  \
  -e WORKFLOW_DB_ENDPOINT=DOMAIN_NAME:PORT -e WORKFLOW_DB_NAME=SCHEMA_NAME -e WORKFLOW_DB_USER=DB_USER \
  -e WORKFLOW_DB_PWD=DB_USER_PASSWORD -p 80:8080 -d indigodatacloud/orchestrator
```
using as parameters (`DOMAIN_NAME`, `PORT`, `SCHEMA_NAME`, `DB_USER`, `DB_USER_PASSWORD`) the correct values.
