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

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.exception.NotFoundException;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.context.ToscaContext.Context;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContextExecution;

import it.reply.orchestrator.config.properties.ToscaProperties;
import it.reply.orchestrator.exception.service.ToscaException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.CSARDependencyWithUrl;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Primary
@AllArgsConstructor
@Slf4j
public class RemoteRepositoryServiceImpl implements ICSARRepositorySearchService {

  private static ThreadLocal<Boolean> recursiveCall = new ThreadLocal<>();

  private TemplateParser templateParser;

  private ToscaProperties toscaProperties;

  private RestTemplateBuilder restTemplateBuilder;

  @Override
  public Csar getArchive(CSARDependency dependency) {
    ArchiveRoot root = parseAndRegister(dependency);
    return root == null ? null : root.getArchive();
  }

  @Override
  public List<Csar> getCsarsByName(String archiveName, int numResults) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isElementExistInDependencies(Class<? extends AbstractToscaType> elementClass,
      String elementId, Set<CSARDependency> dependencies) {
    return getElementInDependencies(elementClass, elementId, dependencies) != null;
  }

  @Override
  public <T extends AbstractToscaType> T getRequiredElementInDependencies(Class<T> elementClass,
      String elementId, Set<CSARDependency> dependencies) {
    T element = getElementInDependencies(elementClass, elementId, dependencies);
    if (element == null) {
      throw new NotFoundException(
          "Element elementId: <" + elementId + "> of type <" + elementClass.getSimpleName()
              + "> cannot be found in dependencies " + dependencies);
    }
    return element;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends AbstractToscaType> T getElementInDependencies(Class<T> elementClass,
      Set<CSARDependency> dependencies, String... keyValueFilters) {
	  LOG.info("elementClass: {}", elementClass);
	  Context mycontext = ToscaContext.get();

	  Predicate<AbstractToscaType> filter = e -> {
		  LOG.info("My elementId: {}", e.getElementId());
		  EvaluationContext context = new StandardEvaluationContext(e);

		  LOG.info("Class: {}", context.getRootObject().getValue());


		  String field = keyValueFilters[0];
	      String value = keyValueFilters[1];

	      if (context.lookupVariable(field) != null) {

	    	  LOG.info("NOT NULL");
	      }

		  return false;

	  };

	  mycontext.getElement(elementClass, filter);

    return (T) ToscaContext.get().getElement(elementClass, element -> {
      if (!dependencies
          .contains(new CSARDependency(element.getArchiveName(), element.getArchiveVersion()))) {
        return false;
      }

      LOG.info("keyValueFilters.length: {}", keyValueFilters.length);
      LOG.info("element: {}", element.getElementId());

      EvaluationContext context = new StandardEvaluationContext(element);

      for (int i = 0; i < keyValueFilters.length; i += 2) {
        String field = keyValueFilters[0];
        String value = keyValueFilters[1];
        LOG.info("i={}, value={}, field={}", i, value, field);

        if (context.lookupVariable(field) == null ) {
        	return false;
        }
        else {
        	 // !value.equals(context.lookupVariable(field).toString())){
        	LOG.info("context.lookupVariable(field): {}", context.lookupVariable(field).toString());
        }

/*        try {

          if (!value.equals(context.lookupVariable(field).toString())) {
            return false;
          }
        }
        catch(Exception e) {
        	LOG.info("ERROR!!");
        }*/
      }
      return true;
    }).orElse(null);
  }

  @Override
  public <T extends AbstractToscaType> T getElementInDependencies(Class<T> elementClass,
      String elementId, Set<CSARDependency> dependencies) {
    if (recursiveCall.get() == null) {
      recursiveCall.set(true);
      try {
        // ensure that dependencies are loaded in the ToscaContext
        for (CSARDependency dependency : dependencies) {
          // parse and register the archive from local repository.
          if (!ToscaContext.get().getDependencies().contains(dependency)) {
            parseAndRegister(dependency);
          }
        }
        return ToscaContext.get(elementClass, elementId);
      } finally {
        recursiveCall.remove();
      }
    } else {
      return null;
    }
  }

  private ArchiveRoot parseAndRegister(CSARDependency dependency) {
    // parse and load archive.
    ArchiveRoot archiveRoot = parse(dependency);
    ToscaContext.Context context = ToscaContext.get();
    context.register(archiveRoot);
    return archiveRoot;
  }

  private ArchiveRoot parse(CSARDependency dependency) {
    if (dependency instanceof CSARDependencyWithUrl) {
      return withNewParsingContext(() -> parseRemoteTemplate((CSARDependencyWithUrl) dependency));
    } else {
      return withNewParsingContext(() -> parseLocalTemplate(dependency));
    }

  }

  private ArchiveRoot parseLocalTemplate(CSARDependency dependency) {
    String templateFileName =
        String.format("%s-%s.yaml", dependency.getName(), dependency.getVersion());
    try {
      Resource templateFile = toscaProperties
          .getDefinitionsFolder()
          .createRelative(templateFileName);
      String templatePath = templateFile.getURI().toString();
      LOG.info("Fetching local TOSCA dependency {}", templatePath);
      String template;
      try (InputStream in = templateFile.getInputStream()) {
        template = IOUtils.toString(in, Charsets.UTF_8);
      }
      LOG.info("Fetched local TOSCA dependency {}", templatePath);
      ArchiveRoot result = templateParser.parse(templatePath, templateFileName, template);
      LOG.info("Parsed local TOSCA dependency {}", templatePath);
      return result;
    } catch (ToscaException e) {
      throw new ToscaException(
          "Failed to parse local TOSCA dependency " + templateFileName, e);
    } catch (IOException e) {
      throw new ToscaException(
          "Failed to fetch local TOSCA dependency " + templateFileName, e);
    }
  }

  private ArchiveRoot parseRemoteTemplate(CSARDependencyWithUrl dependency) {
    try {
      URL templateUrl = new URL(dependency.getUrl());
      RestTemplate restTemplate = restTemplateBuilder.build();
      LOG.info("Fetching remote TOSCA dependency {}", templateUrl);
      String template = restTemplate.getForObject(templateUrl.toString(), String.class);
      LOG.info("Fetched remote TOSCA dependency {}", templateUrl);
      ArchiveRoot result = templateParser
          .parse(templateUrl.toString(), templateUrl.getFile(), template);
      LOG.info("Parsed remote TOSCA dependency {}", templateUrl);
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
