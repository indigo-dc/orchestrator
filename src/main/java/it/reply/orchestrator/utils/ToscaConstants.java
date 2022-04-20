/*
 * Copyright © 2015-2021 I.N.F.N.
 * Copyright © 2015-2020 Santer Reply S.p.A.
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

    @UtilityClass
    public static class Types {

      private static final String BASE_INDIGO_NAME = "tosca.nodes.indigo.";
      public static final String DOCKER_APPLICATION =
          BASE_INDIGO_NAME + "Container.Application.Docker";
      public static final String CHRONOS = DOCKER_APPLICATION + ".Chronos";
      public static final String MARATHON = DOCKER_APPLICATION + ".Marathon";
      public static final String COMPUTE = BASE_INDIGO_NAME + "Compute";
      public static final String QCG = BASE_INDIGO_NAME + "Qcg.Job";
      public static final String KUBERNETES_HELM_CHART = BASE_INDIGO_NAME + "Kubernetes.HelmChart";

      public static final String DOCKER_RUNTIME = BASE_INDIGO_NAME + "Container.Runtime.Docker";
      public static final String ONEDATA_SPACE = BASE_INDIGO_NAME + "OnedataSpace";
      public static final String ONEDATA_SERVICE_SPACE = BASE_INDIGO_NAME + "OnedataServiceSpace";
      public static final String DYNAFED = BASE_INDIGO_NAME + "Dynafed";

      public static final String CENTRAL_POINT = BASE_INDIGO_NAME + "VR.CentralPoint";
      public static final String VROUTER = BASE_INDIGO_NAME + "VR.VRouter";
      public static final String CLIENT = BASE_INDIGO_NAME + "VR.Client";
      public static final String ELASTIC_CLUSTER = BASE_INDIGO_NAME + "ElasticCluster";

      public static final String WORKER_NODE = BASE_INDIGO_NAME + "LRMS.WorkerNode";
      public static final String SLURM_WN = WORKER_NODE + ".Slurm";
      public static final String TORQUE_WN = WORKER_NODE  + ".Torque";
      public static final String GALAXY_WN = WORKER_NODE  + ".SlurmGalaxy";
      public static final String MESOS_WN = WORKER_NODE  + ".Mesos";
      public static final String KUBERNETES_WN = WORKER_NODE  + ".Kubernetes";

      public static final String FRONT_END = BASE_INDIGO_NAME  + "LRMS.FrontEnd";
      public static final String SLURM_FE = FRONT_END  + ".Slurm";
      public static final String TORQUE_FE = FRONT_END  + ".Torque";
      public static final String GALAXY_FE = FRONT_END  + ".SlurmGalaxy";
      public static final String MESOS_FE = FRONT_END  + ".Mesos";
      public static final String KUBERNETES_FE = FRONT_END  + ".Kubernetes";

      public static final String BASE_NETWORK_NAME = "tosca.nodes.network.";
      public static final String NETWORK = BASE_NETWORK_NAME + "Network";
      public static final String PORT = BASE_NETWORK_NAME + "Port";

      public static final String CUSTOM_BASE_NETWORK = "tosca.nodes.indigo.network.";
      public static final String CUSTOM_NETWORK = "tosca.nodes.indigo.network.Network";

      public static final String VOLUME = BASE_INDIGO_NAME +  "BlockStorage";

    }

    @UtilityClass
    public static class RE {
      public static final String FRONT_END_RE =
          "^tosca\\.nodes\\.indigo\\.LRMS\\.FrontEnd\\.\\w+$";
      public static final String WORKER_NODE_RE =
          "^tosca\\.nodes\\.indigo\\.LRMS\\.WorkerNode\\.\\w+$";
      public static final String NETWORK_NODE_RE =
              "^tosca\\.nodes\\.(indigo\\.)?network\\.Network$";
    }

    @UtilityClass
    public static class Attributes {
      public static final String PRIVATE = "private";
      public static final String ISOLATED = "isolated";
      public static final String PUBLIC = "public";
    }

    @UtilityClass
    public static class Properties {
      public static final String NETWORKTYPE = "network_type";
      public static final String NETWORKNAME = "network_name";
      public static final String NETWORKPROXYHOST = "proxy_host";
      public static final String NETWORKPROXYHOSTCRED = "proxy_credential";
      public static final String NETWORKPROXYHOSTUSER = "user";
      public static final String NETWORKPROXYHOSTTOKEN = "token";
      public static final String HYBRID = "hybrid";
    }

    @UtilityClass
    public static class Capabilities {
      public static final String CENTRALPOINT = "central_point";
    }
  }

  @UtilityClass
  public static class Policies {

    @UtilityClass
    public static class Types {

      private static final String BASE_INDIGO_NAME = "tosca.policies.indigo.";
      public static final String SLA_PLACEMENT = BASE_INDIGO_NAME + "SlaPlacement";
    }

    @UtilityClass
    public static class Properties {

      public static final String PLACEMENT_ID = "sla_id";
    }
  }
}
