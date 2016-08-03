![INDIGO](https://pbs.twimg.com/media/Cldr8SHWYAA0JbY.png)

INDIGO Orchestrator
============================


[![GitHub license](https://img.shields.io/github/license/indigo-dc/orchestrator.svg?maxAge=2592000&style=flat-square)](https://github.com/indigo-dc/orchestrator/blob/master/LICENSE)
[![GitHub release](https://img.shields.io/github/release/indigo-dc/orchestrator.svg?maxAge=2592000&style=flat-square)](https://github.com/indigo-dc/orchestrator/releases/latest)

[![Jenkins](https://img.shields.io/jenkins/s/https/ci.cloud.reply.eu/job/INDIGO/orchestrator-unittest-master.svg?maxAge=2592000&style=flat-square)](https://ci.cloud.reply.eu/job/INDIGO/job/orchestrator-unittest-master/)
[![Jenkins tests](https://img.shields.io/jenkins/t/https/ci.cloud.reply.eu/job/INDIGO/orchestrator-unittest-master.svg?maxAge=2592000&style=flat-square)](https://ci.cloud.reply.eu/job/INDIGO/job/orchestrator-unittest-master/)
[![Jenkins coverage](https://img.shields.io/jenkins/c/https/ci.cloud.reply.eu/job/INDIGO/orchestrator-coverage-master.svg?maxAge=2592000&style=flat-square)](https://ci.cloud.reply.eu/job/INDIGO/job/orchestrator-coverage-master/)


This is the Orchestrator of the PaaS layer, a core component of the INDIGO project. It receives high-level deployment requests and coordinates the deployment process over the CMFs and Mesos.

You can find the REST APIs docs [orchestrator-rest-doc] (http://indigo-dc.github.io/orchestrator/restdocs/).

### DEPENDENCIES TO OTHER SERVICES

The Orchestrator coordinates the deploy of the INDIGO applications. In order to do it, it needs the presence of the following INDIGO services:

 1. `SLAM`: [SLA Manager](https://github.com/indigo-dc/slam)
 2. `CMDB`: Configuration Manager DataBase
 3. `Zabbix Wrapper`: [REST wrapper for Zabbix](https://github.com/indigo-dc/Monitoring)
 4. `CPR` [Cloud Provider Ranker](https://github.com/indigo-dc/CloudProviderRanker)





# COMPILE

## REQUISITES


To build this project you will need the JDK 1.8 and maven 3.3. Thanks to maven all the dependencies will automatically downloaded.

## COMPILE THE CODE


Go to the same folder where the `pom.xml` file is and type:
```
mvn install
```
This command will download the dependencies, compile the code and create a war package, `orchestrator.war`, which will be put inside the `docker` folder.


## COMPILE THE DOCKER IMAGE


After the previous step, you can build the Docker image of the Orchestrator with the command:
```
docker build -t indigodatacloud/orchestrator docker/ 
```



# RUN

## REQUISITES

To run the Orchestrator you need [Docker](https://www.docker.com) and at least a MySQL Server instance (which may be local, remote, or in a docker container). 

The MySQL server needs the following customized settings (use `my.cnf` file to edit those settings):
- `max_allowed_packet = 256M`

## RUN THE CONTAINER

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

## CONFIGURATION

Besides those used to link the Orchestrator to the database, there are other environment variables that can be set in order to configure the Orchestrator behavior.

### Configure other platform services dependencies
 1. `IM_URL`: the Infrastructure Manager REST endpoint (e.g. http://{host}:{port})
 2. `CMDB_ENDPOINT`: the CMDB REST endpoint (e.g. http://{host}:{port}/cmdb/)
 3. `SLAM_ENDPOINT`: the SLAM REST endpoint (e.g. http://{host}:{port}/slam/)
 4. `CPR_ENDPOINT`: the Cloud Provider Ranker endpoint (e.g. https://{host}:{port}/cpr/rank)
 5. `WRAPPER_URL`: the Zabbix Wrapper endpoint (e.g. http://{host}:{port}/monitoring/adapters/zabbix/zones/indigo/types/service/groups/Cloud_Providers/hosts/)

### Configure security
 1. `SECURITY_ENABLE`: if set to `true` enable AAI OAuth2 authentication and authorization
 2. `OIDC_ISSUERS`: string containing a list of comma separated values of white-listed AAI token issuers (e.g. http://{host}:{port})
 3. `OIDC_CLIENT_ID`: the OAuth2 client ID
 4. `OIDC_CLIENT_SECRET` the OAuth2 client secret
 
### Configure Chronos
 1. `CHRONOS_ENDPOINT`: the Chronos REST endpoint (e.g. http://{host}:{port})
 2. `CHRONOS_USERNAME`: the Chronos username
 3. `CHRONOS_PASSWORD`: the Chronos password
 4. `CHRONOS_PROVIDER`: the Chronos Cloud Provider
 
### Configure OneData
 1. `ONEZONE_DEFAULT_URL`: the default OneZone endpoint (e.g. http://{host}:{port})
 2. `SERVICE_SPACE_TOKEN`: the OneData service space token
 3. `SERVICE_SPACE_NAME`: the OneData service space name
 4. `SERVICE_SPACE_PROVIDER`: the OneData service space provider (e.g. http://{host}:{port})
  
