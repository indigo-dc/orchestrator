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

package it.reply.orchestrator.dto.security;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import it.reply.utils.json.JsonUtility;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;

public class AccessGrantTest {

  @Test
  public void testNoExpireAndNoScope()
      throws JsonParseException, JsonMappingException, IOException {
    AccessGrant grant = new AccessGrant("token", "Bearer");
    AccessGrant actual =
        JsonUtility.deserializeJson(JsonUtility.serializeJson(grant), AccessGrant.class);
    Assertions.assertThat(grant).isEqualTo(actual);
    Assertions.assertThat(actual.getScope()).isNotNull().isEmpty();
    Assertions.assertThat(actual.getExpiration()).isNull();
    Assertions.assertThat(actual.isExpired()).isFalse();
  }

  @Test
  public void testExpiredAndNoScope() throws JsonParseException, JsonMappingException, IOException {
    AccessGrant grant = new AccessGrant("token", "Bearer");
    grant.setExpiration(new Date(0));
    AccessGrant actual =
        JsonUtility.deserializeJson(JsonUtility.serializeJson(grant), AccessGrant.class);
    Assertions.assertThat(actual).isEqualToIgnoringGivenFields(
        JsonUtility.deserializeJson(JsonUtility.serializeJson(grant), AccessGrant.class),
        // real value is lost between conversion for expired grant
        // see https://tools.ietf.org/html/rfc6749#section-3.3
        "expiration");
    Assertions.assertThat(actual.getScope()).isNotNull().isEmpty();
    Assertions.assertThat(actual.getExpiration()).isBefore(new Date());
    Assertions.assertThat(actual.isExpired()).isTrue();
  }

  @Test
  public void testNotExpiredAndWithScope()
      throws JsonParseException, JsonMappingException, IOException {
    AccessGrant grant = new AccessGrant("token", "Bearer");
    grant.setExpiration(new Date(Long.MAX_VALUE));
    grant.getScope().add("  openid  ");
    grant.getScope().add("profile");
    AccessGrant actual =
        JsonUtility.deserializeJson(JsonUtility.serializeJson(grant), AccessGrant.class);
    Assertions.assertThat(actual).isEqualToIgnoringGivenFields(
        JsonUtility.deserializeJson(JsonUtility.serializeJson(grant), AccessGrant.class),
        // real value is lost between conversion for expired grant
        // see https://tools.ietf.org/html/rfc6749#section-3.3
        "expiration");
    Assertions.assertThat(actual.getExpiration()).isAfter((new Date()));
    Assertions.assertThat(actual.isExpired()).isFalse();
    Assertions.assertThat(actual.getScope()).contains("profile", "openid");
  }

}
