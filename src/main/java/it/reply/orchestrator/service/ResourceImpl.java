package it.reply.orchestrator.service;

import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.exception.http.NotFoudException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ResourceImpl implements ResourceService {

  @Autowired
  private ResourceRepository resourceRepository;

  @Override
  public Page<Resource> getResources(Pageable pageable) {
    return resourceRepository.findAll(pageable);
  }

  @Override
  public Resource getResource(String uuid) {
    if (resourceRepository.exists(uuid)) {
      return resourceRepository.findOne(uuid);
    } else {
      throw new NotFoudException("The resource <" + uuid + "> doesn't exist");
    }
  }

}
