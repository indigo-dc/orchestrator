package it.reply.orchestrator.dto.im;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class InfrastructureStatus {

  @JsonProperty("vm_states")
  private Map<String, String> vmStates;
  @JsonProperty("state")
  private String state;

  @JsonProperty("vm_states")
  public Map<String, String> getVmStates() {
    return vmStates;
  }

  @JsonProperty("vm_states")
  public void setVmStates(Map<String, String> vmStates) {
    this.vmStates = vmStates;
  }

  @JsonProperty("state")
  public String getState() {
    return state;
  }

  @JsonProperty("state")
  public void setState(String state) {
    this.state = state;
  }

}