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

package it.reply.orchestrator.dto.security;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.NonTypedScalarSerializerBase;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

public class Jackson2ScopeSerializer extends NonTypedScalarSerializerBase<Set<String>> {

  private static final long serialVersionUID = 1L;

  protected Jackson2ScopeSerializer() {
    super(Set.class, false);
  }

  @Override
  public boolean isEmpty(SerializerProvider provider, Set<String> value) {
    return value == null || value
        .stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .count() == 0;
  }

  @Override
  public void serialize(Set<String> value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeString(value
        .stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .collect(Collectors.joining(" ")));
  }

}
