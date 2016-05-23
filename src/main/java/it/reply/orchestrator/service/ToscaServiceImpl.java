package it.reply.orchestrator.service;

import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.ListPropertyValue;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.PropertyValue;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.security.model.Role;
import alien4cloud.tosca.ArchiveParser;
import alien4cloud.tosca.ArchiveUploadService;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.serializer.VelocityUtil;
import alien4cloud.utils.FileUtil;

import com.google.common.io.ByteStreams;

import it.reply.orchestrator.exception.service.ToscaException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Service
public class ToscaServiceImpl implements ToscaService {

  private static final Logger LOG = LogManager.getLogger(ToscaServiceImpl.class);

  @Resource
  private ArchiveParser parser;

  @Autowired
  private ArchiveUploadService archiveUploadService;

  @Autowired
  private IndigoInputsPreProcessorService indigoInputsPreProcessorService;

  @Value("${directories.alien}/${directories.csar_repository}")
  private String alienRepoDir;

  @Value("${tosca.definitions.basepath}")
  private String basePath;
  @Value("${tosca.definitions.normative}")
  private String normativeLocalName;
  @Value("${tosca.definitions.indigo}")
  private String indigoLocalName;

  /**
   * Load normative and non-normative types.
   * 
   */
  @PostConstruct
  public void init() throws IOException, CSARVersionAlreadyExistsException, ParsingException {
    if (Files.exists(Paths.get(alienRepoDir))) {
      FileUtil.delete(Paths.get(alienRepoDir));
    }

    setAutentication();

    ClassLoader cl = this.getClass().getClassLoader();

    try (InputStream is = cl.getResourceAsStream(basePath + "/" + normativeLocalName)) {
      Path zipFile = File.createTempFile(normativeLocalName, ".zip").toPath();
      zip(is, zipFile);
      ParsingResult<Csar> result = archiveUploadService.upload(zipFile);
      if (!result.getContext().getParsingErrors().isEmpty()) {
        LOG.warn("Error parsing definition {}:\n{}", () -> normativeLocalName,
            () -> Arrays.toString(result.getContext().getParsingErrors().toArray()));
      }
    }

    try (InputStream is = cl.getResourceAsStream(basePath + "/" + indigoLocalName)) {
      Path zipFile = File.createTempFile(indigoLocalName, ".zip").toPath();
      zip(is, zipFile);
      ParsingResult<Csar> result = archiveUploadService.upload(zipFile);
      if (!result.getContext().getParsingErrors().isEmpty()) {
        LOG.warn("Error parsing definition {}:\n{}", () -> indigoLocalName,
            () -> Arrays.toString(result.getContext().getParsingErrors().toArray()));
      }
    }

  }

