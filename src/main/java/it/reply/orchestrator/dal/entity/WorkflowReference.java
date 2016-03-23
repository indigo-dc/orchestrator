package it.reply.orchestrator.dal.entity;

import it.reply.workflowManager.orchestrator.bpm.BusinessProcessManager;
import it.reply.workflowManager.orchestrator.bpm.BusinessProcessManager.RUNTIME_STRATEGY;

import org.springframework.hateoas.Identifiable;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

@Entity
public class WorkflowReference implements Identifiable<Long>, Serializable {

  private static final long serialVersionUID = -610233480056664663L;

  public WorkflowReference() {
    super();
  }

  /**
   * Constructor with fields.
   * 
   * @param processId
   *          the process id
   * @param runtimeStrategy
   *          the strategy {@Link RUNTIME_STRATEGY}
   */
  public WorkflowReference(long processId, RUNTIME_STRATEGY runtimeStrategy) {
    super();
    this.processId = processId;
    this.runtimeStrategy = runtimeStrategy;
  }

  @Id
  @Column(name = "process_id", unique = true, nullable = false)
  private long processId;

  @Enumerated(EnumType.STRING)
  @Column(name = "runtime_strategy", length = 100, nullable = false)
  private BusinessProcessManager.RUNTIME_STRATEGY runtimeStrategy;

  @ManyToOne
  @JoinColumn(name = "deployment_uuid")
  private Deployment deployment;

  public Deployment getDeployment() {
    return deployment;
  }

  public void setDeployment(Deployment deployment) {
    this.deployment = deployment;
  }

  @Override
  @Transient
  public Long getId() {
    return processId;
  }

  public Long getProcessId() {
    return processId;
  }

  public void setProcessId(long processId) {
    this.processId = processId;
  }

  public BusinessProcessManager.RUNTIME_STRATEGY getRuntimeStrategy() {
    return runtimeStrategy;
  }

  public void setRuntimeStrategy(BusinessProcessManager.RUNTIME_STRATEGY runtimeStrategy) {
    this.runtimeStrategy = runtimeStrategy;
  }

}
