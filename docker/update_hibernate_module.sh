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

cd $JBOSS_HOME/modules/system/layers/base/org/hibernate/main

HIBERNATE_OLD_VERSION=4.3.10.Final
HIBERNATE_NEW_VERSION=4.3.11.Final

for ARTIFACT_NAME in hibernate-core hibernate-entitymanager hibernate-envers
do 
  ARTIFACT_FILE_NAME=${ARTIFACT_NAME}-${HIBERNATE_NEW_VERSION}.jar
  echo "Downloading ${ARTIFACT_FILE_NAME}"
  curl -# -O https://search.maven.org/remotecontent?filepath=org/hibernate/${ARTIFACT_NAME}/${HIBERNATE_NEW_VERSION}/${ARTIFACT_FILE_NAME}
  ARTIFACT_SHA1_EXPECTED=`curl -s https://search.maven.org/remotecontent?filepath=org/hibernate/${ARTIFACT_NAME}/${HIBERNATE_NEW_VERSION}/${ARTIFACT_FILE_NAME}.sha1`
  ARTIFACT_SHA1_CALCULATED=`sha1sum ${ARTIFACT_FILE_NAME} | cut -d " " -f 1`
  if [[ "$ARTIFACT_SHA1_EXPECTED" != "$ARTIFACT_SHA1_CALCULATED" ]]; then
    echo "Artifact checksum mismatch: expected ${ARTIFACT_SHA1_EXPECTED} but it was ${ARTIFACT_SHA1_CALCULATED}";
    exit 1;
  fi
done

sed -i "s|${HIBERNATE_OLD_VERSION}|${HIBERNATE_NEW_VERSION}|" module.xml