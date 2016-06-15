package it.reply.orchestrator.dal.entity;

import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.enums.Task;
import it.reply.utils.json.JsonUtility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

@Entity
public class Deployment extends AbstractResourceEntity {

  private static final long serialVersionUID = 3866893436735377053L;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 500)
  private Status status;

  @Column(name = "statusReason", columnDefinition = "LONGTEXT")
  private String statusReason;

  @Enumerated(EnumType.STRING)
  @Column(name = "task")
  private Task task;

  @Enumerated(EnumType.STRING)
  @Column(name = "deploymentProvider")
  private DeploymentProvider deploymentProvider;

  @Column(name = "endpoint")
  private String endpoint;

  @Column(name = "callback")
  private String callback;

  @Column(name = "template", columnDefinition = "LONGTEXT")
  private String template;

  /**
   * The user's inputs to the template.
   */
  @Transient
  Map<String, Object> unserializedParameters = null;

  @ElementCollection(fetch = FetchType.EAGER)
  @MapKeyColumn(name = "name")
  @Column(name = "value", columnDefinition = "TEXT")
  private Map<String, String> parameters = new HashMap<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @MapKeyColumn(name = "name")
  @Column(name = "value", columnDefinition = "TEXT")
  Map<String, String> outputs = new HashMap<String, String>();

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "deployment", orphanRemoval = true)
  List<Resource> resources = new ArrayList<>();

  @Column(name = "cloudProviderName", length = 128)
  String cloudProviderName;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "deployment", orphanRemoval = true)
  List<WorkflowReference> workflowReferences = new ArrayList<>();

  public Deployment() {
    super();
  }

  public String getCloudProviderName() {
    return cloudProviderName;
  }

  public void setCloudProviderName(String cloudProviderName) {
    this.cloudProviderName = cloudProviderName;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public String getStatusReason() {
    return statusReason;
  }

  public void setStatusReason(String statusReason) {
    this.statusReason = statusReason;
  }

  public Task getTask() {
    return task;
  }

  public void setTask(Task task) {
    this.task = task;
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

  public String getCallback() {
    return callback;
  }

  public void setCallback(String callback) {
    this.callback = callback;
  }

  public String getTemplate() {
    return template;
  }

  public void setTemplate(String template) {
    this.template = template;
  }

  /**
   * The user's inputs to the template.
   */
  public synchronized Map<String, Object> getParameters() {

    if (unserializedParameters != null) {
      return unserializedParameters;
    }

    unserializedParameters = new HashMap<>();
    for (Map.Entry<String, String> serializedParam : parameters.entrySet()) {
      Object paramObject = null;
      if (serializedParam.getValue() != null) {
        try {
          paramObject = JsonUtility.deserializeJson(serializedParam.getValue(), Object.class);
        } catch (IOException ex) {
          throw new RuntimeException("Failed to deserialize parameters in JSON", ex);
        }
      }

      unserializedParameters.put(serializedParam.getKey(), paramObject);
    }

    return unserializedParameters;
  }

  /**
   * The user's inputs to the template.
   *
   */
  public synchronized void setParameters(Map<String, Object> parameters) {
    this.parameters = new HashMap<>();
    for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
      String paramString = null;
      if (parameter.getValue() != null) {
        try {
          paramString = JsonUtility.serializeJson(parameter.getValue());
        } catch (IOException ex) {
          throw new RuntimeException("Failed to serialize parameters in JSON", ex);
        }
      }

      this.parameters.put(parameter.getKey(), paramString);
    }
    this.unserializedParameters = null;
  }

  public Map<String, String> getOutputs() {
    return outputs;
  }

  public void setOutputs(Map<String, String> outputs) {
    this.outputs = outputs;
  }

  public List<Resource> getResources() {
    return resources;
  }

  public List<WorkflowReference> getWorkflowReferences() {
    return workflowReferences;
  }

  public void setWorkflowReferences(List<WorkflowReference> workflowReferences) {
    this.workflowReferences = workflowReferences;
  }

  @Transient
  public void addWorkflowReferences(@Nonnull WorkflowReference workflowReference) {
    workflowReference.setDeployment(this);
    this.workflowReferences.add(workflowReference);
  }

  public void setResources(List<Resource> resources) {
    this.resources = resources;
  }

}
