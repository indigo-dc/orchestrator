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

package it.reply.orchestrator.tosca;

import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.ToscaParser;
import alien4cloud.tosca.serializer.VelocityUtil;

import it.reply.orchestrator.config.properties.ToscaProperties;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.ToscaException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
@ComponentScan(basePackages = { "alien4cloud", "org.alien4cloud" })
@EnableConfigurationProperties(ToscaProperties.class)
public class TemplateParser {

  @Autowired
  private ToscaParser toscaParser;

  /**
   * Parse the TOSCA template (string) and get the in-memory representation.<br/>
   * This also checks for validation errors.
   *
   * @param template
   *          the TOSCA template as string.
   * @return an {@link ArchiveRoot} representing the template..
   * @throws ToscaException
   *           if validation errors occur.
   */
  public ArchiveRoot parse(String template) {
    return parse("template", "template", template);
  }

  /**
   * Parse the TOSCA template (string) and get the in-memory representation.<br/>
   * This also checks for validation errors.
   *
   * @param filePath
   *          the path of the TOSCA template file.
   * @param fileName
   *          the name of the TOSCA template file.
   * @param template
   *          the TOSCA template as string.
   * @return an {@link ArchiveRoot} representing the template..
   * @throws ToscaException
   *           if validation errors occur.
   */
  @ToscaContextual
  public ArchiveRoot parse(String filePath, String fileName, String template) {
    try (InputStream in = IOUtils.toInputStream(template, Charsets.UTF_8)) {
      ParsingResult<ArchiveRoot> result = toscaParser.parseFile(fileName, filePath, in, null);
      Optional<ToscaException> parsingException =
          checkParsingErrors(filePath, result.getContext().getParsingErrors());
      if (parsingException.isPresent()) {
        throw parsingException.get();
      }
      return result.getResult();
    } catch (IOException ex) {
      throw new OrchestratorException("Error closing template stream", ex);
    } catch (ParsingException ex) {
      Optional<ToscaException> parsingException =
          checkParsingErrors(filePath, ex.getParsingErrors());
      throw parsingException.orElse(new ToscaException("Error parsing TOSCA template", ex));
    }

  }

  private Optional<ToscaException> checkParsingErrors(String filePath,
      List<ParsingError> parsingError) {
    Map<ParsingErrorLevel, List<ParsingError>> parsingErrors = parsingError
        .stream()
        .collect(Collectors.groupingBy(ParsingError::getErrorLevel));
    List<ParsingError> warnings = parsingErrors.get(ParsingErrorLevel.WARNING);
    if (!CollectionUtils.isEmpty(warnings) && LOG.isWarnEnabled()) {
      LOG.warn("Non-fatal error parsing TOSCA template "
          + filePath
          + warnings
              .stream()
              .map(Object::toString)
              .collect(Collectors.joining("\n", ":\n", "")));
    }
    List<ParsingError> errors = parsingErrors.get(ParsingErrorLevel.ERROR);
    if (!CollectionUtils.isEmpty(errors)) {
      return Optional.of(new ToscaException("Error parsing TOSCA template "
          + filePath
          + errors
              .stream()
              .map(Object::toString)
              .collect(Collectors.joining("\n", ":\n", ""))));
    }
    return Optional.empty();
  }

  /**
   * Obtain the string TOSCA template representation from the in-memory representation.
   *
   * @param archiveRoot
   *          the {@link ArchiveRoot} from which serialize the TOSCA template
   * @return the serialized TOSCA template
   */
  public String serialize(ArchiveRoot archiveRoot) {
    Map<String, Object> velocityCtx = new HashMap<>();
    velocityCtx.put("tosca_definitions_version",
        archiveRoot.getArchive().getToscaDefinitionsVersion());
    velocityCtx.put("template_description", archiveRoot.getArchive().getDescription());
    velocityCtx.put("template_name", "template");
    velocityCtx.put("template_version", "1.0.0-SNAPSHOT");
    velocityCtx.put("template_author", "orchestrator");
    velocityCtx.put("topology",
        Optional.ofNullable(archiveRoot.getTopology()).orElseGet(Topology::new));

    try (StringWriter writer = new StringWriter()) {
      VelocityUtil.generate("org/alien4cloud/tosca/exporter/topology-tosca_simple_yaml_1_0.yml.vm",
          writer, velocityCtx);
      String template = writer.toString();
      LOG.debug(template);
      return template;
    } catch (IOException ex) {
      throw new OrchestratorException("Error serializing TOSCA template", ex);
    }

  }

}
