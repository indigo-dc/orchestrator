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

package it.reply.orchestrator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.reply.orchestrator.dto.SystemEndpoints;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.service.ConfigurationService;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class MiscController {

  @Autowired
  private ServerProperties serverProperties;
  
  @Autowired
  private ConfigurationService configurationService;
  
  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/", method = RequestMethod.GET)
  public String getRoot() {
    return serverProperties.getDisplayName();
  }

  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/configuration", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public CharSequence getConfiguration() {
    SystemEndpoints enpoints = configurationService.getConfiguration();
    
    ObjectMapper Obj = new ObjectMapper(); 

    try { 
      // get SystemEndpoints object as a json string 
      return  Obj.writeValueAsString(enpoints); 
    } catch (IOException ex) { 
      throw new OrchestratorException("Error serializing system endpoints", ex);
    }     
  }
}
