package it.reply.orchestrator.resource;

import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.resource.common.AbstractResource;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.List;

public class BaseResource extends AbstractResource {

  private NodeStates state;
  private String toscaNodeType;
  private String toscaNodeName;
  private List<String> requiredBy = new ArrayList<String>();

  public NodeStates getState() {
    return state;
  }

  public void setState(NodeStates state) {
    this.state = state;
  }

  public String getToscaNodeType() {
    return this.toscaNodeType;
  }

  public void setToscaNodeType(String toscaNodeType) {
    this.toscaNodeType = toscaNodeType;
  }

  public BaseResource withToscaNodeType(String toscaNodeType) {
    this.toscaNodeType = toscaNodeType;
    return this;
  }

  public String getToscaNodeName() {
    return toscaNodeName;
  }

  public void setToscaNodeName(String toscaNodeName) {
    this.toscaNodeName = toscaNodeName;
  }

  public BaseResource withToscaNodeName(String toscaNodeName) {
    this.toscaNodeName = toscaNodeName;
    return this;
  }

  public List<String> getRequiredBy() {
    return requiredBy;
  }

  public void setRequiredBy(List<String> requiredBy) {
    this.requiredBy = requiredBy;
  }

  public BaseResource withRequiredBy(List<String> requiredBy) {
    this.requiredBy = requiredBy;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}