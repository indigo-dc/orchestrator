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

package it.reply.orchestrator.config.security;

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.config.properties.OidcProperties.ScopedOidcClientProperties;
import it.reply.orchestrator.exception.CustomOAuth2ExceptionRenderer;
import it.reply.orchestrator.service.security.IndigoUserInfoFetcher;
import it.reply.orchestrator.service.security.OAuth2ConfigurationsService;
import it.reply.orchestrator.service.security.UserInfoIntrospectingTokenService;

import org.mitre.jwt.signer.service.impl.JWKSetCacheService;
import org.mitre.oauth2.introspectingfilter.service.IntrospectionConfigurationService;
import org.mitre.oauth2.introspectingfilter.service.impl.JWTParsingIntrospectionConfigurationService;
import org.mitre.oauth2.model.RegisteredClient;
import org.mitre.openid.connect.client.UserInfoFetcher;
import org.mitre.openid.connect.client.service.ClientConfigurationService;
import org.mitre.openid.connect.client.service.ServerConfigurationService;
import org.mitre.openid.connect.client.service.impl.DynamicServerConfigurationService;
import org.mitre.openid.connect.client.service.impl.StaticClientConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Configuration
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
@ConditionalOnProperty(value = OidcProperties.SECURITY_ENABLED_PROPERTY, havingValue = "true")
public class AuthenticatedWebSecurityConfig extends BaseWebSecurityConfig {

  @Autowired
  @Lazy // TODO Ugly
  private OAuth2ConfigurationsService oauth2ConfigurationsService;

  /**
   * Generates a new ServerConfigurationService.
   * 
   * @return the generated ServerConfigurationService
   */
  @Bean
  public ServerConfigurationService serverConfigurationService() {
    DynamicServerConfigurationService serverConfigurationService =
        new DynamicServerConfigurationService();
    serverConfigurationService.setWhitelist(oidcProperties.getIamProperties().keySet());
    return serverConfigurationService;
  }

  /**
   * Generates a new ClientConfigurationService.
   * 
   * @return the generated ClientConfigurationService
   */
  @Bean
  public ClientConfigurationService clientConfigurationService() {
    Map<String, RegisteredClient> clients = new HashMap<>();
    oidcProperties
        .getIamProperties()
        .forEach((issuer, configuration) -> {
          ScopedOidcClientProperties orchestrator = configuration.getOrchestrator();
          RegisteredClient client = new RegisteredClient();
          client.setClientId(orchestrator.getClientId());
          client.setClientSecret(orchestrator.getClientSecret());
          client.setScope(new HashSet<>(orchestrator.getScopes()));
          clients.put(issuer, client);
        });

    StaticClientConfigurationService clientConfigurationService =
        new StaticClientConfigurationService();
    clientConfigurationService.setClients(clients);

    return clientConfigurationService;
  }

  @Bean
  public UserInfoFetcher userInfoFetcher() {
    return new IndigoUserInfoFetcher();
  }

  /**
   * Generates a new IntrospectionConfigurationService.
   * 
   * @return the generated IntrospectionConfigurationService
   */
  @Bean
  public IntrospectionConfigurationService introspectionConfigurationService() {
    JWTParsingIntrospectionConfigurationService introspectionConfigurationService =
        new JWTParsingIntrospectionConfigurationService();
    introspectionConfigurationService
        .setServerConfigurationService(serverConfigurationService());
    introspectionConfigurationService
        .setClientConfigurationService(clientConfigurationService());
    return introspectionConfigurationService;
  }

  @Bean
  public JWKSetCacheService validationServices() {
    return new JWKSetCacheService();
  }

  /**
   * Generates a new ResourceServerTokenServices.
   * 
   * @return the generated ResourceServerTokenServices
   */
  @Bean
  public ResourceServerTokenServices introspectingTokenService() {

    UserInfoIntrospectingTokenService introspectingTokenService =
        new UserInfoIntrospectingTokenService(oauth2ConfigurationsService,
            userInfoFetcher(), validationServices());
    introspectingTokenService.setIntrospectionConfigurationService(
        introspectionConfigurationService());
    introspectingTokenService.setCacheTokens(oidcProperties.isCacheTokens());

    // Disabled for now as there is no revocation
    // introspectingTokenService.setDefaultExpireTime(5* 60 * 1000); // 5 min
    // introspectingTokenService.setForceCacheExpireTime(true);

    return introspectingTokenService;
  }

  @Override
  public void configure(HttpSecurity http) throws Exception {
    super.configure(http);
    http
        .authorizeRequests()
        .anyRequest()
        .fullyAuthenticated()
        .anyRequest()
        .access("#oauth2.hasScopeMatching('openid') and #oauth2.hasScopeMatching('profile')");

    ResourceServerSecurityConfigurer configurer = new ResourceServerSecurityConfigurer();
    configurer.setBuilder(http);
    configurer.tokenServices(introspectingTokenService());

    CustomOAuth2ExceptionRenderer exceptionRenderer = new CustomOAuth2ExceptionRenderer();

    OAuth2AuthenticationEntryPoint authenticationEntryPoint = new OAuth2AuthenticationEntryPoint();
    authenticationEntryPoint.setExceptionRenderer(exceptionRenderer);
    configurer.authenticationEntryPoint(authenticationEntryPoint);

    OAuth2AccessDeniedHandler accessDeniedHandler = new OAuth2AccessDeniedHandler();
    accessDeniedHandler.setExceptionRenderer(exceptionRenderer);
    configurer.accessDeniedHandler(accessDeniedHandler);

    configurer.configure(http);
  }

  @Bean
  @Override
  public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
  }
}
