package it.reply.orchestrator.service;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

import it.reply.orchestrator.config.WebAppConfigurationAware;
import it.reply.orchestrator.dto.slam.SlamPreferences;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

public class SlamServiceTest extends WebAppConfigurationAware {

  @Autowired
  private SlamService service;

  // private MockRestServiceServer mockServer;

  @Autowired
  private RestTemplate restTemplate;

  @Before
  public void setUp() {
    // mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  @Test
  public void getPreferencesTest() throws Exception {

    // mockServer.expect(requestTo("http://test-server.com")).andExpect(method(HttpMethod.POST))
    // .andRespond(withSuccess());

    SlamPreferences pref = service.getCustomerPreferences();

    assertEquals("4401ac5dc8cfbbb737b0a02575ee3b58", pref.getSla().get(0).getId());

    // mockServer.verify();

  }

}
