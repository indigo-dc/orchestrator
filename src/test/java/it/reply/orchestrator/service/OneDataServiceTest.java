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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;

import it.reply.orchestrator.config.properties.OneDataProperties;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.CloudProvider;
import it.reply.orchestrator.dto.cmdb.CloudServiceType;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.onedata.OneData.OneDataProviderInfo;
import it.reply.orchestrator.dto.onedata.ProviderDetails;
import it.reply.orchestrator.dto.onedata.SpaceDetails;
import it.reply.orchestrator.dto.onedata.Token;
import it.reply.orchestrator.dto.onedata.Tokens;
import it.reply.orchestrator.dto.onedata.UserSpaces;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.function.ThrowingFunction;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.JsonUtils;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpStatusCodeException;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

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

  @MockBean
  private OAuth2TokenService oauth2TokenService;

  private static final String defaultOneZoneEndpoint = "http://default.example.com";
  private static final String customOneZoneEndpoint = "http://custom.example.com";

  private static final String onedataToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
  private static final String serviceSpaceName = "service-space-name";

  private static final String deploymentId = "deploymentId";
  private static final OidcTokenId oidcTokenId = new OidcTokenId();

  @Before
  public void setup() throws Exception {
    oneDataProperties.setOnezoneUrl(URI.create(defaultOneZoneEndpoint));
    oneDataProperties.getServiceSpace().setToken(onedataToken);
    oneDataProperties.getServiceSpace().setName(serviceSpaceName);

    when(oauth2TokenService.getOrganization(oidcTokenId)).thenReturn("OrganizationName");
    when(oauth2TokenService.executeWithClientForResult(eq(oidcTokenId), any(), any()))
        .then(a -> ((ThrowingFunction) a.getArguments()[1]).apply("AccessToken"));
  }

  @Test
  public void testSuccessGetUserSpaceId() throws IOException {
    UserSpaces userSpace = generateUserSpaces();

    mockServer
        .expect(requestTo(
            defaultOneZoneEndpoint + oneDataProperties.getOnezoneBasePath() + "user/spaces"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", onedataToken))
        .andRespond(
            withSuccess(JsonUtils.serialize(userSpace), MediaType.APPLICATION_JSON_UTF8));

    assertThat(oneDataService.getUserSpacesId(defaultOneZoneEndpoint, onedataToken))
        .isEqualTo(userSpace);
    mockServer.verify();
  }

  @Test
  public void testFailGetUserSpaceId() {
    mockServer
        .expect(requestTo(
            defaultOneZoneEndpoint + oneDataProperties.getOnezoneBasePath() + "user/spaces"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", onedataToken))
        .andRespond(withBadRequest());

    assertThatThrownBy(() -> oneDataService.getUserSpacesId(defaultOneZoneEndpoint, onedataToken))
        .isInstanceOf(DeploymentException.class)
        .hasCauseInstanceOf(HttpStatusCodeException.class);
    mockServer.verify();
  }

  @Test
  public void testSuccessGetSpaceDetailsFromId() throws IOException {

    SpaceDetails details = generateSpaceDetails("space-name-1");
    String spaceId = details.getSpaceId();

    mockServer
        .expect(requestTo(
            defaultOneZoneEndpoint + oneDataProperties.getOnezoneBasePath() + "user/spaces/"
                + spaceId))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", onedataToken))
        .andRespond(
            withSuccess(JsonUtils.serialize(details), MediaType.APPLICATION_JSON_UTF8));

    assertThat(oneDataService.getSpaceDetailsFromId(defaultOneZoneEndpoint, onedataToken, spaceId))
        .isEqualTo(details);
    mockServer.verify();
  }

  @Test
  public void testFailGetSpaceDetailsFromId() {

    String spaceId = UUID.randomUUID().toString();
    mockServer
        .expect(requestTo(
            defaultOneZoneEndpoint + oneDataProperties.getOnezoneBasePath() + "user/spaces/"
                + spaceId))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", onedataToken))
        .andRespond(withBadRequest());

    assertThatThrownBy(
        () -> oneDataService.getSpaceDetailsFromId(defaultOneZoneEndpoint, onedataToken, spaceId))
        .isInstanceOf(DeploymentException.class)
        .hasCauseInstanceOf(HttpStatusCodeException.class);
    mockServer.verify();
  }

  @Test
  public void testSuccessGetProviderDetailsFromId()
      throws IOException {

    ProviderDetails providerDetail = generateProviderDetails(1);
    String providerId = providerDetail.getProviderId();

    mockServer
        .expect(requestTo(
            defaultOneZoneEndpoint + oneDataProperties.getOnezoneBasePath() + "providers/"
                + providerId))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", onedataToken))
        .andRespond(
            withSuccess(JsonUtils.serialize(providerDetail), MediaType.APPLICATION_JSON_UTF8));

    assertThat(
        oneDataService.getProviderDetailsFromId(defaultOneZoneEndpoint, onedataToken, providerId))
        .isEqualTo(providerDetail);
    mockServer.verify();
  }

  @Test
  public void testFailGetProviderDetailsFromId() {

    String providerId = UUID.randomUUID().toString();

    mockServer
        .expect(requestTo(
            defaultOneZoneEndpoint + oneDataProperties.getOnezoneBasePath() + "providers/"
                + providerId))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", onedataToken))
        .andRespond(withBadRequest());

    assertThatThrownBy(
        () -> oneDataService
            .getProviderDetailsFromId(defaultOneZoneEndpoint, onedataToken, providerId))
            .isInstanceOf(DeploymentException.class)
            .hasCauseInstanceOf(HttpStatusCodeException.class);
    mockServer.verify();
  }

  @Test
  public void testSuccessGetTokens() throws JsonProcessingException {

    Tokens tokens = Tokens.builder().tokens(Lists.newArrayList(onedataToken)).build();
    mockServer
        .expect(requestTo(defaultOneZoneEndpoint + oneDataProperties.getOnezoneBasePath()
            + "user/client_tokens"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", "OrganizationName:AccessToken"))
        .andRespond(withSuccess(JsonUtils.serialize(tokens), MediaType.APPLICATION_JSON_UTF8));

    assertThat(oneDataService.getOneDataTokens(defaultOneZoneEndpoint, oidcTokenId))
        .isEqualTo(tokens);
    mockServer.verify();
  }

  @Test
  public void testFailGetTokens() {

    mockServer
        .expect(requestTo(defaultOneZoneEndpoint + oneDataProperties.getOnezoneBasePath()
            + "user/client_tokens"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", "OrganizationName:AccessToken"))
        .andRespond(withBadRequest());

    assertThatThrownBy(
        () -> oneDataService.getOneDataTokens(defaultOneZoneEndpoint, oidcTokenId))
        .isInstanceOf(DeploymentException.class)
        .hasCauseInstanceOf(HttpStatusCodeException.class);
    mockServer.verify();
  }

  @Test
  public void testSuccessGenerateToken() throws JsonProcessingException {

    Token token = Token.builder().token(onedataToken).build();
    mockServer
        .expect(requestTo(defaultOneZoneEndpoint + oneDataProperties.getOnezoneBasePath()
            + "user/client_tokens"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("X-Auth-Token", "OrganizationName:AccessToken"))
        .andRespond(withSuccess(JsonUtils.serialize(token), MediaType.APPLICATION_JSON_UTF8));

    assertThat(oneDataService.generateOneDataToken(defaultOneZoneEndpoint, oidcTokenId))
        .isEqualTo(token);
    mockServer.verify();
  }

  @Test
  public void testFailGenerateToken() throws JsonProcessingException {

    mockServer
        .expect(requestTo(defaultOneZoneEndpoint + oneDataProperties.getOnezoneBasePath()
            + "user/client_tokens"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("X-Auth-Token", "OrganizationName:AccessToken"))
        .andRespond(withBadRequest());

    assertThatThrownBy(
        () -> oneDataService.generateOneDataToken(defaultOneZoneEndpoint, oidcTokenId))
        .isInstanceOf(DeploymentException.class)
        .hasCauseInstanceOf(HttpStatusCodeException.class);
    mockServer.verify();
  }

  private void mockForProvidersInfo(String onezoneEndpoint, String spaceName, int... ids)
      throws JsonProcessingException {
    UserSpaces userSpace = generateUserSpaces();
    SpaceDetails spaceDetails = generateSpaceDetails(spaceName, ids);

    mockServer
        .expect(requestTo(
            onezoneEndpoint + oneDataProperties.getOnezoneBasePath() + "user/spaces"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", onedataToken))
        .andRespond(withSuccess(JsonUtils.serialize(userSpace), MediaType.APPLICATION_JSON_UTF8));

    mockServer
        .expect(requestTo(
            onezoneEndpoint + oneDataProperties.getOnezoneBasePath() + "user/spaces/"
                + "space-id-1"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", onedataToken))
        .andRespond(
            withSuccess(JsonUtils.serialize(spaceDetails), MediaType.APPLICATION_JSON_UTF8));

    for (Integer id : ids) {
      mockServer
          .expect(requestTo(
              onezoneEndpoint + oneDataProperties.getOnezoneBasePath() + "providers/"
                  + "provider-id-" + id))
          .andExpect(method(HttpMethod.GET))
          .andExpect(header("X-Auth-Token", onedataToken))
          .andRespond(
              withSuccess(JsonUtils.serialize(generateProviderDetails(id)),
                  MediaType.APPLICATION_JSON_UTF8));
    }
  }

  @Parameters({"true", "false"})
  @Test
  public void testAddProviderInfoForServiceSpace(boolean isSmartScheduling)
      throws IOException {

    mockForProvidersInfo(defaultOneZoneEndpoint, "service-space-name", 1, 2);

    OneData oneData = OneData
        .builder()
        .serviceSpace(true)
        .smartScheduling(isSmartScheduling)
        .build();

    Map<String, CloudProvider> cloudProviders = generateCloudProviders(2, 2);

    oneDataService.populateProviderInfo(oneData, cloudProviders, oidcTokenId, deploymentId);

    assertThat(oneData.getOnezone()).isEqualTo(defaultOneZoneEndpoint);
    assertThat(oneData.getSpace()).isEqualTo("service-space-name");
    assertThat(oneData.getToken()).isEqualTo(onedataToken);
    assertThat(oneData.getPath()).isEqualTo("/" + deploymentId + "/");

    if (isSmartScheduling) {
      OneDataProviderInfo providerInfo = generateOneDataProviderInfo(2, 2);
      assertThat(oneData
          .getOneproviders())
          .hasSize(1)
          .contains(providerInfo);
    } else {
      assertThat(oneData
          .getOneproviders()).extracting(OneDataProviderInfo::getEndpoint)
          .hasSize(2)
          .contains("provider-1.example.com", "provider-2.example.com");
    }

    mockServer.verify();
  }

  @Parameters({
      "true|false",
      "true|true",
      "false|false",
      "false|true"
  })
  @Test
  public void testAddProviderInfoWithoutRequirementsForUserSpace(boolean isSmartScheduling,
      boolean withOnezoneEndpoint)
      throws IOException {

    mockForProvidersInfo(withOnezoneEndpoint ? customOneZoneEndpoint : defaultOneZoneEndpoint,
        "space-name-1", 1, 2);

    OneData oneData = OneData
        .builder()
        .token(onedataToken)
        .space("space-name-1")
        .smartScheduling(isSmartScheduling)
        .build();

    if (withOnezoneEndpoint) {
      oneData.setOnezone(customOneZoneEndpoint);
    }

    Map<String, CloudProvider> cloudProviders = generateCloudProviders(2, 2);

    oneDataService.populateProviderInfo(oneData, cloudProviders, oidcTokenId, deploymentId);

    OneDataProviderInfo providerInfo = generateOneDataProviderInfo(2, 2);

    if (isSmartScheduling) {
      assertThat(oneData
          .getOneproviders())
          .hasSize(1)
          .contains(providerInfo);
    } else {
      assertThat(oneData
          .getOneproviders()).extracting(OneDataProviderInfo::getEndpoint)
          .hasSize(2)
          .contains("provider-1.example.com", "provider-2.example.com");
    }
    mockServer.verify();
  }

  @Parameters({
      "true|false",
      "true|true",
      "false|false",
      "false|true"
  })
  @Test
  public void testAddProviderInfoWithRequirementsForUserSpace(boolean isSmartScheduling,
      boolean withOnezoneEndpoint)
      throws IOException {

    mockForProvidersInfo(withOnezoneEndpoint ? customOneZoneEndpoint : defaultOneZoneEndpoint,
        "space-name-1", 1, 2);

    OneData oneData = OneData
        .builder()
        .token(onedataToken)
        .space("space-name-1")
        .smartScheduling(isSmartScheduling)
        .oneproviders(Lists.newArrayList(OneDataProviderInfo
            .builder()
            .endpoint("provider-2.example.com")
            .build()))
        .build();

    if (withOnezoneEndpoint) {
      oneData.setOnezone(customOneZoneEndpoint);
    }

    Map<String, CloudProvider> cloudProviders = generateCloudProviders(2, 1);

    if (isSmartScheduling) {
      assertThatCode(
          () -> oneDataService
              .populateProviderInfo(oneData, cloudProviders, oidcTokenId, deploymentId))
          .isInstanceOf(DeploymentException.class)
          .hasMessage(
              "Requested OneProvider %s not registered in CMDB hence not eligible for smart scheduling",
              "provider-2.example.com");
    } else {
      oneDataService.populateProviderInfo(oneData, cloudProviders, oidcTokenId, deploymentId);
      assertThat(oneData
          .getOneproviders().stream().map(OneDataProviderInfo::getEndpoint))
          .hasSize(1)
          .contains("provider-2.example.com");
    }
    mockServer.verify();
  }

  @Test
  public void testFailNoProviders() throws IOException {

    mockForProvidersInfo(defaultOneZoneEndpoint, "space-name-1");

    OneData oneData = OneData
        .builder()
        .token(onedataToken)
        .space("space-name-1")
        .build();
    Map<String, CloudProvider> cloudProviders = new HashMap<>();

    assertThatCode(
        () -> oneDataService
            .populateProviderInfo(oneData, cloudProviders, oidcTokenId, deploymentId))
        .isInstanceOf(DeploymentException.class)
        .hasMessage("No OneProviders available for the space %s", "space-name-1");
    mockServer.verify();
  }

  @Test
  public void testFailMissingRequiredProvider() throws IOException {

    mockForProvidersInfo(defaultOneZoneEndpoint, "space-name-1", 1);

    OneData oneData = OneData
        .builder()
        .token(onedataToken)
        .space("space-name-1")
        .oneproviders(Lists.newArrayList(OneDataProviderInfo
            .builder()
            .endpoint("provider-2.example.com")
            .build()))
        .build();
    Map<String, CloudProvider> cloudProviders = new HashMap<>();

    assertThatCode(
        () -> oneDataService
            .populateProviderInfo(oneData, cloudProviders, oidcTokenId, deploymentId))
        .isInstanceOf(DeploymentException.class)
        .hasMessage("These requested OneProviders are not supporting the space %s:\n%s",
            "space-name-1", Arrays.toString(new String[]{"provider-2.example.com"}));
    mockServer.verify();
  }

  @Test
  public void testAddProviderInfoGeneratingToken() throws IOException {
    Tokens tokens = Tokens.builder().build();
    Token token = Token.builder().token(onedataToken).build();
    mockServer
        .expect(requestTo(defaultOneZoneEndpoint + oneDataProperties.getOnezoneBasePath()
            + "user/client_tokens"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Auth-Token", "OrganizationName:AccessToken"))
        .andRespond(withSuccess(JsonUtils.serialize(tokens),
            MediaType.APPLICATION_JSON_UTF8));
    mockServer
        .expect(requestTo(defaultOneZoneEndpoint + oneDataProperties.getOnezoneBasePath()
            + "user/client_tokens"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("X-Auth-Token", "OrganizationName:AccessToken"))
        .andRespond(withSuccess(JsonUtils.serialize(token),
            MediaType.APPLICATION_JSON_UTF8));

    mockForProvidersInfo(defaultOneZoneEndpoint, "space-name-1", 1);

    OneData oneData = OneData
        .builder()
        .space("space-name-1")
        .build();
    Map<String, CloudProvider> cloudProviders = new HashMap<>();

    oneDataService.populateProviderInfo(oneData, cloudProviders, oidcTokenId, deploymentId);
    assertThat(oneData.getToken()).isEqualTo(onedataToken);
    mockServer.verify();
  }

  private OneDataProviderInfo generateOneDataProviderInfo(int cloudProviderId, int oneProviderId) {
    return OneDataProviderInfo
        .builder()
        .id("provider-id-" + oneProviderId)
        .endpoint("provider-" + oneProviderId + ".example.com")
        .cloudProviderId("cloud-provider-id-" + cloudProviderId)
        .cloudServiceId("oneprovider-service-id-" + oneProviderId)
        .build();
  }

  private Map<String, CloudProvider> generateCloudProviders(int cloudProviderId, int... ids) {
    Map<String, CloudService> cloudServices = new HashMap<>();
    for (Integer id : ids) {
      CloudService oneProviderService = CloudService
          .builder()
          .id("oneprovider-service-id-" + id)
          .providerId("cloud-provider-id-" + cloudProviderId)
          .endpoint("provider-id-" + id)
          .serviceType(CloudService.ONEPROVIDER_STORAGE_SERVICE)
          .type(CloudServiceType.STORAGE)
          .hostname("provider-" + id + ".example.com")
          .build();
      cloudServices.put(oneProviderService.getId(), oneProviderService);
    }

    Map<String, CloudProvider> cloudProviders = new HashMap<>();
    CloudProvider cloudProvider = CloudProvider
        .builder()
        .id("cloud-provider-id-" + cloudProviderId)
        .name("cloud-provider-name-" + cloudProviderId)
        .services(cloudServices)
        .build();
    cloudProviders.put(cloudProvider.getId(), cloudProvider);
    return cloudProviders;
  }

  private UserSpaces generateUserSpaces() {
    return UserSpaces
        .builder()
        .spaces(Lists.newArrayList("space-id-1", "space-id-2"))
        .build();
  }

  private SpaceDetails generateSpaceDetails(String spaceName, int... ids) {
    Map<String, Long> providers = new HashMap<>();
    for (Integer id : ids) {
      providers.put("provider-id-" + id, 1L);
    }
    return SpaceDetails
        .builder()
        .name(spaceName)
        .spaceId("space-id-1")
        .providers(providers)
        .build();
  }

  private ProviderDetails generateProviderDetails(int id) {
    return ProviderDetails
        .builder()
        .domain("provider-" + id + ".example.com")
        .latitude(0.0)
        .longitude(0.0)
        .providerId("provider-id-" + id)
        .build();
  }

}
