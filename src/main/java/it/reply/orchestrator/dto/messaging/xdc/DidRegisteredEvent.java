package it.reply.orchestrator.dto.messaging.xdc;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DidRegisteredEvent extends GenericEvent {

  public static final String EVENT_TYPE = "did_registered";

  @JsonProperty("event_type")
  @NonNull
  @NotNull
  private String eventType;

  @JsonProperty("scope")
  @NonNull
  @NotNull
  private String scope;

  @JsonProperty("name")
  @NonNull
  @NotNull
  private String name;

}
