#!/bin/sh
CLUSTER_MESSAGING_PASSWORD="pwd"
#IPADDR=$(ip a s | sed -ne '/127.0.0.1/!{s/^[ \t]*inet[ \t]*\([0-9.]\+\)\/.*$/\1/p}')
$JBOSS_HOME/bin/standalone.sh -c $JBOSS_CONF_FILE -Djboss.bind.address=$HOSTNAME -Djboss.bind.address.management=$HOSTNAME -Djgroups.bind_addr=$HOSTNAME -Djboss.node.name=$HOSTNAME -Djboss.messaging.cluster.password=$CLUSTER_MESSAGING_PASSWORD