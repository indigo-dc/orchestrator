package it.reply.orchestrator.service;

import com.google.common.collect.Lists;

import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.onedata.ProviderDetails;
import it.reply.orchestrator.dto.onedata.SpaceDetails;
import it.reply.orchestrator.dto.onedata.UserSpaces;
import it.reply.orchestrator.exception.service.DeploymentException;

import org.elasticsearch.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

import jersey.repackaged.com.google.common.collect.Maps;

@Service
@PropertySource(value = { "classpath:application.properties", "${onedata.conf.file.path}" })
public class OneDataServiceImpl implements OneDataService {

  @Autowired
  private RestTemplate restTemplate;

  @Value("${onezone.default.url}")
  private String defaultOneZoneEndpoint;

  @Value("${onezone.base.rest.path}")
  private String oneZoneBaseRestPath;

  @Value("${oneprovider.base.rest.path}")
  private String oneProviderBaseRestPath;

  @Value("$service.space.token")
  private String serviceSpaceToken;

  @Value("$service.space.name")
  private String serviceSpaceName;

  @Value("$service.space.provider")
  private String serviceSpaceProvider;

  @Value("$service.space.path:''")
  private String serviceSpacePath;

  @PostConstruct
  private void init() {
    if (!defaultOneZoneEndpoint.endsWith("/")) {
      defaultOneZoneEndpoint += "/";
    }
    if (oneZoneBaseRestPath.startsWith("/")) {
      oneZoneBaseRestPath = oneZoneBaseRestPath.substring(1);
    }
    if (!oneZoneBaseRestPath.endsWith("/")) {
      oneZoneBaseRestPath += "/";
    }
    if (oneProviderBaseRestPath.startsWith("/")) {
      oneProviderBaseRestPath = oneProviderBaseRestPath.substring(1);
    }
    if (!oneProviderBaseRestPath.endsWith("/")) {
      oneProviderBaseRestPath += "/";
    }
  }

  @Override
  public String getServiceSpaceToken() {
    return serviceSpaceToken;
  }

  @Override
  public String getServiceSpaceName() {
    return serviceSpaceName;
  }

  @Override
  public String getServiceSpaceProvider() {
    return serviceSpaceProvider;
  }

  @Override
  public String getServiceSpacePath() {
    return serviceSpacePath;
  }

