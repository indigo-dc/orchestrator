package it.reply.orchestrator.dto.kubernetes;

import it.reply.orchestrator.utils.Named;

import lombok.Data;
import lombok.Getter;

import org.checkerframework.checker.nullness.qual.NonNull;

@Data
public class KubernetesPortMapping {
  
  @Getter
  public enum Protocol implements Named {

    TCP("tcp"),
    UDP("udp"),
    IGMP("igmp");

    private final String name;

    Protocol(String name) {
      this.name = name;
    }

  }

  @NonNull
  private Integer containerPort;

  private Integer servicePort;

  @NonNull
  private Protocol protocol = Protocol.TCP;

}
