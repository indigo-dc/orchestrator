package it.reply.orchestrator.util;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestUtil {

  public static byte[] convertObjectToJsonBytes(Object object) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return mapper.writeValueAsBytes(object);
  }

  public static String createStringWithLength(int length) {
    StringBuilder builder = new StringBuilder();

    for (int index = 0; index < length; index++) {
      builder.append("a");
    }

    return builder.toString();
  }
}