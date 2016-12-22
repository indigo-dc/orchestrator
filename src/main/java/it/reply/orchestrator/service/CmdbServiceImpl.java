package it.reply.orchestrator.service;

import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.CmdbHasManyList;
import it.reply.orchestrator.dto.cmdb.CmdbRow;
import it.reply.orchestrator.dto.cmdb.Image;
import it.reply.orchestrator.dto.cmdb.Provider;
import it.reply.orchestrator.dto.cmdb.Type;
import it.reply.orchestrator.exception.service.DeploymentException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@PropertySource("classpath:cmdb/cmdb.properties")
public class CmdbServiceImpl implements CmdbService {

  private static final Logger LOG = LoggerFactory.getLogger(CmdbServiceImpl.class);

  @Autowired
  private RestTemplate restTemplate;

  @Value("${cmdb.url}")
  private String url;

  @Value("${service.id}")
  private String serviceIdUrlPath;

  @Value("${provider.id}")
  private String providerIdUrlPath;

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public CloudService getServiceById(String id) {

    ResponseEntity<CloudService> response =
        restTemplate.getForEntity(url.concat(serviceIdUrlPath).concat(id), CloudService.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to find service <" + id + "> in the CMDB."
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public Provider getProviderById(String id) {
    ResponseEntity<Provider> response =
        restTemplate.getForEntity(url.concat(providerIdUrlPath).concat(id), Provider.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to find provider <" + id + "> in the CMDB."
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public Image getImageById(String imageId) {
    ResponseEntity<Image> response =
        restTemplate.getForEntity(url.concat("image/id").concat(imageId), Image.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to find image <" + imageId + "> in the CMDB."
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public List<Image> getImagesByService(String serviceId) {
    ResponseEntity<CmdbHasManyList<CmdbRow<Image>>> response = restTemplate.exchange(
        url.concat(serviceIdUrlPath).concat(serviceId).concat("/has_many/images?include_docs=true"),
        HttpMethod.GET, null, new ParameterizedTypeReference<CmdbHasManyList<CmdbRow<Image>>>() {
        });

    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody().getRows().stream().map(e -> e.getDoc())
          .collect(Collectors.toList());
    }
    throw new DeploymentException("Unable to find images for service <" + serviceId
        + "> in the CMDB." + response.getStatusCode().toString() + " "
        + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public List<CloudService> getServicesByProvider(String providerId) {
    ResponseEntity<CmdbHasManyList<CmdbRow<CloudService>>> response = restTemplate.exchange(
        url.concat(providerIdUrlPath).concat(providerId)
            .concat("/has_many/services?include_docs=true"),
        HttpMethod.GET, null,
        new ParameterizedTypeReference<CmdbHasManyList<CmdbRow<CloudService>>>() {
        });

    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody().getRows().stream().map(e -> e.getDoc())
          .collect(Collectors.toList());
    }
    throw new DeploymentException("Unable to find services for provider <" + providerId
        + "> in the CMDB." + response.getStatusCode().toString() + " "
        + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public CloudProvider fillCloudProviderInfo(CloudProvider cp) {
    // Get provider's data
    cp.setCmdbProviderData(getProviderById(cp.getId()));
    cp.setName(cp.getCmdbProviderData().getId());

    Map<String, CloudService> allServices = getServicesByProvider(cp.getId()).stream()
        .collect(Collectors.toMap(CloudService::getId, Function.identity()));
    // Get provider's services' data
    for (Map.Entry<String, CloudService> serviceEntry : cp.getCmdbProviderServices().entrySet()) {
      if (allServices.containsKey(serviceEntry.getKey())) {
        serviceEntry.setValue(allServices.get(serviceEntry.getKey()));
      } else {
        serviceEntry.setValue(getServiceById(serviceEntry.getKey()));
      }
    }
    for (CloudService service : allServices.values()) {
      if (service.isOneProviderStorageService()) {
        cp.getCmdbProviderServices().put(service.getId(), service);
      }
    }

    // FIXME Get other data (i.e. OneData, Images mapping, etc)

    // Get images for provider (requires to know the compute service)
    // FIXME: What if there are multiple compute service for a provider (remember that those are
    // SLAM given)?
    List<CloudService> imageServices = cp.getCmbdProviderServicesByType(Type.COMPUTE);
    for (CloudService imageService : imageServices) {
      if (imageService != null) {
        LOG.debug("Retrieving image list for service <{}> of provider <{}>", imageService.getId(),
            cp.getId());
        cp.addCmdbCloudServiceImages(imageService.getId(), getImagesByService(imageService.getId())
            .stream().map(e -> e.getData()).collect(Collectors.toList()));
      } else {
        LOG.debug("No image service to retrieve image list from for provider <{}>", cp.getId());
      }
    }

    return cp;
  }

  // @Override
  // public List<CmdbImage> getImagesAndMetadataByService(String serviceId) {
  // List<CmdbImage> images = new ArrayList<>();
  // for (CmdbImage image : getImagesByService(serviceId)) {
  // images.add(getImageById(image.getId()));
  // }
  // return images;
  // }

  // @Override
  // public List<Image> getImagesByProvider(String providerId) {
  //
  //
  // providerId = "STUB:" + providerId;
  // // FIXME: Stub
  // return Arrays.asList(
  // new Image().withImageName("indigodatacloud/ubuntu-sshd").withImageId(providerId + "/5")
  // .withArchitecture("x86_64").withType("linux").withDistribution("ubuntu")
  // .withVersion("14.04"),
  // new Image().withImageName("linux-ubuntu-14.04").withImageId(providerId + "/5")
  // .withArchitecture("x86_64").withType("linux").withDistribution("ubuntu")
  // .withVersion("14.04"),
  // new Image().withImageName("indigodatacloudapps/docker-galaxy")
  // .withImageId(providerId + "/13"));
  // }

}
