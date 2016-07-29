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

  /**
   * @return the csr
   */
  public String getCsr() {
    return csr;
  }

  /**
   * @param csr
   *          the csr to set
   */
  public void setCsr(String csr) {
    this.csr = csr;
  }

  /**
   * @return the providerId
   */
  public String getProviderId() {
    return providerId;
  }

  /**
   * @return the clientName
   */
  public String getClientName() {
    return clientName;
  }

  /**
   * @return the redirectionPoint
   */
  public String getRedirectionPoint() {
    return redirectionPoint;
  }

  /**
   * @return the urls
   */
  public List<String> getUrls() {
    return urls;
  }

  /**
   * @return the latitude
   */
  public Double getLatitude() {
    return latitude;
  }

  /**
   * @return the longitude
   */
  public Double getLongitude() {
    return longitude;
  }

  /**
   * @param providerId
   *          the providerId to set
   */
  public void setProviderId(String providerId) {
    this.providerId = providerId;
  }

  /**
   * @param clientName
   *          the clientName to set
   */
  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  /**
   * @param redirectionPoint
   *          the redirectionPoint to set
   */
  public void setRedirectionPoint(String redirectionPoint) {
    this.redirectionPoint = redirectionPoint;
  }

  /**
   * @param urls
   *          the urls to set
   */
  public void setUrls(List<String> urls) {
    this.urls = urls;
  }

  /**
   * @param latitude
   *          the latitude to set
   */
  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  /**
   * @param longitude
   *          the longitude to set
   */
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
