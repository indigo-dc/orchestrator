package it.reply.orchestrator.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.List;

public class Links {

  @JsonProperty("links")
  private List<Link> links;

  /**
   * 
   * @return The links.
   */
  @JsonProperty("links")
  public List<Link> getLinks() {
    return links;
  }

  /**
   * 
   * @param href
   *          The href.
   */
  @JsonProperty("links")
  public void setLinks(List<Link> links) {
    this.links = links;
  }

  public Links withLinks(List<Link> links) {
    this.links = links;
    return this;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(links).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof Links) == false) {
      return false;
    }
    Links rhs = ((Links) other);
    return new EqualsBuilder().append(links, rhs.links).isEquals();
  }

}