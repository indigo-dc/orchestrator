package it.reply.orchestrator.dto.messaging.rucio;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GenericEvent extends Event<GenericEvent.GenericEventPayload> {

  public GenericEvent(@NonNull String eventType, @NonNull String createdAt, @NonNull GenericEventPayload payload) {
    super(eventType, createdAt, payload);
  }

  @Data
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class GenericEventPayload implements EventPayload {

  }
}
