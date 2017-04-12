![INDIGO](https://pbs.twimg.com/media/Cldr8SHWYAA0JbY.png)

INDIGO PaaS Orchestrator
============================

The INDIGO PaaS Orchestrator is a component of the PaaS layer that allows to instantiate resources on Cloud Management Frameworks (like [OpenStack](https://www.openstack.org/) and [OpenNebula](http://opennebula.org/)) and [Mesos](http://mesos.apache.org/) clusters.

It takes the deployment requests, expressed through templates written in [TOSCA YAML Simple Profile v1.0](http://docs.oasis-open.org/tosca/TOSCA-Simple-Profile-YAML/v1.0/TOSCA-Simple-Profile-YAML-v1.0.html), and deploys them on the best cloud site available. In order to do that
 1. it gathers SLAs, monitoring info and other data from other platform services,
 2. it asks to the cloud provider ranker for a list of the best cloud sites.

The exposed REST APIs are consumed by the Future Gateway portal.
Here you can find the [REST APIs documentation](http://indigo-dc.github.io/orchestrator/restdocs/) and the [Java doc](http://indigo-dc.github.io/orchestrator/apidocs/).

### DEPENDENCIES TO OTHER SERVICES

The PaaS Orchestrator needs the presence of the following INDIGO services:

 * [**SLAM** (SLA Manager)](https://indigo-dc.gitbooks.io/slam/content): allows to retrieve all the SLAs of the user
 * [**CMDB** (Configuration Manager DataBase)](https://indigo-dc.gitbooks.io/cmdb/content): contains all the cloud sites information, like the identity endpoint, the OCCI endpoint, etc...
 * [**Zabbix Wrapper** (REST wrapper for Zabbix)](https://indigo-dc.gitbooks.io/monitoring/content#1-zabbix-wrapper): allows to retrieve monitoring metrics to zabbix through a REST interface
 * [**CPR** (Cloud Provider Ranker)](https://www.gitbook.com/book/indigo-dc/cloud-provider-ranker/content): it receives all the information retrieved from the aforementioned services and provides the ordered list of the best sites
