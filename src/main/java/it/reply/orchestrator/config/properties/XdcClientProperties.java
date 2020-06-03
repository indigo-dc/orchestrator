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
  private String host;
  private int port;
  private String rucioDestination = "/queue/Consumer.xdc.doma.rucio.events";
  private String xdcDestination = "/queue/doma.orchestrator.events";
  private String username;
  private String password;
  private boolean enabled = false;
}
