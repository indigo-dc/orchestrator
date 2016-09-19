FROM jboss/wildfly:9.0.2.Final

MAINTAINER Alberto Brigandì <a.brigandi@reply.it>

USER root
RUN yum install -y libaio && yum clean all
USER jboss

ENV MYSQL_CONNECTOR_VERSION=5.1.38 \
	JBOSS_CONF_FILE=standalone-full-ha.xml
COPY add_mysql_driver_module.sh otherConfigs.xsl datasourcesConfig.xsl launch.sh /

RUN chmod +x /add_mysql_driver_module.sh \
	&& chmod +x /launch.sh \
	&& /add_mysql_driver_module.sh \
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
	ORCHESTRATOR_URL="" \
	WORKFLOW_DB_ENDPOINT="databaseworkflow:3306" \
	WORKFLOW_DB_NAME=workflow \
	WORKFLOW_DB_USER=root \
	WORKFLOW_DB_PWD=root \
	IM_URL="" \
	PROXY_DIR="" \
	OPENNEBULA_AUTH_FILE_PATH="" \
	OPENSTACK_AUTH_FILE_PATH="" \
	SECURITY_ENABLE="" \
	OIDC_CLIENT_ID="" \
	OIDC_CLIENT_SECRET=""

ENTRYPOINT sh /launch.sh


