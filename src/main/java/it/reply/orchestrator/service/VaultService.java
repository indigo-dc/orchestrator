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

import java.util.List;
import java.util.Map;

import org.springframework.vault.support.VaultResponse;

public interface VaultService {

  public VaultResponse writeSecret(String token, String path, Object secret);

  public <T> T readSecret(String token, String path, Class<T> type);

  public Map<String,Object> readSecret(String token, String path);

  public void deleteSecret(String token, String path);

  public List<String> listSecrets(String token, String path);

  public String retrieveToken(String accessToken);
}