package it.reply.orchestrator.dto.monitoring;

import it.reply.domain.dsl.prisma.restprotocol.Meta;
import it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.MonitoringWrappedResponsePaas;

import java.io.Serializable;

public class MonitoringResponse implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  private Meta meta;
  private MonitoringWrappedResponsePaas result;

  public Meta getMeta() {
    return meta;
  }

  public void setMeta(Meta meta) {
    this.meta = meta;
  }

  public MonitoringWrappedResponsePaas getResult() {
    return result;
  }

  public void setResult(MonitoringWrappedResponsePaas result) {
    this.result = result;
  }

}
