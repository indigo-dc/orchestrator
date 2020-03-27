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

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.exception.NotFoundException;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContextExecution;
import it.reply.orchestrator.tosca.cache.TemplateCacheService;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.normative.ToscaNormativeImports;
import org.springframework.context.annotation.Primary;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

@Service
@Primary
@AllArgsConstructor
@Slf4j
public class RemoteRepositoryServiceImpl implements ICSARRepositorySearchService {

  private static ThreadLocal<Boolean> recursiveCall = new ThreadLocal<>();

  private TemplateCacheService templateCacheService;

  @Override
  public Csar getArchive(CSARDependency dependency) {
    ParsingContextExecution.Context context = ParsingContextExecution.get();
    if (context.getFileName().contains(ToscaNormativeImports.TOSCA_NORMATIVE_TYPES)) {
      return null;
    }
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

  @Override
  public <T extends AbstractToscaType> T getElementInDependencies(Class<T> elementClass,
      Set<CSARDependency> dependencies, String... keyValueFilters) {
    return (T) ToscaContext.get().getElement(elementClass, element -> {
      if (!dependencies
          .contains(new CSARDependency(element.getArchiveName(), element.getArchiveVersion()))) {
        return false;
      }

      EvaluationContext context = new StandardEvaluationContext(element);

      for (int i = 0; i < keyValueFilters.length; i += 2) {
        String field = keyValueFilters[0];
        String value = keyValueFilters[1];
        if (!value.equals(context.lookupVariable(field).toString())) {
          return false;
        }
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
    return templateCacheService.get(dependency);
  }

}
