package it.reply.orchestrator.dto.messaging.rucio;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  property = "event_type",
  defaultImpl = GenericEvent.class,
  visible = true,
  include = JsonTypeInfo.As.EXISTING_PROPERTY
)
@JsonSubTypes({
  @JsonSubTypes.Type(
    value = RuleOkEvent.class,
    name = RuleOkEvent.EVENT_TYPE
  ),
  @JsonSubTypes.Type(
    value = TransferFailedEvent.class,
    name = TransferFailedEvent.EVENT_TYPE
  )
})
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Event<T extends EventPayload> {

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
  private T payload;

}

