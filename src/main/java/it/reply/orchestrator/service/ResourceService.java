package it.reply.orchestrator.service;

import it.reply.orchestrator.dal.entity.Resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ResourceService {

  public Page<Resource> getResources(Pageable pageable);

  public Resource getResource(String id);

}
