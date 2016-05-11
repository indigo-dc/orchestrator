package it.reply.orchestrator.service;

import it.reply.orchestrator.dto.cmdb.Service;

public interface CmdbService {

  public Service getServiceById(String id);

}
