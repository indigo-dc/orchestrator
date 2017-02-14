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
