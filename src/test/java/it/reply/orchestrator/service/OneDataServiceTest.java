/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import it.reply.orchestrator.config.properties.OneDataProperties;
import it.reply.orchestrator.config.properties.OneDataProperties.ServiceSpaceProperties;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.onedata.OneData.OneDataProviderInfo;
import it.reply.orchestrator.dto.onedata.ProviderDetails;
import it.reply.orchestrator.dto.onedata.SpaceDetails;
import it.reply.orchestrator.dto.onedata.UserSpaces;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.utils.json.JsonUtility;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;

@RunWith(JUnitParamsRunner.class)
public class OneDataServiceTest {

  @InjectMocks
  private OneDataServiceImpl oneDataService;

  @Spy
  private OneDataProperties oneDataProperties;

  @Spy
  private ServiceSpaceProperties serviceSpaceProperties;

  @Spy
  private RestTemplate restTemplate;

  private MockRestServiceServer mockServer;

  private String defaultOneZoneEndpoint = "http://localhost";
  private String onezoneBasePath = "/api/v3/onezone/";
  private String onedataToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    oneDataProperties.setOnezoneUrl(URI.create(defaultOneZoneEndpoint));
    oneDataProperties.setServiceSpace(serviceSpaceProperties);
    serviceSpaceProperties.setToken(onedataToken);
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  private String generateExpectedOneZoneEndpoint(String oneZoneEndpoint) {
    return oneZoneEndpoint != null ? oneZoneEndpoint : defaultOneZoneEndpoint;
  }

  @Parameters({ "null", "http://endpoint.com" })
  @Test
  public void testSuccessGetUserSpaceId(@Nullable String oneZoneEndpoint) throws IOException {
    UserSpaces userSpace = generateUserSpaces();
    String endpoint = generateExpectedOneZoneEndpoint(oneZoneEndpoint);

    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "user/spaces"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("macaroon", onedataToken))
        .andRespond(
            withSuccess(JsonUtility.serializeJson(userSpace), MediaType.APPLICATION_JSON_UTF8));

