package it.reply.orchestrator.dal.repository;

import it.reply.orchestrator.dal.entity.Deployment;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeploymentRepository extends PagingAndSortingRepository<Deployment, String> {

}
