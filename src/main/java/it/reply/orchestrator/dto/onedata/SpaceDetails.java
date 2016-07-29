package it.reply.orchestrator.dto.onedata;

import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpaceDetails implements Serializable {

  private static final long serialVersionUID = -368387049626457198L;

  @JsonProperty("spaceId")
  private String spaceId;
  @JsonProperty("name")
  private String name;
  @JsonProperty("canonicalName")
  private String canonicalName;
  @JsonProperty("providersSupports")
  private Map<String, Integer> providersSupports = Maps.newHashMap();

  /**
   * @return the spaceId
   */
  public String getSpaceId() {
    return spaceId;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the canonicalName
   */
  public String getCanonicalName() {
    return canonicalName;
  }

  /**
   * @return the providersSupports
   */
  public Map<String, Integer> getProvidersSupports() {
    return providersSupports;
  }

  /**
   * @param spaceId
   *          the spaceId to set
   */
  public void setSpaceId(String spaceId) {
    this.spaceId = spaceId;
  }

  /**
   * @param name
   *          the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @param canonicalName
   *          the canonicalName to set
   */
  public void setCanonicalName(String canonicalName) {
    this.canonicalName = canonicalName;
  }

  /**
   * @param providersSupports
   *          the providersSupports to set
   */
  public void setProvidersSupports(Map<String, Integer> providersSupports) {
    this.providersSupports = providersSupports;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public boolean equals(Object other) {
    return EqualsBuilder.reflectionEquals(this, other);
  }
}
