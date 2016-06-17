package it.reply.orchestrator.service;

import static org.junit.Assert.assertEquals;

import it.reply.orchestrator.config.specific.WebAppConfigurationAwareIT;
import it.reply.orchestrator.dto.cmdb.Data;
import it.reply.orchestrator.dto.cmdb.Provider;
import it.reply.orchestrator.dto.cmdb.ProviderData;
import it.reply.orchestrator.dto.cmdb.Service;
import it.reply.orchestrator.dto.cmdb.Type;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This integration test makes real request to the CMDB APIs.
 * 
 * @author l.biava
 *
 */
public class CmdbServiceIT extends WebAppConfigurationAwareIT {

  private final String recasId = "4401ac5dc8cfbbb737b0a02575e6f4bc";
  private final String recasProviderName = "provider-RECAS-BARI";

  @Autowired
  private CmdbService service;

  @Test
  public void getServiceTest() throws Exception {

    Service serviceRecas = service.getServiceById(recasId);
    Data data = new Data().withServiceType("eu.egi.cloud.vm-management.openstack")
        .withEndpoint("http://cloud.recas.ba.infn.it:5000/v2.0")
        .withProviderId("provider-RECAS-BARI").withType(Type.COMPUTE);

    Service service = new Service().withId(recasId).withRev("1-256d36283315ea9bb045e6d5038657b6")
        .withType("service").withData(data);

    assertEquals(service, serviceRecas);
  }

  @Test
  public void getProviderTest() throws Exception {
    Provider providerRecas = service.getProviderById(recasProviderName);
    ProviderData data =
        new ProviderData().withId("476").withPrimaryKey("83757G0").withName("RECAS-BARI")
            .withCountry("Italy").withCountryCode("IT").withRoc("NGI_IT").withSubgrid("")
            .withGiisUrl("ldap://cloud-bdii.recas.ba.infn.it:2170/GLUE2DomainID=RECAS-BARI,o=glue");

    Provider provider = new Provider().withId(recasProviderName)
        .withRev("1-c7dbe4d8be30aa4c0f14d3ad0411d962").withType("provider").withData(data);

    assertEquals(provider, providerRecas);
  }

}
