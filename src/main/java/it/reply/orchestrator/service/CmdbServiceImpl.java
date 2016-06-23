package it.reply.orchestrator.service;

import it.reply.orchestrator.dto.cmdb.CmdbHasManyList;
import it.reply.orchestrator.dto.cmdb.CmdbImage;
import it.reply.orchestrator.dto.cmdb.CmdbImageRow;
import it.reply.orchestrator.dto.cmdb.Provider;
import it.reply.orchestrator.exception.service.DeploymentException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
@PropertySource("classpath:cmdb/cmdb.properties")
public class CmdbServiceImpl implements CmdbService {

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
  public it.reply.orchestrator.dto.cmdb.Service getServiceById(String id) {

    ResponseEntity<it.reply.orchestrator.dto.cmdb.Service> response = restTemplate.getForEntity(
        url.concat(serviceIdUrlPath).concat(id), it.reply.orchestrator.dto.cmdb.Service.class);
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
  public CmdbImage getImageById(String imageId) {
    ResponseEntity<CmdbImage> response =
        restTemplate.getForEntity(url.concat("image/id").concat(imageId), CmdbImage.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to find image <" + imageId + "> in the CMDB."
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public List<CmdbImage> getImagesByService(String serviceId) {
    ResponseEntity<CmdbHasManyList<CmdbImageRow>> response = restTemplate.exchange(
        url.concat(serviceIdUrlPath).concat(serviceId).concat("/has_many/images?include_docs=true"),
        HttpMethod.GET, null, new ParameterizedTypeReference<CmdbHasManyList<CmdbImageRow>>() {
        });

    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody().getRows().stream().map(e -> e.getImage())
          .collect(Collectors.toList());
    }
    throw new DeploymentException("Unable to find images for service <" + serviceId
        + "> in the CMDB." + response.getStatusCode().toString() + " "
        + response.getStatusCode().getReasonPhrase());
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
