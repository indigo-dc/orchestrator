package it.reply.orchestrator.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@JsonPropertyOrder({ "href", "rel" })
public class Link {

  @JsonProperty("href")
  private String href;
  @JsonProperty("rel")
  private String rel;

  /**
   * 
   * @return The href.
   */
  @JsonProperty("href")
  public String getHref() {
    return href;
  }

  /**
   * 
   * @param href
   *          The href.
   */
  @JsonProperty("href")
  public void setHref(String href) {
    this.href = href;
  }

  public Link withHref(String href) {
    this.href = href;
    return this;
  }

  /**
   * 
   * @return The rel.
   */
  @JsonProperty("rel")
  public String getRel() {
    return rel;
  }

  /**
   * 
   * @param rel
   *          The rel.
   */
  @JsonProperty("rel")
  public void setRel(String rel) {
    this.rel = rel;
  }

  public Link withRel(String rel) {
    this.rel = rel;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(href).append(rel).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof Link) == false) {
      return false;
    }
    Link rhs = ((Link) other);
    return new EqualsBuilder().append(href, rhs.href).append(rel, rhs.rel).isEquals();
  }

}