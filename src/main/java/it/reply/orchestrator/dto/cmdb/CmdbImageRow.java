package it.reply.orchestrator.dto.cmdb;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({ "id", "key", "value", "doc" })
public class CmdbImageRow {

  @JsonProperty("id")
  private String id;
  @JsonProperty("key")
  private List<String> key = new ArrayList<String>();
  @JsonProperty("doc")
  private CmdbImage image;

  /**
   * 
   * @return The id
   */
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  /**
   * 
   * @param id
   *          The id
   */
  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  public CmdbImageRow withId(String id) {
    this.id = id;
    return this;
  }

  /**
   * 
   * @return The key
   */
  @JsonProperty("key")
  public List<String> getKey() {
    return key;
  }

  /**
   * 
   * @param key
   *          The key
   */
  @JsonProperty("key")
  public void setKey(List<String> key) {
    this.key = key;
  }

  public CmdbImageRow withKey(List<String> key) {
    this.key = key;
    return this;
  }

  /**
   * 
   * @return The image
   */
  @JsonProperty("doc")
  public CmdbImage getImage() {
    return image;
  }

  /**
   * 
   * @param image
   *          The image
   */
  @JsonProperty("doc")
  public void setImage(CmdbImage image) {
    this.image = image;
  }

  public CmdbImageRow withImage(CmdbImage image) {
    this.image = image;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}