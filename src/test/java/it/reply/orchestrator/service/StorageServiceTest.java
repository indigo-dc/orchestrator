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

package it.reply.orchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;

import it.reply.orchestrator.dal.entity.StoragePathEntity;
import it.reply.orchestrator.dal.repository.StorageRepository;

import java.util.HashMap;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

public class StorageServiceTest {


  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Mock
  StorageRepository storageRepository;

  @InjectMocks
  StorageService storageService = new StorageServiceImpl(storageRepository);

  @Test
  public void addStoragePathNew() throws Exception {
    
    Map<String, Object> parameters = new HashMap<>();

    Mockito.when(storageRepository.findByStoragePath("stringUrl1"))
    .thenReturn(null);

    Mockito.when(storageRepository.save(any(StoragePathEntity.class)))
    .thenReturn(new StoragePathEntity("stringUrl1", "template1", null, null, parameters));

    assertThat(storageService.addStoragePath("stringUrl1", "template1")!=(null));

  }

  @Test
  public void addStoragePathExisting() throws Exception {
    
    Map<String, Object> parameters = new HashMap<>();

    Mockito.when(storageRepository.findByStoragePath("stringUrl1"))
    .thenReturn(new StoragePathEntity("stringUrl1", "template1", null, null, parameters));

    Mockito.when(storageRepository.save(new StoragePathEntity("stringUrl1", "template1", null, null, parameters)))
    .thenReturn(new StoragePathEntity("stringUrl1", "template1", null, null, parameters));

    assertThat(storageService.addStoragePath("stringUrl1", "template1") == null);

  }

}
