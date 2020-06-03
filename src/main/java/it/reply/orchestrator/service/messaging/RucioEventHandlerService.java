/*
 * Copyright © 2015-2020 Santer Reply S.p.A.
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
 */

package it.reply.orchestrator.service.messaging;

import it.reply.orchestrator.dto.messaging.rucio.Event;
import it.reply.orchestrator.dto.messaging.rucio.RuleOkEvent;
import it.reply.orchestrator.utils.MdcUtils;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RucioEventHandlerService {

  @Autowired
  private RuntimeService wfService;

  /**
   * Handle a Rucio Event.
   * @param rucioEvent the event
   */
  public void handleEvent(Event<?> rucioEvent) {
    try (MdcUtils.MdcCloseable requestId = MdcUtils
        .setRequestIdCloseable(UUID.randomUUID().toString())) {
      if (rucioEvent instanceof RuleOkEvent) {
        LOG.debug(rucioEvent.toString());
        RuleOkEvent ruleOkEvent = (RuleOkEvent) rucioEvent;
        String wfEventName = String
            .format("%s-%s", ruleOkEvent.getEventType(), ruleOkEvent.getPayload().getRuleId());
        wfService.signalEventReceivedAsync(wfEventName);
      }
    }
  }
}
