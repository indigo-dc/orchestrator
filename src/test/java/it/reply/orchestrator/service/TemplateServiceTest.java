package it.reply.orchestrator.service;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.exception.http.NotFoundException;

public class TemplateServiceTest {

  @InjectMocks
  private TemplateServiceImpl templateServiceImpl = new TemplateServiceImpl();

  @Mock
  private DeploymentRepository deploymentRepository;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void getTemplate() {
    Deployment createDeployment = ControllerTestUtils.createDeployment();
    String id = UUID.randomUUID().toString();
    Mockito.when(deploymentRepository.findOne(id)).thenReturn(createDeployment);

    Assert.assertEquals(templateServiceImpl.getTemplate(id), createDeployment.getTemplate());
  }


  @Test(expected = NotFoundException.class)
  public void failGetTemplate() {
    String id = UUID.randomUUID().toString();
    Mockito.when(deploymentRepository.findOne(id)).thenReturn(null);
    templateServiceImpl.getTemplate(id);
  }

}
