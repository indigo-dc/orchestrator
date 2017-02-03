package it.reply.orchestrator.dto.deployment;

import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.ScalarPropertyValue;

import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;

/**
 * @author a.brigandi
 *
 */
public class SlaPlacementPolicy implements PlacementPolicy {

  private static final long serialVersionUID = 2712997001319905444L;

  private List<String> nodes;

  private String slaId;

  public SlaPlacementPolicy(List<String> nodes, String slaId) {
    this.setNodes(nodes);
    this.setSlaId(slaId);
  }

  public SlaPlacementPolicy(List<String> nodes, AbstractPropertyValue slaId) {
    this.setNodes(nodes);
    this.setSlaId(slaId);
  }

  @Override
  public List<String> getNodes() {
    return nodes;
  }

  @Override
  public void setNodes(List<String> nodes) {
    Objects.requireNonNull(nodes, "nodes list must not be null");
    this.nodes = nodes;
  }

  public String getSlaId() {
    return slaId;
  }

  public void setSlaId(String slaId) {
    Objects.requireNonNull(slaId, "slaId must not be null");
    this.slaId = slaId;
  }

  /**
   * Set the SLA id from a {@link AbstractPropertyValue}.
   * 
   * @param slaId
   *          the SLA id
   */
  public void setSlaId(AbstractPropertyValue slaId) {
    Objects.requireNonNull(slaId, "slaId must not be null");
    Assert.isInstanceOf(ScalarPropertyValue.class, slaId, "slaId must be a scalar value");
    this.slaId = ((ScalarPropertyValue) slaId).getValue();
  }

}
