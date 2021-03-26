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

import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.vault.TokenAuthenticationExtended;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.support.VaultResponse;

public interface VaultService {

  public Optional<URI> getServiceUri();

  public String getServicePath();

  public VaultResponse writeSecret(ClientAuthentication token, String path, Object secret);

  public VaultResponse writeSecret(URI uri, ClientAuthentication token, String path,
      Object secret);

  public <T> T readSecret(ClientAuthentication token, String path, Class<T> type);

  public <T> T readSecret(URI uri, ClientAuthentication token, String path, Class<T> type);

  public Map<String, Object> readSecret(URI uri, ClientAuthentication token, String path);

  public Map<String, Object> readSecret(ClientAuthentication token, String path);

  public void deleteSecret(ClientAuthentication token, String path);

  public void deleteSecret(URI uri, ClientAuthentication token, String path);

  public List<String> listSecrets(ClientAuthentication token, String path);

  public List<String> listSecrets(URI uri, ClientAuthentication token, String path);

  public TokenAuthenticationExtended retrieveToken(String accessToken);

  public TokenAuthenticationExtended retrieveToken(URI uri, String accessToken);

  public TokenAuthenticationExtended retrieveToken(OidcTokenId oidcTokenId);

  public TokenAuthenticationExtended retrieveToken(URI uri, OidcTokenId oidcTokenId);

}
