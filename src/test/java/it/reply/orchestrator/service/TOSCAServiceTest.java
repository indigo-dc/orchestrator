package it.reply.orchestrator.service;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import alien4cloud.model.components.PropertyValue;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.parser.ParsingException;
import es.upv.i3m.grycap.file.FileIO;
import it.reply.orchestrator.config.WebAppConfigurationAware;
import it.reply.orchestrator.exception.service.TOSCAException;

public class TOSCAServiceTest extends WebAppConfigurationAware {

  @Autowired
  private ToscaService toscaService;

  private String deploymentId = "deployment_id";

  @Test(expected = TOSCAException.class)
  public void customizeTemplateWithError() throws Exception {

    String template = FileIO
        .readUTF8File("./src/test/resources/tosca/galaxy_tosca_clues_error.yaml");
    toscaService.customizeTemplate(template, deploymentId);

  }

  @Test
  public void customizeTemplateWithDeplymentIdSuccessfully() throws Exception {

    String template = FileIO.readUTF8File("./src/test/resources/tosca/galaxy_tosca_clues.yaml");
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
  public void getRemovalList() throws IOException, ParsingException {
    List<String> expectedRemovalList = new ArrayList<>();
    expectedRemovalList.add("to-be-deleted");
    String template = FileIO
        .readUTF8File("./src/test/resources/tosca/galaxy_tosca_clues_removal_list.yaml");
    NodeTemplate node = toscaService.getArchiveRootFromTemplate(template).getResult().getTopology()
        .getNodeTemplates().get("torque_wn");
    List<String> removal_list = toscaService.getRemovalList(node);
    assertEquals(expectedRemovalList, removal_list);
  }
}
