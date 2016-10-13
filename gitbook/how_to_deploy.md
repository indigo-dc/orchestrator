# HOW TO DEPLOY

## REQUISITES

To run the Orchestrator you need Docker and at least a MySQL Server instance (which may be local, remote, or in a docker container). 

You can either run the image built accordingly to the [previous chapter](how_to_build.md) or run the image already pushed on Docker Hub [indigodatacloud/orchestrator](https://hub.docker.com/r/indigodatacloud/orchestrator/).

## RUN THE CONTAINER

### With MySQL dockerized on the same host
The Orchestrator can be run in 3 steps:

1. Run the MySQL deployments database with the command:

    ```bash
    docker run --name databaseOrchestrator -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=orchestrator -d mysql:5.7
    ```

2. Run the MySQL workflow database with the command:

    ```bash
    docker run --name databaseWorkflow -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=workflow -d mysql:5.7
    ```

3. Run the Orchestrator with the command:

    ```bash
    docker run --name orchestrator --link databaseWorkflow:databaseWorkflow \
    --link databaseOrchestrator:databaseOrchestrator -p 80:8080 -d indigodatacloud/orchestrator
    ```

### With external databases

The Orchestrator can also be run using already deployed DBs; you just need to start it with the command

```bash
docker run --name orchestrator -e ORCHESTRATOR_DB_ENDPOINT=DOMAIN_NAME:PORT \
-e ORCHESTRATOR_DB_NAME=SCHEMA_NAME -e ORCHESTRATOR_DB_USER=DB_USER -e ORCHESTRATOR_DB_PWD=DB_USER_PASSWORD  \
-e WORKFLOW_DB_ENDPOINT=DOMAIN_NAME:PORT -e WORKFLOW_DB_NAME=SCHEMA_NAME -e WORKFLOW_DB_USER=DB_USER \
-e WORKFLOW_DB_PWD=DB_USER_PASSWORD -p 80:8080 -d indigodatacloud/orchestrator
```

replacing the parameters `DOMAIN_NAME`, `PORT`, `SCHEMA_NAME`, `DB_USER`, `DB_USER_PASSWORD` with the correct values.

### Required ports

The Orchestrator Docker image exposes the `8080 TCP` port; please remeber to publish it if you want to make your instance reachable from the outside.

## CONFIGURATION

This is the list of additional parameters that allows to configure the orchestrator behaviour.

|Parameter name|Description|Format|
|:--------:|:--------------------------------------------------------------------:|:---:|
|`ORCHESTRATOR_URL`|Self reference to the orchestrator REST interface|http://`host`:`port`/orchestrator|
|`IM_URL`|The Infrastructure Manager REST endpoint|http://`host`:`port`|
| `CMDB_ENDPOINT`|The CMDB REST endpoint|http://`host`:`port`/cmdb/|
|`SLAM_ENDPOINT`|The SLAM REST endpoint|http://`host`:`port`/slam/|
|`CPR_ENDPOINT`|The Cloud Provider Ranker endpoint|https://`host`:`port`/cpr/rank|
|`WRAPPER_URL`|The Zabbix Wrapper endpoint| http://`host`:`port`/monitoring/adapters/zabbix/<br />zones/indigo/types/service/groups/Cloud_Providers/hosts/|

### Configure security (optional)
By default the REST APIs are not authenticated; if you want to enable the `IAM` integration you must configure the following parameters:

|Parameter name|Description|Format|
|:--------:|:--------------------------------------------------------------------:|:---:|
|`SECURITY_ENABLE`|Determines if the OAuth2 authentication and authorization is enabled|`true` or `false`|
|`OIDC_ISSUERS`|`iss` value of the IAM to which the orchestrator has been registered|https://`host`:`port`/|
|`OIDC_CLIENT_ID`|The OAuth2 client ID||
|`OIDC_CLIENT_SECRET`|The OAuth2 client secret||

Please make reference to the [IAM guide](https://indigo-dc.gitbooks.io/iam/content) to understand how to register the Orchestrator as protected resource server on `IAM` and retrieve the `iss` the `client id` and the `client secret`.

:warning: When registering the Orchestrator on `IAM`, make sure that the `openid` connect scope is selected.

:warning::warning: Even if the authentication is optional and disabled by default, you are highly encuraged to enable it, otherwise you will not be able to create deployments neither on OpenStack nor on OpenNebula.
 
### Configure Chronos (optional)
The orchestrator allows to run jobs on Chronos; to do that you need to configure the following parameters 

|Parameter name|Description|Format|
|:--------:|:--------------------------------------------------------------------:|:---:|
|`CHRONOS_ENDPOINT`|The Chronos REST endpoint|http://`host`:`port`|
|`CHRONOS_USERNAME`|The Chronos username||
|`CHRONOS_PASSWORD`|The Chronos password||
|`CHRONOS_PROVIDER`|The Cloud Provider that hosts the Mesos cluster on which it runs Chronos; it should be the same id that's returned from the CMDB service|`provider-RECAS-BARI`|
 
### Configure OneData (optional)
The Orchestrator, when the Chronos parameters are set, allows to exploit a [OneData](https://onedata.org/) service space. This enables the users to execute tasks on Chronos that use temporary files hosted on a shared OneData space.

To enable this functionality you need to configure the following parameters:

|Parameter name|Description|Format|
|:--------:|:--------------------------------------------------------------------:|:---:|
|`ONEZONE_DEFAULT_URL`|The default OneZone endpoint to which your OneData user is registered|http://`host`:`port`|
|`SERVICE_SPACE_TOKEN`|The OneData service space token; you can retrieve it from the OneZone user interface||
|`SERVICE_SPACE_NAME`|The name of the OneData space that you want to use as service space||
|`SERVICE_SPACE_PROVIDER`|The OneData service space provider that hosts the service space|http://`host`:`port`|
  

