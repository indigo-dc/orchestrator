package it.reply.orchestrator.service;

import static org.junit.Assert.assertEquals;

import it.reply.orchestrator.config.specific.WebAppConfigurationAwareIT;
import it.reply.orchestrator.dto.slam.SlamPreferences;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This integration test makes real request to the SLAM APIs.
 * 
 * @author l.biava
 *
 */
public class SlamServiceIT extends WebAppConfigurationAwareIT {

  @Autowired
  private SlamService service;

  @Test
  public void getPreferencesTest() throws Exception {
    SlamPreferences pref = service.getCustomerPreferences(null);
    assertEquals("4401ac5dc8cfbbb737b0a02575ee3b58", pref.getSla().get(0).getId());
  }

}
