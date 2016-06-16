package it.reply.orchestrator.service;

import static org.junit.Assert.assertEquals;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

import it.reply.orchestrator.config.specific.WebAppConfigurationAware;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.exception.http.NotFoundException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

@DatabaseTearDown("/data/database-empty.xml")
public class ResourceServiceTest extends WebAppConfigurationAware {

  @Autowired
  private ResourceService service;

  @Test(
      expected = NotFoundException.class)
  public void getResourcesNotFoundDeployment() throws Exception {
    service.getResources("not-found", null);
  }

  @Test
  @DatabaseSetup("/data/database-resource-init.xml")
  public void getResources() throws Exception {
    Page<Resource> resources = service.getResources("0748fbe9-6c1d-4298-b88f-06188734ab42", null);
    assertEquals(2, resources.getTotalElements());
  }

  @Test(
      expected = NotFoundException.class)
  public void getResourceNotFoundDeployment() throws Exception {
    service.getResource("mmd34483-d937-4578-bfdb-ebe196bf82dd", "not-found");
  }

  @Test(
      expected = NotFoundException.class)
  public void getResourceNotFoundResource() throws Exception {
    service.getResource("not-found", "0748fbe9-6c1d-4298-b88f-06188734ab42");
  }

  @Test
  @DatabaseSetup("/data/database-resource-init.xml")
  public void getResource() throws Exception {
    Resource resource =
        service.getResource("mmd34483-d937-4578-bfdb-ebe196bf82dd",
            "0748fbe9-6c1d-4298-b88f-06188734ab42");
    assertEquals("mmd34483-d937-4578-bfdb-ebe196bf82dd", resource.getId());

  }
}