  /**
   * Utility to zip an inputStream.
   * 
   */
  public static void zip(@Nonnull InputStream fileStream, @Nonnull Path outputPath)
      throws IOException {
    FileUtil.touch(outputPath);
    try (ZipOutputStream zipOutputStream =
        new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(outputPath)))) {
      zipOutputStream.putNextEntry(new ZipEntry("definition.yml"));
      ByteStreams.copy(fileStream, zipOutputStream);
      zipOutputStream.closeEntry();
      zipOutputStream.flush();
    }
  }

  @Override
  @Nonnull
  public ParsingResult<ArchiveRoot> getArchiveRootFromTemplate(@Nonnull String toscaTemplate)
      throws IOException, ParsingException {
    Path zipPath = Files.createTempFile("csar", ".zip");
    try (InputStream is = new ByteArrayInputStream(toscaTemplate.getBytes());) {
      zip(is, zipPath);
    }
    try {
      return parser.parse(zipPath);
    } catch (InvalidArgumentException iae) {
      // FIXME NOTE that InvalidArgumentException should not be thrown by the parse method, but it
      // is...
      // throw new ParsingException("DUMMY", new ParsingError(ErrorCode.INVALID_YAML, ));
      throw iae;
    }
  }

  @Override
  @Nonnull
  public String getTemplateFromTopology(@Nonnull ArchiveRoot archiveRoot) throws IOException {
    Map<String, Object> velocityCtx = new HashMap<>();
    velocityCtx.put("tosca_definitions_version",
        archiveRoot.getArchive().getToscaDefinitionsVersion());
    velocityCtx.put("description", archiveRoot.getArchive().getDescription());
    velocityCtx.put("template_name", archiveRoot.getArchive().getName());
    velocityCtx.put("template_version", archiveRoot.getArchive().getVersion());
    velocityCtx.put("template_author", archiveRoot.getArchive().getTemplateAuthor());
    velocityCtx.put("repositories", archiveRoot.getArchive().getRepositories());
    velocityCtx.put("topology", archiveRoot.getTopology());
    StringWriter writer = new StringWriter();
    VelocityUtil.generate("templates/topology-1_0_0_INDIGO.yml.vm", writer, velocityCtx);
    String template = writer.toString();

    // Log the warning because Alien4Cloud uses an hard-coded Velocity template to encode the string
    // and some information might be missing!!
    LOG.warn(
        "TOSCA template conversion from in-memory: WARNING: Some nodes or properties might be missing!! Use at your own risk!");
    LOG.debug(template);

    return template;
  }

  /**
   * Customize the template with INDIGO requirements, for example it adds the deploymentId.
   * 
   * @param toscaTemplate
   *          the TOSCA template
   * @param deploymentId
   *          the deploymentId
   * @return the customized template
   * 
   * @throws ParsingException
   *           if the template is not valid
   * @throws IOException
   *           if there is an IO error
   * @throws ToscaException
   */
  @Override
  public String customizeTemplate(@Nonnull String toscaTemplate, @Nonnull String deploymentId)
      throws IOException, ToscaException, ParsingException {

    ArchiveRoot ar = parseTemplate(toscaTemplate);

    addDeploymentId(ar, deploymentId);

    return getTemplateFromTopology(ar);

  }

  @Override
  public void replaceInputFunctions(ArchiveRoot archiveRoot, Map<String, Object> inputs)
      throws ToscaException {
    indigoInputsPreProcessorService.processGetInput(archiveRoot, inputs);
  }

  @Override
  public ArchiveRoot parseAndValidateTemplate(String toscaTemplate, Map<String, Object> inputs)
      throws IOException, ParsingException, ToscaException {
    ArchiveRoot ar = parseTemplate(toscaTemplate);
    validateUserInputs(ar.getTopology().getInputs(), inputs);
    return ar;
  }

  @Override
  public ArchiveRoot prepareTemplate(String toscaTemplate, Map<String, Object> inputs)
      throws IOException, ParsingException, ToscaException {
    ArchiveRoot ar = parseAndValidateTemplate(toscaTemplate, inputs);
    replaceInputFunctions(ar, inputs);
    return ar;
  }

  @Override
  public void validateUserInputs(Map<String, PropertyDefinition> templateInputs,
      Map<String, Object> inputs) throws ToscaException {

    // Check if every required input has been given by the user
    for (Map.Entry<String, PropertyDefinition> templateInput : templateInputs.entrySet()) {
      if (templateInput.getValue().isRequired() && !inputs.containsKey(templateInput.getKey())) {
        // Input required and not in user's input list -> error
        throw new ToscaException(
            String.format("Input <%s> is required and is not present in the user's input list",
                templateInput.getKey()));
      } else {
        if (!templateInput.getValue().isRequired()
            && templateInput.getValue().getDefault() == null) {
          // Input not required and no default value -> error
          throw new ToscaException(String.format(
              "Input <%s> is neither required nor has a default value", templateInput.getKey()));
        }
      }
    }

    // Reference:
    // http://docs.oasis-open.org/tosca/TOSCA-Simple-Profile-YAML/v1.0/csprd02/TOSCA-Simple-Profile-YAML-v1.0-csprd02.html#TYPE_YAML_STRING
    // FIXME Check input type compatibility ?
    // templateInput.getValue().getType()
  }

  @Override
  public ArchiveRoot parseTemplate(@Nonnull String toscaTemplate)
      throws IOException, ParsingException, ToscaException {

    ParsingResult<ArchiveRoot> result = null;
    try {
      result = getArchiveRootFromTemplate(toscaTemplate);
    } catch (ParsingException ex) {
      checkParsingErrors(ex.getParsingErrors());
    }
    checkParsingErrors(result.getContext().getParsingErrors());

    return result.getResult();

  }

  @Override
  public String updateTemplate(String template) throws IOException {
    ParsingResult<ArchiveRoot> result = null;
    try {
      result = getArchiveRootFromTemplate(template);
    } catch (ParsingException ex) {
      checkParsingErrors(ex.getParsingErrors());
    }
    checkParsingErrors(result.getContext().getParsingErrors());
    removeRemovalList(result);
    return getTemplateFromTopology(result.getResult());
  }

  private void checkParsingErrors(List<ParsingError> errorList) throws ToscaException {
    String errorMessage = "";
    if (!errorList.isEmpty()) {
      for (ParsingError error : errorList) {
        if (!error.getErrorLevel().equals(ParsingErrorLevel.INFO)) {
          errorMessage = errorMessage + error.toString() + "; ";
        }
      }
      throw new ToscaException(errorMessage);
    }

  }

  private void addDeploymentId(ArchiveRoot parsingResult, String deploymentId) {
    Map<String, NodeTemplate> nodes = parsingResult.getTopology().getNodeTemplates();
    for (Map.Entry<String, NodeTemplate> entry : nodes.entrySet()) {
      if (entry.getValue().getType().equals("tosca.nodes.indigo.ElasticCluster")) {
        // Create new property with the deploymentId and set as printable
        ScalarPropertyValue scalarPropertyValue = new ScalarPropertyValue(deploymentId);
        scalarPropertyValue.setPrintable(true);
        entry.getValue().getProperties().put("deployment_id", scalarPropertyValue);
      }
    }
  }

  private void removeRemovalList(ParsingResult<ArchiveRoot> parsingResult) {
    Map<String, NodeTemplate> nodes = parsingResult.getResult().getTopology().getNodeTemplates();
    for (Map.Entry<String, NodeTemplate> entry : nodes.entrySet()) {
      Capability scalable = getNodeCapabilityByName(entry.getValue(), "scalable");
      if (scalable != null && scalable.getProperties().containsKey("removal_list")) {
        scalable.getProperties().remove("removal_list");
      }
    }
  }

  private static void setAutentication() {
    Authentication auth = new PreAuthenticatedAuthenticationToken(Role.ADMIN.name().toLowerCase(),
        "", AuthorityUtils.createAuthorityList(Role.ADMIN.name()));
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @Override
  public Capability getNodeCapabilityByName(NodeTemplate node, String propertyName) {
    if (node != null && node.getCapabilities() != null) {
      for (Entry<String, Capability> entry : node.getCapabilities().entrySet()) {
        if (entry.getKey().equals(propertyName)) {
          return entry.getValue();
        }
      }
    }
    return null;
  }

  @Override
  public AbstractPropertyValue getNodePropertyByName(NodeTemplate node, String propertyName) {
    if (node != null && node.getProperties() != null) {
      for (Entry<String, AbstractPropertyValue> entry : node.getProperties().entrySet()) {
        if (entry.getKey().equals(propertyName)) {
          return entry.getValue();
        }
      }
    }
    return null;
  }

  public AbstractPropertyValue getCapabilityPropertyByName(Capability capability,
      String propertyName) {
    if (capability != null && capability.getProperties() != null) {
      for (Entry<String, AbstractPropertyValue> entry : capability.getProperties().entrySet()) {
        if (entry.getKey().equals(propertyName)) {
          return entry.getValue();
        }
      }
    }
    return null;
  }

  @Override
  public PropertyValue<?> getNodePropertyValueByName(NodeTemplate node, String propertyName) {
    return (PropertyValue<?>) getNodePropertyByName(node, propertyName);
  }

  @Override
  public PropertyValue<?> getCapabilityPropertyValueByName(Capability capability,
      String propertyName) {
    return (PropertyValue<?>) getCapabilityPropertyByName(capability, propertyName);
  }

  @Override
  public RelationshipTemplate getRelationshipTemplateByCapabilityName(
      Map<String, RelationshipTemplate> relationships, String capabilityName) {
    if (relationships == null)
      return null;

    for (Map.Entry<String, RelationshipTemplate> relationship : relationships.entrySet()) {
      if (relationship.getValue().getTargetedCapabilityName().equals(capabilityName))
        return relationship.getValue();
    }
    return null;
  }

  @Override
  public Map<String, NodeTemplate> getCountNodes(ArchiveRoot archiveRoot) {
    Map<String, NodeTemplate> nodes = new HashMap<>();

    for (Map.Entry<String, NodeTemplate> entry : archiveRoot.getTopology().getNodeTemplates()
        .entrySet()) {
      Capability scalable = getNodeCapabilityByName(entry.getValue(), "scalable");
      if (scalable != null) {
        ScalarPropertyValue scalarPropertyValue =
            (ScalarPropertyValue) scalable.getProperties().get("count");
        // Check if this value is read from the template and is not a default value
        if (scalarPropertyValue.isPrintable()) {
          nodes.put(entry.getKey(), entry.getValue());
        }
      }
    }
    return nodes;
  }

  @Override
  public int getCount(NodeTemplate nodeTemplate) {

    Capability scalable = getNodeCapabilityByName(nodeTemplate, "scalable");
    if (scalable != null) {
      ScalarPropertyValue scalarPropertyValue =
          (ScalarPropertyValue) scalable.getProperties().get("count");
      // Check if this value is read from the template and is not a default value
      if (scalarPropertyValue.isPrintable()) {
        return Integer.parseInt(scalarPropertyValue.getValue());
      }
    }
    return -1;
  }

  @Override
  public List<String> getRemovalList(NodeTemplate nodeTemplate) {
    List<String> removalList = new ArrayList<String>();
    Capability scalable = getNodeCapabilityByName(nodeTemplate, "scalable");
    if (scalable != null) {
      ListPropertyValue listPropertyValue =
          (ListPropertyValue) scalable.getProperties().get("removal_list");
      if (listPropertyValue != null) {
        for (Object o : listPropertyValue.getValue()) {
          if (o instanceof ScalarPropertyValue) {
            removalList.add(((ScalarPropertyValue) o).getValue());
          } else if (o instanceof String) {
            removalList.add((String) o);
          }
        }
      }
    }
    return removalList;
  }

  @Override
  public String updateCount(ArchiveRoot archiveRoot, int count) throws IOException {
    for (Map.Entry<String, NodeTemplate> entry : archiveRoot.getTopology().getNodeTemplates()
        .entrySet()) {
      Capability scalable = getNodeCapabilityByName(entry.getValue(), "scalable");
      if (scalable != null) {
        ScalarPropertyValue scalarPropertyValue =
            (ScalarPropertyValue) scalable.getProperties().get("count");
        if (scalarPropertyValue.isPrintable()) {
          scalarPropertyValue.setValue(String.valueOf(count));
          scalable.getProperties().put("count", scalarPropertyValue);
        }
      }
    }
    return getTemplateFromTopology(archiveRoot);
  }

}
