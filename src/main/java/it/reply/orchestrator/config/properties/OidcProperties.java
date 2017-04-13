/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.reply.orchestrator.config.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.security.SecurityPrerequisite;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.validation.constraints.NotNull;

@Slf4j
@Data
@ConfigurationProperties(prefix = "oidc")
public class OidcProperties implements SecurityPrerequisite, InitializingBean {

  private boolean enabled;

  private boolean cacheTokens;

  @NotNull
  @NonNull
  private Map<String, IamProperties> iamProperties = new HashMap<>();

  /**
   * Throw an {@link IllegalStateException} if the security is disabled.
   */
  public void throwIfSecurityDisabled() {
    runIfSecurityDisabled(() -> {
      throw new IllegalStateException("Security is not enabled");
    });
  }

  /**
   * Run the function if the security is disabled.
   * 
   * @param function
   *          the function to run
   */
  public void runIfSecurityDisabled(Runnable function) {
    if (!enabled) {
      function.run();
    }
  }

  public Optional<IamProperties> getIamConfiguration(String issuer) {
    return Optional.ofNullable(iamProperties.get(issuer));
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (enabled) {
      for (Entry<String, IamProperties> iamConfigurationEntry : iamProperties.entrySet()) {
        String issuer = iamConfigurationEntry.getKey();
        IamProperties iamConfiguration = iamConfigurationEntry.getValue();
        OrchestratorProperties orchestratorConfiguration = iamConfiguration.getOrchestrator();
        Assert.notNull(orchestratorConfiguration,
            "Orchestrator OAuth2 client for issuer " + issuer + " must be provided");
        Assert.hasText(orchestratorConfiguration.getClientId(),
            "Orchestrator OAuth2 clientId for issuer " + issuer + " must be provided");
        Assert.hasText(orchestratorConfiguration.getClientSecret(),
            "Orchestrator OAuth2 clientSecret for issuer " + issuer + " must be provided");
        if (orchestratorConfiguration.getScopes().isEmpty()) {
          LOG.warn("No Orchestrator OAuth2 scopes provided for issuer {}", issuer);
        }

        Optional<OidcClientProperties> cluesConfiguration = iamConfiguration.getClues();
        if (cluesConfiguration.isPresent()) {
          Assert.hasText(cluesConfiguration.get().getClientId(),
              "CLUES OAuth2 clientId for issuer " + issuer + " must be provided");
          Assert.hasText(cluesConfiguration.get().getClientSecret(),
              "CLUES OAuth2 clientSecret for issuer " + issuer + " must be provided");
        } else {
          LOG.warn("No CLUES OAuth2 configuration provided for issuer {}", issuer);
        }
      }
      if (iamProperties.keySet().isEmpty()) {
        LOG.warn("Empty OIDC configuration list provided");
      } else {
        LOG.info("OIDC configuration successfully parsed for issuers {}", iamProperties.keySet());
      }
    } else {
      LOG.info("OIDC support is disabled");
    }

  }

  @Data
  @NoArgsConstructor
  public static class IamProperties {

    @NotNull
    @NonNull
    private OrchestratorProperties orchestrator;

    @MonotonicNonNull
    private OidcClientProperties clues;

    public Optional<OidcClientProperties> getClues() {
      return Optional.ofNullable(clues);
    }

    public void setClues(OidcClientProperties clues) {
      Assert.notNull(clues);
      this.clues = clues;
    }
  }

  @Data
  @ToString(exclude = "clientSecret")
  @Validated
  @NoArgsConstructor
  public static class OidcClientProperties {

    @NotNull
    @NonNull
    private String clientId;

    @NotNull
    @NonNull
    private String clientSecret;

  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @Validated
  @NoArgsConstructor
  public static class OrchestratorProperties extends OidcClientProperties {

    @NotNull
    @NonNull
    private List<String> scopes = new ArrayList<>();

  }

}
