package it.reply.orchestrator.dto.rucio;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.reply.orchestrator.service.RucioMessageReceiver;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RucioRuleEvent {

  public static final String RULE_OK = "rule_ok";

  @JsonProperty("event_type")
  @NonNull
  @NotNull
  private String eventType;

  @JsonProperty("created_at")
  @NonNull
  @NotNull
  private String createdAt;

  @JsonProperty("payload")
  @NonNull
  @NotNull
  private Payload payload;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class Payload {

    @JsonProperty("scope")
    @NonNull
    @NotNull
    private String scope;

    @JsonProperty("name")
    @NonNull
    @NotNull
    private String name;

    @JsonProperty("rule_id")
    @NonNull
    @NotNull
    private String ruleId;
  }
}
