package it.reply.orchestrator.dal.repository;

import it.reply.orchestrator.dal.entity.Deployment;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface DeploymentRepository extends PagingAndSortingRepository<Deployment, String> {

}
