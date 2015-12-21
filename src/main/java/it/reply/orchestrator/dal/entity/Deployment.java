package it.reply.orchestrator.dal.entity;

import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.Tasks;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MapKeyColumn;

@Entity
public class Deployment extends AbstractResourceEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "task")
  private Tasks task;

  @Enumerated(EnumType.STRING)
  @Column(name = "deploymentProvider")
  private DeploymentProvider deploymentProvider;

  @Column(name = "endpoint")
  private String endpoint;

  @Column(name = "template", columnDefinition = "LONGTEXT")
  private String template;

  @ElementCollection
  @MapKeyColumn(name = "name")
  @Column(name = "value")
  Map<String, String> parameters = new HashMap<String, String>();

  public Deployment() {
    super();
  }

  public Tasks getTask() {
    return task;
  }

  public void setTask(Tasks task) {
    this.task = task;
  }

  public String getTemplate() {
    return template;
  }

  public DeploymentProvider getDeploymentProvider() {
    return deploymentProvider;
  }

  public void setDeploymentProvider(DeploymentProvider deploymentProvider) {
    this.deploymentProvider = deploymentProvider;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public void setTemplate(String template) {
    this.template = template;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }

}
