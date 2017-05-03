/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.reply.orchestrator.command;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

import it.reply.orchestrator.config.specific.WebAppConfigurationAware;
import it.reply.orchestrator.service.WorkflowConstants;
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
        .withParam(WorkflowConstants.WF_PARAM_DEPLOYMENT_ID, "mmd34483-d937-4578-bfdb-ebe196bf82dd")
        .get();
    boolean result = (boolean) notifyCommand.execute(ctx).getData(Constants.RESULT);

    assertEquals(true, result);
    mockServer.verify();

  }

  @Test
  @DatabaseSetup("/data/database-init.xml")
  public void doexecuteWithoutUrl() throws Exception {
    CommandContext ctx = TestCommandHelper.buildCommandContext()
        .withParam(WorkflowConstants.WF_PARAM_DEPLOYMENT_ID, "mmd34483-d937-4578-bfdb-ebe196bf82de")
        .get();
    boolean result = (boolean) notifyCommand.execute(ctx).getData(Constants.RESULT);
    assertEquals(false, result);

  }

}
