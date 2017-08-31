#!/bin/sh
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

set -o errexit
set -o pipefail
set -o nounset

export DATASOURCE_ORCHESTRATOR_URL="jdbc:mysql://${ORCHESTRATOR_DB_ENDPOINT}/${ORCHESTRATOR_DB_NAME}?useSSL=false"
export DATASOURCE_ORCHESTRATOR_USERNAME="${ORCHESTRATOR_DB_USER}"
export DATASOURCE_ORCHESTRATOR_PASSWORD="${ORCHESTRATOR_DB_PWD}"

export DATASOURCE_WORKFLOW_URL="jdbc:mysql://${WORKFLOW_DB_ENDPOINT}/${WORKFLOW_DB_NAME}?useSSL=false"
export DATASOURCE_WORKFLOW_USERNAME="${WORKFLOW_DB_USER}"
export DATASOURCE_WORKFLOW_PASSWORD="${WORKFLOW_DB_PWD}"

if [ "${ENABLE_DEBUG:-false}" = "true" ];
	then JAVA_OPTS="${JAVA_OPTS} -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=8787,suspend=n";
fi

wait_for() {
  HOST="${1%:*}"
  PORT="${1#*:}"
  echo Waiting for "${1}" to be ready...
  while ! echo -n | nc -w1 "${HOST}" "${PORT}" > /dev/null 2>&1; do echo Still waiting for "${1}"...; sleep 2; done
  echo "${1}" is ready!
}
wait_for "${ORCHESTRATOR_DB_ENDPOINT}"
wait_for "${WORKFLOW_DB_ENDPOINT}"

exec "${@}" ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar "${ARTIFACT_NAME}" --spring.config.name=application,security
