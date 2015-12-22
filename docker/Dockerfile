FROM  jboss/wildfly:9.0.2.Final

MAINTAINER a.brigandi@reply.it

COPY /modules/ $JBOSS_HOME/modules/

ENV JBOSS_CONF_FILE standalone-full-ha.xml
ENV JBOSS_CONF_FILEPATH $JBOSS_HOME/standalone/configuration/$JBOSS_CONF_FILE

COPY /datasourcesConfig.xsl /datasourcesConfig.xsl

ARG ORCHESTRATOR_DB_DN="databaseorchestrator"
ARG ORCHESTRATOR_DB_NAME="orchestrator"
ARG ORCHESTRATOR_DB_USER="root"
ARG ORCHESTRATOR_DB_PWD="root"

RUN java -jar /usr/share/java/saxon.jar -o:$JBOSS_HOME/standalone/configuration/$JBOSS_CONF_FILE -s:$JBOSS_HOME/standalone/configuration/$JBOSS_CONF_FILE -xsl:/datasourcesConfig.xsl \
	orchestrator.DB.DN=$ORCHESTRATOR_DB_DN \
	orchestrator.DB.name=$ORCHESTRATOR_DB_NAME \
	orchestrator.DB.user=$ORCHESTRATOR_DB_USER \
	orchestrator.DB.pwd=$ORCHESTRATOR_DB_PWD

#RUN $JBOSS_HOME/bin/add-user.sh --silent=true admin admin123

ARG WAR_NAME=orchestrator

COPY /$WAR_NAME.war $JBOSS_HOME/standalone/deployments/$WAR_NAME.war.zip
RUN unzip $JBOSS_HOME/standalone/deployments/$WAR_NAME.war.zip -d $JBOSS_HOME/standalone/deployments/$WAR_NAME.war \
	&& touch $JBOSS_HOME/standalone/deployments/$WAR_NAME.war.dodeploy \
	&& rm $JBOSS_HOME/standalone/deployments/$WAR_NAME.war.zip

ADD launch.sh /
USER root
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
		9990 \
		9993 \
		23364 \
		45688 \
		45700 \
		54200 \
		55200 \
		57600

ENTRYPOINT sh /launch.sh

