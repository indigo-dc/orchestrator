/*
 * Copyright © 2015-2020 Santer Reply S.p.A.
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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
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
import it.reply.orchestrator.enums.PrivateNetworkType;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.exception.service.ToscaException;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.tosca.TemplateParser;
import it.reply.orchestrator.utils.CommonUtils;
import it.reply.orchestrator.utils.ToscaConstants;
import it.reply.orchestrator.utils.ToscaConstants.Nodes;
import it.reply.orchestrator.utils.ToscaUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
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
import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DirectedMultigraph;
import org.springframework.beans.factory.annotation.Autowired;
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
  private IndigoInputsPreProcessorService indigoInputsPreProcessorService;

  @Autowired
  private OAuth2TokenService oauth2tokenService;

  @Autowired
  private OrchestratorProperties orchestratorProperties;

  @Autowired
  private TemplateParser templateParser;

  @Override
  public void replaceInputFunctions(ArchiveRoot archiveRoot, Map<String, Object> inputs) {
    indigoInputsPreProcessorService.processGetInput(archiveRoot, inputs);
  }

  @Override
  public ArchiveRoot parseAndValidateTemplate(String toscaTemplate, Map<String, Object> inputs) {
    ArchiveRoot ar = templateParser.parse(toscaTemplate);
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
  public String updateTemplate(String template) {
    ArchiveRoot parsedTempalte = templateParser.parse(template);
    removeRemovalList(parsedTempalte);
    return templateParser.serialize(parsedTempalte);
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
          return String.format("aws://%s", imageId);
        case CloudService.AZURE_COMPUTE_SERVICE:
          return String.format("azr://%s", imageId);
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

    boolean imageNameIsPresent = StringUtils.isNotBlank(requiredImageMetadata.getImageName());
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
  public PrivateNetworkType getPrivateNetworkType(ArchiveRoot archiveRoot) {
    // set private network name / cidr
    Optional<NodeTemplate> pn = getNodesOfType(archiveRoot, ToscaConstants.Nodes.Types.NETWORK)
        .stream()
        .filter(node -> {
          Optional<String> nt = ToscaUtils.extractScalar(node.getProperties(),
              ToscaConstants.Nodes.Properties.NETWORKTYPE);
          return nt.isPresent() && (nt.get().equals(ToscaConstants.Nodes.Attributes.PRIVATE)
              || nt.get().equals(ToscaConstants.Nodes.Attributes.ISOLATED));
        }).findFirst();
    if (pn.isPresent()) {
      Optional<String> nt = ToscaUtils.extractScalar(pn.get().getProperties(),
          ToscaConstants.Nodes.Properties.NETWORKTYPE);
      if (nt.isPresent()) {
        if (nt.get().equals(ToscaConstants.Nodes.Attributes.PRIVATE)) {
          return PrivateNetworkType.PRIVATE;
        }
        if (nt.get().equals(ToscaConstants.Nodes.Attributes.ISOLATED)) {
          return PrivateNetworkType.ISOLATED;
        }
      }
    }
    return PrivateNetworkType.NONE;
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
  public boolean isElasticClusterDeployment(ArchiveRoot archiveRoot) {
    return !getNodesOfType(archiveRoot, ToscaConstants.Nodes.Types.ELASTIC_CLUSTER).isEmpty();
  }

  @Override
  public boolean isMesosGpuRequired(ArchiveRoot archiveRoot) {
    return this.getNodesOfType(archiveRoot, Nodes.Types.DOCKER_RUNTIME)
        .stream()
        .anyMatch(node -> getNodeCapabilityByName(node, "host")
            .flatMap(capability -> ToscaUtils
                .extractScalar(capability.getProperties(), "num_gpus", IntegerType.class))
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

  @Override
  public Optional<AbstractPropertyValue> getNodePropertyByName(NodeTemplate node,
      String propertyName) {
    return CommonUtils.getFromOptionalMap(node.getProperties(), propertyName);
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

  /**
   * Set a new capability at the specified node.
   * @param node the node to add the new capability
   * @param typeCapability the type of the capability
   * @param nameCapability the name of the capability
   */
  public void setNodeCapability(NodeTemplate node, String typeCapability, String nameCapability) {
    Map<String, Capability> capabilities =
        Optional.ofNullable(node.getCapabilities()).orElseGet(() -> {
          node.setCapabilities(new HashMap<>());
          return node.getCapabilities();
        });
    capabilities.computeIfAbsent(nameCapability, key -> {
      Capability capability = new Capability();
      capability.setType(typeCapability);
      return capability;
    });
  }

  /**
   * Set a new requirement at the specified node.
   * @param node the node to add the new requirement
   * @param nameRequirement name of the requirement
   * @param targetRequirement target of the requirement
   * @param typeRequirement type of the requirement
   */
  public void setNodeRequirement(NodeTemplate node, String nameRequirement,
      String targetRequirement,
      String typeRequirement) {
    Map<String, RelationshipTemplate> req =
        Optional.ofNullable(node.getRelationships()).orElseGet(() -> {
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
  }

  @Override
  public ArchiveRoot setHybridDeployment(ArchiveRoot ar,
      String publicNetworkName,
      String privateNetworkName,
      String privateNetworkCidr) {

    // check if exist a centralpoint node, if not create it
    if (getNodesOfType(ar, ToscaConstants.Nodes.Types.CENTRAL_POINT).isEmpty()) {
      NodeTemplate cp = new NodeTemplate();
      cp.setType(ToscaConstants.Nodes.Types.CENTRAL_POINT);
      cp.setName("indigovr_cp");
      this.setNodeCapability(cp, REQUIREMENT_DEPENDENCY_CAPABILITY, "dependency");
      ar.getTopology().getNodeTemplates().put("indigovr_cp", cp);
    }

    // set public network name
    setPublicNetworkName(ar, publicNetworkName);

    getNodesOfType(ar, ToscaConstants.Nodes.Types.CENTRAL_POINT).stream()
        .forEach(centralPointNode -> {
          getNodesOfType(ar, ToscaConstants.Nodes.Types.ELASTIC_CLUSTER).stream()
              .forEach(elasticClusterNode -> {
                elasticClusterNode.getRelationships().forEach((s, r) -> {
                  // get dependency of name "lrms" -> "lmrs_front_end"
                  if (r.getRequirementName().contains("lrms")) {
                    NodeTemplate lrmsNode = ar.getTopology().getNodeTemplates().get(r.getTarget());
                    //add requirement dependency to "indigovr_cp"
                    this.setNodeRequirement(lrmsNode, "dependency", centralPointNode.getName(),
                        REQUIREMENT_DEPENDENCY_RELATIONSHIP);

                    //find "host" node in "lmrs_front_end" -> "lrms_server"
                    lrmsNode.getRelationships().forEach((s1, r1) -> {
                      if (r1.getRequirementName().contains("host")) {
                        NodeTemplate hostNode =
                            ar.getTopology().getNodeTemplates().get(r1.getTarget());

                        //get lrms_server properties
                        Map<String, Capability> capabilities =
                            Optional.ofNullable(hostNode.getCapabilities())
                                .orElseGet(() -> {
                                  hostNode.setCapabilities(new HashMap<>());
                                  return hostNode.getCapabilities();
                                });

                        //create "endpoint" if absent in "lrms_server"
                        Capability endpointCapability =
                            capabilities.computeIfAbsent("endpoint", key -> {
                              Capability capability = new Capability();
                              capability.setType("tosca.capabilities.indigo.Endpoint");
                              return capability;
                            });
                        Map<String, AbstractPropertyValue> endpointCapabilityProperties = Optional
                            .ofNullable(endpointCapability.getProperties()).orElseGet(() -> {
                              endpointCapability.setProperties(new HashMap<>());
                              return endpointCapability.getProperties();
                            });

                        //create VPN ports on "lrms_server"
                        Map<String, Object> portProp = new HashMap<>();
                        ComplexPropertyValue portPropProperty = new ComplexPropertyValue(portProp);
                        endpointCapabilityProperties.put("ports", portPropProperty);

                        Map<String, Object> openvpnProp = new HashMap<>();
                        ComplexPropertyValue openvpnPropProperty =
                            new ComplexPropertyValue(openvpnProp);
                        openvpnProp.put("protocol", "udp");
                        openvpnProp.put("source", "1194");
                        portProp.put("openvpn", openvpnPropProperty);

                        // set requirement hostName = "lrms_server" in "indigovr_cp"
                        String hostName = hostNode.getName();
                        this.setNodeRequirement(centralPointNode, "host", hostName,
                            REQUIREMENT_HOST_RELATIONSHIP);

                        //retrieve private network node if present
                        Optional<NodeTemplate> pn = getNodesOfType(ar,
                            ToscaConstants.Nodes.Types.NETWORK)
                            .stream()
                            .filter(node -> {
                              Optional<String> nt = ToscaUtils.extractScalar(node.getProperties(),
                                  ToscaConstants.Nodes.Properties.NETWORKTYPE);
                              return nt.isPresent() && (nt.get()
                                  .equals(ToscaConstants.Nodes.Attributes.PRIVATE)
                                  || nt.get().equals(ToscaConstants.Nodes.Attributes.ISOLATED));
                            }).findFirst();
                        setNetworkProperties(ar, privateNetworkName, privateNetworkCidr,
                            hostName, pn);
                      }
                    });
                  }
                  //handle hybrid flag for ISOLATED network environment
                  Optional<NodeTemplate> pn = getNodesOfType(ar,
                      ToscaConstants.Nodes.Types.NETWORK)
                      .stream()
                      .filter(node -> {
                        Optional<String> nt = ToscaUtils.extractScalar(node.getProperties(),
                            ToscaConstants.Nodes.Properties.NETWORKTYPE);
                        return nt.isPresent() && (nt.get()
                            .equals(ToscaConstants.Nodes.Attributes.ISOLATED));
                      }).findFirst();
                  if (pn.isPresent()) {
                    if (r.getRequirementName().contains("wn")) {
                      NodeTemplate wnNode = ar.getTopology().getNodeTemplates().get(r.getTarget());
                      if (wnNode.getProperties()
                          .containsKey(ToscaConstants.Nodes.Properties.HYBRID)) {
                        //force to false
                        wnNode.getProperties().put(ToscaConstants.Nodes.Properties.HYBRID,
                            new ScalarPropertyValue("false"));
                      }
                    }
                  }
                });
              });
        });
    return ar;
  }

  private void setPublicNetworkName(ArchiveRoot ar, String publicNetworkName) {
    if (StringUtils.isNotEmpty(publicNetworkName)) {
      Optional<NodeTemplate> pn = getNodesOfType(ar, ToscaConstants.Nodes.Types.NETWORK)
          .stream()
          .filter(node -> {
            Optional<String> nt = ToscaUtils.extractScalar(node.getProperties(),
                ToscaConstants.Nodes.Properties.NETWORKTYPE);
            return nt.isPresent() && nt.get().equals("public");
          }).findFirst();
      if (pn.isPresent()) {
        pn.get().getProperties().put(ToscaConstants.Nodes.Properties.NETWORKNAME,
            new ScalarPropertyValue(publicNetworkName));
      }
    }
  }

  private void setNetworkProperties(ArchiveRoot ar, String privateNetworkName,
      String privateNetworkCidr, String hostName, Optional<NodeTemplate> pn) {
    if (pn.isPresent()) {
      Optional<String> nt = ToscaUtils.extractScalar(pn.get().getProperties(),
          ToscaConstants.Nodes.Properties.NETWORKTYPE);
      if (nt.isPresent()) {
        if (nt.get().equals(ToscaConstants.Nodes.Attributes.PRIVATE)) {
          // set private network name
          pn.get().getProperties().put(
              ToscaConstants.Nodes.Properties.NETWORKNAME,
              new ScalarPropertyValue(privateNetworkName));
          // create vr_clients
          setHybridClients(ar);
        }
        if (nt.get().equals(ToscaConstants.Nodes.Attributes.ISOLATED)) {
          // set private network cidr and gateway
          pn.get().getProperties().put("cidr",
              new ScalarPropertyValue(privateNetworkCidr));
          String gw = extractGatewayFromCidr(privateNetworkCidr);
          if (StringUtils.isNotEmpty(gw)) {
            pn.get().getProperties().put("gateway_ip",
                new ScalarPropertyValue(gw + "," + hostName));
          }
        }
      }
    }
  }

  private String extractGatewayFromCidr(String cidr) {
    String[] parts = cidr.split("\\.");
    if (parts.length == 4) {
      return parts[0] + "." + parts[1] + ".0.0/16";
    }
    return null;
  }

  @Override
  public ArchiveRoot setHybridUpdateDeployment(
      ArchiveRoot ar,
      boolean newResourcesOnDifferentService,
      String publicNetworkName,
      String privateNetworkName,
      String privateNetworkCidr) {

    // check if exist aToscaConstants.Nodes.Types.CENTRAL_POINT, if not create it
    if (getNodesOfType(ar, ToscaConstants.Nodes.Types.CENTRAL_POINT).isEmpty()) {
      setHybridDeployment(ar, publicNetworkName, privateNetworkName, privateNetworkCidr);
    }

    PrivateNetworkType nt = getPrivateNetworkType(ar);

    if (nt.equals(PrivateNetworkType.PRIVATE)) {
      setHybridClients(ar);
    }

    if (nt.equals(PrivateNetworkType.ISOLATED)
        && newResourcesOnDifferentService
        && getNodesOfType(ar, ToscaConstants.Nodes.Types.VROUTER).isEmpty()) {

      Optional<NodeTemplate> centralPointNode = getNodesOfType(ar,
          ToscaConstants.Nodes.Types.CENTRAL_POINT)
          .stream().findFirst();
      if (centralPointNode.isPresent()) {

        //create compute node
        NodeTemplate vrC = new NodeTemplate();
        vrC.setType(ToscaConstants.Nodes.Types.COMPUTE);
        vrC.setName("indigovr2_compute");
        vrC.setCapabilities(new HashMap<>());
        //add "endpoint"
        Capability endpointCapability = new Capability();
        endpointCapability.setType("tosca.capabilities.indigo.Endpoint");
        endpointCapability.setProperties(new HashMap<>());
        endpointCapability.getProperties().put("dns_name",
            new ScalarPropertyValue(vrC.getName()));
        Map<String, Capability> capabilities = vrC.getCapabilities();
        capabilities.put("endpoint", endpointCapability);
        //add host
        Capability hostCapability = new Capability();
        hostCapability.setType("tosca.capabilities.indigo.Container");
        hostCapability.setProperties(new HashMap<>());
        hostCapability.getProperties().put("num_cpus", new ScalarPropertyValue("1"));
        hostCapability.getProperties().put("mem_size", new ScalarPropertyValue("1 GB"));
        capabilities.put("host", hostCapability);
        Capability imageCapability = new Capability();
        imageCapability.setType("tosca.capabilities.indigo.OperatingSystem");
        imageCapability.setProperties(new HashMap<>());
        imageCapability.getProperties().put("distribution",
            new ScalarPropertyValue("ubuntu"));
        imageCapability.getProperties().put("version", new ScalarPropertyValue("16.04"));
        imageCapability.getProperties().put("type", new ScalarPropertyValue("linux"));
        capabilities.put("os", imageCapability);
        ar.getTopology().getNodeTemplates().put(vrC.getName(), vrC);

        //create vrouter node
        NodeTemplate vrR = new NodeTemplate();
        vrR.setType(ToscaConstants.Nodes.Types.VROUTER);
        vrR.setName("indigovr2_router");
        this.setNodeCapability(vrR, REQUIREMENT_DEPENDENCY_CAPABILITY, "dependency");
        ar.getTopology().getNodeTemplates().put(vrR.getName(), vrR);
        this.setNodeRequirement(vrR, ToscaConstants.Nodes.Capabilities.CENTRALPOINT,
            centralPointNode.get().getName(),
            REQUIREMENT_DEPENDENCY_RELATIONSHIP);
        this.setNodeRequirement(vrR, "host", vrC.getName(),
            REQUIREMENT_HOST_RELATIONSHIP);

        //create private network
        NodeTemplate vrN = new NodeTemplate();
        vrN.setType(ToscaConstants.Nodes.Types.NETWORK);
        vrN.setName("priv2_network");
        vrN.setProperties(new HashMap<>());
        vrN.getProperties().put(ToscaConstants.Nodes.Properties.NETWORKTYPE,
            new ScalarPropertyValue(ToscaConstants.Nodes.Attributes.ISOLATED));
        vrN.getProperties().put("cidr", new ScalarPropertyValue(privateNetworkCidr));
        String gw = extractGatewayFromCidr(privateNetworkCidr);
        if (StringUtils.isNotEmpty(gw)) {
          vrN.getProperties().put("gateway_ip",
              new ScalarPropertyValue(gw + "," + vrC.getName()));
        }
        ar.getTopology().getNodeTemplates().put(vrN.getName(), vrN);

        //create port for computenode
        NodeTemplate vrCP = new NodeTemplate();
        vrCP.setType(ToscaConstants.Nodes.Types.PORT);
        vrCP.setName("indigovr2_compute_port");
        vrCP.setProperties(new HashMap<>());
        vrCP.getProperties().put("order", new ScalarPropertyValue("0"));
        this.setNodeCapability(vrCP, REQUIREMENT_DEPENDENCY_CAPABILITY, "dependency");
        ar.getTopology().getNodeTemplates().put(vrCP.getName(), vrCP);
        this.setNodeRequirement(vrCP, "binding", vrC.getName(),
            REQUIREMENT_DEPENDENCY_RELATIONSHIP);
        this.setNodeRequirement(vrCP, "link", vrN.getName(),
            REQUIREMENT_DEPENDENCY_RELATIONSHIP);

        //create port for wn_server
        getNodesOfType(ar, ToscaConstants.Nodes.Types.SLURM_WN).stream()
        .forEach(slurmWorkerNode -> {
          slurmWorkerNode.getRelationships().forEach((s, r) -> {
            if (r.getRequirementName().contains("host")) {
              NodeTemplate workerNode = ar.getTopology().getNodeTemplates()
                  .get(r.getTarget());
              NodeTemplate vrNP = new NodeTemplate();
              vrNP.setType(ToscaConstants.Nodes.Types.PORT);
              vrNP.setName("wn_priv2_port");
              vrNP.setProperties(new HashMap<>());
              vrNP.getProperties().put("order", new ScalarPropertyValue("0"));
              this.setNodeCapability(vrNP, REQUIREMENT_DEPENDENCY_CAPABILITY, "dependency");
              ar.getTopology().getNodeTemplates().put(vrNP.getName(), vrNP);
              this.setNodeRequirement(vrNP, "binding", workerNode.getName(),
                  REQUIREMENT_DEPENDENCY_RELATIONSHIP);
              this.setNodeRequirement(vrNP, "link", vrN.getName(),
                  REQUIREMENT_DEPENDENCY_RELATIONSHIP);      
            }
          });
        });
        /*
        NodeTemplate vrNP = new NodeTemplate();
        vrNP.setType(ToscaConstants.Nodes.Types.PORT);
        vrNP.setName("wn_priv2_port");
        vrNP.setProperties(new HashMap<>());
        vrNP.getProperties().put("order", new ScalarPropertyValue("0"));
        this.setNodeCapability(vrNP, REQUIREMENT_DEPENDENCY_CAPABILITY, "dependency");
        ar.getTopology().getNodeTemplates().put(vrNP.getName(), vrNP);
        this.setNodeRequirement(vrNP, "binding", "lrms_wn",
            REQUIREMENT_DEPENDENCY_RELATIONSHIP);
        this.setNodeRequirement(vrNP, "link", vrN.getName(),
            REQUIREMENT_DEPENDENCY_RELATIONSHIP);
        */
        //add  vrouter dependency to wnodes and clear hybrid flag if present
        getNodesOfType(ar, ToscaConstants.Nodes.Types.ELASTIC_CLUSTER).stream()
            .forEach(elasticClusterNode -> {
              elasticClusterNode.getRelationships().forEach((s, r) -> {
                if (r.getRequirementName().contains("wn")) {
                  NodeTemplate wnNode = ar.getTopology().getNodeTemplates()
                      .get(r.getTarget());
                  // add requirement : dependency: vrouter2
                  this.setNodeRequirement(wnNode, "dependency", vrR.getName(),
                      REQUIREMENT_DEPENDENCY_RELATIONSHIP);
                  if (wnNode.getProperties().containsKey(ToscaConstants.Nodes.Properties.HYBRID)) {
                    //force to false
                    wnNode.getProperties().put("hybrid", new ScalarPropertyValue("false"));
                  }
                }
              });
            });
      }
    }
    return ar;
  }

  private ArchiveRoot setHybridClients(ArchiveRoot ar) {
    // check if exist a tosca.nodes.indigo.VR.Client node, if not create it
    if (getNodesOfType(ar, ToscaConstants.Nodes.Types.CLIENT).isEmpty()) {
      NodeTemplate vrC = new NodeTemplate();
      vrC.setType(ToscaConstants.Nodes.Types.CLIENT);
      vrC.setName("indigovr_client");
      this.setNodeCapability(vrC, REQUIREMENT_DEPENDENCY_CAPABILITY, "dependency");
      ar.getTopology().getNodeTemplates().put(vrC.getName(), vrC);
    }
    getNodesOfType(ar, ToscaConstants.Nodes.Types.CENTRAL_POINT).stream()
        .forEach(centralPointNode -> {
          getNodesOfType(ar, ToscaConstants.Nodes.Types.CLIENT).stream().forEach(node ->
              getNodesOfType(ar, ToscaConstants.Nodes.Types.ELASTIC_CLUSTER).stream()
                  .forEach(elasticClusterNode ->
                    elasticClusterNode.getRelationships().forEach((s, r) -> {
                      if (r.getRequirementName().contains("wn")) {
                        NodeTemplate wnNode = ar.getTopology().getNodeTemplates()
                            .get(r.getTarget());
                        // add requirement : dependency: nameVrClient
                        this.setNodeRequirement(wnNode, "dependency", node.getName(),
                            REQUIREMENT_DEPENDENCY_RELATIONSHIP);
                        wnNode.getRelationships().forEach((s1, r1) -> {
                          if (r1.getRequirementName().contains("host")) {
                            // add at vrC : requirement : host : (lrms_wn)
                            // : central_point : (indigovr_cp)
                            this.setNodeRequirement(node, "host", r1.getTarget(),
                                REQUIREMENT_HOST_RELATIONSHIP);
                            this.setNodeCapability(centralPointNode, "tosca.capabilities.Endpoint",
                                ToscaConstants.Nodes.Capabilities.CENTRALPOINT);
                            this.setNodeRequirement(node,
                                ToscaConstants.Nodes.Capabilities.CENTRALPOINT,
                                centralPointNode.getName(), REQUIREMENT_DEPENDENCY_RELATIONSHIP);
                          }
                        });
                      }
                    })
                  )
          );
        });
    return ar;
  }

  @Override
  public String serialize(ArchiveRoot archiveRoot) {
    return templateParser.serialize(archiveRoot);
  }

  @Override
  public ArchiveRoot parse(String toscaTemplate) {
    return templateParser.parse(toscaTemplate);
  }

}
