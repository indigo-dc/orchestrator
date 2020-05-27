package it.reply.orchestrator.service.messaging;

import it.reply.orchestrator.dto.messaging.xdc.DidRegisteredEvent;
import it.reply.orchestrator.dto.messaging.xdc.GenericEvent;
import it.reply.orchestrator.service.DeploymentScheduleServiceImpl;
import it.reply.orchestrator.utils.MdcUtils;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class XdcEventHandlerService {

  @Autowired
  private DeploymentScheduleServiceImpl deploymentScheduleService;

  public void handleEvent(GenericEvent xdcEvent) {
    if (xdcEvent instanceof DidRegisteredEvent) {
      DidRegisteredEvent didRegisteredEvent = (DidRegisteredEvent) xdcEvent;
      deploymentScheduleService.createDeploymentScheduleEvents(didRegisteredEvent.getScope(), didRegisteredEvent.getName());
    }
  }
}
