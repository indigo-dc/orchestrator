package it.reply.orchestrator.dto.im;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class InfrastructureStatus {

  @JsonProperty("vm_states")
  private Map<String, String> vmStates;
  @JsonProperty("state")
  private String state;

  /**
   * 
   * @return The vmStates
   */
  @JsonProperty("vm_states")
  public Map<String, String> getVmStates() {
    return vmStates;
  }

  /**
   * 
   * @param vmStates
   *          The vm_states
   */
  @JsonProperty("vm_states")
  public void setVmStates(Map<String, String> vmStates) {
    this.vmStates = vmStates;
  }

  /**
   * 
   * @return The state
   */
  @JsonProperty("state")
  public String getState() {
    return state;
  }

  /**
   * 
   * @param state
   *          The state
   */
  @JsonProperty("state")
  public void setState(String state) {
    this.state = state;
  }

}