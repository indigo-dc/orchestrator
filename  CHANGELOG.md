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

## [v1.0.0 (Unreleased)] - ERD: 2016-07-18

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
- **[Feature]** **[PARTIALLY IMPLEMENTED]** Support for the Data Location Scheduling (OneData) (#55)

### Changed:
**NONE**

### Deprecated:
**NONE**

### Removed:
**NONE**

### Fixed:
- Cannot delete Chonos job if the TOSCA template has some errors (#71)
- TOSCA: required inputs with default value not handled correctly (#73)
- Provider choice override for Chronos single provider (#77)

### Security:
**NONE**



[v1.0.0 (Unreleased)]: ../../compare/0.0.5...HEAD