    assertThat(oneDataService.getUserSpacesId(endpoint, onedataToken))
        .isEqualTo(userSpace);
    mockServer.verify();
  }

  @Parameters({ "null", "http://endpoint.com" })
  @Test
  public void testFailGetUserSpaceId(@Nullable String oneZoneEndpoint) {
    String endpoint = generateExpectedOneZoneEndpoint(oneZoneEndpoint);
    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "user/spaces"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("macaroon", onedataToken))
        .andRespond(withBadRequest());

    assertThatThrownBy(() -> oneDataService.getUserSpacesId(endpoint, onedataToken))
        .isInstanceOf(DeploymentException.class);
    mockServer.verify();
  }

  @Parameters({ "null", "http://endpoint.com" })
  @Test
  public void testSuccessGetSpaceDetailsFromId(@Nullable String oneZoneEndpoint)
      throws IOException {

    SpaceDetails details = generateSpaceDetails();
    String spaceId = details.getSpaceId();

    String endpoint = generateExpectedOneZoneEndpoint(oneZoneEndpoint);
    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "user/spaces/" + spaceId))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("macaroon", onedataToken))
        .andRespond(
            withSuccess(JsonUtility.serializeJson(details), MediaType.APPLICATION_JSON_UTF8));

    assertThat(oneDataService.getSpaceDetailsFromId(endpoint, onedataToken, spaceId))
        .isEqualTo(details);
    mockServer.verify();
  }

  @Parameters({ "null", "http://endpoint.com" })
  @Test
  public void testFailGetSpaceDetailsFromId(@Nullable String oneZoneEndpoint) {
    String endpoint = generateExpectedOneZoneEndpoint(oneZoneEndpoint);
    String spaceId = UUID.randomUUID().toString();
    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "user/spaces/" + spaceId))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("macaroon", onedataToken))
        .andRespond(withBadRequest());

    assertThatThrownBy(() -> oneDataService.getSpaceDetailsFromId(endpoint, onedataToken, spaceId))
        .isInstanceOf(DeploymentException.class);
    mockServer.verify();
  }

  @Parameters({ "null", "http://endpoint.com" })
  @Test
  public void testSuccessGetProviderDetailsFromId(@Nullable String oneZoneEndpoint)
      throws IOException {

    String spaceId = UUID.randomUUID().toString();

    ProviderDetails providerDetail = generateProviderDetails();
    String providerId = providerDetail.getProviderId();

    String endpoint = generateExpectedOneZoneEndpoint(oneZoneEndpoint);
    mockServer
        .expect(requestTo(
            endpoint + onezoneBasePath + "spaces/" + spaceId + "/providers/" + providerId))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("macaroon", onedataToken))
        .andRespond(MockRestResponseCreators.withSuccess(JsonUtility.serializeJson(providerDetail),
            MediaType.APPLICATION_JSON_UTF8));

    assertThat(oneDataService.getProviderDetailsFromId(endpoint, onedataToken, spaceId, providerId))
        .isEqualTo(providerDetail);
    mockServer.verify();
  }

  @Parameters({ "null", "http://endpoint.com" })
  @Test
  public void testFailGetProviderDetailsFromId(@Nullable String oneZoneEndpoint) {

    String spaceId = UUID.randomUUID().toString();
    String providerId = UUID.randomUUID().toString();

    String endpoint = generateExpectedOneZoneEndpoint(oneZoneEndpoint);

    mockServer
        .expect(requestTo(
            endpoint + onezoneBasePath + "spaces/" + spaceId + "/providers/" + providerId))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("macaroon", onedataToken))
        .andRespond(MockRestResponseCreators.withBadRequest());

    assertThatThrownBy(
        () -> oneDataService.getProviderDetailsFromId(endpoint, onedataToken, spaceId, providerId))
            .isInstanceOf(DeploymentException.class);
    mockServer.verify();
  }

  @Parameters({ "null", "http://endpoint.com" })
  @Test
  public void testEmptyPopulateProviderInfo(@Nullable String oneZoneEndpoint) throws IOException {

    SpaceDetails spaceDetails = generateSpaceDetails();
    String spaceId = spaceDetails.getSpaceId();
    String canonicalName = spaceDetails.getCanonicalName();

    UserSpaces userSpace = generateUserSpaces();
    userSpace.getSpaces().add(spaceId);

    String endpoint = generateExpectedOneZoneEndpoint(oneZoneEndpoint);

    OneData oneData = OneData
        .builder()
        .token(onedataToken)
        .space(canonicalName)
        .zone(endpoint)
        .build();

    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "user/spaces"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("macaroon", onedataToken))
        .andRespond(MockRestResponseCreators.withSuccess(JsonUtility.serializeJson(userSpace),
            MediaType.APPLICATION_JSON_UTF8));

    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "user/spaces/" + spaceId))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("macaroon", onedataToken))
        .andRespond(MockRestResponseCreators.withSuccess(JsonUtility.serializeJson(spaceDetails),
            MediaType.APPLICATION_JSON_UTF8));

    assertThat(oneDataService.populateProviderInfo(oneData).getProviders()).isEmpty();
    mockServer.verify();
  }

  @Parameters({ "null", "http://endpoint.com" })
  @Test
  public void testFailEmptyPopulateProviderInfo(@Nullable String oneZoneEndpoint)
      throws IOException {

    SpaceDetails spaceDetails = generateSpaceDetails();
    String spaceId = spaceDetails.getSpaceId();
    String canonicalName = spaceDetails.getCanonicalName();

    UserSpaces userSpace = generateUserSpaces();
    userSpace.getSpaces().add(spaceId);

    String endpoint = generateExpectedOneZoneEndpoint(oneZoneEndpoint);

    OneData oneData = OneData
        .builder()
        .token(onedataToken)
        .space(UUID.randomUUID().toString())
        .zone(endpoint)
        .build();

    assertThat(canonicalName).isNotEqualTo(oneData.getSpace());

    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "user/spaces"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("macaroon", onedataToken))
        .andRespond(MockRestResponseCreators.withSuccess(JsonUtility.serializeJson(userSpace),
            MediaType.APPLICATION_JSON_UTF8));

    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "user/spaces/" + spaceId))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("macaroon", onedataToken))
        .andRespond(MockRestResponseCreators.withSuccess(JsonUtility.serializeJson(spaceDetails),
            MediaType.APPLICATION_JSON_UTF8));

    assertThatThrownBy(() -> oneDataService.populateProviderInfo(oneData))
        .isInstanceOf(DeploymentException.class);
    mockServer.verify();
  }

  @Parameters({ "null", "http://endpoint.com" })
  @Test
  public void testAddProviderInfoToOneData(@Nullable String oneZoneEndpoint) throws IOException {

    ProviderDetails providerDetails = generateProviderDetails();
    String providerId = providerDetails.getProviderId();

    SpaceDetails spaceDetails = generateSpaceDetails();
    spaceDetails.getProvidersSupports().put(providerId, 1L);
    String spaceId = spaceDetails.getSpaceId();
    String canonicalName = spaceDetails.getCanonicalName();

    UserSpaces userSpace = generateUserSpaces();
    userSpace.getSpaces().add(spaceId);

    String endpoint = generateExpectedOneZoneEndpoint(oneZoneEndpoint);

    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "user/spaces"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("macaroon", onedataToken))
        .andRespond(MockRestResponseCreators.withSuccess(JsonUtility.serializeJson(userSpace),
            MediaType.APPLICATION_JSON_UTF8));

    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "user/spaces/" + spaceId))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("macaroon", onedataToken))
        .andRespond(MockRestResponseCreators.withSuccess(JsonUtility.serializeJson(spaceDetails),
            MediaType.APPLICATION_JSON_UTF8));

    mockServer
        .expect(requestTo(
            endpoint + onezoneBasePath + "spaces/" + spaceId + "/providers/" + providerId))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("macaroon", onedataToken))
        .andRespond(MockRestResponseCreators.withSuccess(JsonUtility.serializeJson(providerDetails),
            MediaType.APPLICATION_JSON_UTF8));

    OneData oneData = OneData
        .builder()
        .token(onedataToken)
        .space(canonicalName)
        .zone(endpoint)
        .build();

    OneDataProviderInfo providerInfo = OneDataProviderInfo
        .builder()
        .id(providerId)
        .endpoint(providerDetails.getRedirectionPoint())
        .build();

    assertThat(oneDataService.populateProviderInfo(oneData).getProviders())
        .hasSize(1)
        .allMatch(providerInfo::equals);
    mockServer.verify();
  }

  private UserSpaces generateUserSpaces() {
    return UserSpaces
        .builder()
        .defaultSpace("defaultSpace")
        .build();
  }

  private SpaceDetails generateSpaceDetails() {
    return SpaceDetails
        .builder()
        .canonicalName("cname")
        .name("name")
        .spaceId(UUID.randomUUID().toString())
        .build();
  }

  private ProviderDetails generateProviderDetails() {
    return ProviderDetails
        .builder()
        .clientName("clientName")
        .csr("csr")
        .latitude(41.25)
        .longitude(-120.9762)
        .providerId(UUID.randomUUID().toString())
        .redirectionPoint("http://www.redirection.example")
        .build();
  }

}
