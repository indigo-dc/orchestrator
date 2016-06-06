package it.reply.orchestrator.service;

import it.reply.orchestrator.dto.ranker.CloudProviderRankerRequest;
import it.reply.orchestrator.dto.ranker.RankedCloudProvider;

import java.util.List;

public interface CloudProviderRankerService {

  public List<RankedCloudProvider>
      getProviderRanking(CloudProviderRankerRequest cloudProviderRankerRequest);

  public String getUrl();
}
