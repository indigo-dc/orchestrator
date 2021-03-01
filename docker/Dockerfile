#
# Copyright © 2015-2021 I.N.F.N.
# Copyright © 2015-2020 Santer Reply S.p.A.
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

FROM openjdk:8-jdk-alpine

MAINTAINER Alberto Brigandì <a.brigandi@reply.com>

# Fix https://github.com/docker-library/openjdk/issues/73
RUN apk add --no-cache fontconfig ttf-dejavu

RUN apk add --no-cache bash

WORKDIR /orchestrator

COPY launch.sh launch.sh

ENV ARTIFACT_NAME="orchestrator.war"

ARG ARTIFACT_SRC="${ARTIFACT_NAME}"

ADD [ "${ARTIFACT_SRC}", "${ARTIFACT_NAME}" ]

RUN addgroup -S orchestrator \
	&& adduser -S -g orchestrator orchestrator \
	&& chown orchestrator:orchestrator /orchestrator

USER orchestrator

ENV JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=1 -XshowSettings:vm" \
	ORCHESTRATOR_DB_ENDPOINT="databaseorchestrator:3306" \
	ORCHESTRATOR_DB_NAME="orchestrator" \
	ORCHESTRATOR_DB_USER="root" \
	ORCHESTRATOR_DB_PWD="root" \
	WORKFLOW_DB_ENDPOINT="databaseworkflow:3306" \
	WORKFLOW_DB_NAME="workflow" \
	WORKFLOW_DB_USER="root" \
	WORKFLOW_DB_PWD="root"

EXPOSE 8080

ENTRYPOINT [ "/orchestrator/launch.sh" ]

CMD [ "java" ]
