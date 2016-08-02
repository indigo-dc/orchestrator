package it.reply.orchestrator.service;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;

import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.model.ArchiveRoot;

import es.upv.i3m.grycap.file.NoNullOrEmptyFile;
import es.upv.i3m.grycap.file.Utf8File;
import es.upv.i3m.grycap.im.exceptions.FileException;

import it.reply.orchestrator.config.specific.WebAppConfigurationAware;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.service.deployment.providers.ChronosServiceImpl;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ChronosServiceTest extends WebAppConfigurationAware {

  @Autowired
  private ChronosServiceImpl chronosServiceImpl;

  @Autowired
  private ToscaService toscaService;

  @Test
  public void checkOneDataHardCodedParamsSubstitutionInUserDefinedTemplate() throws Exception {
    String template = getFileContentAsString(ToscaServiceTest.TEMPLATES_ONEDATA_BASE_DIR
        + "tosca_onedata_requirements_hardcoded_userdefined.yaml");
    Map<String, Object> inputs = new HashMap<String, Object>();
    inputs.put("input_onedata_providers", "input_provider_1,input_provider_2");
    inputs.put("input_onedata_space", "input_onedata_space");
    inputs.put("output_onedata_providers", "output_provider_1,output_provider_2");
    inputs.put("output_onedata_space", "output_onedata_space");
    ArchiveRoot ar = toscaService.prepareTemplate(template, inputs);

    Map<String, OneData> odParameters =
        ImmutableMap.of("service", new OneData("token", "space", "path", "provider"), "input",
            new OneData("token_input", "space_input", "path_input",
                "provider_input_1,provider_input_2"),
            "output", new OneData("token_output", "space_output", "path_output",
                "provider_output_1,provider2_output_2"));

    String customizedTemplate = chronosServiceImpl.replaceHardCodedParams(template, odParameters);

    // Re-parse template (TODO: serialize the template in-memory representation?)
    ar = toscaService.prepareTemplate(customizedTemplate, inputs);

    Map<String, NodeTemplate> nodes = ar.getTopology().getNodeTemplates();
    NodeTemplate chronosJob = nodes.get("chronos_job");
    @SuppressWarnings("unchecked")
    Map<String, Object> envVars = ((Map<String, Object>) toscaService
        .getNodePropertyValueByName(chronosJob, "environment_variables").getValue());

    assertEquals("provider_input_1,provider_input_2",
        ((ScalarPropertyValue) envVars.get("INPUT_ONEDATA_PROVIDERS")).getValue());
    assertEquals("provider_output_1,provider2_output_2",
        ((ScalarPropertyValue) envVars.get("OUTPUT_ONEDATA_PROVIDERS")).getValue());
  }

  @Test
  public void checkOneDataHardCodedParamsSubstitutionInServiceTemplate() throws Exception {
    String template = getFileContentAsString(ToscaServiceTest.TEMPLATES_ONEDATA_BASE_DIR
        + "tosca_onedata_requirements_hardcoded_service.yaml");

    Map<String, Object> inputs = new HashMap<String, Object>();
    ArchiveRoot ar = toscaService.prepareTemplate(template, inputs);
    OneData serviceOd = new OneData("token", "space", "path", "provider");
    Map<String, OneData> odParameters = ImmutableMap.of("service", serviceOd, "input",
        new OneData("token_input", "space_input", "path_input",
            "provider_input_1,provider_input_2"),
        "output", new OneData("token_output", "space_output", "path_output",
            "provider_output_1,provider2_output_2"));

    String customizedTemplate = chronosServiceImpl.replaceHardCodedParams(template, odParameters);

    // Re-parse template (TODO: serialize the template in-memory representation?)
    ar = toscaService.prepareTemplate(customizedTemplate, inputs);

    Map<String, NodeTemplate> nodes = ar.getTopology().getNodeTemplates();
    NodeTemplate chronosJob = nodes.get("chronos_job");
    @SuppressWarnings("unchecked")
    Map<String, Object> envVars = ((Map<String, Object>) toscaService
        .getNodePropertyValueByName(chronosJob, "environment_variables").getValue());

    assertEquals(serviceOd.getToken(),
        ((ScalarPropertyValue) envVars.get("ONEDATA_SERVICE_TOKEN")).getValue());
    assertEquals(serviceOd.getSpace(),
        ((ScalarPropertyValue) envVars.get("ONEDATA_SPACE")).getValue());
    assertEquals(serviceOd.getPath(),
        ((ScalarPropertyValue) envVars.get("ONEDATA_PATH")).getValue());
    assertEquals(serviceOd.getProviders().size(), 1);
    assertEquals(serviceOd.getProviders().stream().map(info -> info.endpoint)
        .collect(Collectors.toList()).get(0),
        ((ScalarPropertyValue) envVars.get("ONEDATA_PROVIDERS")).getValue());
  }

  private String getFileContentAsString(String fileUri) throws FileException {
    return new NoNullOrEmptyFile(new Utf8File(Paths.get(fileUri))).read();
  }
}