  @Override
  public UserSpaces getUserSpacesId(String oneZoneEndpoint, @Nonnull String oneDataToken) {
    if (oneZoneEndpoint == null) {
      oneZoneEndpoint = defaultOneZoneEndpoint;
    }
    oneZoneEndpoint += oneZoneBaseRestPath + "user/spaces";

    ResponseEntity<UserSpaces> response =
        getForEntity(restTemplate, oneZoneEndpoint, oneDataToken, UserSpaces.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to get OneData spaces. "
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public UserSpaces getUserSpacesId(String onedataToken) {
    return getUserSpacesId(null, onedataToken);
  }

  @Override
  public SpaceDetails getSpaceDetailsFromId(String oneZoneEndpoint, @Nonnull String oneDataToken,
      @Nonnull String oneSpaceId) {
    if (oneZoneEndpoint == null) {
      oneZoneEndpoint = defaultOneZoneEndpoint;
    }
    oneZoneEndpoint += oneZoneBaseRestPath + "user/spaces/" + oneSpaceId;

    ResponseEntity<SpaceDetails> response =
        getForEntity(restTemplate, oneZoneEndpoint, oneDataToken, SpaceDetails.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to OneData space details. "
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public SpaceDetails getSpaceDetailsFromId(@Nonnull String oneDataToken,
      @Nonnull String oneSpaceId) {
    return getSpaceDetailsFromId(null, oneDataToken, oneSpaceId);
  }

  @Override
  public String getUserSpaceNameById(String oneZoneEndpoint, @Nonnull String oneDataToken,
      @Nonnull String oneSpaceId) {
    SpaceDetails details = getSpaceDetailsFromId(oneZoneEndpoint, oneDataToken, oneSpaceId);
    return details.getName();
  }

  @Override
  public String getUserSpaceNameById(@Nonnull String onedataToken, @Nonnull String oneSpaceId) {
    return getUserSpaceNameById(null, onedataToken, oneSpaceId);
  }

  @Override
  public List<String> getProvidersIdBySpaceId(String oneZoneEndpoint, @Nonnull String oneDataToken,
      @Nonnull String oneSpaceId) {
    SpaceDetails details = getSpaceDetailsFromId(oneZoneEndpoint, oneDataToken, oneSpaceId);
    return Lists.newArrayList(details.getProvidersSupports().keySet());
  }

  @Override
  public List<String> getProvidersIdBySpaceId(@Nonnull String oneDataToken,
      @Nonnull String oneSpaceId) {
    return getProvidersIdBySpaceId(null, oneDataToken, oneSpaceId);
  }

  @Override
  public ProviderDetails getProviderDetailsFromId(String oneZoneEndpoint,
      @Nonnull String oneDataToken, @Nonnull String oneSpaceId, @Nonnull String oneProviderId) {
    if (oneZoneEndpoint == null) {
      oneZoneEndpoint = defaultOneZoneEndpoint;
    }
    oneZoneEndpoint = String.format("%s%sspaces/%s/providers/%s", oneZoneEndpoint,
        oneZoneBaseRestPath, oneSpaceId, oneProviderId);

    ResponseEntity<ProviderDetails> response =
        getForEntity(restTemplate, oneZoneEndpoint, oneDataToken, ProviderDetails.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to OneData provider details. "
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public ProviderDetails getProviderDetailsFromId(@Nonnull String oneDataToken,
      @Nonnull String oneSpaceId, @Nonnull String oneProviderId) {
    return getProviderDetailsFromId(null, oneDataToken, oneSpaceId, oneProviderId);
  }

  private <T> ResponseEntity<T> getForEntity(RestTemplate restTemplate, String endpoint,
      String oneDataToken, Class<T> entityClass) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("macaroon", oneDataToken);

    HttpEntity<?> entity = new HttpEntity<>(headers);
    return restTemplate.exchange(endpoint, HttpMethod.GET, entity, entityClass);
  }

  @Override
  public Map<OneData, Set<String>> getProvidersId(Collection<OneData> oneDataParams) {
    Map<OneData, Set<String>> returnValue = Maps.newHashMap();
    for (OneData param : oneDataParams) {
      UserSpaces spaces = getUserSpacesId(param.getZone(), param.getToken());
      SpaceDetails spaceDetails = null;
      {
        // find the right space
        SpaceDetails tmpSpaceDetails =
            getSpaceDetailsFromId(param.getToken(), spaces.getDefaultSpace());
        if (!tmpSpaceDetails.getName().equals(param.getSpace())) {
          for (String spaceId : spaces.getSpaces()) {
            tmpSpaceDetails = getSpaceDetailsFromId(param.getToken(), spaceId);
            if (tmpSpaceDetails.getName().equals(param.getSpace())) {
              spaceDetails = tmpSpaceDetails;
            }
          }
        } else {
          spaceDetails = tmpSpaceDetails;
        }
      }
      if (spaceDetails == null) {
        throw new DeploymentException("No space with name " + param.getSpace()
            + " associated with the token " + param.getToken());
      }
      Set<String> supportedProvidersId = spaceDetails.getProvidersSupports().keySet();
      returnValue.put(param, Sets.newHashSet());
      if (param.getProviders() != null) {
        // filter based on the specified providers
        Set<String> providerEndpoints = new HashSet<>(param.getProviders());
        for (String supportedProviderId : supportedProvidersId) {
          ProviderDetails providerDetails = getProviderDetailsFromId(param.getZone(),
              param.getToken(), spaceDetails.getSpaceId(), supportedProviderId);
          if (providerEndpoints.contains(providerDetails.getRedirectionPoint())) {
            providerEndpoints.remove(providerDetails.getRedirectionPoint());
            returnValue.get(param).add(supportedProviderId);
          }
          if (providerEndpoints.isEmpty()) {
            break;
          }
        }
        if (!providerEndpoints.isEmpty()) {
          throw new DeploymentException(
              "Some OneData Providers are not associated to the specified space: "
                  + providerEndpoints.toArray());
        }
      } else {
        returnValue.get(param).addAll(supportedProvidersId);
      }
    }
    return returnValue;
  }

}
