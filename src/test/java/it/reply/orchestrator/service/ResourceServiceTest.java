package it.reply.orchestrator.service;

/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
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

import static org.junit.Assert.assertEquals;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

import it.reply.orchestrator.config.specific.WebAppConfigurationAware;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.exception.http.NotFoundException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

@DatabaseTearDown("/data/database-empty.xml")
@Transactional
public class ResourceServiceTest extends WebAppConfigurationAware {

  @Autowired
  private ResourceService service;

  @Test(expected = NotFoundException.class)
  public void getResourcesNotFoundDeployment() throws Exception {
    service.getResources("not-found", null);
  }

  @Test
  @DatabaseSetup("/data/database-resource-init.xml")
  public void getResources() throws Exception {
    Page<Resource> resources = service.getResources("0748fbe9-6c1d-4298-b88f-06188734ab42", null);
    assertEquals(2, resources.getTotalElements());
  }

  @Test(expected = NotFoundException.class)
  public void getResourceNotFoundDeployment() throws Exception {
    service.getResource("mmd34483-d937-4578-bfdb-ebe196bf82dd", "not-found");
  }

  @Test(expected = NotFoundException.class)
  public void getResourceNotFoundResource() throws Exception {
    service.getResource("not-found", "0748fbe9-6c1d-4298-b88f-06188734ab42");
  }

  @Test
  @DatabaseSetup("/data/database-resource-init.xml")
  public void getResource() throws Exception {
    Resource resource = service.getResource("mmd34483-d937-4578-bfdb-ebe196bf82dd",
        "0748fbe9-6c1d-4298-b88f-06188734ab42");
    assertEquals("mmd34483-d937-4578-bfdb-ebe196bf82dd", resource.getId());

  }
}
