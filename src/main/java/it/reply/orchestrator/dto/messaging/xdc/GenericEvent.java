package it.reply.orchestrator.dto.messaging.xdc;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.reply.orchestrator.dto.messaging.rucio.RuleOkEvent;
import it.reply.orchestrator.dto.messaging.rucio.TransferFailedEvent;

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  property = "event_type",
  defaultImpl = GenericEvent.class,
  visible = true,
  include = JsonTypeInfo.As.EXISTING_PROPERTY
)
@JsonSubTypes({
  @JsonSubTypes.Type(
    value = DidRegisteredEvent.class,
    name = DidRegisteredEvent.EVENT_TYPE
  )
})
public abstract class GenericEvent {
}
