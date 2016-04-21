package it.reply.orchestrator.resource.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Map;

public class CustomSerializer extends JsonSerializer<Map<String, String>> {

  @Override
  public void serialize(Map<String, String> value, JsonGenerator jgen, SerializerProvider provider)
      throws IOException, JsonProcessingException {
    jgen.writeStartObject();
    for (Map.Entry<String, String> e : value.entrySet()) {
      jgen.writeFieldName(e.getKey());
      // Write value as raw data, since it's already JSON text
      jgen.writeRawValue(e.getValue());
    }
    jgen.writeEndObject();
  }
}