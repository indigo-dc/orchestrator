FROM jboss/wildfly:9.0.2.Final

MAINTAINER Alberto Brigandì <a.brigandi@reply.it>

COPY /modules/ $JBOSS_HOME/modules/

USER root
RUN yum install -y libaio && yum clean all

ENV JBOSS_CONF_FILE standalone-full-ha.xml
ENV JBOSS_CONF_FILEPATH $JBOSS_HOME/standalone/configuration/$JBOSS_CONF_FILE

COPY /otherConfigs.xsl /otherConfigs.xsl
COPY /datasourcesConfig.xsl /datasourcesConfig.xsl
COPY /launch.sh /launch.sh

RUN java -jar /usr/share/java/saxon.jar -o:$JBOSS_HOME/standalone/configuration/$JBOSS_CONF_FILE -s:$JBOSS_HOME/standalone/configuration/$JBOSS_CONF_FILE -xsl:/otherConfigs.xsl
# ADD (and so COPY too) does not honor USER: files always owned by root
# https://github.com/docker/docker/issues/6119
RUN chmod 777 /datasourcesConfig.xsl
RUN chmod 777 /launch.sh
USER jboss

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

#RUN $JBOSS_HOME/bin/add-user.sh --silent=true admin admin123

ENV WAR_NAME orchestrator

COPY /$WAR_NAME.war $JBOSS_HOME/standalone/deployments/$WAR_NAME.war.zip
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

ENV IM_URL="" \
	PROXY_DIR="" \
	OPENNEBULA_AUTH_FILE_PATH="" \
	OPENSTACK_AUTH_FILE_PATH="" \
	ONEDOCK_AUTH_FILE_PATH=""

ENTRYPOINT sh /launch.sh


