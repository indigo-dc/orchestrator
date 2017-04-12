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
 * The `SECURITY_ENABLE` parameter has been renamed to `OIDC_ENABLED`
 * The `OIDC_ISSUERS` , `OIDC_CLIENT_ID` and `OIDC_CLIENT_SECRET` have been deprecated and combined into `OIDC_IAM-PROPERTIES[{issuer}]_ORCHESTRATOR_CLIENT-ID` and `OIDC_IAM-PROPERTIES[{issuer}]_ORCHESTRATOR_CLIENT-SECRET`.
  
  If your configuration was:
   * `OIDC_ISSUERS`: _**https://iam-test.indigo-datacloud.eu/**_
   * `OIDC_CLIENT_ID`: _**client_id**_
   * `OIDC_CLIENT_SECRET`: _**client_secret**_
  
  now it wolud be:
   * `OIDC_IAM-PROPERTIES[https://iam-test.indigo-datacloud.eu/]_ORCHESTRATOR_CLIENT-ID`: _**client_id**_
   * `OIDC_IAM-PROPERTIES[https://iam-test.indigo-datacloud.eu/]_ORCHESTRATOR_CLIENT-SECRET`: _**client_secret**_
