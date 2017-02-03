package it.reply.orchestrator.dto.deployment;

import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.ScalarPropertyValue;

import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;

public class CredentialsAwareSlaPlacementPolicy extends SlaPlacementPolicy {

  private static final long serialVersionUID = -7100234156400910682L;

  private String username;
  private String password;

  /**
   * Create a CredentialsAwareSlaPlacementPolicy.
   * 
   * @param nodes
   *          a lis of nodes
   * @param slaId
   *          the slaId
   * @param username
   *          the username
   * @param password
   *          the password
   */
  public CredentialsAwareSlaPlacementPolicy(List<String> nodes, String slaId, String username,
      String password) {
    super(nodes, slaId);
    this.setUsername(username);
    this.setPassword(password);
  }

  /**
   * Create a CredentialsAwareSlaPlacementPolicy from a SlaPlacementPolicy.
   * 
   * @param slaPlacementPolicy
   *          the slaPlacementPolicy
   * @param username
   *          the username
   * @param password
   *          the password
   */
  public CredentialsAwareSlaPlacementPolicy(SlaPlacementPolicy slaPlacementPolicy,
      AbstractPropertyValue username, AbstractPropertyValue password) {
    super(slaPlacementPolicy.getNodes(), slaPlacementPolicy.getSlaId());
    this.setUsername(username);
    this.setPassword(password);
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    Objects.requireNonNull(username, "username list must not be null");
    this.username = username;
  }

  /**
   * Set the username from an {@link AbstractPropertyValue}.
   * 
   * @param username
   *          the username
   */
  public void setUsername(AbstractPropertyValue username) {
    Objects.requireNonNull(username, "username must not be null");
    Assert.isInstanceOf(ScalarPropertyValue.class, username, "username must be a scalar value");
    this.username = ((ScalarPropertyValue) username).getValue();
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    Objects.requireNonNull(password, "password not be null");
    this.password = password;
  }

  /**
   * Set the password from an {@link AbstractPropertyValue}.
   * 
   * @param password
   *          the password
   */
  public void setPassword(AbstractPropertyValue password) {
    Objects.requireNonNull(password, "password must not be null");
    Assert.isInstanceOf(ScalarPropertyValue.class, password, "password must be a scalar value");
    this.password = ((ScalarPropertyValue) password).getValue();
  }
}
