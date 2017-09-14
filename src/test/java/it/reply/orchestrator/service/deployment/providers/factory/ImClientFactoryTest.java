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

package it.reply.orchestrator.service.deployment.providers.factory;

import com.google.common.collect.Lists;

import alien4cloud.tosca.parser.ParsingException;

import es.upv.i3m.grycap.im.InfrastructureManager;
import es.upv.i3m.grycap.im.auth.credentials.providers.ImCredentials;
import es.upv.i3m.grycap.im.auth.credentials.providers.OpenStackCredentials;

import it.reply.orchestrator.config.properties.ImProperties;
import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.CommonUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.net.URI;
import java.util.UUID;

public class ImClientFactoryTest {

  @InjectMocks
  private ImClientFactory imClientFactory;

  @Spy
  private ImProperties imProperties;

  @Spy
  private OidcProperties oidcProperties;

  @Mock
  private OAuth2TokenService oauth2TokenService;

  @Before
  public void setup() throws ParsingException {
    MockitoAnnotations.initMocks(this);
    imProperties.setUrl(CommonUtils.checkNotNull(URI.create("im.url")));
  }

  @Test
  public void testGetClientOpenStack() throws Exception {
    CloudProviderEndpoint cloudProviderEndpoint = new CloudProviderEndpoint();
    cloudProviderEndpoint.setIaasType(IaaSType.OPENSTACK);
    cloudProviderEndpoint.setCpEndpoint("https://recas.ba.infn/");
    cloudProviderEndpoint.setCpComputeServiceId(UUID.randomUUID().toString());

    oidcProperties.setEnabled(true);
    OidcTokenId id = new OidcTokenId();
    Mockito
        .when(oauth2TokenService.getAccessToken(id))
        .thenReturn("J1qK1c18UUGJFAzz9xnH56584l4");
    InfrastructureManager client =
        imClientFactory.build(Lists.newArrayList(cloudProviderEndpoint), id);

    // result
    OpenStackCredentials cred = OpenStackCredentials
        .buildCredentials()
        .withTenant("oidc")
        .withUsername("indigo-dc")
        .withPassword("J1qK1c18UUGJFAzz9xnH56584l4")
        .withHost("https://recas.ba.infn");

    String imAuthHeader =
        ImCredentials.buildCredentials().withToken("J1qK1c18UUGJFAzz9xnH56584l4").serialize();
    String imUrl = imProperties.getUrl().toString();

    InfrastructureManager result =
        new InfrastructureManager(imUrl, String.format("%s\\n%s", imAuthHeader, cred.serialize()));
    // TO-DO: Assert equals both result and client (How?)

    cloudProviderEndpoint.setCpEndpoint("https://www.openstack.org/");
    imClientFactory.build(Lists.newArrayList(cloudProviderEndpoint), id);
    // TO-DO: Assert equals both result and client

  }

  @Test
  public void testGetClientOpenNebula() throws Exception {
    CloudProviderEndpoint cloudProviderEndpoint = new CloudProviderEndpoint();
    cloudProviderEndpoint.setIaasType(IaaSType.OPENNEBULA);
    cloudProviderEndpoint.setCpEndpoint("https://recas.ba.infn/");
    cloudProviderEndpoint.setCpComputeServiceId(UUID.randomUUID().toString());

    oidcProperties.setEnabled(true);
    OidcTokenId id = new OidcTokenId();

    Mockito
        .when(oauth2TokenService.getAccessToken(id))
        .thenReturn("J1qK1c18UUGJFAzz9xnH56584l4");
    InfrastructureManager client =
        imClientFactory.build(Lists.newArrayList(cloudProviderEndpoint), id);

    // TO-DO: Assert equals both result and client

    // AWS
    cloudProviderEndpoint.setIaasType(IaaSType.AWS);
    cloudProviderEndpoint.setUsername("username");
    cloudProviderEndpoint.setPassword("password");
    imClientFactory.build(Lists.newArrayList(cloudProviderEndpoint), id);
    // TO-DO: Assert equals both result and client
  }

  @Test
  public void testGetClientAWS() throws Exception {
    CloudProviderEndpoint cloudProviderEndpoint = new CloudProviderEndpoint();
    cloudProviderEndpoint.setIaasType(IaaSType.AWS);
    cloudProviderEndpoint.setUsername("username");
    cloudProviderEndpoint.setPassword("password");
    cloudProviderEndpoint.setCpEndpoint("https://recas.ba.infn/");
    cloudProviderEndpoint.setCpComputeServiceId(UUID.randomUUID().toString());

    oidcProperties.setEnabled(true);
    OidcTokenId id = new OidcTokenId();
    Mockito
        .when(oauth2TokenService.getAccessToken(id))
        .thenReturn("J1qK1c18UUGJFAzz9xnH56584l4");
    InfrastructureManager client =
        imClientFactory.build(Lists.newArrayList(cloudProviderEndpoint), id);

    // TO-DO: Assert equals both result and client
  }

  @Test(expected = DeploymentException.class)
  public void testGetClientFailDeployment() throws Exception {
    CloudProviderEndpoint cloudProviderEndpoint = new CloudProviderEndpoint();
    cloudProviderEndpoint.setIaasType(IaaSType.OPENSTACK);
    cloudProviderEndpoint.setCpComputeServiceId(UUID.randomUUID().toString());
    cloudProviderEndpoint.setCpEndpoint("lorem.ipsum");

    oidcProperties.setEnabled(true);
    OidcTokenId id = new OidcTokenId();
    Mockito
        .when(oauth2TokenService.getAccessToken(id))
        .thenReturn("J1qK1c18UUGJFAzz9xnH56584l4");

    imClientFactory.build(Lists.newArrayList(cloudProviderEndpoint), id);
  }
}
