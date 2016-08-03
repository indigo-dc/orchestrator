package it.reply.orchestrator.dto.onedata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.elasticsearch.common.collect.Lists;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderDetails implements Serializable {

  private static final long serialVersionUID = -368387049626457198L;

  @JsonProperty("csr")
  private String csr;
  @JsonProperty("providerId")
  private String providerId;
  @JsonProperty("clientName")
  private String clientName;
  @JsonProperty("redirectionPoint")
  private String redirectionPoint;
  @JsonProperty("urls")
  private List<String> urls = Lists.newArrayList();
  @JsonProperty("latitude")
  private Double latitude;
  @JsonProperty("longitude")
  private Double longitude;

  public String getCsr() {
    return csr;
  }

  public void setCsr(String csr) {
    this.csr = csr;
  }

  public String getProviderId() {
    return providerId;
  }

  public String getClientName() {
    return clientName;
  }

  public String getRedirectionPoint() {
    return redirectionPoint;
  }

  public List<String> getUrls() {
    return urls;
  }

  public Double getLatitude() {
    return latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public void setProviderId(String providerId) {
    this.providerId = providerId;
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  public void setRedirectionPoint(String redirectionPoint) {
    this.redirectionPoint = redirectionPoint;
  }

  public void setUrls(List<String> urls) {
    this.urls = urls;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
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
