#!/bin/bash
set -e

EXTRACTION_PATH=$JBOSS_HOME/modules/system/layers/base/biz/paluch/logging/main
mkdir -p $EXTRACTION_PATH
cd $EXTRACTION_PATH

MODULE_VERSION=1.11.0
MODULE_BASE_NAME=logstash-gelf-$MODULE_VERSION
MODULE_NAME=$MODULE_BASE_NAME-logging-module

echo "Downloading $MODULE_BASE_NAME logging module"
curl -L -# -O https://search.maven.org/remotecontent?filepath=biz/paluch/logging/logstash-gelf/$MODULE_VERSION/$MODULE_NAME.zip
unzip -qq -j $MODULE_NAME.zip
rm $MODULE_NAME.zip
