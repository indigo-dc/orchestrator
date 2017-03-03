package it.reply.orchestrator.service.security;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonObject;

public class IndigoUserInfoFetcherTest {

  @InjectMocks
  IndigoUserInfoFetcher indigoUserInfoFetcher = new IndigoUserInfoFetcher();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }


  @Test
  public void fromJson() {
    JsonObject userInfoJson = new JsonObject();
    indigoUserInfoFetcher.fromJson(userInfoJson);
  }
}
