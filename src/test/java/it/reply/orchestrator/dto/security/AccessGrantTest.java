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

package it.reply.orchestrator.dto.security;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import it.reply.orchestrator.utils.JsonUtils;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import java.io.IOException;
import java.util.Date;

@JsonTest
public class AccessGrantTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Test
  public void testNoExpireAndNoScope()
      throws JsonParseException, JsonMappingException, IOException {
    AccessGrant grant = AccessGrant
        .builder()
        .accessToken("token")
        .tokenType("Bearer")
        .build();

    AccessGrant actual = JsonUtils.deserialize(JsonUtils.serialize(grant), AccessGrant.class);

    assertThat(actual).isEqualToComparingOnlyGivenFields(grant, "accessToken", "tokenType",
        "refreshToken");

    // checking if value gets trimmed
    assertThat(actual.getScopes()).isNotNull().isEmpty();
    assertThat(actual.getExpiration()).isNull();
    assertThat(actual.isExpired()).isFalse();
  }

  @Test
  public void testExpiredAndNoScope() throws JsonParseException, JsonMappingException, IOException {
    AccessGrant grant = AccessGrant
        .builder()
        .accessToken("token")
        .tokenType("Bearer")
        .expiration(new Date(0))
        .build();

    AccessGrant actual = JsonUtils.deserialize(JsonUtils.serialize(grant), AccessGrant.class);

    assertThat(actual).isEqualToComparingOnlyGivenFields(grant, "accessToken", "tokenType",
        "refreshToken");

    // check scopes and expiration separately
    assertThat(actual.getScopes()).isNotNull().isEmpty();
    assertThat(actual.getExpiration()).isBefore(new Date());
    assertThat(actual.isExpired()).isTrue();
  }

  @Test
  public void testNotExpiredAndWithScope()
      throws JsonParseException, JsonMappingException, IOException {
    AccessGrant grant = AccessGrant
        .builder()
        .accessToken("token")
        .tokenType("Bearer")
        .expiration(new Date(Long.MAX_VALUE))
        .build();
    grant.getScopes().add("  openid  "); // checking if value gets trimmed
    grant.getScopes().add("profile");

    AccessGrant actual = JsonUtils.deserialize(JsonUtils.serialize(grant), AccessGrant.class);

    assertThat(actual).isEqualToComparingOnlyGivenFields(grant, "accessToken", "tokenType",
        "refreshToken");

    // check scopes and expiration separately
    assertThat(actual.getScopes()).containsOnly("profile", "openid");
    assertThat(actual.getExpiration()).isCloseTo(grant.getExpiration(), 2_000L);
    assertThat(actual.getExpiration()).isAfter(new Date());
    assertThat(actual.isExpired()).isFalse();
  }

}
