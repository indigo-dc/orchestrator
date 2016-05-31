package it.reply.orchestrator.service;

import it.reply.orchestrator.dto.cmdb.Provider;
import it.reply.orchestrator.dto.cmdb.Service;

public interface CmdbService {

  public Service getServiceById(String id);

  public Provider getProviderById(String id);

  public String getUrl();
}
