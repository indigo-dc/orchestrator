/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
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

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

public class Jackson2ScopeDeserializer extends FromStringDeserializer<Set<String>> {

  private static final long serialVersionUID = 3457325186733406722L;

  private static final Pattern SPLITTER = Pattern.compile("[\\s+]");

  public Jackson2ScopeDeserializer() {
    super(Set.class);
  }

  @Override
  protected Set<String> _deserialize(String value, DeserializationContext ctxt) {
    return SPLITTER
        .splitAsStream(value)
        .filter(StringUtils::hasText)
        .map(String::trim)
        .collect(Collectors.toSet());
  }
}
