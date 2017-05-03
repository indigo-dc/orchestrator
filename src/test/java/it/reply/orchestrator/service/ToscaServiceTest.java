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

package it.reply.orchestrator.service;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.ComplexPropertyValue;
import alien4cloud.model.components.ListPropertyValue;
import alien4cloud.model.components.PropertyValue;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;

import es.upv.i3m.grycap.file.NoNullOrEmptyFile;
import es.upv.i3m.grycap.file.Utf8File;
import es.upv.i3m.grycap.im.exceptions.FileException;

import it.reply.orchestrator.config.specific.WebAppConfigurationAware;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.utils.CommonUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ToscaServiceTest extends WebAppConfigurationAware {

  @Autowired
  private ToscaService toscaService;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  public static final String TEMPLATES_BASE_DIR = "./src/test/resources/tosca/";
  public static final String TEMPLATES_INPUT_BASE_DIR = TEMPLATES_BASE_DIR + "inputs/";
  public static final String TEMPLATES_ONEDATA_BASE_DIR =
      TEMPLATES_BASE_DIR + "onedata_requirements/";

  private String deploymentId = "deployment_id";

  // @Test(expected = ToscaException.class)
  // public void customizeTemplateWithInvalidTemplate() throws Exception {
  //
  // String template = getFileContentAsString(TEMPLATES_BASE_DIR + "galaxy_tosca_clues_error.yaml");
  // toscaService.customizeTemplate(template, deploymentId);
  // }
  //
  // @SuppressWarnings("unchecked")
  // @Test
  // public void customizeTemplate() throws Exception {
  //
  // String template = getFileContentAsString(TEMPLATES_BASE_DIR + "galaxy_tosca_clues.yaml");
  // String customizedTemplate = toscaService.customizeTemplate(template, deploymentId);
  // Map<String, NodeTemplate> nodes = toscaService.getArchiveRootFromTemplate(customizedTemplate)
  // .getResult().getTopology().getNodeTemplates();
  // for (Map.Entry<String, NodeTemplate> entry : nodes.entrySet()) {
  // if (entry.getValue().getType().equals("tosca.nodes.indigo.ElasticCluster")) {
  // String templateDeploymentId =
  // ((PropertyValue<String>) entry.getValue().getProperties().get("deployment_id"))
  // .getValue();
  //
  // String templateOrchestratorUrl =
  // ((PropertyValue<String>) entry.getValue().getProperties().get("orchestrator_url"))
  // .getValue();
  //
  // assertEquals(deploymentId, templateDeploymentId);
  // assertNotNull(new URL(templateOrchestratorUrl));
  // }
  // }
  //
  // }

  @Test
  public void getRemovalList() throws IOException, ParsingException, FileException {
    List<String> expectedRemovalList = Arrays.asList("to-be-deleted-1", "to-be-deleted-2");
    String template =
        getFileContentAsString(TEMPLATES_BASE_DIR + "galaxy_tosca_clues_removal_list.yaml");
    NodeTemplate node = toscaService.getArchiveRootFromTemplate(template).getResult().getTopology()
        .getNodeTemplates().get("torque_wn");
    List<String> removalList = toscaService.getRemovalList(node);
    assertEquals(expectedRemovalList, removalList);
  }

  @Test
  public void checkUserInputDefaultReplaced() throws Exception {
    String template =
        getFileContentAsString(TEMPLATES_INPUT_BASE_DIR + "tosca_inputs_default_replaced.yaml");
    Map<String, Object> inputs = new HashMap<String, Object>();
    ArchiveRoot ar = toscaService.prepareTemplate(template, inputs);
    AbstractPropertyValue numCpus = ar.getTopology().getNodeTemplates().get("my_server")
        .getCapabilities().get("host").getProperties().get("num_cpus");
    assertThat(numCpus, instanceOf(PropertyValue.class));
    assertEquals("8", ((PropertyValue<?>) numCpus).getValue());
  }

  @Test
  public void checkUserInputReplacedInNodeArtifactsRelationshipsCapabilitiesProperties()
      throws Exception {
    String template =
        getFileContentAsString(TEMPLATES_INPUT_BASE_DIR + "tosca_inputs_replaced_all_types.yaml");
    Map<String, Object> inputs = new HashMap<String, Object>();
    inputs.put("input_urls", Arrays.asList("http://a.it", "http://b.it"));
    inputs.put("output_filenames", "test1, test2");
    inputs.put("command", "command");
    inputs.put("cpus", 1.0d);
    inputs.put("mem", "256 MB");
    inputs.put("docker_image", "docker_image");

    ArchiveRoot ar = toscaService.prepareTemplate(template, inputs);
    Map<String, NodeTemplate> nodes = ar.getTopology().getNodeTemplates();
    NodeTemplate chronosJob = nodes.get("chronos_job");
    // Node's properties
    assertEquals(
        inputs
            .get(
                "command"),
        CommonUtils.<ScalarPropertyValue> optionalCast(
            toscaService.getNodePropertyByName(chronosJob, "command")).get().getValue());
    // Validate list replacement (little bit hard-coded... should be improved)
    AbstractPropertyValue uris = toscaService.getNodePropertyByName(chronosJob, "uris").get();
    assertThat(uris, instanceOf(ListPropertyValue.class));
    AbstractPropertyValue urisOne =
        (AbstractPropertyValue) ((ListPropertyValue) uris).getValue().get(0);
    AbstractPropertyValue urisTwo =
        (AbstractPropertyValue) ((ListPropertyValue) uris).getValue().get(1);
    assertThat(urisOne, instanceOf(ScalarPropertyValue.class));
    assertThat(urisTwo, instanceOf(ScalarPropertyValue.class));
    assertEquals(inputs.get("input_urls"), Arrays.asList(((ScalarPropertyValue) urisOne).getValue(),
        ((ScalarPropertyValue) urisTwo).getValue()));

    // Recursive node's properties
    AbstractPropertyValue outputFileNames = (AbstractPropertyValue) CommonUtils
        .<ComplexPropertyValue> optionalCast(
            toscaService.getNodePropertyByName(chronosJob, "environment_variables"))
        .get().getValue().get("OUTPUT_FILENAMES");

    assertThat(outputFileNames, instanceOf(ScalarPropertyValue.class));
    assertEquals(inputs.get("output_filenames").toString(),
        ((ScalarPropertyValue) outputFileNames).getValue());

    // Artifacts' properties
    assertEquals(inputs.get("docker_image"),
        ((ScalarPropertyValue) chronosJob.getArtifacts().get("image").getFile()).getValue());

    // Requirements' properties
    Map<String, NodeTemplate> dockerRelationships =
        toscaService.getAssociatedNodesByCapability(nodes, chronosJob, "host");

    NodeTemplate dockerNode = dockerRelationships.values().iterator().next();
    Capability dockerCapability = dockerNode.getCapabilities().get("host");
    assertEquals(inputs.get("cpus").toString(),
        CommonUtils
            .<ScalarPropertyValue> optionalCast(
                toscaService.getCapabilityPropertyByName(dockerCapability, "num_cpus"))
            .get().getValue());
    assertEquals(inputs.get("mem").toString(),
        CommonUtils
            .<ScalarPropertyValue> optionalCast(
                toscaService.getCapabilityPropertyByName(dockerCapability, "mem_size"))
            .get().getValue());

    // FIXME: Also test relationships' properties
  }

  @Test
  public void checkUserInputRequiredNoDefaultValueNotGiven() throws Exception {
    checkUserInputGeneric("tosca_inputs_required_not_given.yaml", "required and is not present");
  }

  @Test
  public void checkUserInputNotRequiredNoDefaultValueNotGiven() throws Exception {
    checkUserInputGeneric("tosca_inputs_not_required_no_default_not_given.yaml",
        "No given input or default value available");
  }

  @Test
  public void checkUserInputPresentButEmptyInputList() throws Exception {
    checkUserInputGeneric("tosca_inputs_empty_input_list.yaml", "Empty template input list");
  }

  private void checkUserInputGeneric(String templateName, String expectedMessage) throws Exception {
    thrown.expect(ToscaException.class);
    thrown.expectMessage(expectedMessage);

    String template = getFileContentAsString(TEMPLATES_INPUT_BASE_DIR + templateName);
    Map<String, Object> inputs = new HashMap<String, Object>();
    toscaService.prepareTemplate(template, inputs);
  }

  @Test
  public void checkOneDataHardCodedRequirementsExtractionInUserDefinedTemplate() throws Exception {
    String template = getFileContentAsString(
        TEMPLATES_ONEDATA_BASE_DIR + "tosca_onedata_requirements_hardcoded_userdefined.yaml");
    Map<String, Object> inputs = new HashMap<String, Object>();
    inputs.put("input_onedata_providers", "input_provider_1,input_provider_2");
    inputs.put("input_onedata_space", "input_onedata_space");
    inputs.put("output_onedata_providers", "output_provider_1,output_provider_2");
    inputs.put("output_onedata_space", "output_onedata_space");
    ArchiveRoot ar = toscaService.prepareTemplate(template, inputs);
    Map<String, OneData> odr = toscaService.extractOneDataRequirements(ar, inputs);
    assertEquals(true, odr.containsKey("input"));
    assertArrayEquals(inputs.get("input_onedata_providers").toString().split(","), odr.get("input")
        .getProviders().stream().map(info -> info.getEndpoint()).collect(Collectors.toList()).toArray());
    assertEquals(true, odr.containsKey("output"));
    assertArrayEquals(inputs.get("output_onedata_providers").toString().split(","),
        odr.get("output").getProviders().stream().map(info -> info.getEndpoint())
            .collect(Collectors.toList()).toArray());
  }

  @Test
  public void checkOneDataHardCodedRequirementsExtractionInServiceTemplate() throws Exception {
    String template = getFileContentAsString(
        TEMPLATES_ONEDATA_BASE_DIR + "tosca_onedata_requirements_hardcoded_service.yaml");

    Map<String, Object> inputs = new HashMap<String, Object>();
    ArchiveRoot ar = toscaService.prepareTemplate(template, inputs);
    Map<String, OneData> odr = toscaService.extractOneDataRequirements(ar, inputs);
    assertEquals(0, odr.size());
  }

  private String getFileContentAsString(String fileUri) throws FileException {
    return new NoNullOrEmptyFile(new Utf8File(Paths.get(fileUri))).read();
  }
}
