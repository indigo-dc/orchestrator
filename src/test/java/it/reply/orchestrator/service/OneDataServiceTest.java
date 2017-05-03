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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.formula.functions.T;
import org.hibernate.mapping.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.Lists;

import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.onedata.ProviderDetails;
import it.reply.orchestrator.dto.onedata.SpaceDetails;
import it.reply.orchestrator.dto.onedata.UserSpaces;
import it.reply.orchestrator.dto.onedata.OneData.OneDataProviderInfo;
import it.reply.orchestrator.exception.service.DeploymentException;

public class OneDataServiceTest {

  @InjectMocks
  private OneDataService oneDataService = new OneDataServiceImpl();

  @Mock
  private RestTemplate restTemplate;

  private String oneZoneBaseRestPath = "test";
  private String defaultOneZoneEndpoint = "zone";
  String onedataToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    // injection spring value in onedataservice
    ReflectionTestUtils.setField(oneDataService, "oneZoneBaseRestPath", oneZoneBaseRestPath);
    ReflectionTestUtils.setField(oneDataService, "defaultOneZoneEndpoint", defaultOneZoneEndpoint);
  }

  @Test
  public void testGetAttributes() {
    String serviceSpaceToken = "serviceSpaceToken";
    String serviceSpaceName = "serviceSpaceName";
    String serviceSpaceProvider = "serviceSpaceProvider";
    String serviceSpacePath = "getServiceSpacePath";

    ReflectionTestUtils.setField(oneDataService, "serviceSpaceToken", serviceSpaceToken);
    ReflectionTestUtils.setField(oneDataService, "serviceSpaceName", serviceSpaceName);
    ReflectionTestUtils.setField(oneDataService, "serviceSpaceProvider", serviceSpaceProvider);
    ReflectionTestUtils.setField(oneDataService, "serviceSpacePath", serviceSpacePath);

    Assert.assertEquals(oneDataService.getServiceSpaceToken(), serviceSpaceToken);
    Assert.assertEquals(oneDataService.getServiceSpaceName(), serviceSpaceName);
    Assert.assertEquals(oneDataService.getServiceSpaceProvider(), serviceSpaceProvider);
    Assert.assertEquals(oneDataService.getServiceSpacePath(), serviceSpacePath);

  }

  @Test
  public void testSuccessGetUserSpaceId() {
    UserSpaces userSpace = getUserSpaces();

    HttpEntity<UserSpaces> entity = getEntity(onedataToken);
    ResponseEntity<UserSpaces> responseEntity =
        new ResponseEntity<UserSpaces>(userSpace, HttpStatus.OK);

    Mockito.when(
        restTemplate.exchange(defaultOneZoneEndpoint + "/" + oneZoneBaseRestPath + "user/spaces",
            HttpMethod.GET, entity, UserSpaces.class))
        .thenReturn(responseEntity);

    Assert.assertEquals(oneDataService.getUserSpacesId(defaultOneZoneEndpoint, onedataToken),
        userSpace);
    Assert.assertEquals(oneDataService.getUserSpacesId(onedataToken), userSpace);

  }

  @Test(expected = DeploymentException.class)
  public void testFailGetUserSpaceId() {

    UserSpaces userSpace = getUserSpaces();

    HttpEntity<UserSpaces> entity = getEntity(onedataToken);

    // bad status
    ResponseEntity<UserSpaces> responseEntity =
        new ResponseEntity<UserSpaces>(userSpace, HttpStatus.BAD_REQUEST);

    Mockito.when(
        restTemplate.exchange(defaultOneZoneEndpoint + "/" + oneZoneBaseRestPath + "user/spaces",
            HttpMethod.GET, entity, UserSpaces.class))
        .thenReturn(responseEntity);

    oneDataService.getUserSpacesId(defaultOneZoneEndpoint, onedataToken);

  }

  @Test
  public void testSuccessGetSpaceDetailsFromId() {

    SpaceDetails details = getSpaceDetails();
    String spaceId = details.getSpaceId();

    String keyProvider = "x";
    HashMap<String, Long> providerSupports = new HashMap<>();
    providerSupports.put(keyProvider, 1L);
    details.setProvidersSupports(providerSupports);

    HttpEntity<SpaceDetails> entity = getEntity(onedataToken);
    ResponseEntity<SpaceDetails> responseEntity =
        new ResponseEntity<SpaceDetails>(details, HttpStatus.OK);

    Mockito.when(restTemplate.exchange(
        defaultOneZoneEndpoint + "/" + oneZoneBaseRestPath + "user/spaces/" + spaceId,
        HttpMethod.GET, entity, SpaceDetails.class)).thenReturn(responseEntity);

    Assert.assertEquals(
        oneDataService.getSpaceDetailsFromId(defaultOneZoneEndpoint, onedataToken, spaceId),
        details);
    Assert.assertEquals(oneDataService.getSpaceDetailsFromId(onedataToken, spaceId), details);
    Assert.assertEquals(
        oneDataService.getUserSpaceNameById(defaultOneZoneEndpoint, onedataToken, spaceId),
        details.getName());
    Assert.assertEquals(oneDataService.getUserSpaceNameById(onedataToken, spaceId),
        details.getName());

    // Result providerSupports

    ArrayList<String> result = Lists.newArrayList(providerSupports.keySet());
    Assert.assertEquals(
        oneDataService.getProvidersIdBySpaceId(defaultOneZoneEndpoint, onedataToken, spaceId),
        result);
    Assert.assertEquals(oneDataService.getProvidersIdBySpaceId(onedataToken, spaceId), result);

  }

  @Test(expected = DeploymentException.class)
  public void testFailGetSpaceDetailsFromId() {

    // UserSpaces userSpace = getUserSpaces();
    SpaceDetails details = getSpaceDetails();
    String spaceId = details.getSpaceId();

    HttpEntity<SpaceDetails> entity = getEntity(onedataToken);
    ResponseEntity<SpaceDetails> responseEntity =
        new ResponseEntity<SpaceDetails>(HttpStatus.BAD_REQUEST);

    Mockito.when(restTemplate.exchange(
        defaultOneZoneEndpoint + "/" + oneZoneBaseRestPath + "user/spaces/" + spaceId,
        HttpMethod.GET, entity, SpaceDetails.class)).thenReturn(responseEntity);

    oneDataService.getSpaceDetailsFromId(defaultOneZoneEndpoint, onedataToken, spaceId);
    oneDataService.getSpaceDetailsFromId(onedataToken, spaceId);

  }

  @Test
  public void testSuccessGetProviderDetailsFromId() {

    String spaceId = UUID.randomUUID().toString();
    ProviderDetails providerDetail = getProviderDetails();
    String providerId = providerDetail.getProviderId();

    HttpEntity<ProviderDetails> entity = getEntity(onedataToken);
    ResponseEntity<ProviderDetails> responseEntity =
        new ResponseEntity<ProviderDetails>(providerDetail, HttpStatus.OK);

    Mockito.when(restTemplate.exchange(
        "zone/" + oneZoneBaseRestPath + "spaces/" + spaceId + "/providers/" + providerId,
        HttpMethod.GET, entity, ProviderDetails.class)).thenReturn(responseEntity);

    Assert.assertEquals(oneDataService.getProviderDetailsFromId(defaultOneZoneEndpoint,
        onedataToken, spaceId, providerId), providerDetail);
    Assert.assertEquals(oneDataService.getProviderDetailsFromId(onedataToken, spaceId, providerId),
        providerDetail);

  }

  @Test(expected = DeploymentException.class)
  public void testFailGetProviderDetailsFromId() {

    String spaceId = UUID.randomUUID().toString();
    ProviderDetails providerDetail = getProviderDetails();
    String providerId = providerDetail.getProviderId();

    HttpEntity<ProviderDetails> entity = getEntity(onedataToken);
    ResponseEntity<ProviderDetails> responseEntity =
        new ResponseEntity<ProviderDetails>(HttpStatus.BAD_REQUEST);

    Mockito.when(restTemplate.exchange(
        "zone/" + oneZoneBaseRestPath + "spaces/" + spaceId + "/providers/" + providerId,
        HttpMethod.GET, entity, ProviderDetails.class)).thenReturn(responseEntity);

    oneDataService.getProviderDetailsFromId(defaultOneZoneEndpoint, onedataToken, spaceId,
        providerId);
    oneDataService.getProviderDetailsFromId(onedataToken, spaceId, providerId);

  }

  @Test
  public void testEmptyPopulateProviderInfo() {

    SpaceDetails details = getSpaceDetails();
    String spaceId = details.getSpaceId();

    UserSpaces userSpace = getUserSpaces();
    userSpace.getSpaces().add(spaceId);

    OneData oneData =
        new OneData(onedataToken, "cname2", null, new ArrayList<>(), defaultOneZoneEndpoint);

    HttpEntity<UserSpaces> userSpaceEntity = getEntity(onedataToken);
    ResponseEntity<UserSpaces> responseUserSpaceEntity =
        new ResponseEntity<UserSpaces>(userSpace, HttpStatus.OK);

    Mockito.when(
        restTemplate.exchange(defaultOneZoneEndpoint + "/" + oneZoneBaseRestPath + "user/spaces",
            HttpMethod.GET, userSpaceEntity, UserSpaces.class))
        .thenReturn(responseUserSpaceEntity);

    HttpEntity<SpaceDetails> spaceDetailsEntity = getEntity(onedataToken);
    ResponseEntity<SpaceDetails> responseSpaceDetailsEntity =
        new ResponseEntity<SpaceDetails>(details, HttpStatus.OK);

    Mockito
        .when(restTemplate.exchange(
            defaultOneZoneEndpoint + "/" + oneZoneBaseRestPath + "user/spaces/"
                + userSpace.getSpaces().get(0),
            HttpMethod.GET, spaceDetailsEntity, SpaceDetails.class))
        .thenReturn(responseSpaceDetailsEntity);

    Assert.assertEquals(oneDataService.populateProviderInfo(oneData), oneData);

  }

  @Test(expected = DeploymentException.class)
  public void testFailEmptyPopulateProviderInfo() {

    SpaceDetails details = getSpaceDetails();
    String spaceId = details.getSpaceId();

    UserSpaces userSpace = getUserSpaces();
    List<String> spaces = new ArrayList<String>();
    userSpace.getSpaces().add(spaceId);

    OneData oneData =
        new OneData(onedataToken, "cname2", null, new ArrayList<>(), defaultOneZoneEndpoint);

    HttpEntity<UserSpaces> entity = getEntity(onedataToken);
    ResponseEntity<UserSpaces> responseEntity =
        new ResponseEntity<UserSpaces>(userSpace, HttpStatus.OK);

    spaces.clear();
    userSpace.setSpaces(spaces);
    responseEntity = new ResponseEntity<UserSpaces>(userSpace, HttpStatus.OK);

    Mockito.when(
        restTemplate.exchange(defaultOneZoneEndpoint + "/" + oneZoneBaseRestPath + "user/spaces",
            HttpMethod.GET, entity, UserSpaces.class))
        .thenReturn(responseEntity);

    oneDataService.populateProviderInfo(oneData);
  }

  @Test
  public void testAddProviderInfoToOneData() {
    String keyProvider = "x";
    HashMap<String, Long> providerSupports = new HashMap<>();
    providerSupports.put(keyProvider, 1L);

    SpaceDetails spaceDetails = getSpaceDetails();
    spaceDetails.setCanonicalName("cname2");
    spaceDetails.setProvidersSupports(providerSupports);
    String spaceId = spaceDetails.getSpaceId();

    UserSpaces userSpace = getUserSpaces();
    userSpace.getSpaces().add("cname2");

    ProviderDetails providerDetails = getProviderDetails();

    // user space
    HttpEntity<UserSpaces> userSpaceEntity = getEntity(onedataToken);
    ResponseEntity<UserSpaces> responseUserSpaceEntity =
        new ResponseEntity<UserSpaces>(userSpace, HttpStatus.OK);

    Mockito.when(
        restTemplate.exchange(defaultOneZoneEndpoint + "/" + oneZoneBaseRestPath + "user/spaces",
            HttpMethod.GET, userSpaceEntity, UserSpaces.class))
        .thenReturn(responseUserSpaceEntity);

    // space details
    HttpEntity<SpaceDetails> spaceDetailsEntity = getEntity(onedataToken);
    ResponseEntity<SpaceDetails> responseSpaceDetailsEntity =
        new ResponseEntity<SpaceDetails>(spaceDetails, HttpStatus.OK);

    Mockito
        .when(restTemplate.exchange(
            defaultOneZoneEndpoint + "/" + oneZoneBaseRestPath + "user/spaces/"
                + userSpace.getSpaces().get(0),
            HttpMethod.GET, spaceDetailsEntity, SpaceDetails.class))
        .thenReturn(responseSpaceDetailsEntity);

    // provider details
    HttpEntity<ProviderDetails> providerDetailsEntity = getEntity(onedataToken);
    ResponseEntity<ProviderDetails> responseProviderDetailsEntity =
        new ResponseEntity<ProviderDetails>(providerDetails, HttpStatus.OK);

    Mockito
        .when(
            restTemplate.exchange(
                defaultOneZoneEndpoint + "/" + oneZoneBaseRestPath + "spaces/" + spaceId
                    + "/providers/" + keyProvider,
                HttpMethod.GET, providerDetailsEntity, ProviderDetails.class))
        .thenReturn(responseProviderDetailsEntity);

    // onedata without new provider details
    OneData oneData =
        new OneData(onedataToken, "cname2", null, new ArrayList<>(), defaultOneZoneEndpoint);
    OneData populateProviderInfo = oneDataService.populateProviderInfo(oneData);

    // correct result with new provider details
    OneDataProviderInfo providerInfo = new OneDataProviderInfo();
    providerInfo.setId(providerDetails.getProviderId());
    providerInfo.setEndpoint(providerDetails.getRedirectionPoint());
    oneData.getProviders().add(providerInfo);

    Assert.assertEquals(populateProviderInfo, oneData);
  }

  private UserSpaces getUserSpaces() {
    UserSpaces userSpace = new UserSpaces();
    userSpace.setDefaultSpace("defaultSpace");
    userSpace.setSpaces(new ArrayList<String>());
    return userSpace;
  }

  private SpaceDetails getSpaceDetails() {
    SpaceDetails details = new SpaceDetails();
    details.setCanonicalName("cname");
    details.setName("name");
    details.setSpaceId(UUID.randomUUID().toString());
    return details;
  }

  private ProviderDetails getProviderDetails() {
    ProviderDetails details = new ProviderDetails();
    details.setClientName("clientName");
    details.setCsr("csr");
    details.setLatitude(41.25);
    details.setLongitude(-120.9762);
    details.setProviderId(UUID.randomUUID().toString());
    details.setRedirectionPoint("http://www.redirection.example");
    return details;
  }

  private <T> HttpEntity<T> getEntity(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("macaroon", token);
    return new HttpEntity<>(headers);
  }

}
