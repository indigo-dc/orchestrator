# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

---

**Please view this file on the `master` branch; on stable branches it's out of date.**

## Legend:
- **[Feature]** indicates that an interface feature (a user level or an API level one) has changed. Changes not tagged with this indicate an internal code change, that should not affect the external system behavior and hence *should be ignored during Acceptance Testing*.
- **[Bug]** indicates a fixed bug.
- `#<number>` is a link to an issue, `!<number>` is a link to a merge request in the internal issue system.

---
## [v1.3.0] - 2016-04-10

### Added:
- IAM token exchange and refresh (#96)
- Selection of deployment sites through TOSCA SLA policies (#174)
- Support the deployment on AWS (#196)
- Send the Clues client credentials through TOSCA templates (#172)

### Changed:
**NONE**

### Deprecated:
**NONE**

### Removed:
**NONE**

### Fixed:
- Fix the deploy never timing out (#186)
- Fix TOSCA interfaces not sent to IaaS deployers (#181)

### Security:
**NONE**




## [v1.2.2] - 2016-03-27

### Added:
- SLAM REST endpoint authentication (#189)

### Changed:
**NONE**

### Deprecated:
**NONE**

### Removed:
**NONE**

### Fixed:
**NONE**

### Security:
**NONE**




## [v1.2.1] - 2016-12-23

### Added:
- Check access token expiration date and signature (#157)

### Changed:
- Update default monitoring endpoint (#158)
- Update custom INDIGO TOSCA types (#161) 
- Improve DB connection handling during shutdown (#163)

### Deprecated:
**NONE**

### Removed:
**NONE**

### Fixed:
**NONE**

### Security:
**NONE**



## [v1.2.0] - 2016-10-21

### Added:
- Support input substitution in listValue properties (#142)
- Support TOSCA extended node requirements definition in topology templates (#139)

### Changed:
**NONE**

### Deprecated:
**NONE**

### Removed:
- Removed deprecated occi proxy authentication (#125)

### Fixed:
**NONE**

### Security:
**NONE**



## [v1.1.0] - 2016-09-30

### Added:
- Support `iam_access_token` property in `tosca.nodes.indigo.ElasticCluster` nodes (#109)

### Changed:
- Adapt user info data to the new IAM format (#104)
- Make error reason message in REST response more clear (#119)
- Make the requirement of `openid` scope in auth token explicitly mandatory (#81)
- Sort in reverse-chronological order deployments and resources when retrieved from REST APIs (#101)

### Deprecated:
**NONE**

### Removed:
**NONE**

### Fixed:
- Support multiple SLAs for the same cloud provider (#110)

### Security:
**NONE**



## [v1.0.0] - 2016-08-03

### Added:
- **[Feature]** Image ID substitution in TOSCA template (to support multiple CP) (#53)
- **[Feature]** Use selected Cloud Provider for deploy/update/undeploy (#51)
- **[Feature]** Implement AAI support and IM authentication relay (#37)
- **[Feature]** Job Submission from Tosca to Chronos/Mesos (#34)
- **[Feature]** Ranking of the resources via Cloud Provider Ranker (#47)
- **[Feature]** Obtain the information about the monitoring of the IaaS resources (#46)
- **[Feature]** Support for Configuration Database for the IaaS Resources (#45)
- Enable TOSCA user's inputs substitution
- Cloud Provider choice (SLAM, CMDB, Monitoring, CPR integration)
- Retrieve Provider's Service's Image list from CMDB
- Jobs with Parameter Sweep (up to 10k jobs)
- Enable 'force_pull_image' flag in Chronos jobs by default (#70)
- Support Privileged mode for containers in Chronos jobs (#49)
- **[Feature]** Support for the Data Location Scheduling (OneData) (#55)

### Changed:
- Removed OneDock-specific authentication

### Deprecated:
**NONE**

### Removed:
**NONE**

### Fixed:
- Cannot delete Chronos job if the TOSCA template has some errors (#71)
- TOSCA: required inputs with default value not handled correctly (#73)
- Provider choice override for Chronos single provider (#77)
- Image ID substitution is not done during deployment Update (#86)
- Chronos properties are not handled properly (#84)

### Security:
**NONE**



[v1.0.0 (Unreleased)]: ../../compare/0.0.5...HEAD