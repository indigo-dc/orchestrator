/*
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

package it.reply.orchestrator.service.security.cache;

import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.security.AccessGrant;
import javax.cache.processor.MutableEntry;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GetNewTokenProcessor extends AbstractTokenEntryProcessor {

  @Override
  public AccessGrant process(@NonNull MutableEntry<OidcTokenId, AccessGrant> entry,
                             Object... arguments) {
    OidcTokenId id = entry.getKey();
    LOG.info("Force refresh of access token for {}", id);
    entry.remove();
    return refresh(entry);
  }
}
