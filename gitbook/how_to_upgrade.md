# HOW TO UPGRADE

The Orchestrator is released as Docker Container. In order to upgrade a deployed instance to a new version you have to:
 * if the instance is deployed on a orchestration solution (like Kubernetes), make reference on its guide on how to upgrade the instance to the new version
 * if the instance is deployed on a host with just a docker engine running:
  * Stop the old container:
  ```bash
  sudo docker stop orchestrator
  ```
  * Remove the old container:
  ```bash
  sudo docker rm orchestrator
  ```
  * Pull the new image version:
  ```bash
  sudo docker pull indigodatacloud/orchestrator:{TAG_OF_THE_VERSION}
  ```
  * Start the new version:
  ```bash
  sudo docker run --name orchestrator ...**parameters**... indigodatacloud/orchestrator:{TAG_OF_THE_VERSION}
  ```

---

## UPGRADE AND COMPATIBILITY NOTES
This section highlights important changes upgrading the PaaS Orchestrator. Each section covers the upgrade from the previous release. If you are skipping releases when upgrading, it is recommended to read the sections for all releases in between.

### UPGRADING TO v1.1.x
#### Upgrading to v1.1.0-FINAL
The `openid` scope needs to be added (if not already present) in the IAM protected resource server configuration.
### UPGRADING TO v1.2.x
#### Upgrading to v1.2.0-FINAL
No action required.
#### Upgrading to v1.2.1-FINAL
This release require a parameter change regarding the Zabbix wrapper endpoint; the environment variable must be changed from
```
http://${host}:${port}/monitoring/adapters/zabbix/zones/indigo/types/*service*/groups/Cloud_Providers/hosts/
```
to
```
http://${host}:${port}/monitoring/adapters/zabbix/zones/indigo/types/*infrastructure*/groups/Cloud_Providers/hosts/
```
#### Upgrading to v1.2.2-FINAL
No action required.
### UPGRADING TO v1.3.x
#### Upgrading to v1.3.0-FINAL
In this release the IAM integration has undergone a major rework, thus some changes in the configuration needs to be done:
 * The `profile` and `offline_access` scopes need to be added (if not already present) in the IAM protected resource server configuration.
  :warning: The access tokens used to authenticate API requests to the Orchestrator will need to have this scopes granted. Please check the IAM configuration of the clients calling the Orchestrator and refer to their guide in order to understand how to configure them with this new scopes.
 * The `urn:ietf:params:oauth:grant-type:token-exchange` grant type needs to be added in the IAM protected resource server configuration.
 * The `SECURITY_ENABLE` parameter has been renamed to `OIDC_ENABLED`
 * The `OIDC_ISSUERS` , `OIDC_CLIENT_ID` and `OIDC_CLIENT_SECRET` have been deprecated and combined into `OIDC_IAM-PROPERTIES[{issuer}]_ORCHESTRATOR_CLIENT-ID` and `OIDC_IAM-PROPERTIES[{issuer}]_ORCHESTRATOR_CLIENT-SECRET`.

  If your configuration was:
   * `OIDC_ISSUERS`: _**https://iam-test.indigo-datacloud.eu/**_
   * `OIDC_CLIENT_ID`: _**client_id**_
   * `OIDC_CLIENT_SECRET`: _**client_secret**_

  now it would be:
   * `OIDC_IAM-PROPERTIES[https://iam-test.indigo-datacloud.eu/]_ORCHESTRATOR_CLIENT-ID`: _**client_id**_
   * `OIDC_IAM-PROPERTIES[https://iam-test.indigo-datacloud.eu/]_ORCHESTRATOR_CLIENT-SECRET`: _**client_secret**_
### UPGRADING TO v1.4.x
#### Upgrading to v1.4.0-FINAL
No action required.
### UPGRADING TO v1.5.x
#### Upgrading to v1.5.0-FINAL
The way the service must be configured has been completely revised, making it more coherent and expressive. Please refer to the [deployment guide](./how_to_deploy.md) to properly update the service configuration
#### Upgrading to v1.5.1-FINAL
No action required.
### UPGRADING TO v2.0.x
#### Upgrading to v2.0.0-FINAL
:warning: Due to internal changes in how the data saved on DB is handled, an upgrade path that allows to preserve saved data is **NOT** available. **Both DBs (orchestrator and workflows) need to be recreated**.

No change in configuration file is needed.
### UPGRADING TO v2.1.x
#### Upgrading to v2.1.0-FINAL
With this release 2 major configuration changes have been introduced:
 - It has been introduced the retrieval of the Mesos frameworks information from CMDB. The configuration through properties/YAML file has been therefore deprecated and removed.
 - There have also been some changes with the OneData integration:
   - The property `ONEDATA_SERVICE_SPACE_ONEPROVIDER_URL` have been removed; now the OneProvider endpoint for the Service Space Storage is automatically retrieved from OneZone
   - The property `ONEDATA_SERVICE_SPACE_ONEZONE_URL` have been introduced to optionally allow to use, for the Serivce Space, a OneZone different from the default one.

Additionally, a way to import self-signed certificates has been added. Please check to the [deployment guide](./how_to_deploy.md) to learn more about this feature.
#### Upgrading to v2.1.1-FINAL
No action required.
### UPGRADING TO v2.2.x
#### Upgrading to v2.2.0-FINAL
The following steps are necessary for the upgrade:

- delete all the tokens/refresh tokens stored in the orchestrator DB (database tables 'oidc\_entity' and 'oidc\_refresh\_token'), e.g. using commands like:

```
mysql>use orchestrator;

mysql>delete from oidc_entity;

mysql>delete from oidc_refresh_token;
```
- add the 'audience' property (mandatory) to iam-properties in `application.yml` (see [Configure IAM integration](how_to_deploy.md#configure-iam-integration-optional)); this can be a user-defined string. We recommend to generate a uuid.

### UPGRADING TO v2.3.x
#### Upgrading to v2.3.0-FINAL
The following steps are necessary for the upgrade:

- add the 'admingroup' property (mandatory) to iam-properties in `application.yml` (see [Configure IAM integration](how_to_deploy.md#configure-iam-integration-optional)).

Please note that, starting from this release, the Orchestrator can interact with providers not integrated with IAM getting the user's credentials from Vault. In order to exploit this new functionality you need to configure the integration with Vault as explained in the section [Configure Vault (optional)](how_to_deploy.md#configure-vault-optional) 
