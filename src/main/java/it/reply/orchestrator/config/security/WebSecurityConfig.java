package it.reply.orchestrator.config.security;

import com.google.common.collect.Sets;

import it.reply.orchestrator.service.security.IndigoUserInfoFetcher;
import it.reply.orchestrator.service.security.UserInfoIntrospectingTokenService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.oauth2.introspectingfilter.service.IntrospectionConfigurationService;
import org.mitre.oauth2.introspectingfilter.service.impl.JWTParsingIntrospectionConfigurationService;
import org.mitre.oauth2.model.RegisteredClient;
import org.mitre.openid.connect.client.UserInfoFetcher;
import org.mitre.openid.connect.client.service.ClientConfigurationService;
import org.mitre.openid.connect.client.service.ServerConfigurationService;
import org.mitre.openid.connect.client.service.impl.DynamicServerConfigurationService;
import org.mitre.openid.connect.client.service.impl.StaticClientConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
@PropertySource(value = { "classpath:security.properties" })
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  static final Logger LOG = LogManager.getLogger(WebSecurityConfig.class);

  @Value("${security.enabled}")
  private boolean securityEnabled;

  @Configuration
  public static class OidcConfig {
    @Value("${OIDC.issuer}")
    private String oidcIssuer;

    @Value("${OIDC.clientID}")
    private String oidcClientId;

    @Value("${OIDC.clientSecret}")
    private String oidcClientSecret;

    @Value("${OIDC.cacheTokens}")
    private boolean oidcCacheTokens;

    private Set<String> oidcClientScopes;

    public void setClientScopes(@Value("${OIDC.clientScopes}") String scopesString) {
      oidcClientScopes = Sets.newHashSet(scopesString.split(", ?"));
    }

    @Bean
    protected ServerConfigurationService serverConfigurationService() {
      DynamicServerConfigurationService serverConfigurationService =
          new DynamicServerConfigurationService();
      serverConfigurationService.setWhitelist(Sets.newHashSet(oidcIssuer));
      return serverConfigurationService;
    }

    @Bean
    protected ClientConfigurationService clientConfigurationService() {
      RegisteredClient client = new RegisteredClient();
      client.setClientId(oidcClientId);
      client.setClientSecret(oidcClientSecret);
      client.setScope(oidcClientScopes);
      Map<String, RegisteredClient> clients = new HashMap<>();
      clients.put(oidcIssuer, client);

      StaticClientConfigurationService clientConfigurationService =
          new StaticClientConfigurationService();
      clientConfigurationService.setClients(clients);

      return clientConfigurationService;
    }

    @Bean
    protected UserInfoFetcher userInfoFetcher() {
      return new IndigoUserInfoFetcher();
    }

    @Bean
    protected IntrospectionConfigurationService introspectionConfigurationService() {
      JWTParsingIntrospectionConfigurationService introspectionConfigurationService =
          new JWTParsingIntrospectionConfigurationService();
      introspectionConfigurationService.setServerConfigurationService(serverConfigurationService());
      introspectionConfigurationService.setClientConfigurationService(clientConfigurationService());
      return introspectionConfigurationService;
    }

    @Bean
    protected ResourceServerTokenServices introspectingTokenService() {
      UserInfoIntrospectingTokenService introspectingTokenService =
          new UserInfoIntrospectingTokenService();
      introspectingTokenService
          .setIntrospectionConfigurationService(introspectionConfigurationService());
      introspectingTokenService.setCacheTokens(oidcCacheTokens);
      introspectingTokenService.setServerConfigurationService(serverConfigurationService());
      introspectingTokenService.setUserInfoFetcher(userInfoFetcher());
      return introspectingTokenService;
    }
  }

  private static final class NoOpAuthenticationProvider implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication)
        throws AuthenticationException {
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
    if (securityEnabled) {
      webSecurity.ignoring().regexMatchers("/?");
    } else {
      webSecurity.ignoring().anyRequest();
    }
  }

  @Autowired
  @Lazy
  private ResourceServerTokenServices tokenServices;

  @Override
  public void configure(HttpSecurity http) throws Exception {
    if (securityEnabled) {
      http.csrf().disable();
      http.authorizeRequests().anyRequest().fullyAuthenticated().and().sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
      ResourceServerSecurityConfigurer configurer = new ResourceServerSecurityConfigurer();
      configurer.setBuilder(http);
      configurer.tokenServices(tokenServices);
      configurer.configure(http);

      // TODO Customize the authentication entry point in order to align the response body error
      // coming from the security filter chain to the ones coming from the REST controllers
      // see https://github.com/spring-projects/spring-security-oauth/issues/605
      // configurer.authenticationEntryPoint(new CustomAuthenticationEntryPoint());
    } else {
      super.configure(http);
    }
  }
}