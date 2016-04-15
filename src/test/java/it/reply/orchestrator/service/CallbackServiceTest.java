package it.reply.orchestrator.service;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

import it.reply.orchestrator.config.WebAppConfigurationAware;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@DatabaseTearDown("/data/database-empty.xml")
public class CallbackServiceTest extends WebAppConfigurationAware {

  @Autowired
  private CallbackService service;

  private MockRestServiceServer mockServer;

  @Autowired
  private RestTemplate restTemplate;

  @Before
  public void setUp() {
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  @Test
  @DatabaseSetup("/data/database-init.xml")
  public void doCallback() throws Exception {

    mockServer.expect(requestTo("http://test-server.com")).andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess());

    boolean result = service.doCallback("mmd34483-d937-4578-bfdb-ebe196bf82dd");

    assertEquals(true, result);
    mockServer.verify();

  }

  @Test
  @DatabaseSetup("/data/database-init.xml")
  public void doCallbackDeploymentNotExist() throws Exception {
    boolean result = service.doCallback("mmd34483-d937-4578-bfdb-ebe196bf82de");
    assertEquals(false, result);

  }

}
