package it.reply.orchestrator.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class TestUtil {

  /**
   * Convert Object To a Json byte array.
   * 
   * @return byte[]
   */
  public static byte[] convertObjectToJsonBytes(Object object) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return mapper.writeValueAsBytes(object);
  }

}