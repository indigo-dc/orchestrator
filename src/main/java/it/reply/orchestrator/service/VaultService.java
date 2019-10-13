/*
 * Copyright © 2019 I.N.F.N.
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

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.support.VaultResponse;

public interface VaultService {

  public Optional<URI> getServiceUri();

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

  public TokenAuthentication retrieveToken(String accessToken);

  public TokenAuthentication retrieveToken(URI uri, String accessToken);

  public TokenAuthentication retrieveToken(OidcTokenId oidcTokenId);

  public TokenAuthentication retrieveToken(URI uri, OidcTokenId oidcTokenId);

}
