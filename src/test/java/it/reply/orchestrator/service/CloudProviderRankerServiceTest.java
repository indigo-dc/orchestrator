package it.reply.orchestrator.service;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import it.reply.orchestrator.config.WebAppConfigurationAware;
import it.reply.orchestrator.dto.ranker.CloudProviderRankerRequest;
import it.reply.orchestrator.dto.ranker.RankedCloudProvider;
import it.reply.utils.json.JsonUtility;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

public class CloudProviderRankerServiceTest extends WebAppConfigurationAware {

  @Autowired
  private CloudProviderRankerService cloudProviderRankerService;

  private MockRestServiceServer mockServer;

  @Autowired
  private RestTemplate restTemplate;

  @Before
  public void setUp() {
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  @Test
  public void doCallback() throws Exception {

    List<RankedCloudProvider> response =
        Arrays.asList(new RankedCloudProvider("Name1", 1, true, ""),
            new RankedCloudProvider("Name2", 0, false, "Error msg"));

    mockServer.expect(requestTo(cloudProviderRankerService.getUrl()))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(JsonUtility.serializeJson(response), MediaType.APPLICATION_JSON));

    List<RankedCloudProvider> result =
        cloudProviderRankerService.getProviderRanking(new CloudProviderRankerRequest());

    assertEquals(response, result);
    mockServer.verify();

  }

}
