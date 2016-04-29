package it.reply.orchestrator.service;

import static org.junit.Assert.assertEquals;

import alien4cloud.model.components.PropertyValue;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.parser.ParsingException;

import es.upv.i3m.grycap.file.NoNullOrEmptyFile;
import es.upv.i3m.grycap.file.Utf8File;
import es.upv.i3m.grycap.im.exceptions.FileException;

import it.reply.orchestrator.config.WebAppConfigurationAware;
import it.reply.orchestrator.exception.service.ToscaException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ToscaServiceTest extends WebAppConfigurationAware {

  @Autowired
  private ToscaService toscaService;

  private String deploymentId = "deployment_id";

  @Test(expected = ToscaException.class)
  public void customizeTemplateWithError() throws Exception {

    String template = getFileContentAsString(
        "./src/test/resources/tosca/galaxy_tosca_clues_error.yaml");
    toscaService.customizeTemplate(template, deploymentId);
  }

  @Test
  public void customizeTemplateWithDeplymentIdSuccessfully() throws Exception {

    String template = getFileContentAsString(
        "./src/test/resources/tosca/galaxy_tosca_clues.yaml");
    String customizedTemplate = toscaService.customizeTemplate(template, deploymentId);
    String templateDeploymentId = "";
    Map<String, NodeTemplate> nodes = toscaService.getArchiveRootFromTemplate(customizedTemplate)
        .getResult().getTopology().getNodeTemplates();
    for (Map.Entry<String, NodeTemplate> entry : nodes.entrySet()) {
      if (entry.getValue().getType().equals("tosca.nodes.indigo.ElasticCluster")) {
        templateDeploymentId = ((PropertyValue<String>) entry.getValue().getProperties()
            .get("deployment_id")).getValue();
      }
    }

    assertEquals(deploymentId, templateDeploymentId);
  }

  @Test
  public void getRemovalList() throws IOException, ParsingException, FileException {
    List<String> expectedRemovalList = Arrays.asList("to-be-deleted-1", "to-be-deleted-2");
    String template = getFileContentAsString(
        "./src/test/resources/tosca/galaxy_tosca_clues_removal_list.yaml");
    NodeTemplate node = toscaService.getArchiveRootFromTemplate(template).getResult().getTopology()
        .getNodeTemplates().get("torque_wn");
    List<String> removalList = toscaService.getRemovalList(node);
    assertEquals(expectedRemovalList, removalList);
  }
  
  private String getFileContentAsString(String fileUri) throws FileException{
    return new NoNullOrEmptyFile(new Utf8File(Paths.get(fileUri))).read();
  }
}
