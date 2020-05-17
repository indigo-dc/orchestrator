package it.reply.orchestrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.infn.ba.xdc.rucio.client.Rucio;
import it.infn.ba.xdc.rucio.client.RucioClient;
import it.reply.orchestrator.dto.rucio.RucioRuleEvent;
import java.io.IOException;
import java.util.Objects;
import javax.jms.JMSException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RucioMessageReceiver {

  public static final String DISCRIMINATOR_PROPERTY = "event_type";

  @Autowired
  private ObjectMapper objectMapper;

//    public void processMessage(StompJmsMessage message) {
//      try {
//        String eventType = Objects.toString(message.getStringProperty(DISCRIMINATOR_PROPERTY));
//        String content = message.getContent().utf8().toString();
//        switch (eventType) {
//          case RucioRuleEvent.RULE_OK:
//            RucioRuleEvent ruleEvent = objectMapper.readValue(content, RucioRuleEvent.class);
//            LOG.debug("Received Rule Event message {}", ruleEvent);
//            break;
//          default:
//            LOG.debug("Ignored message <{}> with unknown eventType <{}>", content, eventType);
//            break;
//        }
//
//      } catch (RuntimeException | IOException | JMSException ex) {
//        LOG.error("Error while handling message {}", message.getMessageID(), ex);
//      }

//      String token = "eyJraWQiOiJyc2ExIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiI0MTlmYTUzMy0wMWZkLTRiNGYtODYzZi0yNzlkZTg2ZGJkNGEiLCJpc3MiOiJodHRwczpcL1wvaWFtLmV4dHJlbWUtZGF0YWNsb3VkLmV1XC8iLCJncm91cHMiOlsiWERDIl0sInByZWZlcnJlZF91c2VybmFtZSI6ImEuYnJpZ2FuZGkiLCJvcmdhbmlzYXRpb25fbmFtZSI6ImVYdHJlbWUtRGF0YUNsb3VkIiwiY2xpZW50X2lkIjoiYTg4ZjdjYWEtMDQwOC00ZGY5LWI3ZDMtOWY2OWVlM2Q0Njc4IiwiYXVkIjoicnVjaW8iLCJhY3QiOnsic3ViIjoiYTg4ZjdjYWEtMDQwOC00ZGY5LWI3ZDMtOWY2OWVlM2Q0Njc4In0sIm5iZiI6MTU4ODU5OTA1OSwic2NvcGUiOiJvcGVuaWQgb2ZmbGluZV9hY2Nlc3MgcHJvZmlsZSIsIm5hbWUiOiJBbGJlcnRvIEJyaWdhbmTDrCIsImV4cCI6MTU4ODYwMjY1OSwiaWF0IjoxNTg4NTk5MDU5LCJqdGkiOiJlNmM2OWFkOC0zZjBiLTQyZDQtYTE1ZS1mYzUyZjg4NjdhYjkifQ.tD8F_ejNtoSzJ9-hGlLuwY0VafTWmwhTQUO7NyyHeB8pIfOiiZ7AN-WGCj8wtxtV3jgzeXb7ZUyHnb-S5RZtTQVfNa5Na1ehKM-IMDkGKK4BqiwV8vW2h99nM6Qf2pEP4lUhVZNVF1OpTBEhTUvCTU29IEtnlUKWQUXJU4GPWsg";
//      Rucio client = RucioClient.getInstanceWithOidcAuth("https://rucio-doma.cern.ch", token);
//      client.getRule("d58d64c39f814667bc70ed8df305d04d");

}
