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

  public void handleEvent(Event<?> rucioEvent) {
    try(MdcUtils.MdcCloseable requestId = MdcUtils.setRequestIdCloseable(UUID.randomUUID().toString())) {
      if (rucioEvent instanceof RuleOkEvent) {
        LOG.debug(rucioEvent.toString());
        RuleOkEvent ruleOkEvent = (RuleOkEvent) rucioEvent;
        String wfEventName = String.format("%s-%s", ruleOkEvent.getEventType(), ruleOkEvent.getPayload().getRuleId());
        wfService.signalEventReceivedAsync(wfEventName);
      }
    }
  }
}
