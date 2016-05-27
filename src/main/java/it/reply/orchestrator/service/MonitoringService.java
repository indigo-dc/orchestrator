package it.reply.orchestrator.service;

import it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.MonitoringWrappedResponsePaas;

public interface MonitoringService {

  public MonitoringWrappedResponsePaas getProviderData(String providerId);

}
