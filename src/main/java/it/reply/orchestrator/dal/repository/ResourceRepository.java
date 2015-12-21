package it.reply.orchestrator.dal.repository;

import it.reply.orchestrator.dal.entity.Resource;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceRepository extends PagingAndSortingRepository<Resource, String> {

}
