![INDIGO](https://pbs.twimg.com/media/Cldr8SHWYAA0JbY.png)

INDIGO PaaS Orchestrator
============================


[![GitHub license](https://img.shields.io/github/license/indigo-dc/orchestrator.svg?maxAge=2592000&style=flat-square)](https://github.com/indigo-dc/orchestrator/blob/master/LICENSE)
[![GitHub release](https://img.shields.io/github/release/indigo-dc/orchestrator.svg?maxAge=2592000&style=flat-square)](https://github.com/indigo-dc/orchestrator/releases/latest)

[![Jenkins](https://img.shields.io/jenkins/s/https/ci.cloud.reply.eu/job/INDIGO/orchestrator-unittest-master.svg?maxAge=2592000&style=flat-square)](https://ci.cloud.reply.eu/job/INDIGO/job/orchestrator-unittest-master/)
[![Jenkins tests](https://img.shields.io/jenkins/t/https/ci.cloud.reply.eu/job/INDIGO/orchestrator-unittest-master.svg?maxAge=2592000&style=flat-square)](https://ci.cloud.reply.eu/job/INDIGO/job/orchestrator-unittest-master/)
[![Jenkins coverage](https://img.shields.io/jenkins/c/https/ci.cloud.reply.eu/job/INDIGO/orchestrator-coverage-master.svg?maxAge=2592000&style=flat-square)](https://ci.cloud.reply.eu/job/INDIGO/job/orchestrator-coverage-master/)


The INDIGO PaaS Orchestrator is a component of the PaaS layer that allows to instantiate resources on Cloud Management Frameworks (like [OpenStack](https://www.openstack.org/) and [Opennebula](http://opennebula.org/)) and [Mesos](http://mesos.apache.org/) clusters.

It takes the deployment requests, expressed through templates written in [TOSCA YAML Simple Profile](http://docs.oasis-open.org/tosca/TOSCA-Simple-Profile-YAML/v1.0/TOSCA-Simple-Profile-YAML-v1.0.html), and deploys them on the best cloud site available. In order to do that
 1. it gathers SLAs, monitoring info and other data to other platform services,
 2. it asks to the cloud provider ranker for a list of the best cloud sites.

The exposed REST APIs are consumed by the Future Gateway portal.
Here you can find the [REST APIs documentation](http://indigo-dc.github.io/orchestrator/restdocs/) and the [Java doc](http://indigo-dc.github.io/orchestrator/apidocs/).

### DEPENDENCIES TO OTHER SERVICES

The Orchestrator needs the presence of the following INDIGO services:

 1. `SLAM`: [SLA Manager](https://indigo-dc.gitbooks.io/slam/content); allows to retrieve all the SLAs of the user
 2. `CMDB`: Configuration Manager DataBase; contains all the cloud sites information, like the identity endpoint, the OCCI endpoint, etc...
 3. `Zabbix Wrapper`: [REST wrapper for Zabbix](https://indigo-dc.gitbooks.io/monitoring/content#1-zabbix-wrapper); allows to retrieve monitoring metrics to zabbix through a REST interface
 4. `CPR` [Cloud Provider Ranker](https://www.gitbook.com/book/indigo-dc/cloud-provider-ranker/content); it receives all the information retrieved from the aforementioned services and provides the ordered list of the best sites
