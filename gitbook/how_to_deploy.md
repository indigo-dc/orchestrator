# HOW TO DEPLOY

## REQUISITES

To run the Orchestrator you need [Docker](https://www.docker.com) and at least a MySQL Server instance (which may be local, remote, or in a docker container). 

You can either run the image built accordingly to the [previous chapter](how_to_build.md) or run the image already pushed on docker hub [indigodatacloud/orchestrator](https://hub.docker.com/r/indigodatacloud/orchestrator/).

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

### Configure security (optional)
By default the REST APIs are not authenticated; if you want to enable the `IAM` integration you must configure the following parameters:

 1. `SECURITY_ENABLE`: if set to `true` enable AAI OAuth2 authentication and authorization
 2. `OIDC_ISSUERS`: string containing a list of comma separated values of white-listed AAI token issuers (e.g. http://{host}:{port})
 3. `OIDC_CLIENT_ID`: the OAuth2 client ID
 4. `OIDC_CLIENT_SECRET` the OAuth2 client secret

Please make reference to the [IAM guide](https://indigo-dc.gitbooks.io/iam/content) to understand how to register the Orchestrator as resource server on `IAM` and retrieve these parameters.

:warning: When registering the Orchestrator on `IAM`, make sure that the `openid` connect scope is selected.
 
### Configure Chronos (optional)
The orchestrator allows to deploy tasks on Chronos; to do that you need to configure the following parameters 
 1. `CHRONOS_ENDPOINT`: the Chronos REST endpoint (e.g. http://{host}:{port})
 2. `CHRONOS_USERNAME`: the Chronos username
 3. `CHRONOS_PASSWORD`: the Chronos password
 4. `CHRONOS_PROVIDER`: the Cloud Provider that hosts the Mesos cluster on which runs Chronos; it should be the same id that's returned from the CMDB service (e.g. `provider-RECAS-BARI`)
 
### Configure OneData (optional)
The Orchestrator, when the Chronos parameters are set, allows to exploit a [OneData](https://onedata.org/) service space. This enables the users to execute tasks on Chronos that use temporary files hosted on a shared OneData space. To enable this functionality you need to configure the following parameters:

 1. `ONEZONE_DEFAULT_URL`: the default OneZone endpoint (e.g. http://{host}:{port}) to which your OneData user is registered
 2. `SERVICE_SPACE_TOKEN`: the OneData service space token; you can retrieve it from the OneZone user interface
 3. `SERVICE_SPACE_NAME`: the name of the OneData space that you want to use as service space
 4. `SERVICE_SPACE_PROVIDER`: the OneData service space provider (e.g. http://{host}:{port}) that hosts your space
  

