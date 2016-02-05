package it.reply.orchestrator.dal.repository;

import it.reply.orchestrator.dal.entity.Resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceRepository extends PagingAndSortingRepository<Resource, String> {

  public Page<Resource> findByDeployment_id(String deploymentId, Pageable pageable);

  public Resource findByIdAndDeployment_id(String uuid, String deploymentId);
}
