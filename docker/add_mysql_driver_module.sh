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

mkdir -p $JBOSS_HOME/modules/system/layers/base/com/mysql/main/
cd $JBOSS_HOME/modules/system/layers/base/com/mysql/main

MYSQL_CONNECTOR_FILE_NAME=mysql-connector-java-${MYSQL_CONNECTOR_VERSION}.jar
echo "Downloading connector ${MYSQL_CONNECTOR_FILE_NAME}"
curl -# -O https://repo1.maven.org/maven2/mysql/mysql-connector-java/${MYSQL_CONNECTOR_VERSION}/${MYSQL_CONNECTOR_FILE_NAME}
MYSQL_CONNECTOR_FILE_SHA1_EXPECTED=`curl -s https://repo1.maven.org/maven2/mysql/mysql-connector-java/${MYSQL_CONNECTOR_VERSION}/${MYSQL_CONNECTOR_FILE_NAME}.sha1`
MYSQL_CONNECTOR_FILE_SHA1_CALCULATED=`sha1sum ${MYSQL_CONNECTOR_FILE_NAME} | cut -d " " -f 1`

if [[ "$MYSQL_CONNECTOR_FILE_SHA1_EXPECTED" != "$MYSQL_CONNECTOR_FILE_SHA1_CALCULATED" ]]; then
	echo "Driver checksum mismatch: expected ${MYSQL_CONNECTOR_FILE_SHA1_EXPECTED} but it was ${MYSQL_CONNECTOR_FILE_SHA1_CALCULATED}";
	exit 1;
fi

cat <<EOF > module.xml
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.3" name="com.mysql">
   <resources>
     <resource-root path="${MYSQL_CONNECTOR_FILE_NAME}"/>
   </resources>

   <dependencies>
      <module name="javax.api"/>
      <module name="javax.transaction.api"/>
    </dependencies>
</module>
EOF

