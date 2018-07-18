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

import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.ComplexPropertyValue;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.ListPropertyValue;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.security.model.Role;
import alien4cloud.tosca.ArchiveParser;
import alien4cloud.tosca.ArchiveUploadService;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.normative.InvalidPropertyValueException;
import alien4cloud.tosca.parser.ParsingContext;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.serializer.VelocityUtil;
import alien4cloud.utils.FileUtil;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

import it.reply.orchestrator.config.properties.OidcProperties.OidcClientProperties;
import it.reply.orchestrator.config.properties.OrchestratorProperties;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.ImageData;
import it.reply.orchestrator.dto.cmdb.ImageData.ImageDataBuilder;
import it.reply.orchestrator.dto.cmdb.Type;
import it.reply.orchestrator.dto.deployment.PlacementPolicy;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.ToscaConstants;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.validation.ValidationException;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DirectedMultigraph;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ToscaServiceImpl implements ToscaService {

  public static final String REMOVAL_LIST_PROPERTY_NAME = "removal_list";

  public static final String SCALABLE_CAPABILITY_NAME = "scalable";

  public static final String OS_CAPABILITY_NAME = "os";

  @Autowired
  private ApplicationContext ctx;

  @Autowired
  private ArchiveParser parser;

  @Autowired
  private ArchiveUploadService archiveUploadService;

  @Autowired
  private IndigoInputsPreProcessorService indigoInputsPreProcessorService;

  @Autowired
  private OAuth2TokenService oauth2tokenService;

  @Value("${directories.alien}/${directories.csar_repository}")
  private String alienRepoDir;

  @Value("${tosca.definitions.normative}")
  private String normativeLocalName;

  @Value("${tosca.definitions.indigo}")
  private String indigoLocalName;

  @Autowired
  private OrchestratorProperties orchestratorProperties;

  /**
   * Load normative and non-normative types.
   * 
   */
  @PostConstruct
  public void init() throws IOException, CSARVersionAlreadyExistsException, ParsingException {
    if (Paths.get(alienRepoDir).toFile().exists()) {
      FileUtil.delete(Paths.get(alienRepoDir));
    }

    // set requiredAuth to upload TOSCA types
    Optional<Authentication> oldAuth = setAutenticationForToscaImport();

    try (InputStream is = ctx.getResource(normativeLocalName).getInputStream()) {
      Path zipFile = File.createTempFile(normativeLocalName, ".zip").toPath();
      zip(is, zipFile);
      ParsingResult<Csar> result = archiveUploadService.upload(zipFile);
      if (!result.getContext().getParsingErrors().isEmpty()) {
        LOG.warn("Error parsing definition {}:\n{}", normativeLocalName,
            Arrays.toString(result.getContext().getParsingErrors().toArray()));
      }
    }

    try (InputStream is = ctx.getResource(indigoLocalName).getInputStream()) {
      Path zipFile = File.createTempFile(indigoLocalName, ".zip").toPath();
      zip(is, zipFile);
      ParsingResult<Csar> result = archiveUploadService.upload(zipFile);
      if (!result.getContext().getParsingErrors().isEmpty()) {
        LOG.warn("Error parsing definition {}:\n{}", indigoLocalName,
            Arrays.toString(result.getContext().getParsingErrors().toArray()));
      }
    }

    // restore old auth if present
    oldAuth.ifPresent(SecurityContextHolder.getContext()::setAuthentication);

  }

  /**
   * Utility to zip an inputStream.
   * 
   */
  public static void zip(InputStream fileStream, Path outputPath) throws IOException {
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
  public ParsingResult<ArchiveRoot> getArchiveRootFromTemplate(String toscaTemplate)
      throws ParsingException {
    try (ByteArrayInputStream is = new ByteArrayInputStream(toscaTemplate.getBytes());) {
      Path zipPath = Files.createTempFile("csar", ".zip");
      zip(is, zipPath);
      ParsingResult<ArchiveRoot> result = parser.parse(zipPath);
      try {
        Files.delete(zipPath);
      } catch (IOException ioe) {
        LOG.warn("Error deleting tmp csar {} from FS", zipPath, ioe);
      }
      return result;
    } catch (InvalidArgumentException iae) {
      // FIXME NOTE that InvalidArgumentException should not be thrown by the parse method, but it
      // is...
      // throw new ParsingException("DUMMY", new ParsingError(ErrorCode.INVALID_YAML, ));
      throw iae;
    } catch (IOException ex) {
      throw new OrchestratorException("Error Parsing TOSCA Template", ex);
    }
  }

  @Override
  public String getTemplateFromTopology(ArchiveRoot archiveRoot) {
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
    try {
      VelocityUtil.generate("templates/topology-1_0_0_INDIGO.yml.vm", writer, velocityCtx);
    } catch (IOException ex) {
      throw new OrchestratorException("Error serializing TOSCA template", ex);
    }
    String template = writer.toString();

    // Log the warning because Alien4Cloud uses an hard-coded Velocity template to encode the string
    // and some information might be missing!!
    LOG.warn("TOSCA template conversion from in-memory: "
        + "WARNING: Some nodes or properties might be missing!! Use at your own risk!");
    LOG.debug(template);

    return template;
  }

  @Override
  public void replaceInputFunctions(ArchiveRoot archiveRoot, Map<String, Object> inputs) {
    indigoInputsPreProcessorService.processGetInput(archiveRoot, inputs);
  }

  @Override
  public ArchiveRoot parseAndValidateTemplate(String toscaTemplate, Map<String, Object> inputs) {
    ArchiveRoot ar = parseTemplate(toscaTemplate);
    Optional.ofNullable(ar.getTopology()).map(topology -> topology.getInputs()).ifPresent(
        topologyInputs -> validateUserInputs(topologyInputs, inputs));
    return ar;
  }

  @Override
  public ArchiveRoot prepareTemplate(String toscaTemplate, Map<String, Object> inputs) {
    ArchiveRoot ar = parseAndValidateTemplate(toscaTemplate, inputs);
    replaceInputFunctions(ar, inputs);
    return ar;
  }

  @Override
  public void validateUserInputs(Map<String, PropertyDefinition> templateInputs,
      Map<String, Object> inputs) {

    // Check if every required input has been given by the user or has a default value
    templateInputs.forEach((inputName, inputDefinition) -> {
      if (inputDefinition.isRequired()
          && inputs.getOrDefault(inputName, inputDefinition.getDefault()) == null) {
        // Input required and no value to replace -> error
        throw new ToscaException(
            String.format("Input <%s> is required and is not present in the user's input list,"
                + " nor has a default value", inputName));
      }
    });

    // Reference:
    // http://docs.oasis-open.org/tosca/TOSCA-Simple-Profile-YAML/v1.0/csprd02/TOSCA-Simple-Profile-YAML-v1.0-csprd02.html#TYPE_YAML_STRING
    // FIXME Check input type compatibility ?
    // templateInput.getValue().getType()
  }

  @Override
  public ArchiveRoot parseTemplate(String toscaTemplate) {

    try {
      ParsingResult<ArchiveRoot> result = getArchiveRootFromTemplate(toscaTemplate);
      Optional<ToscaException> exception = checkParsingErrors(
          Optional.ofNullable(result.getContext()).map(ParsingContext::getParsingErrors));
      if (exception.isPresent()) {
        throw exception.get();
      }
      return result.getResult();
    } catch (ParsingException ex) {
      Optional<ToscaException> exception =
          checkParsingErrors(Optional.ofNullable(ex.getParsingErrors()));
      if (exception.isPresent()) {
        throw exception.get();
      } else {
        throw new ToscaException("Failed to parse template, ex");
      }
    }
  }

  @Override
  public String updateTemplate(String template) {
    ArchiveRoot parsedTempalte = parseTemplate(template);
    removeRemovalList(parsedTempalte);
    return getTemplateFromTopology(parsedTempalte);
  }

  private Optional<ToscaException> checkParsingErrors(Optional<List<ParsingError>> errorList) {
    return filterNullAndInfoErrorFromParsingError(errorList)
        .map(list -> list.stream().map(Object::toString).collect(Collectors.joining("; ")))
        .map(ToscaException::new);
  }

  private Optional<List<ParsingError>> filterNullAndInfoErrorFromParsingError(
      Optional<List<ParsingError>> listToFilter) {
    return listToFilter.map(list -> list.stream()
        .filter(Objects::nonNull)
        .filter(error -> !ParsingErrorLevel.INFO.equals(error.getErrorLevel()))
        .collect(Collectors.toList())).filter(list -> !list.isEmpty());
  }

  @Override
  public void contextualizeAndReplaceImages(ArchiveRoot parsingResult, CloudProvider cloudProvider,
      String cloudServiceId, DeploymentProvider deploymentProvider) {
    Map<Boolean, Map<NodeTemplate, ImageData>> contextualizedImages =
        contextualizeImages(parsingResult, cloudProvider, cloudServiceId);
    Preconditions.checkState(contextualizedImages.get(Boolean.FALSE).isEmpty(),
        "Error contextualizing images; images for nodes %s couldn't be contextualized",
        contextualizedImages
            .get(Boolean.FALSE)
            .keySet()
            .stream()
            .map(NodeTemplate::getName)
            .collect(Collectors.toList()));
    replaceImage(contextualizedImages.get(Boolean.TRUE), cloudProvider, deploymentProvider);
  }

  @Override
  public Map<NodeTemplate, ImageData> extractImageRequirements(ArchiveRoot parsingResult) {

    Map<String, Function<ImageDataBuilder, Function<String, ImageDataBuilder>>> 
        capabilityPropertiesMapping = new HashMap<>();

    capabilityPropertiesMapping.put("image",
        imageMetadataBuilder -> imageMetadataBuilder::imageName);

    capabilityPropertiesMapping.put("architecture",
        imageMetadataBuilder -> imageMetadataBuilder::architecture);

    capabilityPropertiesMapping.put("type",
        imageMetadataBuilder -> imageMetadataBuilder::type);

    capabilityPropertiesMapping.put("distribution",
        imageMetadataBuilder -> imageMetadataBuilder::distribution);

    capabilityPropertiesMapping.put("version",
        imageMetadataBuilder -> imageMetadataBuilder::version);

    // Only indigo.Compute nodes are relevant
    return getNodesOfType(parsingResult, ToscaConstants.Nodes.COMPUTE)
        .stream()
        .map(node -> {
          ImageDataBuilder imageMetadataBuilder = ImageData.builder();
          Optional
              .ofNullable(node.getCapabilities())
              .map(capabilities -> capabilities.get(OS_CAPABILITY_NAME))
              .ifPresent(osCapability -> {
                // We've got an OS capability -> Check the attributes to find best match for the
                // image
                capabilityPropertiesMapping.forEach((capabilityPropertyName, mappingFunction) -> {
                  this
                      .<ScalarPropertyValue>getTypedCapabilityPropertyByName(osCapability,
                          capabilityPropertyName)
                      .map(ScalarPropertyValue::getValue)
                      .filter(Objects::nonNull)
                      .ifPresent(
                          property -> mappingFunction.apply(imageMetadataBuilder).apply(property));
                });
              });
          return new SimpleEntry<>(node, imageMetadataBuilder.build());
        })
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  @Override
  public Map<Boolean, Map<NodeTemplate, ImageData>> contextualizeImages(ArchiveRoot parsingResult,
      CloudProvider cloudProvider, String cloudServiceId) {
    try {
      return extractImageRequirements(parsingResult).entrySet().stream().map(entry -> {
        NodeTemplate node = entry.getKey();
        ImageData imageMetadata = entry.getValue();

        final Optional<ImageData> image;
        // TODO FILTER ON DEPLOYMENT PROVIDER?
        if (isImImageUri(imageMetadata.getImageName())) {
          image = Optional.of(imageMetadata);
        } else {
          List<ImageData> images = cloudProvider.getCmdbProviderImages()
              .getOrDefault(cloudServiceId, Collections.emptyList());
          image = getBestImageForCloudProvider(imageMetadata, images);
        }

        if (image.isPresent()) {
          // Found a good image -> replace the image attribute with the provider-specific ID
          LOG.debug(
              "Found image match in <{}> for image metadata <{}>"
                  + ", provider-specific image id <{}>",
              cloudProvider.getId(), imageMetadata, image.get().getImageId());
        } else {
          // No image match found -> throw error
          LOG.debug("Failed to found a match in provider <{}> for image metadata <{}>",
              cloudProvider.getId(), imageMetadata);
        }
        return new SimpleEntry<>(node, image);
      }).collect(Collectors.partitioningBy(entry -> entry.getValue().isPresent(),
          // do not use the Collectors.toMap() because it doesn't play nice with null values
          // https://bugs.openjdk.java.net/browse/JDK-8148463
          CommonUtils.toMap(Entry::getKey, entry -> entry.getValue().orElse(null))));
    } catch (RuntimeException ex) {
      throw new RuntimeException("Failed to contextualize images", ex);
    }
  }

  private void replaceImage(Map<NodeTemplate, ImageData> contextualizedImages,
      CloudProvider cloudProvider, DeploymentProvider deploymentProvider) {
    contextualizedImages.forEach((node, image) -> {
      Map<String, Capability> capabilities =
          Optional.ofNullable(node.getCapabilities()).orElseGet(() -> {
            node.setCapabilities(new HashMap<>());
            return node.getCapabilities();
          });
      // The node doesn't have an OS Capability -> need to add a dummy one to hold a
      // random image for underlying deployment systems
      Capability osCapability = capabilities.computeIfAbsent(OS_CAPABILITY_NAME, key -> {
        LOG.debug("Generating default OperatingSystem capability for node <{}>", node.getName());
        Capability capability = new Capability();
        capability.setType("tosca.capabilities.indigo.OperatingSystem");
        return capability;
      });
      String imageId = image.getImageId();
      if (deploymentProvider == DeploymentProvider.IM) {
        if (isImImageUri(image.getImageName())) {
          imageId = image.getImageName();
        } else {
          imageId = generateImImageUri(cloudProvider, image);
        }
      }
      ScalarPropertyValue scalarPropertyValue = createScalarPropertyValue(imageId);
      osCapability.getProperties().put("image", scalarPropertyValue);

      if (StringUtils.isNotBlank(image.getUserName())) {
        Map<String, Object> credential = new HashMap<>();
        ComplexPropertyValue credentialProperty = new ComplexPropertyValue(credential);
        credentialProperty.setPrintable(true);
        osCapability.getProperties().put("credential", credentialProperty);

        scalarPropertyValue = createScalarPropertyValue(image.getUserName());
        credential.put("user", scalarPropertyValue);

        scalarPropertyValue = createScalarPropertyValue("");
        credential.put("token", scalarPropertyValue);
      }
    });
  }

  @Deprecated
  private boolean isImImageUri(String imageName) {
    try {
      List<String> schemes = ImmutableList.of("ost", "one", "aws", "azr");
      return schemes.contains(URI.create(imageName).getScheme().trim());
    } catch (RuntimeException ex) {
      return false;
    }
  }

  @Deprecated
  private String generateImImageUri(CloudProvider cloudProvider, ImageData image) {
    try {
      CloudService cs;
      if (image.getService() != null
          && cloudProvider.getCmdbProviderServices().get(image.getService()) != null) {
        cs = cloudProvider.getCmdbProviderServices().get(image.getService());
      } else {
        if (cloudProvider.getCmbdProviderServicesByType(Type.COMPUTE).isEmpty()) {
          throw new DeploymentException(
              "No compute service available fo cloud provider " + cloudProvider.getId());
        } else {
          cs = cloudProvider.getCmbdProviderServicesByType(Type.COMPUTE).get(0);
        }
      }
      StringBuilder sb = new StringBuilder();

      if (cs.isOpenStackComputeProviderService() || cs.isOtcComputeProviderService()) {
        sb
            .append("ost")
            .append("://")
            .append(new URL(cs.getData().getEndpoint()).getHost())
            .append("/");
      } else if (cs.isOpenNebulaComputeProviderService() || cs.isOpenNebulaToscaProviderService()) {
        sb
            .append("one")
            .append("://")
            .append(new URL(cs.getData().getEndpoint()).getHost())
            .append("/");
      } else if (cs.isOcciComputeProviderService()) {
        // DO NOTHING ??
      } else if (cs.isAwsComputeProviderService()) {
        sb
            .append("aws")
            .append("://");
      } else if (cs.isAzureComputeProviderService()) {
        sb
            .append("azr")
            .append("://");
      } else {
        throw new DeploymentException("Unknown IaaSType of cloud provider " + cloudProvider);
      }

      sb.append(image.getImageId());
      return sb.toString();
    } catch (RuntimeException | MalformedURLException ex) {
      LOG.error("Cannot retrieve Compute service host for IM image id generation", ex);
      return image.getImageId();
    }
  }

  protected Optional<ImageData> getBestImageForCloudProvider(ImageData imageMetadata,
      Collection<ImageData> images) {

    // Match image name first (for INDIGO specific use case, if the image cannot be found with the
    // specified name it means that a base image + Ansible configuration have to be used -> the
    // base image will be chosen with the other filters and image metadata - architecture, type,
    // distro, version)
    Optional<ImageData> imageFoundByName = findImageByName(imageMetadata, images);
    if (imageFoundByName.isPresent()) {
      return imageFoundByName;
    } else {
      return findImageByFallbackFields(imageMetadata, images);
    }
  }

  protected Optional<ImageData> findImageByName(ImageData requiredImageMetadata,
      Collection<ImageData> cloudProviderServiceImages) {
    String requiredImageName = requiredImageMetadata.getImageName();
    if (requiredImageName != null) {
      LOG.debug("Looking up images by name <{}>", requiredImageName);
      Optional<ImageData> imageWithName = cloudProviderServiceImages
          .stream()
          .filter(image -> requiredImageMetadata(requiredImageName, image.getImageName()))
          .findFirst();

      if (imageWithName.isPresent()) {
        LOG.debug("Image <{}> found with name <{}>", imageWithName.get().getImageId(),
            requiredImageName);
        return imageWithName;
      }
    }
    return Optional.empty();
  }

  protected boolean requiredImageMetadata(@NonNull String requiredImageName,
      @Nullable String availableImageName) {
    if (availableImageName == null) {
      return false;
    }
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

  protected Optional<ImageData> findImageByFallbackFields(ImageData requiredImageMetadata,
      Collection<ImageData> cloudProviderServiceImages) {
    LOG.debug("Looking up images by fallback metatada");
    List<Function<ImageData, String>> fallbackFieldExtractors = Lists.newArrayList(
        ImageData::getType,
        ImageData::getArchitecture,
        ImageData::getDistribution,
        ImageData::getVersion);

    Stream<ImageData> imageStream = cloudProviderServiceImages.stream();

    boolean filteredOnSomeField = false;
    for (Function<ImageData, String> fieldExtractor : fallbackFieldExtractors) {
      String metadataField = fieldExtractor.apply(requiredImageMetadata);
      if (metadataField != null) {
        filteredOnSomeField = true;
        // if the field is populated for requiredImageMetadata, filter on it
        imageStream = imageStream
            .filter(image -> metadataField.equalsIgnoreCase(fieldExtractor.apply(image)));
      }
    }

    boolean imageNameIsPresent = requiredImageMetadata.getImageName() != null;
    if (!filteredOnSomeField && imageNameIsPresent) {
      return Optional.empty();
    } else {
      return imageStream.findFirst();
    }
    
  }

  private Collection<NodeTemplate> getNodesFromArchiveRoot(ArchiveRoot archiveRoot) {
    return Optional.ofNullable(archiveRoot.getTopology())
        .map(this::getNodesFromTopology)
        .orElseGet(ArrayList::new);
  }

  private Collection<NodeTemplate> getNodesFromTopology(Topology topology) {
    return Optional.ofNullable(topology.getNodeTemplates())
        .map(Map::values)
        .map(nodes -> nodes.stream().filter(Objects::nonNull).collect(Collectors.toList()))
        .orElseGet(ArrayList::new);
  }

  @Override
  public Collection<NodeTemplate> getNodesOfType(ArchiveRoot archiveRoot, String type) {
    Preconditions.checkNotNull(type);
    return getNodesFromArchiveRoot(archiveRoot).stream()
        .filter(node -> isOfToscaType(node, type))
        .collect(Collectors.toList());
  }

  @Override
  public boolean isOfToscaType(NodeTemplate node, String nodeType) {
    return isSubTypeOf(Preconditions.checkNotNull(node).getType(), nodeType);
  }
  
  @Override
  public boolean isOfToscaType(Resource resource, String nodeType) {
    return isSubTypeOf(Preconditions.checkNotNull(resource).getToscaNodeType(), nodeType);
  }
  
  private boolean isSubTypeOf(@Nullable String optionalNodeType, String superNodeType) {
    return Optional
        .ofNullable(optionalNodeType)
        // FIXME: Check inheritance
        .filter(nodeType -> CommonUtils
            .checkNotNull(nodeType)
            .equals(Preconditions.checkNotNull(superNodeType)))
        .isPresent();
  }

  @Override
  public boolean isHybridDeployment(ArchiveRoot archiveRoot) {
    // check if there is a "hybrid" ScalarPropertyValue with "true" as value
    return getNodesOfType(archiveRoot, ToscaConstants.Nodes.ELASTIC_CLUSTER).stream()
        .anyMatch(node -> getNodePropertyByName(node, "hybrid")
            .filter(ScalarPropertyValue.class::isInstance)
            .map(ScalarPropertyValue.class::cast)
            .map(ScalarPropertyValue::getValue)
            .filter(Boolean::valueOf)
            .isPresent());
  }

  @Override
  public void addElasticClusterParameters(ArchiveRoot archiveRoot, String deploymentId,
      @Nullable String oauthToken) {
    getNodesOfType(archiveRoot, ToscaConstants.Nodes.ELASTIC_CLUSTER).forEach(node -> {
      // create properties Map if null
      Map<String, AbstractPropertyValue> properties =
          Optional.ofNullable(node.getProperties()).orElseGet(() -> {
            node.setProperties(new HashMap<>());
            return node.getProperties();
          });

      // Create new property with the deploymentId and set as printable
      properties.put("deployment_id", createScalarPropertyValue(deploymentId));

      // Create new property with the orchestrator_url and set as printable
      properties.put("orchestrator_url",
          createScalarPropertyValue(orchestratorProperties.getUrl().toString()));

      if (oauthToken != null) {
        // Create new property with the iam_access_token and set as printable
        properties.put("iam_access_token", createScalarPropertyValue(oauthToken));

        Optional<OidcClientProperties> cluesInfo = oauth2tokenService.getCluesInfo(oauthToken);
        cluesInfo.ifPresent(info -> {
          // Create new property with the iam_clues_client_id and set as printable
          properties.put("iam_clues_client_id", createScalarPropertyValue(info.getClientId()));

          // Create new property with the iam_clues_client_secret and set as printable
          properties.put("iam_clues_client_secret",
              createScalarPropertyValue(info.getClientSecret()));
        });
      }
    });
  }

  private static ScalarPropertyValue createScalarPropertyValue(String value) {
    ScalarPropertyValue scalarPropertyValue = new ScalarPropertyValue(value);
    scalarPropertyValue.setPrintable(true);
    return scalarPropertyValue;
  }


  public void removeRemovalList(ArchiveRoot archiveRoot) {
    getNodesFromArchiveRoot(archiveRoot)
        .forEach(this::removeRemovalList);
  }

  @Override
  public void removeRemovalList(NodeTemplate node) {
    getNodeCapabilityByName(node, SCALABLE_CAPABILITY_NAME)
        .ifPresent(scalable -> CommonUtils
            .removeFromOptionalMap(scalable.getProperties(), REMOVAL_LIST_PROPERTY_NAME));
  }

  private static Optional<Authentication> setAutenticationForToscaImport() {
    Authentication oldAuth = SecurityContextHolder.getContext().getAuthentication();

    Authentication newAuth = new PreAuthenticatedAuthenticationToken(
        Role.ADMIN.name().toLowerCase(), "", AuthorityUtils.createAuthorityList(Role.ADMIN.name()));

    SecurityContextHolder.getContext().setAuthentication(newAuth);
    return Optional.ofNullable(oldAuth);
  }

  @Override
  public Optional<Capability> getNodeCapabilityByName(NodeTemplate node, String capabilityName) {
    return CommonUtils.getFromOptionalMap(node.getCapabilities(), capabilityName);
  }

  @Override
  public Optional<AbstractPropertyValue> getNodePropertyByName(NodeTemplate node,
      String propertyName) {
    return CommonUtils.getFromOptionalMap(node.getProperties(), propertyName);

  }

  @Override
  public <T extends AbstractPropertyValue> Optional<T> getTypedNodePropertyByName(NodeTemplate node,
      String propertyName) {
    return CommonUtils.optionalCast(getNodePropertyByName(node, propertyName));
  }

  @Override
  public Optional<AbstractPropertyValue> getCapabilityPropertyByName(Capability capability,
      String propertyName) {
    return CommonUtils.getFromOptionalMap(capability.getProperties(), propertyName);
  }

  @Override
  public <T extends AbstractPropertyValue> Optional<T> getTypedCapabilityPropertyByName(
      Capability capability, String propertyName) {
    return CommonUtils.optionalCast(getCapabilityPropertyByName(capability, propertyName));
  }

  @Override
  public Optional<DeploymentArtifact> getNodeArtifactByName(NodeTemplate node,
      String artifactName) {
    return CommonUtils.getFromOptionalMap(node.getArtifacts(), artifactName);
  }

  @Override
  public Map<String, NodeTemplate> getAssociatedNodesByCapability(Map<String, NodeTemplate> nodes,
      NodeTemplate nodeTemplate, String capabilityName) {

    return getRelationshipTemplatesByCapabilityName(nodeTemplate.getRelationships(), capabilityName)
        .stream().map(relationship -> relationship.getTarget()).collect(
            Collectors.toMap(associatedNodeName -> associatedNodeName,
                associatedNodeName -> nodes.get(associatedNodeName)));
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
  public Optional<Integer> getCount(NodeTemplate nodeTemplate) {
    return getNodeCapabilityByName(nodeTemplate, SCALABLE_CAPABILITY_NAME)
        .flatMap(capability -> this
            .<ScalarPropertyValue>getTypedCapabilityPropertyByName(capability, "count"))
        .map(property -> property.getValue())
        .map(value -> Integer.parseInt(value));
  }
  
  @Override
  public boolean isScalable(NodeTemplate nodeTemplate) {
    return getCount(nodeTemplate).isPresent();
  }
  
  @Override
  public Collection<NodeTemplate> getScalableNodes(ArchiveRoot archiveRoot) {
    return getNodesFromArchiveRoot(archiveRoot)
        .stream()
        .filter(this::isScalable)
        .collect(Collectors.toList());
  }

  @Override
  public List<String> getRemovalList(NodeTemplate nodeTemplate) {

    Optional<ListPropertyValue> listPropertyValue =
        getNodeCapabilityByName(nodeTemplate, SCALABLE_CAPABILITY_NAME)
            .flatMap(capability -> getTypedCapabilityPropertyByName(capability,
                REMOVAL_LIST_PROPERTY_NAME));

    List<Object> items =
        listPropertyValue.map(property -> property.getValue()).orElseGet(Collections::emptyList);

    List<String> removalList = new ArrayList<>();
    for (Object item : items) {
      if (item instanceof ScalarPropertyValue) {
        removalList.add(((ScalarPropertyValue) item).getValue());
      } else if (item instanceof String) {
        removalList.add((String) item);
      } else {
        LOG.warn("Skipped unsupported value <{}> in {} of node {}", item,
            REMOVAL_LIST_PROPERTY_NAME, nodeTemplate.getName());
      }
    }
    return removalList;
  }

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
      OneData oneDataInput = null;
      if (inputs.get("input_onedata_space") != null) {
        oneDataInput = OneData.builder()
            .token((String) inputs.get("input_onedata_token"))
            .space((String) inputs.get("input_onedata_space"))
            .path((String) inputs.get("input_path"))
            .providers((String) inputs.get("input_onedata_providers"))
            .zone((String) inputs.get("input_onedata_zone"))
            .build();
        if (oneDataInput.getProviders().isEmpty()) {
          oneDataInput.setSmartScheduling(true);
        }
        result.put("input", oneDataInput);
        LOG.debug("Extracted OneData requirement for node <{}>: <{}>", "input", oneDataInput);
      }

      if (inputs.get("output_onedata_space") != null) {
        OneData oneDataOutput = OneData.builder()
            .token((String) inputs.get("output_onedata_token"))
            .space((String) inputs.get("output_onedata_space"))
            .path((String) inputs.get("output_path"))
            .providers((String) inputs.get("output_onedata_providers"))
            .zone((String) inputs.get("output_onedata_zone"))
            .build();
        if (oneDataOutput.getProviders().isEmpty()) {
          if (oneDataInput != null) {
            oneDataOutput.setProviders(oneDataInput.getProviders());
            oneDataOutput.setSmartScheduling(oneDataInput.isSmartScheduling());
          } else {
            oneDataOutput.setSmartScheduling(true);
          }
        }
        result.put("output", oneDataOutput);
        LOG.debug("Extracted OneData requirement for node <{}>: <{}>", "output", oneDataOutput);
      }

      if (result.size() == 0) {
        LOG.debug("No OneData requirements to extract");
      }

      return result;
    } catch (RuntimeException ex) {
      throw new RuntimeException("Failed to extract OneData requirements: " + ex.getMessage(), ex);
    }
  }

  @Override
  public Map<String, PlacementPolicy> extractPlacementPolicies(ArchiveRoot archiveRoot) {
    Map<String, PlacementPolicy> placementPolicies = new HashMap<>();
    Optional.ofNullable(archiveRoot.getTopology())
        .map(topology -> topology.getPolicies())
        .orElseGet(Collections::emptyList)
        .forEach(policy -> {
          if (policy instanceof alien4cloud.model.topology.PlacementPolicy) {
            PlacementPolicy placementPolicy =
                PlacementPolicy.fromToscaType((alien4cloud.model.topology.PlacementPolicy) policy);
            placementPolicies.put(policy.getName(), placementPolicy);
          } else {
            LOG.warn("Skipping unsupported Policy {}", policy);
          }
        });

    return placementPolicies;
  }

  @Override
  public DirectedMultigraph<NodeTemplate, RelationshipTemplate> buildNodeGraph(
      Map<String, NodeTemplate> nodes, boolean checkForCycles) {

    DirectedMultigraph<NodeTemplate, RelationshipTemplate> graph =
        new DirectedMultigraph<>(RelationshipTemplate.class);

    nodes.entrySet().forEach(nodeEntry -> {
      NodeTemplate toNode = nodeEntry.getValue();
      graph.addVertex(toNode);

      Map<String, RelationshipTemplate> relationships =
          Optional.ofNullable(toNode.getRelationships()).orElseGet(HashMap::new);

      relationships.values().forEach(relationship -> {
        NodeTemplate fromNode = nodes.get(relationship.getTarget());
        graph.addVertex(fromNode);
        graph.addEdge(fromNode, toNode, relationship);
      });
    });
    if (checkForCycles) {
      CycleDetector<NodeTemplate, RelationshipTemplate> cycleDetector = new CycleDetector<>(graph);
      Set<NodeTemplate> cyclyingNodes = cycleDetector.findCycles();
      if (!cyclyingNodes.isEmpty()) {
        String message = "Found node depencency loop in TOSCA topology; involved nodes: "
            + Arrays.toString(cyclyingNodes.stream().map(NodeTemplate::getName).toArray());
        LOG.error(message);
        throw new ValidationException(message);
      }
    }
    return graph;
  }

  @Override
  public <T extends IPropertyType<V>, V> V parseScalarPropertyValue(ScalarPropertyValue value,
      Class<T> clazz) {
    T propertyParser = BeanUtils.instantiate(clazz);
    try {
      return propertyParser.parse(value.getValue());
    } catch (InvalidPropertyValueException ex) {
      throw new ToscaException(String.format("Error parsing scalar value <%s> as <%s>",
          value.getValue(), propertyParser.getTypeName()), ex);
    }
  }

  @Override
  public <V> List<V> parseListPropertyValue(ListPropertyValue value, Function<Object, V> mapper) {
    return Optional.ofNullable(value.getValue())
        .orElseGet(Collections::emptyList)
        .stream()
        .filter(item -> item != null)
        .map(mapper)
        .collect(Collectors.toList());
  }

  @Override
  public <V> Map<String, V> parseComplexPropertyValue(ComplexPropertyValue value,
      Function<Object, V> mapper) {
    return Optional.ofNullable(value.getValue())
        .orElseGet(Collections::emptyMap)
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue() != null)
        .collect(Collectors.toMap(Entry::getKey, mapper.compose(Entry::getValue)));
  }

}
