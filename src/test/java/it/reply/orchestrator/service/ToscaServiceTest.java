/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
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

import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;

import it.reply.orchestrator.config.specific.ToscaParserAwareTest;
import it.reply.orchestrator.dto.cmdb.ImageData;
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.util.TestUtil;
import it.reply.orchestrator.utils.ToscaUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.normative.types.FloatType;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.OptionalAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(JUnitParamsRunner.class)
public class ToscaServiceTest extends ToscaParserAwareTest {

  @Autowired
  protected ToscaServiceImpl toscaService;

  public static final String TEMPLATES_BASE_DIR = "./src/test/resources/tosca/";
  public static final String TEMPLATES_INPUT_BASE_DIR = TEMPLATES_BASE_DIR + "inputs/";
  public static final String TEMPLATES_ONEDATA_BASE_DIR =
      TEMPLATES_BASE_DIR + "onedata_requirements/";

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
  public void getRemovalList() throws IOException, ParsingException {
    List<String> expectedRemovalList = Arrays.asList("to-be-deleted-1", "to-be-deleted-2");
    String template =
        TestUtil.getFileContentAsString(TEMPLATES_BASE_DIR + "galaxy_tosca_clues_removal_list.yaml");
    NodeTemplate node = toscaService.getArchiveRootFromTemplate(template).getResult().getTopology()
        .getNodeTemplates().get("torque_wn");
    List<String> removalList = toscaService.getRemovalList(node);
    assertEquals(expectedRemovalList, removalList);
  }

  @Test
  public void checkUserInputDefaultReplaced() throws Exception {
    String template =
        TestUtil.getFileContentAsString(TEMPLATES_INPUT_BASE_DIR + "tosca_inputs_default_replaced.yaml");
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
        TestUtil.getFileContentAsString(TEMPLATES_INPUT_BASE_DIR + "tosca_inputs_replaced_all_types.yaml");
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
        inputs.get("command"),
        ToscaUtils.extractScalar(chronosJob.getProperties(), "command").get()
    );
    // Validate list replacement (little bit hard-coded... should be improved)
    List<String> uris = ToscaUtils
        .extractList(chronosJob.getProperties(), "uris", String.class::cast).get();
    assertEquals(inputs.get("input_urls"), uris);

    // Recursive node's properties
    String outputFileNames = ToscaUtils
        .extractMap(chronosJob.getProperties(), "environment_variables", String.class::cast)
        .get().get("OUTPUT_FILENAMES");

    assertEquals(inputs.get("output_filenames"), outputFileNames);

    // Artifacts' properties
    assertEquals(inputs.get("docker_image"),
        chronosJob.getArtifacts().get("image").getArtifactRef());

    // Requirements' properties
    Map<String, NodeTemplate> dockerRelationships =
        toscaService.getAssociatedNodesByCapability(nodes, chronosJob, "host");

    NodeTemplate dockerNode = dockerRelationships.values().iterator().next();
    Capability dockerCapability = dockerNode.getCapabilities().get("host");
    assertEquals(
        inputs.get("cpus"),
        ToscaUtils.extractScalar(dockerCapability.getProperties(), "num_cpus", FloatType.class)
            .get()
    );
    assertEquals(
        inputs.get("mem"),
        ToscaUtils.extractScalar(dockerCapability.getProperties(), "mem_size").get()
    );

    // FIXME: Also test relationships' properties
  }

  @Test
  public void checkUserInputRequiredNoDefaultValueNotGiven() throws Exception {
    checkUserInputGeneric("tosca_inputs_required_not_given.yaml",
        "Input <cpus> is required and is not present in the user's input list, nor has a default value");
  }

  @Test
  public void checkUserInputNotRequiredNoDefaultValueNotGiven() throws Exception {
    checkUserInputGeneric("tosca_inputs_not_required_no_default_not_given.yaml",
        "Failed to evaluate node_templates[my_server][capabilities][host][properties][num_cpus][get_input][cpus]: No input provided for <cpus> and no default value provided in the definition");
  }

  private void checkUserInputGeneric(String templateName, String expectedMessage) throws Exception {

    String template = TestUtil.getFileContentAsString(TEMPLATES_INPUT_BASE_DIR + templateName);
    Map<String, Object> inputs = new HashMap<String, Object>();
    Assertions
        .assertThatThrownBy(() -> toscaService.prepareTemplate(template, inputs))
        .isInstanceOf(ToscaException.class)
        .hasMessage(expectedMessage);
  }

  @Test
  @Parameters({ "ubuntu, ubuntu, true",
      "ubuntu, ubuntu:16.04, true",
      "ubuntu:16.04, ubuntu:16.04, true",
      "ubuntu:16.04, ubuntu, false",
      "ubuntu, centos, false",
      "ubuntu, centos:7, false",
      "ubuntu:16.04, centos, false",
      "ubuntu:16.04, centos:7, false",
      "ubuntu, null, false",
      "ubuntu:16.04, null, false" })
  public void checkRequiredImageMetadata(String requiredImageName,
      @Nullable String availableImageName, boolean expectedResult) {
    Assertions.assertThat(toscaService.requiredImageMetadata(requiredImageName, availableImageName))
        .isEqualTo(expectedResult);
  }

  @Test
  @Parameters({
      "type, 1, null, 1",
      "type, 1, 1, 1",
      "type, 1, 2, 1",
      "type, 2, null, null",
      "type, 2, 1, 1",
      "type, 2, 2, null",
      "architecture, 1, null, 1",
      "architecture, 1, 1, 1",
      "architecture, 1, 2, 1",
      "architecture, 2, null, null",
      "architecture, 2, 1, 1",
      "architecture, 2, 2, null",
      "distribution, 1, null, 1",
      "distribution, 1, 1, 1",
      "distribution, 1, 2, 1",
      "distribution, 2, null, null",
      "distribution, 2, 1, 1",
      "distribution, 2, 2, null",
      "version, 1, null, 1",
      "version, 1, 1, 1",
      "version, 1, 2, 1",
      "version, 2, null, null",
      "version, 2, 1, 1",
      "version, 2, 2, null",
      "null, 1, null, 0",
      "null, 1, 1, 1",
      "null, 1, 2, null"
  })
  public void checkRequiredImageMetadata(@Nullable String fieldname, String fieldValue,
      @Nullable String imageName, @Nullable String expectedId)
      throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException,
      SecurityException {
    List<ImageData> images = new ArrayList<>();

    for (int i = 0; i < 2; ++i) {
      images.add(ImageData
          .builder()
          .imageId(String.valueOf(i))
          .imageName(String.valueOf(i))
          .type(String.valueOf(i))
          .architecture(String.valueOf(i))
          .distribution(String.valueOf(i))
          .version(String.valueOf(i))
          .build());
    }
    ImageData imageMetadata = ImageData.builder().build();
    if (imageName != null) {
      imageMetadata.setImageName(imageName);
    }
    if (fieldname != null) {
      Field field = ImageData.class.getDeclaredField(fieldname);
      field.setAccessible(true);
      field.set(imageMetadata, fieldValue);
    }

    OptionalAssert<ImageData> assertion =
        Assertions.assertThat(toscaService.getBestImageForCloudProvider(imageMetadata, images));
    if (expectedId != null) {
      assertion.hasValueSatisfying(image -> {
        image.getImageId().equals(expectedId);
      });
    } else {
      assertion.isEmpty();
    }
  }

}
