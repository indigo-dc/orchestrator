/*
 * Copyright © 2015-2021 I.N.F.N.
 * Copyright © 2015-2020 Santer Reply S.p.A.
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

package it.reply.orchestrator.service.deployment.providers.factory;

import feign.Feign;
import feign.Logger.Level;
import feign.RequestInterceptor;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.slf4j.Slf4jLogger;

import it.infn.ba.indigo.chronos.client.Chronos;
import it.infn.ba.indigo.chronos.client.utils.ChronosException;
import it.reply.orchestrator.dto.cmdb.ChronosService;
import it.reply.orchestrator.service.deployment.providers.CredentialProviderService;

import lombok.extern.slf4j.Slf4j;

import mesosphere.client.common.ModelUtils;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChronosClientFactory extends MesosFrameworkClientFactory<ChronosService, Chronos> {

  public ChronosClientFactory(CredentialProviderService credProvServ) {
    super(credProvServ);
  }

  @Override
  public Chronos build(String chronosEndpoint, RequestInterceptor authInterceptor) {
    LOG.info("Generating Chronos client with endpoint {}", chronosEndpoint);

    return Feign
        .builder()
        .encoder(new GsonEncoder(ModelUtils.GSON))
        .decoder(new GsonDecoder(ModelUtils.GSON))
        .logger(new Slf4jLogger(Chronos.class))
        .logLevel(Level.FULL)
        .errorDecoder(
            (methodKey, response) -> new ChronosException(response.status(), response.reason()))
        .requestInterceptor(authInterceptor)
        .requestInterceptor(template -> {
          template.header(HttpHeaders.ACCEPT, "application/json");
          template.header(HttpHeaders.CONTENT_TYPE, "application/json");
        })
        .target(Chronos.class, chronosEndpoint);
  }

}
