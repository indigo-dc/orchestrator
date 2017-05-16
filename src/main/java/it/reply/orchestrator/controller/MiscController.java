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

package it.reply.orchestrator.controller;

import it.reply.domain.dsl.info.DebugInformation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;

@RestController
@PropertySource("classpath:version.properties")
@Slf4j
public class MiscController {

  @Value("${build.version}")
  private String projectVersion;

  @Value("${build.revision}")
  private String projectRevision;

  @Value("${build.timestamp}")
  private String projectTimestamp;

  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/", method = RequestMethod.GET)
  public String getOrchestrator() {
    return "INDIGO-Orchestrator";
  }

  /**
   * Get the orchestrator status info.
   * 
   * @return the orchestrator status info.
   */
  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/info", method = RequestMethod.GET)
  public DebugInformation getInfo() {
    DebugInformation info = new DebugInformation();
    info.setProjectVersion(projectVersion);
    info.setProjectRevision(projectRevision);
    info.setProjectTimestamp(projectTimestamp);

    String hostname;
    try {
      hostname = InetAddress.getLocalHost().getHostName();
    } catch (Exception ex) {
      LOG.warn("Error retrieving hostname", ex);
      hostname = "-NOT AVAILABLE-";
    }
    info.setServerHostname(hostname);

    return info;
  }

}
