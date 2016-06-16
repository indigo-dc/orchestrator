package it.reply.orchestrator.dal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

import it.reply.orchestrator.config.specific.WebAppConfigurationAware;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.ResourceRepository;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

@DatabaseTearDown("/data/database-empty.xml")
@DatabaseSetup("/data/database-resource-init.xml")
public class ResourceRepositoryTest extends WebAppConfigurationAware {

  final String deploymentId = "0748fbe9-6c1d-4298-b88f-06188734ab42";
  final String resourceId = "mmd34483-d937-4578-bfdb-ebe196bf82dd";

  @Autowired
  private ResourceRepository resourceRepository;

  /**
   * No resource found for a not existing deployment.
   */
  @Test
  public void resourcesNotFound() {
    Page<Resource> resources = resourceRepository
        .findByDeployment_id("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", null);
    assertThat(resources.getTotalElements(), equalTo(0L));
  }

  /**
   * Resource found for an existing deployment.
   */
  @Test
  public void resourcesFound() {
    Page<Resource> resources = resourceRepository.findByDeployment_id(deploymentId, null);
    assertThat(resources.getTotalElements(), equalTo(2L));

  }

  /**
   * Test find resource by Id.
   */
  @Test
  public void resourceFound() {
    Resource resource = resourceRepository.findByIdAndDeployment_id(resourceId, deploymentId);
    assertThat(resource.getId(), equalTo(resourceId));

  }

  /**
   * Test not found resource for a not existing resource id.
   */
  @Test
  public void resourceNotFound() {
    Resource resource = resourceRepository
        .findByIdAndDeployment_id("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", deploymentId);
    assertThat(resource, equalTo(null));

  }

  /**
   * Test not found resource for an existing resource but not existing deployment.
   */
  @Test
  public void resourceNotFoundForExistingResourceButNotExistingDeployment() {
    Resource resource = resourceRepository.findByIdAndDeployment_id(resourceId,
        "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    assertThat(resource, equalTo(null));

  }

}