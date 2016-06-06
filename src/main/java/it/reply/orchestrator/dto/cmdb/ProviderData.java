package it.reply.orchestrator.dto.cmdb;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderData implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  @JsonProperty("id")
  private String id;
  @JsonProperty("primary_key")
  private String primaryKey;
  @JsonProperty("name")
  private String name;
  @JsonProperty("country")
  private String country;
  @JsonProperty("country_code")
  private String countryCode;
  @JsonProperty("roc")
  private String roc;
  @JsonProperty("subgrid")
  private String subgrid;
  @JsonProperty("giis_url")
  private String giisUrl;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<String, Object>();

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

  public ProviderData withId(String id) {
    this.id = id;
    return this;
  }

  /**
   * 
   * @return The primaryKey
   */
  @JsonProperty("primary_key")
  public String getPrimaryKey() {
    return primaryKey;
  }

  /**
   * 
   * @param primaryKey
   *          The primary_key
   */
  @JsonProperty("primary_key")
  public void setPrimaryKey(String primaryKey) {
    this.primaryKey = primaryKey;
  }

  public ProviderData withPrimaryKey(String primaryKey) {
    this.primaryKey = primaryKey;
    return this;
  }

  /**
   * 
   * @return The name
   */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
   * 
   * @param name
   *          The name
   */
  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public ProviderData withName(String name) {
    this.name = name;
    return this;
  }

  /**
   * 
   * @return The country
   */
  @JsonProperty("country")
  public String getCountry() {
    return country;
  }

  /**
   * 
   * @param country
   *          The country
   */
  @JsonProperty("country")
  public void setCountry(String country) {
    this.country = country;
  }

  public ProviderData withCountry(String country) {
    this.country = country;
    return this;
  }

  /**
   * 
   * @return The countryCode
   */
  @JsonProperty("country_code")
  public String getCountryCode() {
    return countryCode;
  }

  /**
   * 
   * @param countryCode
   *          The country_code
   */
  @JsonProperty("country_code")
  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }

  public ProviderData withCountryCode(String countryCode) {
    this.countryCode = countryCode;
    return this;
  }

  /**
   * 
   * @return The roc
   */
  @JsonProperty("roc")
  public String getRoc() {
    return roc;
  }

  /**
   * 
   * @param roc
   *          The roc
   */
  @JsonProperty("roc")
  public void setRoc(String roc) {
    this.roc = roc;
  }

  public ProviderData withRoc(String roc) {
    this.roc = roc;
    return this;
  }

  /**
   * 
   * @return The subgrid
   */
  @JsonProperty("subgrid")
  public String getSubgrid() {
    return subgrid;
  }

  /**
   * 
   * @param subgrid
   *          The subgrid
   */
  @JsonProperty("subgrid")
  public void setSubgrid(String subgrid) {
    this.subgrid = subgrid;
  }

  public ProviderData withSubgrid(String subgrid) {
    this.subgrid = subgrid;
    return this;
  }

  /**
   * 
   * @return The giisUrl
   */
  @JsonProperty("giis_url")
  public String getGiisUrl() {
    return giisUrl;
  }

  /**
   * 
   * @param giisUrl
   *          The giis_url
   */
  @JsonProperty("giis_url")
  public void setGiisUrl(String giisUrl) {
    this.giisUrl = giisUrl;
  }

  public ProviderData withGiisUrl(String giisUrl) {
    this.giisUrl = giisUrl;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  public ProviderData withAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
    return this;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(id).append(primaryKey).append(name).append(country)
        .append(countryCode).append(roc).append(subgrid).append(giisUrl)
        .append(additionalProperties).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof ProviderData) == false) {
      return false;
    }
    ProviderData rhs = ((ProviderData) other);
    return new EqualsBuilder().append(id, rhs.id).append(primaryKey, rhs.primaryKey)
        .append(name, rhs.name).append(country, rhs.country).append(countryCode, rhs.countryCode)
        .append(roc, rhs.roc).append(subgrid, rhs.subgrid).append(giisUrl, rhs.giisUrl)
        .append(additionalProperties, rhs.additionalProperties).isEquals();
  }

}