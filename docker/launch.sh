#!/bin/sh

#IPADDR=$(ip a s | sed -ne '/127.0.0.1/!{s/^[ \t]*inet[ \t]*\([0-9.]\+\)\/.*$/\1/p}')

java -jar /usr/share/java/saxon.jar -o:$JBOSS_HOME/standalone/configuration/$JBOSS_CONF_FILE -s:$JBOSS_HOME/standalone/configuration/$JBOSS_CONF_FILE -xsl:/datasourcesConfig.xsl \
	orchestrator.DB.endpoint=$ORCHESTRATOR_DB_ENDPOINT \
	orchestrator.DB.name=$ORCHESTRATOR_DB_NAME \
	orchestrator.DB.user=$ORCHESTRATOR_DB_USER \
	orchestrator.DB.pwd=$ORCHESTRATOR_DB_PWD \
	workflow.DB.endpoint=$WORKFLOW_DB_ENDPOINT \
	workflow.DB.name=$WORKFLOW_DB_NAME \
	workflow.DB.user=$WORKFLOW_DB_USER \
	workflow.DB.pwd=$WORKFLOW_DB_PWD

CLUSTER_MESSAGING_PASSWORD="pwd"
$JBOSS_HOME/bin/standalone.sh -c $JBOSS_CONF_FILE -Djboss.bind.address=$HOSTNAME -Djboss.bind.address.management=$HOSTNAME \
	-Djgroups.bind_addr=$HOSTNAME -Djboss.node.name=$HOSTNAME -Djboss.messaging.cluster.password=$CLUSTER_MESSAGING_PASSWORD