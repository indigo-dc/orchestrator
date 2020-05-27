package it.reply.orchestrator.dto.messaging.rucio;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RuleOkEvent extends Event<RuleOkEvent.RuleOkEventPayload> {

  public static final String EVENT_TYPE = "rule_ok";

  public RuleOkEvent(@NonNull String eventType, @NonNull String createdAt, @NonNull RuleOkEventPayload payload) {
    super(eventType, createdAt, payload);
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class RuleOkEventPayload implements EventPayload {

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
