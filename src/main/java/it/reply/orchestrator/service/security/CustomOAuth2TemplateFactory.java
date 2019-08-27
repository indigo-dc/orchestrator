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

package it.reply.orchestrator.service.security;

import it.reply.orchestrator.dal.entity.OidcTokenId;

import org.mitre.oauth2.model.RegisteredClient;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.social.oauth2.OAuth2Template;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2TemplateFactory {

  @Autowired
  private OAuth2ConfigurationsService oauth2ConfigurationsService;

  @Autowired
  private RestTemplateBuilder restTemplateBuilder;

  public CustomOAuth2Template generateOAuth2Template(OidcTokenId tokenId) {
    return generateOAuth2Template(tokenId.getOidcEntityId().getIssuer());
  }

  /**
   * Generates a OAuth2Template for a specific issuer.
   * 
   * @param issuer
   *          the issuer for which generate the {@link OAuth2Template}
   * @return the OAuth2Template
   */
  public CustomOAuth2Template generateOAuth2Template(String issuer) {
    ServerConfiguration serverConfiguration =
        oauth2ConfigurationsService.getServerConfiguration(issuer);
    RegisteredClient clientConfiguration =
        oauth2ConfigurationsService.getClientConfiguration(serverConfiguration);
    return new CustomOAuth2Template(serverConfiguration, clientConfiguration, restTemplateBuilder, oauth2ConfigurationsService.getAudience(issuer));

  }
}
