#
# Copyright © 2015-2017 Santer Reply S.p.A.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM jboss/wildfly:9.0.2.Final

MAINTAINER Alberto Brigandì <a.brigandi@reply.it>

USER root
RUN yum install -y libaio && yum clean all
USER jboss

ENV MYSQL_CONNECTOR_VERSION=5.1.38 \
	JBOSS_CONF_FILE=standalone-full-ha.xml
COPY add_mysql_driver_module.sh add_json_log_formatter_module.sh otherConfigs.xsl datasourcesConfig.xsl launch.sh /

RUN /add_mysql_driver_module.sh \
	&& /add_json_log_formatter_module.sh \
	&& java -jar /usr/share/java/saxon.jar -o:$JBOSS_HOME/standalone/configuration/$JBOSS_CONF_FILE \
		-s:$JBOSS_HOME/standalone/configuration/$JBOSS_CONF_FILE -xsl:/otherConfigs.xsl

EXPOSE  3528 \
		3529 \
		4712 \
		4713 \
		7600 \
		8009 \
		8080 \
		8443 \
		8787 \
		9990 \
		9993 \
		23364 \
		45688 \
		45700 \
		54200 \
		55200 \
		57600

ENV WAR_NAME orchestrator

COPY $WAR_NAME.war $JBOSS_HOME/standalone/deployments/$WAR_NAME.war.zip
RUN unzip $JBOSS_HOME/standalone/deployments/$WAR_NAME.war.zip -d $JBOSS_HOME/standalone/deployments/$WAR_NAME.war \
	&& touch $JBOSS_HOME/standalone/deployments/$WAR_NAME.war.dodeploy \
	&& rm $JBOSS_HOME/standalone/deployments/$WAR_NAME.war.zip

ENV ORCHESTRATOR_DB_ENDPOINT="databaseorchestrator:3306" \
	ORCHESTRATOR_DB_NAME=orchestrator \
	ORCHESTRATOR_DB_USER=root \
	ORCHESTRATOR_DB_PWD=root \
	WORKFLOW_DB_ENDPOINT="databaseworkflow:3306" \
	WORKFLOW_DB_NAME=workflow \
	WORKFLOW_DB_USER=root \
	WORKFLOW_DB_PWD=root

ENTRYPOINT ["/launch.sh"]

CMD ["/opt/jboss/wildfly/bin/standalone.sh"]
