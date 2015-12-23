[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

INDIGO Orchestrator
============================

This is the orchestrator of the PaaS layer, a core component of the INDIGO project. It receives high-level deployment requests and coordinates the deployment process over the IaaS platforms or Mesos.

You can find the REST APIs at [INDIGO OpenProject] (https://project.indigo-datacloud.eu/projects/wp5/wiki/Orchestrator_REST_APIs).


1. INSTALLATION
===============

1.1 REQUISITES
--------------

- java 8 sdk;
- https://github.com/indigo-dc/im-java-api
- maven
- docker
- a MySQL database (in chapter 1.3 you can find instructions of how to run one inside a docker container)

1.2 INSTALLING
--------------

First you have to customize:
- the IM endpoint in `/orchestrator/src/main/resources/im-config/im-java-api.properties`;
- the authorization file in `/orchestrator/src/main/resources/im-config/auth.dat`.

### Compile code
To compile the project you need to be in the same folder as the `pom.xml` file and type:
```
mvn clean install -DskipTests
```
This command compiles the code and skip the tests. If you want to compile the code running the tests too you can use:
```
mvn clean install
```
Beware that, in order to run successfully the tests, you must have a running MySQL DB, and you need to customize the file `/orchestrator/src/test/resources/application-test.properties` accordingly.

At compilation completed, the `orchestrator.war` file will be put inside the `target` folder.

### Docker image build

The generated war must then be placed in the docker folder.

You can build the docker image with the command
```
docker build -t indigodatacloud/orchestrator /path/to/the/docker/folder
```

1.3 RUNNING
--------------
### With MySQL dockerized on the same host
The orchestrator can be run in 3 steps:
1. Create a docker bridge network (called `orchestrator_net`) with the command
    ```
    sudo docker network create --driver bridge orchestrator_net
    ```
2. Run the MySQL database with the command
    ```
    sudo docker run --net orchestrator_net --name databaseOrchestrator -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=orchestrator -d mysql
    ```
3. Run the orchestrator with the command
    ```
    sudo docker run --net orchestrator_net --name orchestrator1 -h orchestrator1 -p 80:8080 -d indigodatacloud/orchestrator
    ```
Thanks to the first step, the orchestrator will be able to communicate with the MySQL instance using as domain name its container name (`databaseOrchestrator`)

### With an external DB

The orchestrator can also be run using a pre-existing DB; you just need to start it with the command
```
sudo docker run --name orchestrator1 -h orchestrator1 -e ORCHESTRATOR_DB_ENDPOINT=DOMAIN_NAME:PORT \
  -e ORCHESTRATOR_DB_NAME=SCHEMA_NAME -e ORCHESTRATOR_DB_USER=DB_USER -e ORCHESTRATOR_DB_PWD=DB_USER_PASSWORD  \
  -p 80:8080 -d indigodatacloud/orchestrator
```
using as parameters (`DOMAIN_NAME`, `PORT`, `SCHEMA_NAME`, `DB_USER`, `DB_USER_PASSWORD`) the correct values.
