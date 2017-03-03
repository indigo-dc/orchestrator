package it.reply.orchestrator.service.security;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dto.security.IndigoOAuth2Authentication;

public class OAuth2TokenServiceTest {

  @InjectMocks
  OAuth2TokenService oAuth2TokenService = new OAuth2TokenService();

  @Mock
  OidcProperties oidcProperties;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test(expected = IllegalStateException.class)
  public void failGetOAuth2Token() {
    Mockito.when(oidcProperties.isEnabled()).thenReturn(false);
    oAuth2TokenService.getOAuth2Token();
  }

  @Test(expected = IllegalStateException.class)
  public void failGetOAuth2TokenNull() {
    Mockito.when(oidcProperties.isEnabled()).thenReturn(true);
    oAuth2TokenService.getOAuth2Token();
  }

}
