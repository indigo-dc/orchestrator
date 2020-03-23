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

import static org.junit.Assert.assertEquals;

import it.reply.orchestrator.config.specific.WebAppConfigurationAwareIT;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.CloudProvider;
import it.reply.orchestrator.dto.cmdb.CloudServiceType;
import it.reply.orchestrator.dto.cmdb.Flavor;
import it.reply.orchestrator.dto.cmdb.Image;
import it.reply.orchestrator.dto.cmdb.Tenant;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This integration test makes real request to the CMDB APIs.
 *
 * @author l.biava
 *
 */
public class CmdbServiceIT extends WebAppConfigurationAwareIT {

  private final String flavorId = "5f94fc673a476e2a9fa63c713000b45b";
  private final String imageId = "a8f0d13b52dca71703d23f7c7d6a23b0";
  private final String recasId = "4401ac5dc8cfbbb737b0a02575e6f4bc";
  private final String tenantId = "8a5377c6-a7f4-4d1c-a4cd-074ab92b6035";
  private final String organisationId = "8a5377c6-a7f4-4d1c-a4cd-074ab92b6035";
  private final String recasProviderName = "provider-RECAS-BARI";

  @Autowired
  private CmdbService service;

  @Test
  @Ignore
  public void getServiceTest() throws Exception {

    CloudService serviceRecas = service.getServiceById(recasId);
    CloudService service = CloudService.builder()
        .serviceType("eu.egi.cloud.vm-management.openstack")
        .endpoint("https://cloud.recas.ba.infn.it:5000/v3")
        .providerId("provider-RECAS-BARI")
        .type(CloudServiceType.COMPUTE)
        .region("recas-cloud")
        .build();

    assertEquals(service, serviceRecas);
  }

  @Test
  @Ignore
  public void getProviderTest() throws Exception {
    CloudProvider providerRecas = service.getProviderById(recasProviderName);
    CloudProvider provider = CloudProvider
        .builder()
        .id(recasProviderName)
        .name("RECAS-BARI")
        .build();

    assertEquals(provider, providerRecas);
  }

  @Test
  @Ignore
  public void getTenantsByServiceTest() throws Exception {

    List<Tenant> tenants = service.getTenantsByService(recasId);
  }

  @Test
  @Ignore
  public void getTenantsByOrganisationTest() throws Exception {

    List<Tenant> tenants = service.getTenantsByOrganisation(organisationId);
  }

  @Test
  @Ignore
  public void getTenantByIdTest() throws Exception {

    Tenant tenant = service.getTenantById(tenantId);
  }

  @Test
  @Ignore
  public void getImageForTenantTest() throws Exception {

    List<Image> images = service.getImagesByTenant(tenantId);

    // ProviderData data =
    // new ProviderData().withId("476").withPrimaryKey("83757G0").withName("RECAS-BARI")
    // .withCountry("Italy").withCountryCode("IT").withRoc("NGI_IT").withSubgrid("")
    // .withGiisUrl("ldap://cloud-bdii.recas.ba.infn.it:2170/GLUE2DomainID=RECAS-BARI,o=glue");
    //
    // Provider p = new Provider().withId(recasProviderName)
    // .withRev("1-c7dbe4d8be30aa4c0f14d3ad0411d962").withType("provider").withData(data);

    // assertEquals(p, providerRecas);

  }

  @Test
  @Ignore
  public void getImageByIdTest() throws Exception {

    Image image = service.getImageById(imageId);
  }

  @Test
  @Ignore
  public void getFlavorForTenantTest() throws Exception {

    List<Flavor> flavors = service.getFlavorsByTenant(tenantId);
  }

  @Test
  @Ignore
  public void getFlavorByIdTest() throws Exception {

    Flavor flavor = service.getFlavorById(flavorId);
  }

}
