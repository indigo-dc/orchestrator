/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.reply.orchestrator.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ToscaConstants {
  @UtilityClass
  public static class Nodes {
    private static final String BASE_INDIGO_NAME = "tosca.nodes.indigo.";
    public static final String CHRONOS = BASE_INDIGO_NAME + "Container.Application.Docker.Chronos";
    public static final String MARATHON =
        BASE_INDIGO_NAME + "Container.Application.Docker.Marathon";
    public static final String COMPUTE = BASE_INDIGO_NAME + "Compute";
    public static final String ELASTIC_CLUSTER = BASE_INDIGO_NAME + "ElasticCluster";
  }
}
