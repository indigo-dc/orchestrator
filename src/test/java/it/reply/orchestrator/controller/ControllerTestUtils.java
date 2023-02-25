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

package it.reply.orchestrator.controller;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.DeploymentSchedule;
import it.reply.orchestrator.dal.entity.DeploymentScheduleEvent;
import it.reply.orchestrator.dal.entity.ReplicationRule;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.enums.DeploymentScheduleStatus;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.ReplicationRuleStatus;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ControllerTestUtils {

  public static Pageable createDefaultPageable() {
    return new PageRequest(0, 10,
        new Sort(Direction.DESC, "createdAt"));
  }

  public static Deployment createDeployment(String id, int resourceNumber) {
    Deployment deployment = new Deployment();
    deployment.setId(id);
    deployment.setCreatedAt(new Date());
    deployment.setUpdatedAt(new Date());
    deployment.setVersion(0L);
    deployment.setTask(Task.NONE);
    deployment.setStatus(Status.CREATE_IN_PROGRESS);
    deployment.setCallback("http://localhost");
    deployment.setTemplate("tosca_definitions_version: tosca_simple_yaml_1_0\ntopology_template:\n");
    createResources(deployment, resourceNumber, false);
    return deployment;
  }

  public static DeploymentSchedule createDeploymentSchedule(String id) {
    DeploymentSchedule deploymentSchedule = new DeploymentSchedule();
    deploymentSchedule.setId(id);
    deploymentSchedule.setCreatedAt(new Date());
    deploymentSchedule.setUpdatedAt(new Date());
    deploymentSchedule.setVersion(0L);
    deploymentSchedule.setFileExpression("scope:name*");
    deploymentSchedule.setStatus(DeploymentScheduleStatus.RUNNING);
    deploymentSchedule.setNumberOfReplicas(1);
    deploymentSchedule.setReplicationExpression("RSE_RECAS");
    deploymentSchedule.setCallback("http://localhost");
    deploymentSchedule.setTemplate("tosca_definitions_version: tosca_simple_yaml_1_0\ntopology_template:\n");
    return deploymentSchedule;
  }

  public static Deployment createDeployment(String id) {
    return createDeployment(id, 0);
  }

  public static Deployment createDeployment() {
    return createDeployment(UUID.randomUUID().toString());
  }

  public static DeploymentSchedule createDeploymentSchedule() {
    return createDeploymentSchedule(UUID.randomUUID().toString());
  }

  public static Deployment createDeployment(int resourceNumber) {
    return createDeployment(UUID.randomUUID().toString(), resourceNumber);
  }

  public static List<Deployment> createDeployments(int total) {
    return IntStream
        .range(0, total)
        .mapToObj((i) -> createDeployment())
        .collect(Collectors.toList());
  }

  public static List<DeploymentSchedule> createDeploymentSchedules(int total) {
    return IntStream
        .range(0, total)
        .mapToObj((i) -> createDeploymentSchedule())
        .collect(Collectors.toList());
  }

  public static Resource createResource(Deployment deployment,
      String toscaNodeType, String toscaNodeName, String iaasId) {
    Resource resource = new Resource();
    resource.setId(UUID.randomUUID().toString());
    resource.setCreatedAt(new Date());
    resource.setUpdatedAt(new Date());
    resource.setState(NodeStates.CREATING);
    resource.setToscaNodeType(toscaNodeType);
    resource.setToscaNodeName(toscaNodeName);
    resource.setIaasId(iaasId);
    resource.setDeployment(deployment);
    deployment.getResources().add(resource);
    return resource;
  }

  public static Resource createResource(String id, Deployment deployment) {
    Resource resource = new Resource();
    resource.setId(UUID.randomUUID().toString());
    resource.setCreatedAt(new Date());
    resource.setUpdatedAt(new Date());
    resource.setState(NodeStates.CREATING);
    resource.setToscaNodeType("tosca.nodes.Compute");
    resource.setToscaNodeName("node_" + id);
    resource.setIaasId(id);
    resource.setDeployment(deployment);
    deployment.getResources().add(resource);
    return resource;
  }

  public static Resource createNetworkResource(String id, Deployment deployment) {
    Resource resource = new Resource();
    resource.setId(UUID.randomUUID().toString());
    resource.setCreatedAt(new Date());
    resource.setUpdatedAt(new Date());
    resource.setState(NodeStates.STARTED);
    resource.setToscaNodeType("tosca.nodes.network.Port");
    resource.setToscaNodeName("server_port" + id);
    resource.setDeployment(deployment);
    deployment.getResources().add(resource);
    return resource;
  }

  public static Resource createComputeResource(Deployment deployment) {
    Resource resource = new Resource();
    resource.setId(UUID.randomUUID().toString());
    resource.setCreatedAt(new Date());
    resource.setUpdatedAt(new Date());
    resource.setState(NodeStates.STARTED);
    resource.setToscaNodeType("tosca.nodes.Compute");
    resource.setToscaNodeName("server");
    resource.setIaasId("0");
    Map<String, String> metadata = new HashMap<>();
    metadata.put("VirtualMachineInfo", "{\"vmProperties\":[{\"class\":\"network\",\"id\":\"pub_network\",\"outbound\":\"yes\",\"outports\":\"22/tcp-22/tcp\",\"provider_id\":\"public\"},{\"class\":\"network\",\"id\":\"priv_network\",\"provider_id\":\"INFN_Cloud_HSMDIS-net\"},{\"class\":\"system\",\"id\":\"simple_node\",\"instance_name\":\"simple-node-85d7d822-8ab3-11ed-b5a4-fa163ed94b64\",\"instance_tags\":\"PAAS_URL=http://localhost:8080,PAAS_DEP_UUID=11ed8ab3-6a40-968a-82b7-62f0862dbe46,PAAS_DEP_USER_EMAIL=Marica.Antonacci@ba.infn.it\",\"disk.0.os.flavour\":\"ubuntu\",\"disk.0.os.version\":\"20.04\",\"disk.0.image.url\":\"ost://keystone.cloud.infn.it/2b25c2c6-6398-45c6-9e00-3e7c58dbd750\",\"cpu.count\":1,\"memory.size\":2147483648,\"instance_type\":\"small\",\"net_interface.1.connection\":\"pub_network\",\"net_interface.0.connection\":\"priv_network\",\"cpu.arch\":\"x86_64\",\"disk.0.free_size\":10737418240,\"disk.0.os.credentials.username\":\"cloudadm\",\"provider.type\":\"OpenStack\",\"provider.host\":\"keystone.cloud.infn.it\",\"disk.0.os.credentials.private_key\":\"-----BEGIN RSA PRIVATE KEY-----\\nMIIEowIBAAKCAQEAxTIykFKQKLinzT8QqeJplp2VwegWZGCs07Yn9Su/veJYKdpq\\ny0S/6LKyNikCwYGu2o6ZRIEtj7Per3SexalK94fGLok+HwBkoLLYP3DJu93dgarB\\nM8ze/71gQhEj1YqhWj8ff31/M40/Wf7T91fv0FUNj/aBnpQS83UKDdUp5m5criRt\\n5s7Ji3vexmZN2a6peZytbzvUChlzJKH+vVaWAkte+H5QK7kVldyRjSiOsmArk4ng\\n4xoJtW3KvVDJTR5XHgmJgPmQv7lHGZVMSxcy9tUyZxGJ7Vj4wUIqR86VadgQ8Uez\\nR/khvIIB/yNyKkuLMOMc5MhFq/FkEctqXvZKvQIDAQABAoIBADQKFjvt/esxJ99L\\nSEt8256Kwa2lYNtmg9VvGcZioDwtElUhNdOktfxjxBH06qxEW++DeEES0gI9VoP5\\nA7ln/seqBgvb3g/yW5vo9pZvXl18pGsGs+vGogbdZByrR0igSAp35AkSBlKFcPWA\\nAVIh28Cf4W9ffz9pFkM3XMrfuSn8+TF21l2bZgRFxp2BnxUR7wm9jf9+IegaHYsN\\nHMPCslV1TMX3qFQsVNh/2OUAa/cER2ADmt6uF2QfpySXdPdvhvo8TKx3APqBgHEd\\nL7xXKACvsEb/X/sshC7G7QgZ6Hug/w0Dt2Nb57WbSKJMp5BNPgoPHlcTjFI2+Xf9\\n19tEbmUCgYEA1jsBxkuJs1/bN6btGR2QQ8B4sJ6b+W6QS2KvEl0VI8b86A+qE2/U\\nVonRbV506dqhuLoIOg6vUjI7ir69IWD+k1a7O3RS8esgCopWzp7GA9gACC0nEzeB\\nF9ZAeku35V44+CEyAS+PJ/rIU0wTYVmHmyvYNvuYZW6RSGbNk1Yk/QsCgYEA66Tx\\ni1fOVgQjk+93mUJyQ4GBijPeo1GZgH4wzFx5bQTilZcKtD9eoBlAXQYy6WYin16e\\n7oLuv9P4T6a7qEtDiPWDdcgIoeqhsHMnzT2dZPFWBD/AxFLetjmLUabBlSCiF3c3\\nmd0J+BOYWEbmiIya7lt/ntgA6xBBMQNLpDAkZFcCgYEAthaBc6JQ8H3RPoRLksVm\\nEbBO5RBrUp9/VhQS6nDjvv0ribOGw+6RdNy/fU6D4SFLxE+bHn3M1viLU2q8Jtri\\nhyl5CxfcSHOpUEw1bHnXpPy7SjfWtqa+uxwIweoJ7JMhqQXOEuPxsfGew+4tqtkG\\nQgYKSPGgntZT9k4q/ciCt/ECgYBv8fYF+nq9hk3Yk6S/nD9oLnf5zdZww+0mi8xm\\n6W+OCQoTgqPFKW2spRJcnvtEwg6ko3DhYjZFz09hl5YCx26X+/qt04+2TB7wEmTQ\\nLfs93yC7+AV9vtiqvP+glJXWYKBggXRalhWMUtLG7TfLmvBnV/Ry467b/Obbf/NJ\\nVs2TFQKBgH/VMnMItvJZNouemf0OxePr5nsQKc9eQvRlcXqwhvaHgFjPVzvhUKbN\\nEHSf2PpegkPodrqHgW4l9QezO2xHNkx4LMbHZE1R6dmzi6YYkEar9JuYyzvpwpC/\\nnXxK1hrvni/13VoWc2zZUn0YcEi8HXVBby7Y8hIkrg9GAgVzbt2k\\n-----END RSA PRIVATE KEY-----\\n\",\"state\":\"stopped\",\"instance_id\":\"38e9014e-a303-4f40-92a2-fae26e1fddfa\",\"net_interface.0.dns_name\":\"vnode-0\",\"net_interface.0.ip\":\"192.168.163.69\",\"net_interface.1.ip\":\"90.147.174.56\"}]}");
    resource.setMetadata(metadata);
    resource.setDeployment(deployment);
    deployment.getResources().add(resource);
    return resource;
  }

  public static Resource createResource(Deployment deployment) {
    return createResource(UUID.randomUUID().toString(), deployment);
  }

  public static List<Resource> createResources(Deployment deployment, int total, boolean sorted) {
    return IntStream
        .range(0, total)
        .mapToObj((i) -> createResource(String.valueOf(i), deployment))
        .collect(Collectors.toList());
  }

  public static List<Resource> createMultiTypedResources(Deployment deployment) {
    List<Resource> resources = new ArrayList<>();
    resources.add(createNetworkResource("1", deployment));
    resources.add(createNetworkResource("2", deployment));
    resources.add(createComputeResource(deployment));
    return resources;
  }

  public static DeploymentScheduleEvent createDeploymentScheduleEvent(String deploymentScheduleId, String eventId) {
    DeploymentScheduleEvent deploymentScheduleEvent = new DeploymentScheduleEvent();
    deploymentScheduleEvent.setDeploymentSchedule(createDeploymentSchedule(deploymentScheduleId));
    deploymentScheduleEvent.setDeployment(createDeployment());
    deploymentScheduleEvent.setName("file-name");
    deploymentScheduleEvent.setScope("file-scope");
    deploymentScheduleEvent.setCreatedAt(new Date());
    deploymentScheduleEvent.setUpdatedAt(new Date());
    deploymentScheduleEvent.setVersion(0L);
    deploymentScheduleEvent.setId(eventId);
    ReplicationRule replicationRule = new ReplicationRule();
    replicationRule.setStatus(ReplicationRuleStatus.REPLICATING);
    deploymentScheduleEvent.setMainReplicationRule(replicationRule);
    return deploymentScheduleEvent;
  }
  public static List<DeploymentScheduleEvent> createDeploymentScheduleEvents(String deploymentScheduleId, int total) {
    return IntStream
        .range(0, total)
        .mapToObj((i) -> createDeploymentScheduleEvent(deploymentScheduleId, UUID.randomUUID().toString()))
        .collect(Collectors.toList());
  }
}
