package it.reply.orchestrator.controller;

import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;

import org.elasticsearch.common.collect.Lists;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ControllerTestUtils {

  public static Pageable createDefaultPageable() {
    return new PageRequest(0, 10);
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

  public static List<Deployment> createDeployments(int total) {
    List<Deployment> deployments = Lists.newArrayList();
    for (int i = 0; i < total; ++i) {
      deployments.add(createDeployment());
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

  public static List<Resource> createResources(Deployment deployment, int total) {
    List<Resource> resources = Lists.newArrayList();
    for (int i = 0; i < total; ++i) {
      resources.add(createResource(deployment));
    }
    return resources;
  }

  public static String reverse(String template) {
    return new StringBuilder(template).reverse().toString();
  }

}
