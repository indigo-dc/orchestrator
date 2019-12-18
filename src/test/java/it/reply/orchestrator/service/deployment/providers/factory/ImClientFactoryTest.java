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

package it.reply.orchestrator.service.deployment.providers.factory;

import static org.mockito.Mockito.when;

import alien4cloud.tosca.parser.ParsingException;

import com.google.common.collect.Lists;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;

import es.upv.i3m.grycap.im.InfrastructureManager;

import it.reply.orchestrator.config.properties.ImProperties;
import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dal.entity.OidcEntityId;
import it.reply.orchestrator.dal.repository.OidcEntityRepository;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;
import it.reply.orchestrator.dto.security.GenericServiceCredential;
import it.reply.orchestrator.dto.security.GenericServiceCredentialWithTenant;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.deployment.providers.CredentialProviderService;
import it.reply.orchestrator.utils.CommonUtils;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;

@RunWith(JUnitParamsRunner.class)
public class ImClientFactoryTest {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  private static String paasImUrl = "https://im.url";

  private static final String iamToken = new PlainJWT(new JWTClaimsSet
      .Builder()
      .subject("subject")
      .issuer("https://example.com")
      .build())
      .serialize();

  private static String imTokenAuthHeader =
      "id = im ; type = InfrastructureManager ; token = " + iamToken;

  @InjectMocks
  private ImClientFactory imClientFactory;

  @Spy
  private ImProperties imProperties;

  @Spy
  private OidcProperties oidcProperties;

  @Mock
  private CredentialProviderService credProvServ;

  @Mock
  private OidcEntityRepository oidcEntityRepository;

