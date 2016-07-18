package it.reply.orchestrator.dal.entity;

import it.reply.orchestrator.enums.NodeStates;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(indexes = { @Index(columnList = "toscaNodeName"), @Index(columnList = "deployment_uuid") })
public class Resource extends AbstractResourceEntity {

  private static final long serialVersionUID = -4916577635363604624L;

  @Enumerated(EnumType.STRING)
  @Column(name = "state", length = 500)
  private NodeStates state;

  @Column(name = "iaasId", length = 500)
  private String iaasId;

  // @Enumerated(EnumType.STRING)
  @Column(name = "toscaNodeType")
  private String toscaNodeType;

  @Column(name = "toscaNodeName")
  private String toscaNodeName;

  @ElementCollection
  @Column(name = "requiredBy")
  List<String> requiredBy = new ArrayList<String>();

  @ManyToOne
  @JoinColumn(name = "deployment_uuid")
  private Deployment deployment;

  public Resource() {
    super();
  }

  /**
   * Creates a new Resource object.
   * 
   * @param toscaNodeName
   *          the TOSCA node name of the resource
   */
  public Resource(String toscaNodeName) {
    super();
    this.toscaNodeName = toscaNodeName;
    state = NodeStates.INITIAL;
  }

  public NodeStates getState() {
    return state;
  }

  public void setState(NodeStates state) {
    this.state = state;
  }

  public String getIaasId() {
    return iaasId;
  }

  public void setIaasId(String iaasId) {
    this.iaasId = iaasId;
  }

  public String getToscaNodeType() {
    return toscaNodeType;
  }

  public void setToscaNodeType(String toscaNodeType) {
    this.toscaNodeType = toscaNodeType;
  }

  public String getToscaNodeName() {
    return toscaNodeName;
  }

  public void setToscaNodeName(String toscaNodeName) {
    this.toscaNodeName = toscaNodeName;
  }

  public List<String> getRequiredBy() {
    return requiredBy;
  }

  public void setRequiredBy(List<String> requiredBy) {
    this.requiredBy = requiredBy;
  }

  public Deployment getDeployment() {
    return deployment;
  }

  public void setDeployment(Deployment deployment) {
    this.deployment = deployment;
  }

}
