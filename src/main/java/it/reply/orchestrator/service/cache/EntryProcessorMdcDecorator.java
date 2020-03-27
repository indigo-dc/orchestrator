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

package it.reply.orchestrator.service.cache;

import it.reply.orchestrator.utils.MdcUtils;
import java.io.Serializable;
import java.util.Objects;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import org.apache.ignite.resources.SpringApplicationContextResource;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.context.ApplicationContext;

class EntryProcessorMdcDecorator<E extends EntryProcessor<K, V, T>, K, V, T>
    implements EntryProcessor<K, V, T>, Serializable {

  private static final long serialVersionUID = 1L;

  private final Class<E> delegateClass;

  private final String requestId;
  private final String deploymentId;

  @SpringApplicationContextResource
  private transient ApplicationContext springCtx;

  protected EntryProcessorMdcDecorator(@NonNull Class<E> delegateClass,
                                    String requestId, String deploymentId) {
    this.delegateClass = Objects.requireNonNull(delegateClass);
    this.requestId = requestId;
    this.deploymentId = deploymentId;
  }

  private E getDelegate() {
    return springCtx.getBean(delegateClass);
  }

  @Override
  public T process(@NonNull MutableEntry<K, V> entry, Object... arguments)
      throws EntryProcessorException {
    try (MdcUtils.CloseableMdcEntry oldDeploymentId = MdcUtils.setDeploymentId(deploymentId)) {
      try (MdcUtils.CloseableMdcEntry oldRequestId = MdcUtils.setRequestId(requestId)) {
        return getDelegate().process(entry, arguments);
      }
    }
  }

}
