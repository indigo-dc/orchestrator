/*
 * Copyright © 2019-2020 the original author or authors.
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
 *
 */

package it.reply.orchestrator.service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.reply.orchestrator.dto.messaging.xdc.GenericEvent;
import java.io.IOException;
import java.lang.reflect.Type;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;

@Slf4j
@AllArgsConstructor
public class XdcMessageHandler implements StompFrameHandler {

  private final StompSession session;
  private final ObjectMapper objectMapper;
  private final XdcEventHandlerService xdcEventHandlerService;

  @Override
  public Type getPayloadType(StompHeaders headers) {
    return String.class;
  }

  @Override
  public void handleFrame(StompHeaders headers, Object payload) {
    try {
      if (payload != null) {
        GenericEvent xdcEvent = objectMapper.readValue(payload.toString(), GenericEvent.class);
        xdcEventHandlerService.handleEvent(xdcEvent);
      }
    } catch (RuntimeException | IOException ex) {
      LOG.error("Error handling XDC message {}", payload, ex);
      if (headers.getAck() != null) {
        session.acknowledge(headers.getAck(), false);
      }
      return;
    }
    if (headers.getAck() != null) {
      session.acknowledge(headers.getAck(), true);
    }
  }
}
