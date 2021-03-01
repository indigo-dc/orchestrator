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

package it.reply.orchestrator.tosca;

import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.advanced.LaxImportParser;
import alien4cloud.tosca.parser.impl.base.BaseParserFactory;
import alien4cloud.tosca.parser.impl.base.MapParser;
import alien4cloud.tosca.parser.impl.base.ScalarParser;

import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.CSARDependencyWithUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

@Component
@Primary
public class NormativeLaxImportParser extends LaxImportParser {

  @Autowired
  private ScalarParser scalarParser;

  @Autowired
  private BaseParserFactory baseParserFactory;

  @Override
  public CSARDependency parse(Node node, ParsingContextExecution context) {
    MapParser<String> mapParser = baseParserFactory.<String>getMapParser(scalarParser, "string");
    return mapParser
        .parse(node, context)
        .entrySet()
        .stream()
        .map(entry -> new CSARDependencyWithUrl(entry.getKey(), "1.0.0", entry.getValue()))
        .findFirst()
        .orElse(null);
  }
}
