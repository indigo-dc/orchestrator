package it.reply.orchestrator.config.security;

import com.google.common.collect.Sets;

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

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.config.properties.OidcProperties.IamProperties;
import it.reply.orchestrator.config.properties.OidcProperties.OrchestratorProperties;
import it.reply.orchestrator.exception.CustomOAuth2ExceptionRenderer;
import it.reply.orchestrator.service.security.IndigoUserInfoFetcher;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
@EnableConfigurationProperties(OidcProperties.class)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  static final Logger LOG = LoggerFactory.getLogger(WebSecurityConfig.class);

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private OidcProperties oidcProperties;

  @Configuration
  public static class OidcConfig {

    @Bean
    protected ServerConfigurationService serverConfigurationService(OidcProperties oidcProperties) {
      if (oidcProperties.isEnabled()) {
        DynamicServerConfigurationService serverConfigurationService =
            new DynamicServerConfigurationService();
        serverConfigurationService.setWhitelist(oidcProperties.getIamProperties()
            .stream()
            .map(IamProperties::getIssuer)
            .collect(Collectors.toSet()));
        return serverConfigurationService;
      } else {
        return null;
      }
    }

    @Bean
    protected ClientConfigurationService clientConfigurationService(OidcProperties oidcProperties) {
      if (oidcProperties.isEnabled()) {
        Map<String, RegisteredClient> clients = new HashMap<>();
        for (IamProperties configuration : oidcProperties.getIamProperties()) {
          OrchestratorProperties orchestrator = configuration.getOrchestrator();
          RegisteredClient client = new RegisteredClient();
          client.setClientId(orchestrator.getClientId());
          client.setClientSecret(orchestrator.getClientSecret());
          client.setScope(Sets.newHashSet(orchestrator.getScopes()));
          clients.put(configuration.getIssuer(), client);
        }

        StaticClientConfigurationService clientConfigurationService =
            new StaticClientConfigurationService();
        clientConfigurationService.setClients(clients);

        return clientConfigurationService;
      } else {
        return null;
      }
    }

    @Bean
    protected UserInfoFetcher userInfoFetcher(OidcProperties oidcProperties) {
      if (oidcProperties.isEnabled()) {
        return new IndigoUserInfoFetcher();
      } else {
        return null;
      }
    }

    @Bean
    protected IntrospectionConfigurationService introspectionConfigurationService(
        OidcProperties oidcProperties) {
      if (oidcProperties.isEnabled()) {
        JWTParsingIntrospectionConfigurationService introspectionConfigurationService =
            new JWTParsingIntrospectionConfigurationService();
        introspectionConfigurationService
            .setServerConfigurationService(serverConfigurationService(oidcProperties));
        introspectionConfigurationService
            .setClientConfigurationService(clientConfigurationService(oidcProperties));
        return introspectionConfigurationService;
      } else {
        return null;
      }
    }

    @Bean
    protected JWKSetCacheService validationServices(OidcProperties oidcProperties) {
      if (oidcProperties.isEnabled()) {
        return new JWKSetCacheService();
      } else {
        return null;
      }
    }

    @Bean
    protected ResourceServerTokenServices introspectingTokenService(OidcProperties oidcProperties) {
      if (oidcProperties.isEnabled()) {
        UserInfoIntrospectingTokenService introspectingTokenService =
            new UserInfoIntrospectingTokenService(serverConfigurationService(oidcProperties),
                userInfoFetcher(oidcProperties), validationServices(oidcProperties));
        introspectingTokenService.setIntrospectionConfigurationService(
            introspectionConfigurationService(oidcProperties));
        introspectingTokenService.setCacheTokens(oidcProperties.isCacheTokens());

        // Disabled for now as there is no revocation
        // introspectingTokenService.setDefaultExpireTime(5* 60 * 1000); // 5 min
        // introspectingTokenService.setForceCacheExpireTime(true);

        return introspectingTokenService;
      } else {
        return null;
      }
    }
  }

  private static final class NoOpAuthenticationProvider implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) {
      throw new UnsupportedOperationException(
          "This AuthenticationProvider must not be used to authenticate");
    }

    @Override
    public boolean supports(Class<?> authentication) {
      return false;
    }
  }

  @Bean
  @Override
  public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.authenticationProvider(new NoOpAuthenticationProvider());
  }

  @Override
  public void configure(WebSecurity webSecurity) throws Exception {
    if (oidcProperties.isEnabled()) {
      webSecurity.ignoring().regexMatchers("/", "/info");
    } else {
      webSecurity.ignoring().anyRequest();
    }
  }

  @Override
  public void configure(HttpSecurity http) throws Exception {
    if (oidcProperties.isEnabled()) {
      http.csrf().disable();
      http.authorizeRequests()
          .anyRequest()
          .fullyAuthenticated()
          .anyRequest()
          .access("#oauth2.hasScopeMatching('openid')")
          .and()
          .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
      ResourceServerSecurityConfigurer configurer = new ResourceServerSecurityConfigurer();
      configurer.setBuilder(http);
      configurer.tokenServices(applicationContext.getBean(ResourceServerTokenServices.class));

      CustomOAuth2ExceptionRenderer exceptionRenderer = new CustomOAuth2ExceptionRenderer();

      OAuth2AuthenticationEntryPoint authenticationEntryPoint =
          new OAuth2AuthenticationEntryPoint();
      authenticationEntryPoint.setExceptionRenderer(exceptionRenderer);
      configurer.authenticationEntryPoint(authenticationEntryPoint);

      OAuth2AccessDeniedHandler accessDeniedHandler = new OAuth2AccessDeniedHandler();
      accessDeniedHandler.setExceptionRenderer(exceptionRenderer);
      configurer.accessDeniedHandler(accessDeniedHandler);

      configurer.configure(http);
    } else {
      super.configure(http);
    }
  }
}
