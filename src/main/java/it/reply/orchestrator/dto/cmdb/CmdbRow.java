package it.reply.orchestrator.dto.cmdb;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "id", "key", "value", "doc" })
public class CmdbRow<T> implements Serializable {

  private static final long serialVersionUID = 559476054523810413L;

  @JsonProperty("id")
  private String id;
  @JsonProperty("key")
  private List<String> key = new ArrayList<String>();
  @JsonProperty("doc")
  private T doc;

  @JsonProperty("id")
  public String getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  public CmdbRow<T> withId(String id) {
    this.id = id;
    return this;
  }

  @JsonProperty("key")
  public List<String> getKey() {
    return key;
  }

  @JsonProperty("key")
  public void setKey(List<String> key) {
    this.key = key;
  }

  public CmdbRow<T> withKey(List<String> key) {
    this.key = key;
    return this;
  }

  @JsonProperty("doc")
  public T getDoc() {
    return doc;
  }

  @JsonProperty("doc")
  public void setDoc(T doc) {
    this.doc = doc;
  }

  public CmdbRow<T> withDoc(T doc) {
    this.doc = doc;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}