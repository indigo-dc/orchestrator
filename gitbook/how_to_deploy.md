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
docker run --name orchestrator -e ORCHESTRATOR_DB_ENDPOINT=DOMAIN_NAME:PORT -e ORCHESTRATOR_DB_NAME=SCHEMA_NAME \
	-e ORCHESTRATOR_DB_USER=DB_USER -e ORCHESTRATOR_DB_PWD=DB_USER_PASSWORD  \
	-e WORKFLOW_DB_ENDPOINT=DOMAIN_NAME:PORT -e WORKFLOW_DB_NAME=SCHEMA_NAME -e WORKFLOW_DB_USER=DB_USER \
	-e WORKFLOW_DB_PWD=DB_USER_PASSWORD -p 80:8080 -d indigodatacloud/orchestrator
```

replacing the parameters `DOMAIN_NAME`, `PORT`, `SCHEMA_NAME`, `DB_USER`, `DB_USER_PASSWORD` with the correct values.

### Required ports and network reachability

The Orchestrator Docker image exposes the `8080 TCP` port; please remember to
 1. Publish the exposed port to one of the host
 2. Allow inbound connections to the published port of the host from all the IP addresses

## CONFIGURATION

This is the list of additional parameters that allows to configure the orchestrator behavior.

 * `ORCHESTRATOR_URL`
    * **Description**: Self reference to the orchestrator REST interface
    * **Format**: http://{host}:{port}
    * **Default value**: http://localhost:8080
 * `IM_URL`
    * **Description**: The Infrastructure Manager REST endpoint
    * **Format**: http://{host}:{port}
    * **Default value**: https://servproject.i3m.upv.es:8811
 * `CMDB_URL`
    * **Description**: The CMDB REST endpoint
    * **Format**: http://{host}:{port}/cmdb
    * **Default value**: http://indigo.cloud.plgrid.pl/cmdb
 * `SLAM_URL`
    * **Description**: The SLAM REST endpoint
    * **Format**: http://{host}:{port}/slam
    * **Default value**: http://indigo.cloud.plgrid.pl/slam
 * `CPR_URL`
    * **Description**: The Cloud Provider Ranker endpoint
    * **Format**: https://{host}:{port}
    * **Default value**: https://indigo-paas.cloud.ba.infn.it/cpr
 * `MONITORING_URL`
    * **Description**: The Zabbix Wrapper endpoint
    * **Format**: http://{host}:{port}
    * **Default value**: http://90.147.170.181

### Configure IAM integration (optional)
By default the REST APIs are not authenticated; if you want to enable the IAM integration you must (for each IAM you want to associate):

 1. Register the Orchestrator on IAM as **protected resource server** with
     1. `HTTP Basic` as Token Endpoint Authentication Method
     2. `openid`, `profile` and `offline_access` as scopes
     3. `urn:ietf:params:oauth:grant-type:token-exchange` as additional grant type
     4. Expiration time for the authorization token must be set to 3600 seconds
     5. Expiration time for the id token must be set to 1800 seconds
     6. Expiration info for the exchanged token must not be disabled
 2. Retrieve the _**client id**_ and the _**client secret**_
 3. Retrieve the _**issuer**_ value of the IAM from its WebFinger endpoint `https://{iam-url}/.well-known/openid-configuration`
 4. Provide a file called `application.yml` and mount it (via docker bind-mounting) on `/orchestrator/application.yml`.
 Inside the file you need to provide the following configuration:

     ```yaml
      oidc:
       enabled: true
       iam-properties:
         "[{issuer}]":
           orchestrator:
             client-id: '{client-id}'
             client-secret: '{client-secret}'
           clues:
             client-id: '{client-id}'
             client-secret: '{client-secret}'
     ```
    with, as parameters
    * `oidc.enabled`
       * **Description**: Determines if the OAuth2 authentication and authorization is enabled
       * **Format**: `true` or `false`
       * **Default value**: `false`
    * `{issuer}`
       * **Description**: The issuer value of the IAM to which the orchestrator has been registered
       * **Default value**: `https://iam-test.indigo-datacloud.eu/`	
    * `orchestrator.client-id`
       * **Description**: The Orchestrator OAuth2 client ID	
    * `orchestrator.client-secret`
       * **Description**: The Orchestrator OAuth2 client secret
 5. Additionally, if you have a Clues client registered in the IAM, you can configure the following parameters:
    * `clues.client-id`
       * **Description**: The CLUES OAuth2 client ID
    * `clues.client-secret`
       * **Description**: The CLUES OAuth2 client secret

Please make reference to the [IAM guide](https://indigo-dc.gitbooks.io/iam/content) for further information on how to register the Orchestrator as protected resource server.

:warning: Even if the authentication is optional and disabled by default, you are highly encouraged to enable it, otherwise you will not be able to create deployments neither on OpenStack nor on OpenNebula.

### Configure OneData (optional)
The Orchestrator, when the Chronos parameters are set, allows to exploit a [OneData](https://onedata.org/) service space. This enables the users to execute tasks on Chronos that use temporary files hosted on a shared OneData space.

To enable this functionality you need to configure the following parameters:

 * `ONEDATA_ONEZONE_URL`
    * **Description**: The endpoint of the default OneZone to use
    * **Format**: http://{host}:{port}
    * **Default value**: https://onezone-beta.cloud.ba.infn.it
 * `ONEDATA_SERVICE_SPACE_ONEZONE_URL`
    * **Description**: (Optional) The OneZone to use when dealing with OneData service space
    * **Format**: {host}:{port}
    * **Default value**: The OneZone endpoint defined in `$ONEDATA_ONEZONE_URL`
 * `ONEDATA_SERVICE_SPACE_TOKEN`
    * **Description**: The OneData service space token; you can retrieve it from the OneZone user interface
 * `ONEDATA_SERVICE_SPACE_NAME`
    * **Description**: The name of the OneData space that you want to use as service space
    * **Default value**: `INDIGO Service Space`
 * `ONEDATA_SERVICE_SPACE_BASE_FOLDER_PATH`
    * **Description**: The path (relative to the space one) to the folder that will host the files
    * **Default value**: `/`
