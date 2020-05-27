package it.reply.orchestrator.config.properties;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Slf4j
@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "broker.xdc")
public class XdcClientProperties {
  private String host = "netmon-mb.cern.ch";
  private int port = 61613;
  private String destination = "/queue/Consumer.xdc.doma.orchestrator.events";
  private String username = "domarucioc";
  private String password = "Y2jZ9Xdih2PkNBRZ";
  private boolean enabled = true;
}
