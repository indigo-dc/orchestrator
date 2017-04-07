#!/bin/bash
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

CHRONOS_PROP_FILE="$JBOSS_HOME/standalone/deployments/$WAR_NAME.war/WEB-INF/classes/chronos/chronos.properties"
CMDB_PROP_FILE="$JBOSS_HOME/standalone/deployments/$WAR_NAME.war/WEB-INF/classes/cmdb/cmdb.properties"
SLAM_PROP_FILE="$JBOSS_HOME/standalone/deployments/$WAR_NAME.war/WEB-INF/classes/slam/slam.properties"
CPR_PROP_FILE="$JBOSS_HOME/standalone/deployments/$WAR_NAME.war/WEB-INF/classes/cloud-provider-ranker/cloud-provider-ranker.properties"

if [[ $CHRONOS_ENDPOINT ]];
	then sed -i "s/^\(chronos\.endpoint=\).*$/\1$(echo $CHRONOS_ENDPOINT | sed -e 's/[\/&]/\\&/g')/" ${CHRONOS_PROP_FILE};
fi;

if [[ $CHRONOS_USERNAME ]];
	then sed -i "s/^\(chronos\.username=\).*$/\1$(echo $CHRONOS_USERNAME | sed -e 's/[\/&]/\\&/g')/" ${CHRONOS_PROP_FILE};
fi;

if [[ $CHRONOS_PASSWORD ]];
	then sed -i "s/^\(chronos\.password=\).*$/\1$(echo $CHRONOS_PASSWORD | sed -e 's/[\/&]/\\&/g')/" ${CHRONOS_PROP_FILE};
fi;

if [[ $CHRONOS_PROVIDER ]];
	then sed -i "s/^\(chronos\.cloudProviderName=\).*$/\1$(echo $CHRONOS_PROVIDER | sed -e 's/[\/&]/\\&/g')/" ${CHRONOS_PROP_FILE};
fi;

if [[ $CMDB_ENDPOINT ]];
	then sed -i "s/^\(cmdb\.url=\).*$/\1$(echo $CMDB_ENDPOINT | sed -e 's/[\/&]/\\&/g')/" ${CMDB_PROP_FILE};
fi;

if [[ $SLAM_ENDPOINT ]];
	then sed -i "s/^\(slam\.url=\).*$/\1$(echo $SLAM_ENDPOINT | sed -e 's/[\/&]/\\&/g')/" ${SLAM_PROP_FILE};
fi;

if [[ $CPR_ENDPOINT ]];
	then sed -i "s/^\(cloud-provider-ranker\.url=\).*$/\1$(echo $CPR_ENDPOINT | sed -e 's/[\/&]/\\&/g')/" ${CPR_PROP_FILE};
fi;

################################

if [ "${ENABLE_DEBUG}" = "true" ];
	then DEBUG_ARG="--debug";
	else DEBUG_ARG="";
fi

CLUSTER_MESSAGING_PASSWORD="pwd"
exec "$@" -c $JBOSS_CONF_FILE -Djboss.bind.address=$HOSTNAME -Djboss.bind.address.management=$HOSTNAME \
	-Djgroups.bind_addr=$HOSTNAME -Djboss.node.name=$HOSTNAME -Djboss.messaging.cluster.password=$CLUSTER_MESSAGING_PASSWORD $DEBUG_ARG
