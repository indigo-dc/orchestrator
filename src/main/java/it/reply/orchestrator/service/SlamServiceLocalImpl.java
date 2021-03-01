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

package it.reply.orchestrator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.reply.orchestrator.annotation.ServiceVersion;
import it.reply.orchestrator.config.properties.SlamProperties;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.slam.SlamPreferences;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
@ServiceVersion(SlamServiceLocalImpl.SERVICE_VERSION)
public class SlamServiceLocalImpl implements SlamService {

  public static final String SERVICE_VERSION = "local";

  private final OAuth2TokenService oauth2TokenService;
  private final SlamProperties slamProperties;
  private final ObjectMapper objectMapper;
  private final ResourceLoader resourceLoader;

  /**
   * Creates a new SlamServiceLocalImpl.
   * @param slamProperties the slamProperties
   * @param oauth2TokenService the oauth2TokenService
   * @param objectMapper the objectMapper
   * @param resourceLoader the ResourceLoader
   */
  public SlamServiceLocalImpl(SlamProperties slamProperties, OAuth2TokenService oauth2TokenService,
      ObjectMapper objectMapper, ResourceLoader resourceLoader)  {
    this.oauth2TokenService = oauth2TokenService;
    this.slamProperties = slamProperties;
    this.objectMapper = objectMapper;
    this.resourceLoader = resourceLoader;
  }

  private Map<String, SlamPreferences> loadData() {
    String location = slamProperties.getUrl().toString();
    Resource serializedPreferences = resourceLoader
        .getResource(location);
    try (InputStream is = serializedPreferences.getInputStream()) {
      TypeReference<HashMap<String, SlamPreferences>> typeRef
          = new TypeReference<HashMap<String, SlamPreferences>>() {};
      return objectMapper.readValue(is, typeRef);
    } catch (IOException e) {
      throw new OrchestratorException("Error loading local SLAM preferences from " + location, e);
    }
  }

  @Override
  public SlamPreferences getCustomerPreferences(OidcTokenId tokenId, @Nullable String userGroup) {
    String slamCustomer = Optional.ofNullable(userGroup)
        .orElse(oauth2TokenService.getOrganization(tokenId));

    return loadData().get(slamCustomer);
  }
}
