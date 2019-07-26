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

import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.onedata.ProviderDetails;
import it.reply.orchestrator.dto.onedata.SpaceDetails;
import it.reply.orchestrator.dto.onedata.Token;
import it.reply.orchestrator.dto.onedata.Tokens;
import it.reply.orchestrator.dto.onedata.UserSpaces;

import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface OneDataService {

  public UserSpaces getUserSpacesId(String oneZoneEndpoint, String oneDataToken);

  public SpaceDetails getSpaceDetailsFromId(String oneZoneEndpoint, String oneDataToken,
      String oneSpaceId);

  public ProviderDetails getProviderDetailsFromId(String oneZoneEndpoint, String oneDataToken,
      String oneProviderId);

  Tokens getOneDataTokens(String oneZoneEndpoint, OidcTokenId oidcTokenId);

  Token generateOneDataToken(String oneZoneEndpoint, OidcTokenId oidcTokenId);

  String getOrGenerateOneDataToken(String oneZoneEndpoint, OidcTokenId oidcTokenId);

  public OneData populateProviderInfo(OneData oneDataParameter,
      Map<String, CloudProvider> cloudProviders,
      @Nullable OidcTokenId requestedWithToken,
      String deploymentId);

}
