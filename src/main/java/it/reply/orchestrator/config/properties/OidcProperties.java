package it.reply.orchestrator.config.properties;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.security.SecurityPrerequisite;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "oidc", locations = "${security.conf.file.path}")
public class OidcProperties implements SecurityPrerequisite, InitializingBean {

  static final Logger LOG = LoggerFactory.getLogger(OidcProperties.class);

  private boolean enabled;

  private boolean cacheTokens;

  @NotNull
  private List<IamProperties> iamProperties = Lists.newArrayList();

  @NotNull
  private Map<String, IamProperties> iamPropertiesMap = Maps.newHashMap();

  public List<IamProperties> getIamProperties() {
    return iamProperties;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isCacheTokens() {
    return cacheTokens;
  }

  @Nullable
  public IamProperties getIamConfiguration(String issuer) {
    return iamPropertiesMap.get(issuer);
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void setCacheTokens(boolean cacheTokens) {
    this.cacheTokens = cacheTokens;
  }

  /**
   * Set the IAM properties.
   * 
   * @param iamProperties
   *          the IAM properties to set
   */
  public void setIamProperties(List<IamProperties> iamProperties) {
    Assert.notNull(iamProperties);
    Assert.noNullElements(iamProperties.toArray());
    this.iamProperties = iamProperties;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (enabled) {
      for (IamProperties iamConfiguration : iamProperties) {
        String issuer = iamConfiguration.getIssuer();
        Assert.hasText(issuer, "OIDC Issuer field must not be empty");
        OrchestratorProperties orchestratorConfiguration = iamConfiguration.getOrchestrator();
        Assert.notNull(orchestratorConfiguration,
            "Orchestrator OAuth2 client for issuer " + issuer + " must be provided");
        Assert.isNull(iamPropertiesMap.put(issuer, iamConfiguration),
            "Duplicated configuration provided for OIDC issuer " + issuer);
        Assert.hasText(orchestratorConfiguration.getClientId(),
            "Orchestrator OAuth2 clientId for issuer " + issuer + " must be provided");
        Assert.hasText(orchestratorConfiguration.getClientSecret(),
            "Orchestrator OAuth2 clientSecret for issuer " + issuer + " must be provided");
        if (orchestratorConfiguration.getScopes().isEmpty()) {
          LOG.warn("No Orchestrator OAuth2 scopes provided for issuer {}", issuer);
        }

        OidcClientProperties cluesConfiguration = iamConfiguration.getClues();
        if (cluesConfiguration != null) {
          Assert.hasText(cluesConfiguration.getClientId(),
              "CLUES OAuth2 clientId for issuer " + issuer + " must be provided");
          Assert.hasText(cluesConfiguration.getClientSecret(),
              "CLUES OAuth2 clientSecret for issuer " + issuer + " must be provided");
        } else {
          LOG.warn("No CLUES OAuth2 configuration provided for issuer {}", issuer);
        }
      }
      if (iamPropertiesMap.keySet().isEmpty()) {
        LOG.warn("Empty IAM configuration list provided");
      } else {
        LOG.info("IAM configuration successfully parsed for issuers {}", iamPropertiesMap.keySet());
      }
    } else {
      LOG.info("IAM support is disabled");
    }

  }

  public static class IamProperties {

    @NotNull
    private String issuer;

    @NotNull
    private OrchestratorProperties orchestrator;

    @MonotonicNonNull
    private OidcClientProperties clues;

    public String getIssuer() {
      return issuer;
    }

    public void setIssuer(String issuer) {
      Assert.hasText(issuer);
      this.issuer = issuer;
    }

    public OrchestratorProperties getOrchestrator() {
      return orchestrator;
    }

    public void setOrchestrator(OrchestratorProperties orchestrator) {
      Assert.notNull(orchestrator);
      this.orchestrator = orchestrator;
    }

    @Nullable
    public OidcClientProperties getClues() {
      return clues;
    }

    public void setClues(OidcClientProperties clues) {
      Assert.notNull(clues);
      this.clues = clues;
    }

    @Override
    public String toString() {
      return ToStringBuilder.reflectionToString(this);
    }
  }

  public static class OidcClientProperties {

    @NotNull
    private String clientId;

    @NotNull
    private String clientSecret;

    public String getClientId() {
      return clientId;
    }

    public void setClientId(String clientId) {
      Assert.hasText(clientId);
      this.clientId = clientId;
    }

    public String getClientSecret() {
      return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
      Assert.hasText(clientSecret);
      this.clientSecret = clientSecret;
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this).append("clientId", clientId)
          .append("clientSecret", "<OMITTED>").toString();
    }
  }

  public static class OrchestratorProperties extends OidcClientProperties {

    @NotNull
    private List<String> scopes = Lists.newArrayList();

    public List<String> getScopes() {
      return scopes;
    }

    /**
     * Set the OAuth2 scopes.
     * 
     * @param scopes
     *          the OAuth2 scopes to set
     */
    public void setScopes(List<String> scopes) {
      Assert.notNull(scopes);
      Assert.noNullElements(scopes.toArray());
      this.scopes = scopes;
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this).appendSuper(super.toString()).append("scopes", scopes)
          .toString();
    }
  }

}
