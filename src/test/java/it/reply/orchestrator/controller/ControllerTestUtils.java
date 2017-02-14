package it.reply.orchestrator.controller;

/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
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

import com.google.common.collect.Lists;

import it.reply.orchestrator.dal.entity.AbstractResourceEntity;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ControllerTestUtils {

  public static Pageable createDefaultPageable() {
    return new PageRequest(0, 10,
        new Sort(Direction.DESC, AbstractResourceEntity.CREATED_COLUMN_NAME));
  }

  public static Deployment createDeployment(String id) {
    Deployment deployment = new Deployment();
    deployment.setId(id);
    deployment.setCreated(new Date());
    deployment.setUpdated(new Date());
    deployment.setVersion(0L);
    deployment.setTask(Task.NONE);
    deployment.setStatus(Status.UNKNOWN);
    deployment.setCallback("http://localhost");
    deployment.setTemplate("tosca_definitions_version: tosca_simple_yaml_1_0");
    return deployment;
  }

  public static Deployment createDeployment() {
    return createDeployment(UUID.randomUUID().toString());
  }

  public static List<Deployment> createDeployments(int total, boolean sorted) {
    List<Deployment> deployments = Lists.newArrayList();
    for (int i = 0; i < total; ++i) {
      deployments.add(createDeployment());
    }
    if (sorted) {
      deployments.stream().sorted(new Comparator<Deployment>() {

        @Override
        public int compare(Deployment o1, Deployment o2) {
          return o1.getCreated().compareTo(o2.getCreated());
        }
      }).collect(Collectors.toList());
    }
    return deployments;
  }

  public static Resource createResource(Deployment deployment) {
    Resource resource = new Resource();
    resource.setId(UUID.randomUUID().toString());
    resource.setCreated(new Date());
    resource.setUpdated(new Date());
    resource.setState(NodeStates.CREATED);
    resource.setToscaNodeType("tosca.nodes.Compute");
    resource.setToscaNodeName("node_" + UUID.randomUUID().toString());
    resource.setDeployment(deployment);
    return resource;
  }

  public static List<Resource> createResources(Deployment deployment, int total, boolean sorted) {
    List<Resource> resources = Lists.newArrayList();
    for (int i = 0; i < total; ++i) {
      resources.add(createResource(deployment));
    }

    if (sorted) {
      resources.stream().sorted(new Comparator<Resource>() {

        @Override
        public int compare(Resource o1, Resource o2) {
          return o1.getCreated().compareTo(o2.getCreated());
        }
      }).collect(Collectors.toList());
    }

    return resources;
  }

  public static String reverse(String template) {
    return new StringBuilder(template).reverse().toString();
  }

}
