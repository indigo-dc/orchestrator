/*
 * Copyright © 2015-2019 Santer Reply S.p.A.
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

import alien4cloud.configuration.InitialLoader;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.FunctionalException;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.model.components.CSARSource;
import alien4cloud.security.model.Role;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContext;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.serializer.VelocityUtil;
import alien4cloud.utils.FileUtil;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

import it.reply.orchestrator.config.properties.OrchestratorProperties;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.ComputeService;
import it.reply.orchestrator.dto.cmdb.Flavor;
import it.reply.orchestrator.dto.cmdb.Flavor.FlavorBuilder;
import it.reply.orchestrator.dto.cmdb.Image;
import it.reply.orchestrator.dto.cmdb.Image.ImageBuilder;
import it.reply.orchestrator.dto.dynafed.Dynafed;
import it.reply.orchestrator.dto.dynafed.Dynafed.DynafedBuilder;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.onedata.OneData.OneDataBuilder;
import it.reply.orchestrator.dto.onedata.OneData.OneDataProviderInfo;
import it.reply.orchestrator.dto.policies.SlaPlacementPolicy;
import it.reply.orchestrator.dto.policies.ToscaPolicy;
import it.reply.orchestrator.dto.policies.ToscaPolicyFactory;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.ToscaConstants;
import it.reply.orchestrator.utils.ToscaConstants.Nodes;
import it.reply.orchestrator.utils.ToscaUtils;

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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.alien4cloud.tosca.catalog.ArchiveParser;
import org.alien4cloud.tosca.catalog.ArchiveUploadService;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.normative.types.BooleanType;
import org.alien4cloud.tosca.normative.types.IntegerType;
import org.alien4cloud.tosca.normative.types.SizeType;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.elasticsearch.index.query.QueryBuilders;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DirectedMultigraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

  public static final String HOST_CAPABILITY_NAME = "host";

  public static final String REQUIREMENT_DEPENDENCY_CAPABILITY = "tosca.capabilities.Node";
  public static final String REQUIREMENT_DEPENDENCY_RELATIONSHIP = "tosca.relationships.DependsOn";
  public static final String REQUIREMENT_HOST_CAPABILITY = "tosca.capabilities.Container";
  public static final String REQUIREMENT_HOST_RELATIONSHIP = "tosca.relationships.HostedOn";

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

  @Autowired
  @Qualifier("alien-es-dao")
  private IGenericSearchDAO alienDao;

  @Autowired
  private InitialLoader initialLoader;

  /**
   * Load normative and non-normative types.
   *
   */
  @PostConstruct
  public void init() throws IOException, FunctionalException {
    if (Paths.get(alienRepoDir).toFile().exists()) {
      FileUtil.delete(Paths.get(alienRepoDir));
    }
    alienDao.delete(Csar.class, QueryBuilders.matchAllQuery());
    // set requiredAuth to upload TOSCA types
    Optional<Authentication> oldAuth = setAutenticationForToscaImport();

    try (InputStream is = ctx.getResource(normativeLocalName).getInputStream()) {
      Path zipFile = File.createTempFile(normativeLocalName, ".zip").toPath();
      zip(is, zipFile);
      ParsingResult<Csar> result = archiveUploadService
          .upload(zipFile, CSARSource.ORCHESTRATOR, "default");
      if (!result.getContext().getParsingErrors().isEmpty()) {
        LOG.warn("Error parsing definition {}:\n{}", normativeLocalName,
            Arrays.toString(result.getContext().getParsingErrors().toArray()));
      }
    }

    try (InputStream is = ctx.getResource(indigoLocalName).getInputStream()) {
      Path zipFile = File.createTempFile(indigoLocalName, ".zip").toPath();
      zip(is, zipFile);
      ParsingResult<Csar> result = archiveUploadService
          .upload(zipFile, CSARSource.ORCHESTRATOR, "default");
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
      ParsingResult<ArchiveRoot> result = parser.parse(zipPath, "default");
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
    velocityCtx.put("template_description", archiveRoot.getArchive().getDescription());
    velocityCtx.put("template_name", "template");
    velocityCtx.put("template_version", "1.0.0-SNAPSHOT");
    velocityCtx.put("template_author", "orchestrator");
    velocityCtx.put("topology",
        archiveRoot.hasToscaTopologyTemplate() ? archiveRoot.getTopology() : new Topology());
    StringWriter writer = new StringWriter();
    try {
      VelocityUtil
          .generate("org/alien4cloud/tosca/exporter/topology-tosca_simple_yaml_1_0.yml.vm", writer,
              velocityCtx);
    } catch (IOException ex) {
      throw new OrchestratorException("Error serializing TOSCA template", ex);
    }
    String template = writer.toString();

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
    Optional
      .ofNullable(ar.getTopology())
      .map(Topology::getInputs)
      .ifPresent(topologyInputs -> validateUserInputs(topologyInputs, inputs));
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
        throw new ToscaException("Failed to parse template", ex);
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
        .filter(error -> !ErrorCode.INVALID_ARTIFACT_REFERENCE.equals(error.getErrorCode()))
        .collect(Collectors.toList())).filter(list -> !list.isEmpty());
  }

  @Override
  public void contextualizeAndReplaceImages(ArchiveRoot parsingResult,
      ComputeService computeService,
      DeploymentProvider deploymentProvider) {
    Map<Boolean, Map<NodeTemplate, Image>> contextualizedImages =
        contextualizeImages(parsingResult, computeService);
    Preconditions.checkState(contextualizedImages.get(Boolean.FALSE).isEmpty(),
        "Error contextualizing images; images for nodes %s couldn't be contextualized",
        contextualizedImages
            .get(Boolean.FALSE)
            .keySet()
            .stream()
            .map(NodeTemplate::getName)
            .collect(Collectors.toList()));
    replaceImage(contextualizedImages.get(Boolean.TRUE), computeService, deploymentProvider);
  }

  @Override
  public void contextualizeAndReplaceFlavors(ArchiveRoot parsingResult,
      ComputeService computeService,
      DeploymentProvider deploymentProvider) {
    Map<Boolean, Map<NodeTemplate, Flavor>> contextualizedImages =
        contextualizeFlavors(parsingResult, computeService);
    Preconditions.checkState(contextualizedImages.get(Boolean.FALSE).isEmpty(),
        "Error contextualizing flavors; flavors for nodes %s couldn't be contextualized",
        contextualizedImages
            .get(Boolean.FALSE)
            .keySet()
            .stream()
            .map(NodeTemplate::getName)
            .collect(Collectors.toList()));
    replaceFlavor(contextualizedImages.get(Boolean.TRUE), computeService, deploymentProvider);
  }

  @Override
  public Map<NodeTemplate, Image> extractImageRequirements(ArchiveRoot parsingResult) {

    Map<String, Function<ImageBuilder, Function<String, ImageBuilder>>>
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

    capabilityPropertiesMapping.put("gpu_driver",
        imageMetadataBuilder -> (String value) -> imageMetadataBuilder
            .gpuDriver(Boolean.parseBoolean(value)));

    capabilityPropertiesMapping.put("gpu_driver_version",
        imageMetadataBuilder -> imageMetadataBuilder::gpuDriverVersion);

    capabilityPropertiesMapping.put("cuda_support",
        imageMetadataBuilder -> (String value) -> imageMetadataBuilder
            .cudaSupport(Boolean.parseBoolean(value)));

    capabilityPropertiesMapping.put("cuda_min_version",
        imageMetadataBuilder -> imageMetadataBuilder::cudaVersion);

    capabilityPropertiesMapping.put("cuDNN_version",
        imageMetadataBuilder -> imageMetadataBuilder::cuDnnVersion);

    // Only indigo.Compute nodes are relevant
    return getNodesOfType(parsingResult, ToscaConstants.Nodes.Types.COMPUTE)
        .stream()
        .map(node -> {
          ImageBuilder imageMetadataBuilder = Image.builder();
          this.getNodeCapabilityByName(node, OS_CAPABILITY_NAME)
              .ifPresent(osCapability -> {
                // We've got an OS capability -> Check the attributes to find best match for the
                // image
                capabilityPropertiesMapping
                    .forEach((capabilityPropertyName, mappingFunction) -> {
                      ToscaUtils
                          .extractScalar(osCapability.getProperties(), capabilityPropertyName)
                          .ifPresent(
                              property -> mappingFunction.apply(imageMetadataBuilder)
                                  .apply(property));
                    });
              });
          return new SimpleEntry<>(node, imageMetadataBuilder.build());
        })
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  @Override
  public Map<NodeTemplate, Flavor> extractFlavorRequirements(ArchiveRoot parsingResult) {

    Map<String, Function<FlavorBuilder, Function<String, FlavorBuilder>>>
        capabilityPropertiesMapping = new HashMap<>();

    capabilityPropertiesMapping.put("instance_type",
        flavorMetadataBuilder -> flavorMetadataBuilder::flavorName);

    capabilityPropertiesMapping.put("num_cpus",
        flavorMetadataBuilder -> (String numCpus) -> flavorMetadataBuilder
            .numCpus(Integer.parseInt(numCpus)));

    capabilityPropertiesMapping.put("disk_size",
        flavorMetadataBuilder ->
            (String diskSize) -> flavorMetadataBuilder
                .diskSize(ToscaUtils.parseScalar(diskSize, SizeType.class).convert("GB"))
    );

    capabilityPropertiesMapping.put("mem_size",
        flavorMetadataBuilder -> (String memSize) -> flavorMetadataBuilder
            .memSize(ToscaUtils.parseScalar(memSize, SizeType.class).convert("MB")));

    capabilityPropertiesMapping.put("num_gpus",
        flavorMetadataBuilder -> (String numGpus) -> flavorMetadataBuilder
            .numGpus(Integer.parseInt(numGpus)));

    capabilityPropertiesMapping.put("gpu_vendor",
        flavorMetadataBuilder -> flavorMetadataBuilder::gpuVendor);

    capabilityPropertiesMapping.put("gpu_model",
        flavorMetadataBuilder -> flavorMetadataBuilder::gpuModel);

    // Only indigo.Compute nodes are relevant
    return getNodesOfType(parsingResult, ToscaConstants.Nodes.Types.COMPUTE)
        .stream()
        .map(node -> {
          FlavorBuilder flavorMetadataBuilder = Flavor.builder();
          this.getNodeCapabilityByName(node, HOST_CAPABILITY_NAME)
              .ifPresent(hostCapability -> {
                // We've got an HOST capability -> Check the attributes to find best match for the
                // image
                capabilityPropertiesMapping
                    .forEach((capabilityPropertyName, mappingFunction) -> {
                      ToscaUtils
                          .extractScalar(hostCapability.getProperties(), capabilityPropertyName)
                          .ifPresent(
                              property -> mappingFunction.apply(flavorMetadataBuilder)
                                  .apply(property));
                    });
              });
          return new SimpleEntry<>(node, flavorMetadataBuilder.build());
        })
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  @Override
  public Map<Boolean, Map<NodeTemplate, Image>> contextualizeImages(ArchiveRoot parsingResult,
      ComputeService computeService) {
    try {
      return extractImageRequirements(parsingResult).entrySet().stream().map(entry -> {
        NodeTemplate node = entry.getKey();
        Image imageMetadata = entry.getValue();

        final Optional<Image> image;
        // TODO FILTER ON DEPLOYMENT PROVIDER?
        if (isImImageUri(imageMetadata.getImageName())) {
          image = Optional.of(imageMetadata);
        } else {
          List<Image> images = computeService.getImages();
          image = getBestImageForCloudProvider(imageMetadata, images);
        }

        if (image.isPresent()) {
          // Found a good image -> replace the image attribute with the provider-specific ID
          LOG.debug(
              "Found image match in service <{}> of provider <{}>: {}",
              computeService.getId(), computeService.getProviderId(), image.get());
        } else {
          // No image match found -> throw error
          LOG.debug("Couldn't find a match in service <{}> of provider <{}>",
              computeService.getId(), computeService.getProviderId());
        }
        return new SimpleEntry<>(node, image);
      }).collect(Collectors.partitioningBy(entry -> entry.getValue().isPresent(),
          // do not use the Collectors.toMap() because it doesn't play nice with null values
          // https://bugs.openjdk.java.net/browse/JDK-8148463
          CommonUtils.toMap(Entry::getKey, entry -> entry.getValue().orElse(null))));
    } catch (RuntimeException ex) {
      throw new RuntimeException("Failed to contextualize some image", ex);
    }
  }

  @Override
  public Map<Boolean, Map<NodeTemplate, Flavor>> contextualizeFlavors(ArchiveRoot parsingResult,
      ComputeService computeService) {
    try {
      return extractFlavorRequirements(parsingResult).entrySet().stream().map(entry -> {
        NodeTemplate node = entry.getKey();
        Flavor flavorMetadata = entry.getValue();

        List<Flavor> flavors = computeService
            .getFlavors()
            .stream()
            .sorted()
            .collect(Collectors.toList());
        Optional<Flavor> flavor = getBestFlavorForCloudProvider(flavorMetadata, flavors);

        if (flavor.isPresent()) {
          // Found a good flavor -> replace the flavor attribute with the provider-specific ID
          LOG.debug(
              "Found flavor match in service <{}> of provider <{}>: {}",
              computeService.getId(), computeService.getProviderId(), flavor.get());
        } else {
          // No flavor match found -> throw error
          LOG.debug("Couldn't find a match in service <{}> of provider <{}> for flavor metadata",
              computeService.getId(), computeService.getProviderId());
        }
        return new SimpleEntry<>(node, flavor);
      }).collect(Collectors.partitioningBy(entry -> entry.getValue().isPresent(),
          // do not use the Collectors.toMap() because it doesn't play nice with null values
          // https://bugs.openjdk.java.net/browse/JDK-8148463
          CommonUtils.toMap(Entry::getKey, entry -> entry.getValue().orElse(null))));
    } catch (RuntimeException ex) {
      throw new RuntimeException("Failed to contextualize some flavor", ex);
    }
  }

  private void replaceImage(Map<NodeTemplate, Image> contextualizedImages,
      ComputeService cloudService, DeploymentProvider deploymentProvider) {
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
          imageId = generateImImageUri(cloudService, image);
        }
      }
      osCapability.getProperties().put("image", new ScalarPropertyValue(imageId));

      if (StringUtils.isNotBlank(image.getUserName())) {
        Map<String, Object> credential = new HashMap<>();
        ComplexPropertyValue credentialProperty = new ComplexPropertyValue(credential);
        osCapability.getProperties().put("credential", credentialProperty);
        credential.put("user", image.getUserName());
        credential.put("token", "");
      }
    });
  }

  private void replaceFlavor(Map<NodeTemplate, Flavor> contextualizedFlavors,
      ComputeService cloudService, DeploymentProvider deploymentProvider) {
    contextualizedFlavors.forEach((node, flavor) -> {
      Map<String, Capability> capabilities =
          Optional.ofNullable(node.getCapabilities()).orElseGet(() -> {
            node.setCapabilities(new HashMap<>());
            return node.getCapabilities();
          });
      // The node doesn't have an OS Capability -> need to add a dummy one to hold a
      // random image for underlying deployment systems
      Capability osCapability = capabilities.computeIfAbsent(HOST_CAPABILITY_NAME, key -> {
        LOG.debug("Generating default Container capability for node <{}>", node.getName());
        Capability capability = new Capability();
        capability.setType("tosca.capabilities.indigo.Container");
        return capability;
      });
      String flavorName = flavor.getFlavorName();
      osCapability.getProperties().put("instance_type", new ScalarPropertyValue(flavorName));
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
  private String generateImImageUri(ComputeService cloudService, Image image) {
    try {
      if (image.getService() != null && !image.getService().equals(cloudService.getId())) {
        throw new DeploymentException(
            "Compute service " + cloudService + " doesn't match the image " + image);
      }
      String host = new URL(cloudService.getEndpoint()).getHost();
      String imageId = image.getImageId();
      switch (cloudService.getServiceType()) {
        case CloudService.OPENSTACK_COMPUTE_SERVICE:
        case CloudService.OTC_COMPUTE_SERVICE:
          return String.format("ost://%s/%s", host, imageId);
        case CloudService.OPENNEBULA_COMPUTE_SERVICE:
        case CloudService.OPENNEBULA_TOSCA_SERVICE:
          return String.format("one://%s/%s", host, imageId);
        case CloudService.AWS_COMPUTE_SERVICE:
          return String.format("aws://%s", host);
        case CloudService.AZURE_COMPUTE_SERVICE:
          return String.format("azr://%s", host);
        default:
          throw new DeploymentException(
              "Unknown IaaSType of cloud service " + cloudService.getId());
      }
    } catch (RuntimeException | MalformedURLException ex) {
      LOG.error("Cannot retrieve Compute service host for IM image id generation", ex);
      return image.getImageId();
    }
  }

  protected Optional<Image> getBestImageForCloudProvider(Image imageMetadata,
      Collection<Image> images) {

    // Match image name first (for INDIGO specific use case, if the image cannot be found with the
    // specified name it means that a base image + Ansible configuration have to be used -> the
    // base image will be chosen with the other filters and image metadata - architecture, type,
    // distro, version)
    Optional<Image> imageFoundByName = findImageByName(imageMetadata, images);
    if (imageFoundByName.isPresent()) {
      return imageFoundByName;
    } else {
      return findImageByFallbackFields(imageMetadata, images);
    }
  }

  protected Optional<Flavor> getBestFlavorForCloudProvider(Flavor flavorMetadata,
      Collection<Flavor> flavors) {

    Optional<Flavor> flavorFoundByName = findFlavorByName(flavorMetadata, flavors);
    if (flavorFoundByName.isPresent()) {
      return flavorFoundByName;
    } else {
      return findFlavorByFallbackFields(flavorMetadata, flavors);
    }
  }

  protected Optional<Image> findImageByName(Image requiredImageMetadata,
      Collection<Image> cloudProviderServiceImages) {
    String requiredImageName = requiredImageMetadata.getImageName();
    if (requiredImageName != null) {
      LOG.debug("Looking up images by name <{}>", requiredImageName);
      Optional<Image> imageWithName = cloudProviderServiceImages
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

  protected Optional<Flavor> findFlavorByName(Flavor requiredFlavorMetadata,
      Collection<Flavor> cloudProviderServiceFlavors) {
    String requiredFlavorName = requiredFlavorMetadata.getFlavorName();
    if (requiredFlavorName != null) {
      LOG.debug("Looking up flavors by name <{}>", requiredFlavorName);
      Optional<Flavor> flavorWithName = cloudProviderServiceFlavors
          .stream()
          .filter(flavor -> requiredFlavorName.equals(flavor.getFlavorName()))
          .findFirst();

      if (flavorWithName.isPresent()) {
        LOG.debug("Flavor <{}> found with name <{}>", flavorWithName.get().getFlavorId(),
            requiredFlavorName);
        return flavorWithName;
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
    boolean tagMatch = requiredImageTag == null || requiredImageTag.equals(availableImageTag);

    return nameMatch && tagMatch;
  }

  protected Optional<Image> findImageByFallbackFields(Image requiredImageMetadata,
      Collection<Image> cloudProviderServiceImages) {
    LOG.debug("Looking up images by metatada {}", requiredImageMetadata);
    ArrayList<Filter<Image, ?>> fallbackFieldExtractors = Lists.newArrayList(
        new Filter<>(Image::getType, String::equalsIgnoreCase),
        new Filter<>(Image::getArchitecture, String::equalsIgnoreCase),
        new Filter<>(Image::getDistribution, String::equalsIgnoreCase),
        new Filter<>(Image::getVersion, String::equalsIgnoreCase),
        new Filter<>(Image::getGpuDriver, (a, b) -> !a || (b != null ? b : false)),
        new Filter<>(Image::getGpuDriverVersion, String::equalsIgnoreCase),
        new Filter<>(Image::getCudaSupport, (a, b) -> !a || (b != null ? b : false)),
        new Filter<>(Image::getCudaVersion, String::equalsIgnoreCase),
        new Filter<>(Image::getCuDnnVersion, String::equalsIgnoreCase)
    );

    Stream<Image> imageStream = cloudProviderServiceImages.stream();

    boolean filteredOnSomeField = false;
    for (Filter<Image, ?> fieldExtractor : fallbackFieldExtractors) {
      Stream<Image> newStream = fieldExtractor.filter(requiredImageMetadata, imageStream);
      if (newStream != null) {
        imageStream = newStream;
        filteredOnSomeField = true;
      }
    }

    boolean imageNameIsPresent = requiredImageMetadata.getImageName() != null;
    if (!filteredOnSomeField && imageNameIsPresent) {
      return Optional.empty();
    } else {
      return imageStream.findFirst();
    }
  }

  @AllArgsConstructor
  @RequiredArgsConstructor
  private static class Filter<O, T> {

    @NonNull
    @NotNull
    private Function<O, T> extractor;

    @NonNull
    @NotNull
    private BiFunction<T, T, Boolean> evaluator = Objects::equals;

    public Stream<O> filter(O metadata, Stream<O> stream) {
      return Optional
          .ofNullable(extractor.apply(metadata))
          .map(field -> stream.filter(element -> evaluator.apply(field, extractor.apply(element))))
          .orElse(null);
    }
  }

  protected Optional<Flavor> findFlavorByFallbackFields(Flavor requiredFlavorMetadata,
      Collection<Flavor> cloudProviderServiceFlavors) {
    LOG.debug("Looking up flavors by metatada {}", requiredFlavorMetadata);
    ArrayList<Filter<Flavor, ?>> fallbackFieldExtractors = Lists.newArrayList(
        new Filter<>(Flavor::getNumCpus, (a, b) -> b >= a),
        new Filter<>(Flavor::getMemSize, (a, b) -> b >= a),
        new Filter<>(Flavor::getDiskSize, (a, b) -> b >= a),
        new Filter<>(Flavor::getNumGpus, (a, b) -> b >= a),
        new Filter<>(Flavor::getGpuVendor, String::equalsIgnoreCase),
        new Filter<>(Flavor::getGpuModel, String::equalsIgnoreCase)
    );
    Stream<Flavor> flavorStream = cloudProviderServiceFlavors.stream();

    boolean filteredOnSomeField = false;
    for (Filter<Flavor, ?> fieldExtractor : fallbackFieldExtractors) {
      Stream<Flavor> newStream = fieldExtractor.filter(requiredFlavorMetadata, flavorStream);
      if (newStream != null) {
        flavorStream = newStream;
        filteredOnSomeField = true;
      }
    }

    boolean flavorNameIsPresent = requiredFlavorMetadata.getFlavorName() != null;
    if (!filteredOnSomeField && flavorNameIsPresent) {
      return Optional.empty();
    } else {
      return flavorStream.findFirst();
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
    return getNodesOfType(archiveRoot, ToscaConstants.Nodes.Types.ELASTIC_CLUSTER)
        .stream()
        .anyMatch(node -> ToscaUtils
            .extractScalar(node.getProperties(), "hybrid", BooleanType.class)
            .orElse(false)
        );
  }

  @Override
  public boolean isMesosGpuRequired(ArchiveRoot archiveRoot) {
    return this.getNodesOfType(archiveRoot, Nodes.Types.DOCKER_RUNTIME)
        .stream()
        .anyMatch(node -> getNodeCapabilityByName(node, "host")
            .flatMap(capability -> ToscaUtils
                .extractScalar(node.getProperties(), "num_gpus", IntegerType.class))
            .filter(value -> value > 0)
            .isPresent());
  }

  @Override
  public void addElasticClusterParameters(ArchiveRoot archiveRoot, String deploymentId,
      @Nullable String oauthToken) {
    getNodesOfType(archiveRoot, ToscaConstants.Nodes.Types.ELASTIC_CLUSTER).forEach(node -> {
      // create properties Map if null
      Map<String, AbstractPropertyValue> properties =
          Optional
              .ofNullable(node.getProperties())
              .orElseGet(() -> {
                node.setProperties(new HashMap<>());
                return node.getProperties();
              });

      // Create new property with the deploymentId and set as printable
      properties.put("deployment_id", new ScalarPropertyValue(deploymentId));

      // Create new property with the orchestrator_url and set as printable
      properties.put("orchestrator_url",
          new ScalarPropertyValue(orchestratorProperties.getUrl().toString()));

      if (oauthToken != null) {
        // Create new property with the iam_access_token and set as printable
        properties.put("iam_access_token", new ScalarPropertyValue(oauthToken));

        oauth2tokenService
            .getCluesInfo(oauthToken)
            .ifPresent(info -> {
              // Create new property with the iam_clues_client_id and set as printable
              properties.put("iam_clues_client_id", new ScalarPropertyValue(info.getClientId()));

              // Create new property with the iam_clues_client_secret and set as printable
              properties
                  .put("iam_clues_client_secret", new ScalarPropertyValue(info.getClientSecret()));
            });
      }
    });
  }

  public void removeRemovalList(ArchiveRoot archiveRoot) {
    getNodesFromArchiveRoot(archiveRoot).forEach(this::removeRemovalList);
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
  public Optional<DeploymentArtifact> getNodeArtifactByName(NodeTemplate node,
      String artifactName) {
    return CommonUtils.getFromOptionalMap(node.getArtifacts(), artifactName);
  }

  @Override
  public Map<String, NodeTemplate> getAssociatedNodesByCapability(Map<String, NodeTemplate> nodes,
      NodeTemplate nodeTemplate, String capabilityName) {

    return getRelationshipTemplatesByCapabilityName(nodeTemplate.getRelationships(), capabilityName)
        .stream()
        .map(RelationshipTemplate::getTarget)
        .collect(Collectors.toMap(Function.identity(), nodes::get));
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
  public Optional<Long> getCount(NodeTemplate nodeTemplate) {
    return getNodeCapabilityByName(nodeTemplate, SCALABLE_CAPABILITY_NAME)
        .flatMap(capability -> ToscaUtils
            .extractScalar(capability.getProperties(), "count", IntegerType.class));
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

    List<Object> items =
        getNodeCapabilityByName(nodeTemplate, SCALABLE_CAPABILITY_NAME)
            .flatMap(capability -> ToscaUtils
                .extractList(capability.getProperties(), REMOVAL_LIST_PROPERTY_NAME)
            ).orElseGet(Collections::emptyList);

    List<String> removalList = new ArrayList<>();
    for (Object item : items) {
      if (item instanceof String) {
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

    Map<String, OneData> result = new HashMap<>();

    archiveRoot
        .getTopology()
        .getNodeTemplates()
        .forEach((name, node) -> {
          if (isOfToscaType(node, Nodes.Types.ONEDATA_SPACE)) {
            String space = ToscaUtils.extractScalar(node.getProperties(), "space")
                .orElseThrow(() ->
                    new ToscaException(
                        "Space name for node " + node.getName() + " must be provided")
                );
            OneDataBuilder oneDataBuilder = OneData
                .builder()
                .serviceSpace(false)
                .space(space);
            ToscaUtils.extractScalar(node.getProperties(), "token")
                .ifPresent(oneDataBuilder::token);
            ToscaUtils.extractList(node.getProperties(), "oneproviders", v ->
                OneDataProviderInfo
                    .builder()
                    .endpoint((String) v)
                    .build())
                .ifPresent(oneDataBuilder::oneproviders);
            ToscaUtils.extractScalar(node.getProperties(), "onezone")
                .ifPresent(oneDataBuilder::onezone);
            ToscaUtils.extractScalar(node.getProperties(), "smartScheduling", BooleanType.class)
                .ifPresent(oneDataBuilder::smartScheduling);
            result.put(node.getName(), oneDataBuilder.build());
          } else if (isOfToscaType(node, Nodes.Types.ONEDATA_SERVICE_SPACE)) {
            OneDataBuilder oneDataBuilder = OneData
                .builder()
                .serviceSpace(true);
            ToscaUtils.extractScalar(node.getProperties(), "smartScheduling", BooleanType.class)
                .ifPresent(oneDataBuilder::smartScheduling);
            result.put(node.getName(), oneDataBuilder.build());
          }
        });
    return result;
  }

  @Override
  public Map<String, Dynafed> extractDyanfedRequirements(ArchiveRoot archiveRoot,
      Map<String, Object> inputs) {

    Map<String, Dynafed> result = new HashMap<>();
    getNodesOfType(archiveRoot, Nodes.Types.DYNAFED)
        .forEach(node -> {
          if (isOfToscaType(node, Nodes.Types.DYNAFED)) {
            DynafedBuilder dynafedBuilder = Dynafed.builder();
            ToscaUtils.extractList(node.getProperties(), "files", v ->
                Dynafed.File
                    .builder()
                    .endpoint((String) v)
                    .build())
                .ifPresent(dynafedBuilder::files);
            result.put(node.getName(), dynafedBuilder.build());
          }
        });
    return result;
  }

  @Override
  public Map<String, ToscaPolicy> extractPlacementPolicies(ArchiveRoot archiveRoot) {
    Map<String, ToscaPolicy> placementPolicies = new HashMap<>();
    Optional.ofNullable(archiveRoot.getTopology())
        .map(Topology::getPolicies)
        .orElseGet(Collections::emptyMap)
        .forEach((policyName, policy) -> {
          ToscaPolicy placementPolicy = ToscaPolicyFactory.fromToscaType(policy);
          if (placementPolicy instanceof SlaPlacementPolicy) {
            placementPolicies.put(policyName, placementPolicy);
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

  // set a new capability of a node, params: name and type of the capability, the
  // node
  public void setNodeCapability(NodeTemplate node, String typeCapability, String nameCapability) {
    Map<String, Capability> capabilities = Optional.ofNullable(node.getCapabilities()).orElseGet(() -> {
      node.setCapabilities(new HashMap<>());
      return node.getCapabilities();
    });
    capabilities.computeIfAbsent(nameCapability, key -> {
      Capability capability = new Capability();
      capability.setType(typeCapability);
      return capability;
    });
  }

  // set a new requirement of a node, params: name, target and type of the
  // requirement, the node
  public void setNodeRequirement(NodeTemplate node, String nameRequirement, String targetRequirement,
      String typeRequirement) {
    Map<String, RelationshipTemplate> req = Optional.ofNullable(node.getRelationships()).orElseGet(() -> {
      node.setRelationships(new HashMap<>());
      return node.getRelationships();
    });
    req.computeIfAbsent(nameRequirement, key -> {
      RelationshipTemplate rt = new RelationshipTemplate();
      rt.setRequirementName(nameRequirement);
      rt.setTarget(targetRequirement);
      rt.setTargetedCapabilityName(nameRequirement);
      rt.setType(typeRequirement);
      return rt;
    });

    // req.put("", rt);
  }

  @Override
  public ArchiveRoot setHybridDeployment(ArchiveRoot ar) {
    // check if exist a centralpoint node, if not create it
    if (getNodesOfType(ar, ToscaConstants.Nodes.Types.CENTRAL_POINT).isEmpty()) {
      NodeTemplate cp = new NodeTemplate();
      cp.setType(ToscaConstants.Nodes.Types.CENTRAL_POINT);
      cp.setName("indigovr_cp");
      this.setNodeCapability(cp, REQUIREMENT_DEPENDENCY_CAPABILITY, "dependency");
      ar.getTopology().getNodeTemplates().put("indigovr_cp", cp);
    }

    getNodesOfType(ar, ToscaConstants.Nodes.Types.CENTRAL_POINT).stream().forEach(centralPointNode -> {
      getNodesOfType(ar, ToscaConstants.Nodes.Types.ELASTIC_CLUSTER).stream().forEach(elasticClusterNode -> {
        elasticClusterNode.getRelationships().forEach((s, r) -> {
          if (r.getRequirementName().contains("lrms")) {
            NodeTemplate lrmsNode = ar.getTopology().getNodeTemplates().get(r.getTarget());
            this.setNodeRequirement(lrmsNode, "dependency", centralPointNode.getName(),
                REQUIREMENT_DEPENDENCY_RELATIONSHIP);

            lrmsNode.getRelationships().forEach((s1, r1) -> {
              if (r1.getRequirementName().contains("host")) {
                NodeTemplate hostNode = ar.getTopology().getNodeTemplates().get(r1.getTarget());

                Map<String, Capability> capabilities = Optional.ofNullable(hostNode.getCapabilities())
                    .orElseGet(() -> {
                      hostNode.setCapabilities(new HashMap<>());
                      return hostNode.getCapabilities();
                    });
                Capability endpointCapability = capabilities.computeIfAbsent("endpoint", key -> {
                  Capability capability = new Capability();
                  capability.setType("tosca.capabilities.indigo.Endpoint");
                  return capability;
                });
                Map<String, AbstractPropertyValue> endpointCapabilityProperties = Optional
                    .ofNullable(endpointCapability.getProperties()).orElseGet(() -> {
                      endpointCapability.setProperties(new HashMap<>());
                      return endpointCapability.getProperties();
                    });
                endpointCapabilityProperties.put("network_name", new ScalarPropertyValue("PUBLIC"));

                Map<String, Object> openvpnProp = new HashMap<>();
                ComplexPropertyValue openvpnPropProperty = new ComplexPropertyValue(openvpnProp);
                openvpnProp.put("protocol", "udp");
                openvpnProp.put("source", "1194");

                Map<String, Object> portProp = new HashMap<>();
                ComplexPropertyValue portPropProperty = new ComplexPropertyValue(portProp);
                endpointCapabilityProperties.put("ports", portPropProperty);
                portProp.put("openvpn", openvpnPropProperty);
                String hostName = hostNode.getName();
                this.setNodeRequirement(centralPointNode, "host", hostName,
                    REQUIREMENT_HOST_RELATIONSHIP);
              }
            });
          }
        });
        LOG.debug(this.getTemplateFromTopology(ar));
      });
    });
    return ar;
  }

  @Override
  public ArchiveRoot setHybridUpdateDeployment(ArchiveRoot ar) {
    // check if exist aToscaConstants.Nodes.Types.CENTRAL_POINT, if not create it
    if (getNodesOfType(ar, ToscaConstants.Nodes.Types.CENTRAL_POINT).isEmpty()) {
      setHybridDeployment(ar);
    }
  // check if exist a tosca.nodes.indigo.VR.Client node, if not create it
    if (getNodesOfType(ar, "tosca.nodes.indigo.VR.Client").isEmpty()) {
      NodeTemplate vrC = new NodeTemplate();
      vrC.setType("tosca.nodes.indigo.VR.Client");
      vrC.setName("indigovr_client");
      this.setNodeCapability(vrC, REQUIREMENT_DEPENDENCY_CAPABILITY, "dependency");
      ar.getTopology().getNodeTemplates().put("indigovr_client", vrC);
    }
    getNodesOfType(ar, ToscaConstants.Nodes.Types.CENTRAL_POINT).stream().forEach(centralPointNode -> {
      getNodesOfType(ar, "tosca.nodes.indigo.VR.Client").stream().forEach(node -> {
        getNodesOfType(ar, ToscaConstants.Nodes.Types.ELASTIC_CLUSTER).stream().forEach(elasticClusterNode -> {
          elasticClusterNode.getRelationships().forEach((s, r) -> {
            if (r.getRequirementName().contains("wn")) {
              NodeTemplate wnNode = ar.getTopology().getNodeTemplates().get(r.getTarget());
              // add requirement : dependency: nameVrClient
              this.setNodeRequirement(wnNode, "dependency", node.getName(),
                  REQUIREMENT_DEPENDENCY_RELATIONSHIP);
              wnNode.getRelationships().forEach((s1, r1) -> {
                if (r1.getRequirementName().contains("host")) {
                  // add at vrC : requirement : host : (lrms_wn)
                  // : central_point : (indigovr_cp)
                  this.setNodeRequirement(node, "host", r1.getTarget(), REQUIREMENT_HOST_RELATIONSHIP);
                  this.setNodeCapability(centralPointNode, "tosca.capabilities.Endpoint", "central_point");
                  this.setNodeRequirement(node, "central_point", centralPointNode.getName(), REQUIREMENT_DEPENDENCY_RELATIONSHIP);
                }
                ;
              });
            }
            ;
          });
        });
      });
    });
    LOG.debug(this.getTemplateFromTopology(ar));
    return ar;
  }

}
