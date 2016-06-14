package it.reply.orchestrator.command;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

import it.reply.orchestrator.config.WebAppConfigurationAware;
import it.reply.orchestrator.service.commands.Notify;
import it.reply.workflowmanager.utils.Constants;

import org.junit.Before;
import org.junit.Test;
import org.kie.api.executor.CommandContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@DatabaseTearDown("/data/database-empty.xml")
public class NotifyCommandTest extends WebAppConfigurationAware {

  @Autowired
  private Notify notifyCommand;

  private MockRestServiceServer mockServer;

  @Autowired
  private RestTemplate restTemplate;

  @Before
  public void setUp() {
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  @Test
  @DatabaseSetup("/data/database-init.xml")
  public void doexecuteSuccesfully() throws Exception {

    mockServer.expect(requestTo("http://test-server.com")).andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess());

    CommandContext ctx = TestCommandHelper.buildCommandContext()
        .withParam("DEPLOYMENT_ID", "mmd34483-d937-4578-bfdb-ebe196bf82dd").get();
    boolean result = (boolean) notifyCommand.execute(ctx).getData(Constants.RESULT);

    assertEquals(true, result);
    mockServer.verify();

  }

  @Test
  @DatabaseSetup("/data/database-init.xml")
  public void doexecuteWithoutUrl() throws Exception {
    CommandContext ctx = TestCommandHelper.buildCommandContext()
        .withParam("DEPLOYMENT_ID", "mmd34483-d937-4578-bfdb-ebe196bf82de").get();
    boolean result = (boolean) notifyCommand.execute(ctx).getData(Constants.RESULT);
    assertEquals(false, result);

  }

}
