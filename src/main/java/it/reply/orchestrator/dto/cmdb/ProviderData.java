package it.reply.orchestrator.dto.cmdb;

/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
   * Get the id of the provider.
   * 
   * @return The id
   */
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  /**
   * Set the id of the provider.
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
   * Get the primary key of the provider.
   * 
   * @return The primaryKey
   */
  @JsonProperty("primary_key")
  public String getPrimaryKey() {
    return primaryKey;
  }

  /**
   * Set the primary key of the provider.
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
   * Get the name of the provider.
   * 
   * @return The name
   */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
   * Set the name of the provider.
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
   * Get the country of the provider.
   * 
   * @return The country
   */
  @JsonProperty("country")
  public String getCountry() {
    return country;
  }

  /**
   * Set the country of the provider.
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
   * Get the country code of the provider.
   * 
   * @return The countryCode
   */
  @JsonProperty("country_code")
  public String getCountryCode() {
    return countryCode;
  }

  /**
   * Set the country code of the provider.
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
   * Get the ROC of the provider.
   * 
   * @return The roc
   */
  @JsonProperty("roc")
  public String getRoc() {
    return roc;
  }

  /**
   * Set the ROC of the provider.
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
   * Get the subgrid of the provider.
   * 
   * @return The subgrid
   */
  @JsonProperty("subgrid")
  public String getSubgrid() {
    return subgrid;
  }

  /**
   * Set the subgrid of the provider.
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
   * Get the giisUrl of the provider.
   * 
   * @return The giisUrl
   */
  @JsonProperty("giis_url")
  public String getGiisUrl() {
    return giisUrl;
  }

  /**
   * Set the giisUrl of the provider.
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
    if (other == null) {
      return false;
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