  @Before
  public void setup() throws ParsingException {
    imProperties.setUrl(CommonUtils.checkNotNull(URI.create(paasImUrl)));
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Parameters({ "custom_id", "null" })
  public void testGetClientOst(@Nullable String headerId) {
    CloudProviderEndpoint cloudProviderEndpoint =
        CloudProviderEndpoint
            .builder()
            .iaasType(IaaSType.OPENSTACK)
            .cpEndpoint("https://host:5000/v3")
            .cpComputeServiceId(UUID.randomUUID().toString())
            .iaasHeaderId(headerId)
            .iamEnabled(true)
            .build();

    String iaasAuthHeader =
        "id = " + (headerId != null ? headerId : "ost")
            + " ; type = OpenStack ; tenant = oidc ; username = oidc-organization ; password = "
            + iamToken
            + " ; host = https://host:5000 ; auth_version = 3.x_oidc_access_token";

    OidcEntity entity = new OidcEntity();
    entity.setOidcEntityId(OidcEntityId.fromAccesToken(iamToken));
    entity.setOrganization("oidc-organization");
    when(oidcEntityRepository.findByOidcEntityId(entity.getOidcEntityId()))
        .thenReturn(Optional.of(entity));
    testGetClient(cloudProviderEndpoint, ImClientFactoryTest.paasImUrl, iaasAuthHeader);
  }

  @Test
  @Parameters({ "custom_id", "null" })
  public void testGetClientOstNoIam(@Nullable String headerId) {
    CloudProviderEndpoint cloudProviderEndpoint =
        CloudProviderEndpoint
            .builder()
            .iaasType(IaaSType.OPENSTACK)
            .cpEndpoint("https://host:5000/v3")
            .cpComputeServiceId(UUID.randomUUID().toString())
            .iaasHeaderId(headerId)
            .iamEnabled(false)
            .build();

    String serviceId = cloudProviderEndpoint.getCpComputeServiceId();

    Mockito
    .when(credProvServ.credentialProvider(serviceId, iamToken, GenericServiceCredentialWithTenant.class))
    .thenReturn(new GenericServiceCredentialWithTenant("username", "password", "tenant"));

    String iaasAuthHeader =
        "id = " + (headerId != null ? headerId : "ost")
            + " ; type = OpenStack ; tenant = tenant ; username = username ; password = password"
            + " ; host = https://host:5000 ; auth_version = 3.x_oidc_access_token";

    OidcEntity entity = new OidcEntity();
    entity.setOidcEntityId(OidcEntityId.fromAccesToken(iamToken));
    entity.setOrganization("oidc-organization");
    when(oidcEntityRepository.findByOidcEntityId(entity.getOidcEntityId()))
        .thenReturn(Optional.of(entity));
    testGetClient(cloudProviderEndpoint, ImClientFactoryTest.paasImUrl, iaasAuthHeader);
  }

  @Test
  @Parameters({ "custom_id, https://local.im",
      "custom_id, null",
      "null, https://local.im",
      "null, null" })
  public void testGetClientOne(@Nullable String headerId, @Nullable String localImEndpoint) {
    CloudProviderEndpoint cloudProviderEndpoint = CloudProviderEndpoint
        .builder()
        .iaasType(IaaSType.OPENNEBULA)
        .cpEndpoint("https://host")
        .cpComputeServiceId(UUID.randomUUID().toString())
        .iaasHeaderId(headerId)
        .imEndpoint(localImEndpoint)
        .iamEnabled(true)
        .build();

    String iaasAuthHeader =
        "id = " + (headerId != null ? headerId : "one")
            + " ; type = OpenNebula ; host = https://host ; token = " + iamToken;
    testGetClient(cloudProviderEndpoint, (localImEndpoint != null ? localImEndpoint : paasImUrl),
        iaasAuthHeader);
  }

  @Test
  @Parameters({ "custom_id, https://local.im",
      "custom_id, null",
      "null, https://local.im",
      "null, null" })
  public void testGetClientOneNoIam(@Nullable String headerId, @Nullable String localImEndpoint) {
    CloudProviderEndpoint cloudProviderEndpoint = CloudProviderEndpoint
        .builder()
        .iaasType(IaaSType.OPENNEBULA)
        .cpEndpoint("https://host")
        .cpComputeServiceId(UUID.randomUUID().toString())
        .iaasHeaderId(headerId)
        .imEndpoint(localImEndpoint)
        .iamEnabled(false)
        .build();

    String serviceId = cloudProviderEndpoint.getCpComputeServiceId();

    Mockito
    .when(credProvServ.credentialProvider(serviceId, iamToken, GenericServiceCredential.class))
    .thenReturn(new GenericServiceCredential("username", "password"));

    String iaasAuthHeader =
        "id = " + (headerId != null ? headerId : "one")
            + " ; type = OpenNebula ; host = https://host"
            + " ; username = username ; password = password";
    testGetClient(cloudProviderEndpoint, (localImEndpoint != null ? localImEndpoint : paasImUrl),
        iaasAuthHeader);
  }

  @Test
  public void testMultipleOneWithLocalIm() {
    CloudProviderEndpoint cloudProviderEndpoint1 = CloudProviderEndpoint
        .builder()
        .iaasType(IaaSType.OPENNEBULA)
        .cpEndpoint("https://host1")
        .cpComputeServiceId(UUID.randomUUID().toString())
        .iaasHeaderId("one1")
        .imEndpoint("https://local.im1")
        .iamEnabled(true)
        .build();

    CloudProviderEndpoint cloudProviderEndpoint2 = CloudProviderEndpoint
        .builder()
        .iaasType(IaaSType.OPENNEBULA)
        .cpEndpoint("https://host2")
        .cpComputeServiceId(UUID.randomUUID().toString())
        .iaasHeaderId("one2")
        .imEndpoint("https://local.im2")
        .iamEnabled(true)
        .build();
    String iaasAuthHeader =
        "id = one1 ; type = OpenNebula ; host = https://host1 ; token = " + iamToken +
            "\\nid = one2 ; type = OpenNebula ; host = https://host2 ; token = " + iamToken;
    testGetClient(Lists.newArrayList(cloudProviderEndpoint1, cloudProviderEndpoint2), paasImUrl,
        iaasAuthHeader);
  }

  @Test
  public void testMultipleOneWithLocalImNoIam() {
    CloudProviderEndpoint cloudProviderEndpoint1 = CloudProviderEndpoint
        .builder()
        .iaasType(IaaSType.OPENNEBULA)
        .cpEndpoint("https://host1")
        .cpComputeServiceId(UUID.randomUUID().toString())
        .iaasHeaderId("one1")
        .imEndpoint("https://local.im1")
        .iamEnabled(false)
        .build();

    CloudProviderEndpoint cloudProviderEndpoint2 = CloudProviderEndpoint
        .builder()
        .iaasType(IaaSType.OPENNEBULA)
        .cpEndpoint("https://host2")
        .cpComputeServiceId(UUID.randomUUID().toString())
        .iaasHeaderId("one2")
        .imEndpoint("https://local.im2")
        .iamEnabled(false)
        .build();

    String serviceId1 = cloudProviderEndpoint1.getCpComputeServiceId();
    String serviceId2 = cloudProviderEndpoint2.getCpComputeServiceId();

    Mockito
    .when(credProvServ.credentialProvider(serviceId1, iamToken, GenericServiceCredential.class))
    .thenReturn(new GenericServiceCredential("username", "password"));

    Mockito
    .when(credProvServ.credentialProvider(serviceId2, iamToken, GenericServiceCredential.class))
    .thenReturn(new GenericServiceCredential("username2", "password2"));

    String iaasAuthHeader =
        "id = one1 ; type = OpenNebula ; host = https://host1 ;"
        + " username = username ; password = password"
        + "\\nid = one2 ; type = OpenNebula ; host = https://host2 ;"
        + " username = username2 ; password = password2";
    testGetClient(Lists.newArrayList(cloudProviderEndpoint1, cloudProviderEndpoint2), paasImUrl,
        iaasAuthHeader);
  }

  @Test
  @Parameters({ "custom_id", "null" })
  public void testGetClientAws(@Nullable String headerId) {
    CloudProviderEndpoint cloudProviderEndpoint = CloudProviderEndpoint
        .builder()
        .iaasType(IaaSType.AWS)
        .cpEndpoint("https://host/")
        .cpComputeServiceId(UUID.randomUUID().toString())
        .iaasHeaderId(headerId)
        .build();

    String serviceId = cloudProviderEndpoint.getCpComputeServiceId();

    Mockito
        .when(credProvServ.credentialProvider(serviceId, iamToken, GenericServiceCredential.class))
        .thenReturn(new GenericServiceCredential("username", "password"));

    String iaasAuthHeader =
        "id = " + (headerId != null ? headerId : "ec2")
            + " ; type = EC2 ; username = username ; password = password";
    testGetClient(cloudProviderEndpoint, ImClientFactoryTest.paasImUrl, iaasAuthHeader);
  }

  @Test
  @Parameters({ "custom_id", "null" })
  public void testGetClientAzure(@Nullable String headerId) {

    CloudProviderEndpoint cloudProviderEndpoint = CloudProviderEndpoint
        .builder()
        .iaasType(IaaSType.AZURE)
        .cpEndpoint("https://host/")
        .cpComputeServiceId(UUID.randomUUID().toString())
        .iaasHeaderId(headerId)
        .build();

    String serviceId = cloudProviderEndpoint.getCpComputeServiceId();

    Mockito
        .when(
            credProvServ.credentialProvider(serviceId, iamToken, GenericServiceCredentialWithTenant.class))
        .thenReturn(new GenericServiceCredentialWithTenant("username", "password", "subscription_id"));

    String iaasAuthHeader =
        "id = " + (headerId != null ? headerId : "azure")
            + " ; type = Azure ; username = username ; password = password ; subscription_id = subscription_id";
    testGetClient(cloudProviderEndpoint, ImClientFactoryTest.paasImUrl, iaasAuthHeader);
  }

  @Test
  @Parameters({ "custom_id", "null" })
  public void testGetClientOtcOldUsername(@Nullable String headerId) {
    CloudProviderEndpoint cloudProviderEndpoint = CloudProviderEndpoint
        .builder()
        .iaasType(IaaSType.OTC)
        .cpEndpoint("https://host/")
        .cpComputeServiceId(UUID.randomUUID().toString())
        .iaasHeaderId(headerId)
        .build();

    String serviceId = cloudProviderEndpoint.getCpComputeServiceId();

    Mockito
        .when(
            credProvServ.credentialProvider(serviceId, iamToken, GenericServiceCredentialWithTenant.class))
        .thenReturn(new GenericServiceCredentialWithTenant("034 domain-info", "password", "eu-de"));


    String iaasAuthHeader = "id = " + (headerId != null ? headerId : "ost")
        + " ; type = OpenStack ; domain = domain-info ; username = 034 domain-info ; password = password ; tenant = eu-de ; "
        + "auth_version = 3.x_password ;"
        + " host = https://host ; service_name = None ; service_region = eu-de";
    testGetClient(cloudProviderEndpoint, ImClientFactoryTest.paasImUrl, iaasAuthHeader);
  }

  @Test
  @Parameters({ "custom_id", "null" })
  public void testGetClientOtcNewUsername(@Nullable String headerId) {
    CloudProviderEndpoint cloudProviderEndpoint = CloudProviderEndpoint
        .builder()
        .iaasType(IaaSType.OTC)
        .cpEndpoint("https://host/")
        .cpComputeServiceId(UUID.randomUUID().toString())
        .iaasHeaderId(headerId)
        .build();

    String serviceId = cloudProviderEndpoint.getCpComputeServiceId();

    Mockito
        .when(
            credProvServ.credentialProvider(serviceId, iamToken, GenericServiceCredentialWithTenant.class))
        .thenReturn(new GenericServiceCredentialWithTenant("username", "password", "domain-info"));

    String iaasAuthHeader = "id = " + (headerId != null ? headerId : "ost")
        + " ; type = OpenStack ; domain = domain-info ; username = username ; password = password ; tenant = eu-de ; "
        + "auth_version = 3.x_password ;"
        + " host = https://host ; service_name = None ; service_region = eu-de";
    testGetClient(cloudProviderEndpoint, ImClientFactoryTest.paasImUrl, iaasAuthHeader);
  }

  @Test
  @Parameters({ "custom_id", "null" })
  public void testGetClientOtcNewUsernameAndTenantInfo(@Nullable String headerId) {
    CloudProviderEndpoint cloudProviderEndpoint = CloudProviderEndpoint
        .builder()
        .iaasType(IaaSType.OTC)
        .cpEndpoint("https://host/")
        .cpComputeServiceId(UUID.randomUUID().toString())
        .iaasHeaderId(headerId)
        .build();

    String serviceId = cloudProviderEndpoint.getCpComputeServiceId();

    Mockito
        .when(
            credProvServ.credentialProvider(serviceId, iamToken, GenericServiceCredentialWithTenant.class))
        .thenReturn(new GenericServiceCredentialWithTenant("username", "password", "domain-info"));

    String iaasAuthHeader = "id = " + (headerId != null ? headerId : "ost")
        + " ; type = OpenStack ; domain = domain-info ; username = username ; password = password ; tenant = eu-de ; "
        + "auth_version = 3.x_password ;"
        + " host = https://host ; service_name = None ; service_region = eu-de";
    testGetClient(cloudProviderEndpoint, ImClientFactoryTest.paasImUrl, iaasAuthHeader);
  }

  private void testGetClient(CloudProviderEndpoint cloudProviderEndpoint,
      String imUrl, String iaasAuthHeader) {
    testGetClient(Lists.newArrayList(cloudProviderEndpoint), imUrl, iaasAuthHeader);
  }

  private void testGetClient(List<CloudProviderEndpoint> cloudProviderEndpoints,
      String imUrl, String iaasAuthHeader) {

    oidcProperties.setEnabled(true);

    InfrastructureManager result = imClientFactory.build(cloudProviderEndpoints, iamToken);

    Assertions
        .assertThat(result)
        .extracting("imClient")
        .extracting("targetUrl", String.class)
        .containsExactly(imUrl);
    Assertions
        .assertThat(result)
        .extracting("imClient")
        .extracting("authorizationHeader", String.class)
        .containsExactly(imTokenAuthHeader + "\\n" + iaasAuthHeader);

  }

  @Test
  @Parameters({ "OPENSTACK", "OTC" })
  public void testOstWrongEndpointFormat(IaaSType iaasType) throws Exception {
    CloudProviderEndpoint cloudProviderEndpoint = CloudProviderEndpoint
        .builder()
        .iaasType(iaasType)
        .cpComputeServiceId(UUID.randomUUID().toString())
        .cpEndpoint("lorem.ipsum")
        .build();
    oidcProperties.setEnabled(true);
    Assertions
        .assertThatCode(
            () -> imClientFactory.build(Lists.newArrayList(cloudProviderEndpoint), iamToken))
        .isInstanceOf(DeploymentException.class)
        .hasMessage("Wrong OS endpoint format: lorem.ipsum");
  }
}
