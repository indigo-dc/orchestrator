package it.reply.orchestrator.dal.repository;

import it.reply.orchestrator.dal.entity.Resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface ResourceRepository extends PagingAndSortingRepository<Resource, String> {

  public Page<Resource> findByDeployment_id(String deploymentId, Pageable pageable);

  public Resource findByIdAndDeployment_id(String uuid, String deploymentId);

  public Resource findByToscaNodeNameAndDeployment_id(String toscaNodeName, String deploymentId);
}
