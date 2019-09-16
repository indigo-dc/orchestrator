/*
 * Copyright © 2015-2019 Santer Reply S.p.A.
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

package it.reply.orchestrator.service;

import static org.junit.Assert.assertEquals;

import it.reply.orchestrator.config.specific.WebAppConfigurationAwareIT;
import it.reply.orchestrator.dto.slam.SlamPreferences;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This integration test makes real request to the SLAM APIs.
 *
 * @author l.biava
 *
 */
public class SlamServiceIT extends WebAppConfigurationAwareIT {

  @Autowired
  private SlamService service;

  @Test
  @Ignore
  public void getPreferencesTest() throws Exception {
    SlamPreferences pref = service.getCustomerPreferences(null);
    assertEquals("4401ac5dc8cfbbb737b0a02575ee3b58", pref.getSla().get(0).getId());
  }

}
