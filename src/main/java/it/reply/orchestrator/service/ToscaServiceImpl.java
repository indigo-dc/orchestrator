package it.reply.orchestrator.service;

import com.google.common.io.ByteStreams;

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

import es.upv.i3m.grycap.im.auth.credentials.ServiceProvider;

import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.cmdb.Image;
import it.reply.orchestrator.dto.cmdb.Type;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.exception.service.DeploymentException;
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
import java.net.MalformedURLException;
import java.net.URL;
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
  @Value("${orchestrator.url}")
  private String orchestratorUrl;

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
    LOG.warn("TOSCA template conversion from in-memory: "
        + "WARNING: Some nodes or properties might be missing!! Use at your own risk!");
    LOG.debug(template);

    return template;
  }

  @Override
  public String customizeTemplate(@Nonnull String toscaTemplate, @Nonnull String deploymentId)
      throws IOException, ToscaException {

    ArchiveRoot ar = parseTemplate(toscaTemplate);

    addElasticClusterParameters(ar, deploymentId);

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

    // No input to validate
    if (templateInputs == null) {
      return;
    }

    // Check if every required input has been given by the user or has a default value
    for (Map.Entry<String, PropertyDefinition> templateInput : templateInputs.entrySet()) {
      if (templateInput.getValue().isRequired() && templateInput.getValue().getDefault() == null
          && !inputs.containsKey(templateInput.getKey())) {
        // Input required and no value to replace -> error
        throw new ToscaException(
            String.format("Input <%s> is required and is not present in the user's input list,"
                + " nor has a default value", templateInput.getKey()));
      }
    }

    // Reference:
    // http://docs.oasis-open.org/tosca/TOSCA-Simple-Profile-YAML/v1.0/csprd02/TOSCA-Simple-Profile-YAML-v1.0-csprd02.html#TYPE_YAML_STRING
    // FIXME Check input type compatibility ?
    // templateInput.getValue().getType()
  }

  @Override
  public ArchiveRoot parseTemplate(@Nonnull String toscaTemplate)
      throws IOException, ToscaException {

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

  @Override
  public void contextualizeImages(DeploymentProvider deploymentProvider, ArchiveRoot parsingResult,
      CloudProvider cloudProvider) {
    contextualizeImages(deploymentProvider, parsingResult, cloudProvider, true);
  }

  @Override
  public void contextualizeImages(DeploymentProvider deploymentProvider, ArchiveRoot parsingResult,
      CloudProvider cloudProvider, boolean replace) {
    try {
      if (parsingResult.getTopology() != null) {
        Map<String, NodeTemplate> nodes = parsingResult.getTopology().getNodeTemplates();
        if (nodes != null) {
          for (Map.Entry<String, NodeTemplate> entry : nodes.entrySet()) {
            NodeTemplate node = entry.getValue();
            // Only indigo.Compute nodes are relevant
            // FIXME: Check inheritance of tosca.nodes.indigo.Compute
            if (node.getType().equals("tosca.nodes.indigo.Compute")) {
              Capability osCapability = null;
              if (node.getCapabilities() == null
                  || (osCapability = node.getCapabilities().get("os")) == null) {
                // The node doesn't have an OS Capability -> need to add a dummy one to hold a
                // random image for underlying deployment systems
                LOG.debug(String.format(
                    "Generating default OperatingSystem capability for node <%s>", node.getName()));
                if (node.getCapabilities() == null) {
                  node.setCapabilities(new HashMap<>());
                }
                osCapability = new Capability();
                osCapability.setType("tosca.capabilities.indigo.OperatingSystem");
                node.getCapabilities().put("os", osCapability);
              }

              // We've got an OS capability -> Check the attributes to find best match for the image
              Image imageMetadata = new Image();
              if (osCapability.getProperties().get("image") != null) {
                imageMetadata.setImageName(
                    (String) getCapabilityPropertyValueByName(osCapability, "image").getValue());
              }
              if (osCapability.getProperties().get("architecture") != null) {
                imageMetadata.setArchitecture(
                    (String) getCapabilityPropertyValueByName(osCapability, "architecture")
                        .getValue());
              }
              if (osCapability.getProperties().get("type") != null) {
                imageMetadata.setType(
                    (String) getCapabilityPropertyValueByName(osCapability, "type").getValue());
              }
              if (osCapability.getProperties().get("distribution") != null) {
                imageMetadata.setDistribution(
                    (String) getCapabilityPropertyValueByName(osCapability, "distribution")
                        .getValue());
              }
              if (osCapability.getProperties().get("version") != null) {
                imageMetadata.setVersion(
                    (String) getCapabilityPropertyValueByName(osCapability, "version").getValue());
              }

              Image image = null;
              if (deploymentProvider == DeploymentProvider.IM
                  && isImImageUri(imageMetadata.getImageName())) {
                image = imageMetadata;
              } else {
                image = getBestImageForCloudProvider(imageMetadata, cloudProvider);
              }

              // No image match found -> throw error
              if (image == null) {
                LOG.error(String.format(
                    "Failed to found a match in provider <%s> for image metadata <%s>",
                    cloudProvider.getId(), imageMetadata));
                throw new IllegalArgumentException(String.format(
                    "Failed to found a match in provider <%s> for image metadata <%s>",
                    cloudProvider.getId(), imageMetadata));
              }

              // Found a good image -> replace the image attribute with the provider-specific ID
              LOG.debug(String.format(
                  "Found image match in <%s> for image metadata <%s>, "
                      + "provider-specific image id <%s>",
                  cloudProvider.getId(), imageMetadata, image.getImageId()));
              if (replace) {
                String imageId = image.getImageId();
                if (deploymentProvider == DeploymentProvider.IM) {
                  if (isImImageUri(image.getImageName())) {
                    imageId = image.getImageName();
                  } else {
                    imageId = generateImImageUri(cloudProvider, imageId);
                  }
                }
                ScalarPropertyValue scalarPropertyValue = new ScalarPropertyValue(imageId);
                scalarPropertyValue.setPrintable(true);
                osCapability.getProperties().put("image", scalarPropertyValue);
              }

            }
          }
        }
      }
    } catch (Exception ex) {
      throw new RuntimeException("Failed to contextualize images: " + ex.getMessage(), ex);
    }
  }

  @Deprecated
  private boolean isImImageUri(String imageName) {
    if (imageName != null && (imageName.trim().matches(ServiceProvider.OPENSTACK.getId() + "://.+")
        || imageName.trim().matches(ServiceProvider.OPENNEBULA.getId() + "://.+"))) {
      return true;
    } else {
      return false;
    }
  }

  @Deprecated
  private String generateImImageUri(CloudProvider cloudProvider, String imageId) {
    try {
      StringBuilder sb = new StringBuilder();
      switch (CloudProviderEndpointServiceImpl.getProviderIaaSType(cloudProvider)) {
        case OPENSTACK:
          sb.append(ServiceProvider.OPENSTACK.getId());
          break;
        case OPENNEBULA:
          sb.append(ServiceProvider.OPENNEBULA.getId());
          break;
        default:
          throw new DeploymentException("Unknown IaaSType of cloud provider " + cloudProvider);
      }
      URL endpoint =
          new URL(cloudProvider.getCmbdProviderServiceByType(Type.COMPUTE).getData().getEndpoint());
      sb.append("://").append(endpoint.getHost()).append("/").append(imageId);
      imageId = sb.toString();
    } catch (MalformedURLException ex) {
      LOG.error("Cannot retrieve Compute service host for IM image id generation", ex);
    }
    return imageId;
  }

  protected Image getBestImageForCloudProvider(Image imageMetadata, CloudProvider cloudProvider) {

    // Match image name first (for INDIGO specific use case, if the image cannot be found with the
    // specified name it means that a base image + Ansible configuration have to be used -> the
    // base image will be chosen with the other filters and image metadata - architecture, type,
    // distro, version)
    if (imageMetadata.getImageName() != null) {
      Image imageWithName =
          findImageWithNameOnCloudProvider(imageMetadata.getImageName(), cloudProvider);

      if (imageWithName != null) {
        LOG.debug("Image <{}> found with name <{}>", imageWithName.getImageId(),
            imageMetadata.getImageName());
        return imageWithName;
      } else {
        if (imageMetadata.getType() == null && imageMetadata.getArchitecture() == null
            && imageMetadata.getDistribution() == null && imageMetadata.getVersion() == null) {
          return null;
        }
        LOG.debug("Image not found with name <{}>, trying with other fields: <{}>",
            imageMetadata.getImageName(), imageMetadata);
      }
    }

    for (Image image : cloudProvider.getCmdbProviderImages()) {
      // Match or skip image based on each additional optional attribute
      if (imageMetadata.getType() != null) {
        if (!imageMetadata.getType().equalsIgnoreCase(image.getType())) {
          continue;
        }
      }

      if (imageMetadata.getArchitecture() != null) {
        if (!imageMetadata.getArchitecture().equalsIgnoreCase(image.getArchitecture())) {
          continue;
        }
      }

      if (imageMetadata.getDistribution() != null) {
        if (!imageMetadata.getDistribution().equalsIgnoreCase(image.getDistribution())) {
          continue;
        }
      }

      if (imageMetadata.getVersion() != null) {
        if (!imageMetadata.getVersion().equalsIgnoreCase(image.getVersion())) {
          continue;
        }
      }

      LOG.debug("Image <{}> found with fields: <{}>", imageMetadata.getImageId(), imageMetadata);
      return image;
    }
    return null;

  }

  protected Image findImageWithNameOnCloudProvider(String requiredImageName,
      CloudProvider cloudProvider) {
    for (Image image : cloudProvider.getCmdbProviderImages()) {
      if (matchImageNameAndTag(requiredImageName, image.getImageName())) {
        return image;
      }
    }
    return null;
  }

  protected boolean matchImageNameAndTag(String requiredImageName, String availableImageName) {
    // Extract Docker tag if available
    String[] requiredImageNameSplit = requiredImageName.split(":");
    String requiredImageBaseName = requiredImageNameSplit[0];
    String requiredImageTag =
        (requiredImageNameSplit.length > 1 ? requiredImageNameSplit[1] : null);

    String[] availableImageNameSplit = availableImageName.split(":");
    String availableImageBaseName = availableImageNameSplit[0];
    String availableImageTag =
        (availableImageNameSplit.length > 1 ? availableImageNameSplit[1] : null);

    // Match name
    boolean nameMatch = requiredImageBaseName.equals(availableImageBaseName);
    // Match tag (if not given the match is true)
    boolean tagMatch =
        (requiredImageTag != null ? requiredImageTag.equals(availableImageTag) : true);

    return nameMatch && tagMatch;
  }

  @Override
  public void addElasticClusterParameters(ArchiveRoot parsingResult, String deploymentId) {
    if (parsingResult.getTopology() != null) {
      Map<String, NodeTemplate> nodes = parsingResult.getTopology().getNodeTemplates();
      if (nodes != null) {
        for (Map.Entry<String, NodeTemplate> entry : nodes.entrySet()) {
          if (entry.getValue().getType().equals("tosca.nodes.indigo.ElasticCluster")) {
            // Create new property with the deploymentId and set as printable
            ScalarPropertyValue scalarPropertyValue = new ScalarPropertyValue(deploymentId);
            scalarPropertyValue.setPrintable(true);
            entry.getValue().getProperties().put("deployment_id", scalarPropertyValue);
            // Create new property with the orchestrator_url and set as printable
            scalarPropertyValue = new ScalarPropertyValue(orchestratorUrl);
            scalarPropertyValue.setPrintable(true);
            entry.getValue().getProperties().put("orchestrator_url", scalarPropertyValue);
          }
        }
      }
    }
  }

  private void removeRemovalList(ParsingResult<ArchiveRoot> parsingResult) {
    if (parsingResult.getResult().getTopology() != null) {
      Map<String, NodeTemplate> nodes = parsingResult.getResult().getTopology().getNodeTemplates();
      if (nodes != null) {
        for (Map.Entry<String, NodeTemplate> entry : nodes.entrySet()) {
          Capability scalable = getNodeCapabilityByName(entry.getValue(), "scalable");
          if (scalable != null && scalable.getProperties().containsKey("removal_list")) {
            scalable.getProperties().remove("removal_list");
          }
        }
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

  /**
   * Find a property with a given name in a capability.
   * 
   * @param capability
   *          the capability
   * @param propertyName
   *          the name of the property
   * @return the {@link AbstractPropertyValue} containing the property value
   */
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
  public Map<String, NodeTemplate> getAssociatedNodesByCapability(Map<String, NodeTemplate> nodes,
      NodeTemplate nodeTemplate, String capabilityName) {
    Map<String, NodeTemplate> associatedNodes = new HashMap<>();

    List<RelationshipTemplate> relationships =
        getRelationshipTemplatesByCapabilityName(nodeTemplate.getRelationships(), capabilityName);
    if (!relationships.isEmpty()) {
      for (RelationshipTemplate relationship : relationships) {
        String associatedNodeName = relationship.getTarget();
        associatedNodes.put(associatedNodeName, nodes.get(associatedNodeName));
      }
    }

    return associatedNodes;
  }

  @Override
  public List<RelationshipTemplate> getRelationshipTemplatesByCapabilityName(
      Map<String, RelationshipTemplate> relationships, String capabilityName) {

    List<RelationshipTemplate> relationshipTemplates = new ArrayList<>();
    if (relationships == null) {
      return relationshipTemplates;
    }

    for (Map.Entry<String, RelationshipTemplate> relationship : relationships.entrySet()) {
      if (relationship.getValue().getTargetedCapabilityName().equals(capabilityName)) {
        relationshipTemplates.add(relationship.getValue());
      }
    }
    return relationshipTemplates;
  }

  @Override
  public Map<String, NodeTemplate> getCountNodes(ArchiveRoot archiveRoot) {
    Map<String, NodeTemplate> nodes = new HashMap<>();
    if (archiveRoot.getTopology() != null) {
      Map<String, NodeTemplate> allNodes = archiveRoot.getTopology().getNodeTemplates();
      if (allNodes != null) {
        for (Map.Entry<String, NodeTemplate> entry : allNodes.entrySet()) {
          Capability scalable = getNodeCapabilityByName(entry.getValue(), "scalable");
          if (scalable != null) {
            ScalarPropertyValue scalarPropertyValue =
                (ScalarPropertyValue) scalable.getProperties().get("count");
            // Check if this value is read from the template and is not a default value
            if (scalarPropertyValue != null && scalarPropertyValue.isPrintable()) {
              nodes.put(entry.getKey(), entry.getValue());
            }
          }
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

  // @Override
  // public String updateCount(ArchiveRoot archiveRoot, int count) throws IOException {
  // for (Map.Entry<String, NodeTemplate> entry : archiveRoot.getTopology().getNodeTemplates()
  // .entrySet()) {
  // Capability scalable = getNodeCapabilityByName(entry.getValue(), "scalable");
  // if (scalable != null) {
  // ScalarPropertyValue scalarPropertyValue =
  // (ScalarPropertyValue) scalable.getProperties().get("count");
  // if (scalarPropertyValue.isPrintable()) {
  // scalarPropertyValue.setValue(String.valueOf(count));
  // scalable.getProperties().put("count", scalarPropertyValue);
  // }
  // }
  // }
  // return getTemplateFromTopology(archiveRoot);
  // }

  @Override
  public Map<String, OneData> extractOneDataRequirements(ArchiveRoot archiveRoot,
      Map<String, Object> inputs) {
    try {

      // FIXME: Remove hard-coded input extraction and search on OneData nodes instead

      /*
       * By now, OneData requirements are in user's input fields: input_onedata_space,
       * output_onedata_space, [input_onedata_providers, output_onedata_providers]
       */
      Map<String, OneData> result = new HashMap<>();
      if (inputs.get("input_onedata_space") != null) {
        // FIXME: Remove temporary check for limited functionalities
        if (inputs.get("input_onedata_providers") == null
            || ((String) inputs.get("input_onedata_providers")).isEmpty()) {
          throw new IllegalArgumentException(
              String.format("TEMPORARY: As for the current implementation,"
                  + " 'input_onedata_providers' cannot be empty"));
        }

        result.put("input", new OneData(null, (String) inputs.get("input_onedata_space"), null,
            (String) inputs.get("input_onedata_providers")));
        LOG.debug("Extracted OneData requirement for node <{}>: <{}>", "input",
            result.get("input"));
      }

      if (inputs.get("output_onedata_space") != null) {
        // FIXME: Remove temporary check for limited functionalities
        if (inputs.get("output_onedata_providers") == null
            || ((String) inputs.get("output_onedata_providers")).isEmpty()) {
          throw new IllegalArgumentException(
              String.format("TEMPORARY: As for the current implementation,"
                  + " 'output_onedata_providers' cannot be empty"));
        }

        result.put("output", new OneData(null, (String) inputs.get("output_onedata_space"), null,
            (String) inputs.get("output_onedata_providers")));
        LOG.debug("Extracted OneData requirement for node <{}>: <{}>", "output",
            result.get("output"));
      }

      if (result.size() == 0) {
        LOG.debug("No OneData requirements to extract");
      }

      return result;
    } catch (Exception ex) {
      throw new RuntimeException("Failed to extract OneData requirements: " + ex.getMessage(), ex);
    }
  }
}