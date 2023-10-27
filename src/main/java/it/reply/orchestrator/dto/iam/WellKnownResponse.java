package it.reply.orchestrator.dto.iam;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class WellKnownResponse {

  private List<String> scopesSupported;
  private String registrationEndpoint;
  private String tokenEndpoint;

  public WellKnownResponse() {
    // Use the setter functions to set the attributes of the class
  }

  public List<String> getScopesSupported() {
    return scopesSupported;
  }

  public void setScopesSupported(List<String> scopesSupported) {
    this.scopesSupported = scopesSupported;
  }

  public String getRegistrationEndpoint() {
    return registrationEndpoint;
  }

  public void setRegistrationEndpoint(String registrationEndpoint) {
    this.registrationEndpoint = registrationEndpoint;
  }

  public String getTokenEndpoint() {
    return tokenEndpoint;
  }

  public void setTokenEndpoint(String tokenEndpoint) {
    this.tokenEndpoint = tokenEndpoint;
  }

}
