/*
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
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import java.util.Date;
import java.util.List;
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

  public static Deployment createDeployment(String id) {
    return createDeployment(id, 0);
  }

  public static Deployment createDeployment() {
    return createDeployment(UUID.randomUUID().toString());
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

  public static Resource createResource(Deployment deployment,
      String toscaNodeType, String toscaNodeName) {
    Resource resource = new Resource();
    resource.setId(UUID.randomUUID().toString());
    resource.setCreatedAt(new Date());
    resource.setUpdatedAt(new Date());
    resource.setState(NodeStates.CREATING);
    resource.setToscaNodeType(toscaNodeType);
    resource.setToscaNodeName(toscaNodeName);
    resource.setIaasId(UUID.randomUUID().toString());
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

  public static Resource createResource(Deployment deployment) {
    return createResource(UUID.randomUUID().toString(), deployment);
  }

  public static List<Resource> createResources(Deployment deployment, int total, boolean sorted) {
    return IntStream
        .range(0, total)
        .mapToObj((i) -> createResource(String.valueOf(i), deployment))
        .collect(Collectors.toList());
  }

}
