package it.reply.orchestrator.service;

import it.reply.orchestrator.dto.cmdb.Image;
import it.reply.orchestrator.dto.cmdb.Provider;
import it.reply.orchestrator.dto.cmdb.Service;

import java.util.List;

public interface CmdbService {

  public Service getServiceById(String id);

  public Provider getProviderById(String id);

  public List<Image> getImagesByProvider(String providerId);

  public String getUrl();
}
