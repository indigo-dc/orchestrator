![INDIGO](https://pbs.twimg.com/media/Cldr8SHWYAA0JbY.png)

INDIGO PaaS Orchestrator
============================


[![GitHub license](https://img.shields.io/github/license/indigo-dc/orchestrator.svg?maxAge=2592000&style=flat-square)](https://github.com/indigo-dc/orchestrator/blob/master/LICENSE)
[![GitHub release](https://img.shields.io/github/release/indigo-dc/orchestrator.svg?maxAge=3600&style=flat-square)](https://github.com/indigo-dc/orchestrator/releases/latest)

[![Travis](https://img.shields.io/travis/indigo-dc/orchestrator/master.svg?maxAge=3600&style=flat-square)](https://travis-ci.org/indigo-dc/orchestrator)
[![Codecov](https://img.shields.io/codecov/c/github/indigo-dc/orchestrator.svg?maxAge=3600&style=flat-square)](https://codecov.io/gh/indigo-dc/orchestrator)
[![SonarQube Tech Debt](https://img.shields.io/sonar/https/sonarcloud.io/it.reply%3Aorchestrator/tech_debt.svg?maxAge=3600&style=flat-square)](https://sonarcloud.io/dashboard?id=it.reply%3Aorchestrator)
[![Known Vulnerabilities](https://snyk.io/test/github/indigo-dc/orchestrator/badge.svg?style=flat-square)](https://snyk.io/test/github/indigo-dc/orchestrator)
[![Jenkins](https://jenkins.indigo-datacloud.eu/job/Pipeline-as-code/job/orchestrator/job/master/badge/icon?style=flat-square)](https://jenkins.indigo-datacloud.eu/job/Pipeline-as-code/job/orchestrator/job/master/)

The INDIGO PaaS Orchestrator is a component of the PaaS layer that allows to instantiate resources on Cloud Management Frameworks (like [OpenStack](https://www.openstack.org/) and [OpenNebula](http://opennebula.org/)), [Mesos](http://mesos.apache.org/) and [Kubernetes](https://kubernetes.io/) clusters.

It takes the deployment requests, expressed through templates written in [TOSCA YAML Simple Profile v1.0](http://docs.oasis-open.org/tosca/TOSCA-Simple-Profile-YAML/v1.0/TOSCA-Simple-Profile-YAML-v1.0.html), and deploys them on the best cloud site available. In order to do that
 1. it gathers SLAs, monitoring info and other data from other platform services,
 2. it asks to the cloud provider ranker for a list of the best cloud sites.

### DEPENDENCIES TO OTHER SERVICES

The PaaS Orchestrator needs the presence of the following INDIGO services:

 * [**SLAM** (SLA Manager)](https://indigo-dc.gitbooks.io/slam/content): allows to retrieve all the SLAs of the user
 * [**CMDB** (Configuration Manager DataBase)](https://indigo-dc.gitbooks.io/cmdb/content): contains all the cloud sites information, like the identity endpoint, the OCCI endpoint, etc...
 * [**Zabbix Wrapper** (REST wrapper for Zabbix)](https://indigo-dc.gitbooks.io/monitoring/content#1-zabbix-wrapper): allows to retrieve monitoring metrics to zabbix through a REST interface
 * [**CPR** (Cloud Provider Ranker)](https://www.gitbook.com/book/indigo-dc/cloud-provider-ranker/content): it receives all the information retrieved from the aforementioned services and provides the ordered list of the best sites

### GUIDES
* [How to build](gitbook/how_to_build.md)
* [How to deploy](gitbook/how_to_deploy.md)
* [How to upgrade](gitbook/how_to_upgrade.md)
* [REST APIs & Java Doc](http://indigo-dc.github.io/orchestrator/)
* [Service Reference Card](gitbook/service_reference_card.md)

### Acknowledgments:

This work has been co-funded by:
* [EOSC-hub project](http://eosc-hub.eu/) (Horizon 2020) under Grant number 777536. <img src="https://wiki.eosc-hub.eu/download/attachments/1867786/eu%20logo.jpeg?version=1&modificationDate=1459256840098&api=v2" height="24"> <img src="https://wiki.eosc-hub.eu/download/attachments/18973612/eosc-hub-web.png?version=1&modificationDate=1516099993132&api=v2" height="24">
* [DEEP-HybridDataCloud project](https://deep-hybrid-datacloud.eu/) (Horizon 2020) under Grant number 777435.
* [eXtreme-DataCloud project](http://www.extreme-datacloud.eu/) (Horizon 2020) under Grant number 777367.
* [INDIGO-DataCloud project](https://www.indigo-datacloud.eu/) (Horizon 2020) under Grant number 653549.
