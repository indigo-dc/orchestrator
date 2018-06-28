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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import it.reply.orchestrator.config.properties.OneDataProperties;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.onedata.OneData.OneDataProviderInfo;
import it.reply.orchestrator.dto.onedata.ProviderDetails;
import it.reply.orchestrator.dto.onedata.SpaceDetails;
import it.reply.orchestrator.dto.onedata.UserSpaces;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.utils.JsonUtils;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpStatusCodeException;

@RunWith(JUnitParamsRunner.class)
@RestClientTest(OneDataService.class)
public class OneDataServiceTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  private OneDataService oneDataService;

  @Autowired
  private OneDataProperties oneDataProperties;

  @Autowired
  private MockRestServiceServer mockServer;

  private String defaultOneZoneEndpoint = "http://localhost";
  private String onezoneBasePath = "/api/v3/onezone/";
  private String onedataToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";

  @Before
  public void setup() {
    oneDataProperties.setOnezoneUrl(URI.create(defaultOneZoneEndpoint));
    oneDataProperties.getServiceSpace().setToken(onedataToken);
  }

  private String generateExpectedOneZoneEndpoint(String oneZoneEndpoint) {
    return oneZoneEndpoint != null ? oneZoneEndpoint : defaultOneZoneEndpoint;
  }

  @Parameters({ "null", "http://example.com" })
  @Test
  public void testSuccessGetUserSpaceId(@Nullable String oneZoneEndpoint) throws IOException {
    UserSpaces userSpace = generateUserSpaces();
    String endpoint = generateExpectedOneZoneEndpoint(oneZoneEndpoint);

    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "user/spaces"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", onedataToken))
        .andRespond(
            withSuccess(JsonUtils.serialize(userSpace), MediaType.APPLICATION_JSON_UTF8));

    assertThat(oneDataService.getUserSpacesId(endpoint, onedataToken))
        .isEqualTo(userSpace);
    mockServer.verify();
  }

  @Parameters({ "null", "http://example.com" })
  @Test
  public void testFailGetUserSpaceId(@Nullable String oneZoneEndpoint) {
    String endpoint = generateExpectedOneZoneEndpoint(oneZoneEndpoint);
    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "user/spaces"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", onedataToken))
        .andRespond(withBadRequest());

    assertThatThrownBy(() -> oneDataService.getUserSpacesId(endpoint, onedataToken))
        .isInstanceOf(DeploymentException.class)
        .hasCauseInstanceOf(HttpStatusCodeException.class);
    mockServer.verify();
  }

  @Parameters({ "null", "http://example.com" })
  @Test
  public void testSuccessGetSpaceDetailsFromId(@Nullable String oneZoneEndpoint)
      throws IOException {

    SpaceDetails details = generateSpaceDetails();
    String spaceId = details.getSpaceId();

    String endpoint = generateExpectedOneZoneEndpoint(oneZoneEndpoint);
    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "user/spaces/" + spaceId))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", onedataToken))
        .andRespond(
            withSuccess(JsonUtils.serialize(details), MediaType.APPLICATION_JSON_UTF8));

    assertThat(oneDataService.getSpaceDetailsFromId(endpoint, onedataToken, spaceId))
        .isEqualTo(details);
    mockServer.verify();
  }

  @Parameters({ "null", "http://example.com" })
  @Test
  public void testFailGetSpaceDetailsFromId(@Nullable String oneZoneEndpoint) {
    String endpoint = generateExpectedOneZoneEndpoint(oneZoneEndpoint);
    String spaceId = UUID.randomUUID().toString();
    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "user/spaces/" + spaceId))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", onedataToken))
        .andRespond(withBadRequest());

    assertThatThrownBy(() -> oneDataService.getSpaceDetailsFromId(endpoint, onedataToken, spaceId))
        .isInstanceOf(DeploymentException.class)
        .hasCauseInstanceOf(HttpStatusCodeException.class);
    mockServer.verify();
  }

  @Parameters({ "null", "http://example.com" })
  @Test
  public void testSuccessGetProviderDetailsFromId(@Nullable String oneZoneEndpoint)
      throws IOException {

    ProviderDetails providerDetail = generateProviderDetails();
    String providerId = providerDetail.getProviderId();

    String endpoint = generateExpectedOneZoneEndpoint(oneZoneEndpoint);
    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "providers/" + providerId))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", onedataToken))
        .andRespond(
            withSuccess(JsonUtils.serialize(providerDetail), MediaType.APPLICATION_JSON_UTF8));

    assertThat(oneDataService.getProviderDetailsFromId(endpoint, onedataToken, providerId))
        .isEqualTo(providerDetail);
    mockServer.verify();
  }

  @Parameters({ "null", "http://example.com" })
  @Test
  public void testFailGetProviderDetailsFromId(@Nullable String oneZoneEndpoint) {

    String providerId = UUID.randomUUID().toString();

    String endpoint = generateExpectedOneZoneEndpoint(oneZoneEndpoint);

    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "providers/" + providerId))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", onedataToken))
        .andRespond(withBadRequest());

    assertThatThrownBy(
        () -> oneDataService.getProviderDetailsFromId(endpoint, onedataToken, providerId))
            .isInstanceOf(DeploymentException.class)
            .hasCauseInstanceOf(HttpStatusCodeException.class);
    mockServer.verify();
  }

  @Parameters({ "null", "http://example.com" })
  @Test
  public void testEmptyPopulateProviderInfo(@Nullable String oneZoneEndpoint) throws IOException {

    SpaceDetails spaceDetails = generateSpaceDetails();
    String spaceId = spaceDetails.getSpaceId();
    String spaceName = spaceDetails.getName();

    UserSpaces userSpace = generateUserSpaces();
    userSpace.getSpaces().add(spaceId);

    String endpoint = generateExpectedOneZoneEndpoint(oneZoneEndpoint);

    OneData oneData = OneData
        .builder()
        .token(onedataToken)
        .space(spaceName)
        .zone(endpoint)
        .build();

    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "user/spaces"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", onedataToken))
        .andRespond(withSuccess(JsonUtils.serialize(userSpace), MediaType.APPLICATION_JSON_UTF8));

    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "user/spaces/" + spaceId))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", onedataToken))
        .andRespond(
            withSuccess(JsonUtils.serialize(spaceDetails), MediaType.APPLICATION_JSON_UTF8));

    assertThat(oneDataService.populateProviderInfo(oneData).getProviders()).isEmpty();
    mockServer.verify();
  }

  @Parameters({ "null", "http://example.com" })
  @Test
  public void testFailEmptyPopulateProviderInfo(@Nullable String oneZoneEndpoint)
      throws IOException {

    SpaceDetails spaceDetails = generateSpaceDetails();
    String spaceId = spaceDetails.getSpaceId();
    String spaceName = spaceDetails.getName();

    UserSpaces userSpace = generateUserSpaces();
    userSpace.getSpaces().add(spaceId);

    String endpoint = generateExpectedOneZoneEndpoint(oneZoneEndpoint);

    OneData oneData = OneData
        .builder()
        .token(onedataToken)
        .space(UUID.randomUUID().toString())
        .zone(endpoint)
        .build();

    assertThat(spaceName).isNotEqualTo(oneData.getSpace());

    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "user/spaces"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", onedataToken))
        .andRespond(withSuccess(JsonUtils.serialize(userSpace), MediaType.APPLICATION_JSON_UTF8));

    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "user/spaces/" + spaceId))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", onedataToken))
        .andRespond(
            withSuccess(JsonUtils.serialize(spaceDetails), MediaType.APPLICATION_JSON_UTF8));

    assertThatThrownBy(() -> oneDataService.populateProviderInfo(oneData))
        .isInstanceOf(DeploymentException.class);
    mockServer.verify();
  }

  @Parameters({ "null", "http://example.com" })
  @Test
  public void testAddProviderInfoToOneData(@Nullable String oneZoneEndpoint) throws IOException {

    ProviderDetails providerDetails = generateProviderDetails();
    String providerId = providerDetails.getProviderId();

    SpaceDetails spaceDetails = generateSpaceDetails();
    spaceDetails.getProvidersSupports().put(providerId, 1L);
    String spaceId = spaceDetails.getSpaceId();
    String spaceName = spaceDetails.getName();

    UserSpaces userSpace = generateUserSpaces();
    userSpace.getSpaces().add(spaceId);

    String endpoint = generateExpectedOneZoneEndpoint(oneZoneEndpoint);

    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "user/spaces"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", onedataToken))
        .andRespond(withSuccess(JsonUtils.serialize(userSpace), MediaType.APPLICATION_JSON_UTF8));

    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "user/spaces/" + spaceId))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", onedataToken))
        .andRespond(
            withSuccess(JsonUtils.serialize(spaceDetails), MediaType.APPLICATION_JSON_UTF8));

    mockServer
        .expect(requestTo(endpoint + onezoneBasePath + "providers/" + providerId))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", onedataToken))
        .andRespond(
            withSuccess(JsonUtils.serialize(providerDetails), MediaType.APPLICATION_JSON_UTF8));

    OneData oneData = OneData
        .builder()
        .token(onedataToken)
        .space(spaceName)
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
        .name("name")
        .spaceId(UUID.randomUUID().toString())
        .build();
  }

  private ProviderDetails generateProviderDetails() {
    return ProviderDetails
        .builder()
        .clientName("clientName")
        .latitude(41.25)
        .longitude(-120.9762)
        .providerId(UUID.randomUUID().toString())
        .redirectionPoint("http://example.com/redirection")
        .build();
  }

}
