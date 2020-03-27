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
import javax.cache.processor.EntryProcessor;
import org.springframework.stereotype.Service;

@Service
public class EntryProcessorFactory {

  /**
   * Generate a new {@link EntryProcessor} wrapped by a {@link EntryProcessorMdcDecorator}.
   * @param delegateClass the class of the {@link EntryProcessor} to generate
   * @param <E> the {@link EntryProcessor} type
   * @param <K> the type of the key
   * @param <V> the type of the value
   * @param <T> the type of the return value
   * @return the return value of the {@link EntryProcessor}
   */
  public <E extends EntryProcessor<K, V, T>, K, V, T> EntryProcessorMdcDecorator<E,K,V,T>
      get(Class<E> delegateClass) {
    String requestId = MdcUtils.getRequestId();
    String deploymentId = MdcUtils.getDeploymentId();
    return new EntryProcessorMdcDecorator<>(delegateClass, requestId, deploymentId);
  }
}
