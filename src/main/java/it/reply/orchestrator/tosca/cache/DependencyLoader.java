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

package it.reply.orchestrator.tosca.cache;

import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContextExecution;
import it.reply.orchestrator.config.properties.ToscaProperties;
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.tosca.TemplateParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.CSARDependencyWithUrl;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@AllArgsConstructor
public class DependencyLoader {

  private TemplateParser templateParser;

  private ToscaProperties toscaProperties;

  private RestTemplateBuilder restTemplateBuilder;

  /**
   * Fetch and parse a {@link CSARDependency}.
   * @param dependency the {@link CSARDependency} to load.
   * @return the {@link ArchiveRoot} containing the loaded dependency
   */
  public ArchiveRoot load(CSARDependency dependency) {
    if (dependency instanceof CSARDependencyWithUrl) {
      return loadRemoteDependency((CSARDependencyWithUrl) dependency);
    } else {
      return loadLocalDependency(dependency);
    }
  }

  /**
   * Download and parse a remote {@link CSARDependencyWithUrl}.
   * @param dependency the {@link CSARDependencyWithUrl} to load.
   * @return the {@link ArchiveRoot} containing the loaded dependency
   */
  public ArchiveRoot loadRemoteDependency(CSARDependencyWithUrl dependency) {
    return withNewParsingContext(() -> {
      try {
        URL templateUrl = new URL(dependency.getUrl());
        RestTemplate restTemplate = restTemplateBuilder.build();
        LOG.info("Fetching remote TOSCA dependency {}", templateUrl);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("fetch");
        final String template = restTemplate.getForObject(templateUrl.toString(), String.class);
        stopWatch.stop();
        LOG.info("Fetched remote TOSCA dependency {} in {}ms",
            templateUrl, stopWatch.getLastTaskTimeMillis());
        stopWatch.start("parse");
        ArchiveRoot result = templateParser
            .parse(templateUrl.toString(), templateUrl.getFile(), template);
        stopWatch.stop();
        LOG.info("Parsed remote TOSCA dependency {} in {}ms",
            templateUrl, stopWatch.getLastTaskTimeMillis());
        return result;
      } catch (RestClientException e) {
        throw new ToscaException(
          "Failed to fetch remote TOSCA dependency " + dependency.getUrl(), e);
      } catch (ToscaException e) {
        throw new ToscaException(
          "Failed to parse remote TOSCA dependency " + dependency.getUrl(), e);
      } catch (MalformedURLException e) {
        throw new ToscaException(
          "Invalid import url for remote TOSCA dependency " + dependency, e);
      }
    });
  }

  /**
   * Fetch from storage and parse a local {@link CSARDependency}.
   * @param dependency the {@link CSARDependency} to load.
   * @return the {@link ArchiveRoot} containing the loaded dependency
   */
  public ArchiveRoot loadLocalDependency(CSARDependency dependency) {
    return withNewParsingContext(() -> {
      String templateFileName =
          String.format("%s-%s.yaml", dependency.getName(), dependency.getVersion());
      try {
        Resource templateFile = toscaProperties
            .getDefinitionsFolder()
            .createRelative(templateFileName);
        String templatePath = templateFile.getURI().toString();
        LOG.info("Fetching local TOSCA dependency {}", templatePath);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("fetch");
        final String template;
        try (InputStream in = templateFile.getInputStream()) {
          template = IOUtils.toString(in, Charsets.UTF_8);
        }
        stopWatch.stop();
        LOG.info("Fetched local TOSCA dependency {} in {}ms",
            templatePath, stopWatch.getLastTaskTimeMillis());
        stopWatch.start("parse");
        ArchiveRoot result = templateParser.parse(templatePath, templateFileName, template);
        stopWatch.stop();
        LOG.info("Parsed local TOSCA dependency {} in {}ms",
            templatePath, stopWatch.getLastTaskTimeMillis());
        return result;
      } catch (ToscaException e) {
        throw new ToscaException(
          "Failed to parse local TOSCA dependency " + templateFileName, e);
      } catch (IOException e) {
        throw new ToscaException(
          "Failed to fetch local TOSCA dependency " + templateFileName, e);
      }
    });
  }

  private <T> T withNewParsingContext(Supplier<T> function) {
    ParsingContextExecution.Context previousContext = ParsingContextExecution.get();
    try {
      ParsingContextExecution.init();
      return function.get();
    } finally {
      ParsingContextExecution.destroy();
      if (previousContext != null) {
        ParsingContextExecution.set(previousContext);
      }
    }
  }
}
