package it.reply.orchestrator.dto.deployment;

public class AwsSlaPlacementPolicy extends CredentialsAwareSlaPlacementPolicy {

  private static final long serialVersionUID = -7011571670045507898L;

  public AwsSlaPlacementPolicy(CredentialsAwareSlaPlacementPolicy policy) {
    super(policy.getNodes(), policy.getSlaId(), policy.getUsername(), policy.getPassword());
  }

  public String getAccessKey() {
    return this.getUsername();
  }

  public String getSecretKey() {
    return this.getPassword();
  }

}
