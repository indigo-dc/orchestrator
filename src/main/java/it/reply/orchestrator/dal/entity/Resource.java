package it.reply.orchestrator.dal.entity;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;

@Entity
public class Resource extends AbstractResourceEntity {

  @Column(name = "resourceType", length = 500)
  private String resourceType;

  @ElementCollection
  @Column(name = "requiredBy")
  List<String> requiredBy = new ArrayList<String>();

  public Resource() {
    super();
  }

  public String getResourceType() {
    return resourceType;
  }

  public void setResourceType(String resourceType) {
    this.resourceType = resourceType;
  }

  public List<String> getRequiredBy() {
    return requiredBy;
  }

  public void setRequiredBy(List<String> requiredBy) {
    this.requiredBy = requiredBy;
  }

}
