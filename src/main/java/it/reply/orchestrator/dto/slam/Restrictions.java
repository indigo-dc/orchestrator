package it.reply.orchestrator.dto.slam;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Restrictions implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  @JsonProperty("total_guaranteed")
  private Integer totalGuaranteed;

  @JsonProperty("total-limit")
  private Integer totalLimit;

  @JsonProperty("instance-guaranteed")
  private Integer instanceGuaranteed;

  @JsonProperty("instance-limit")
  private Integer instanceLimit;

  @JsonProperty("user-guaranteed")
  private Integer userGuaranteed;

  @JsonProperty("user-limit")
  private Integer userLimit;

  @JsonProperty("total_guaranteed")
  public Integer getTotalGuaranteed() {
    return totalGuaranteed;
  }

  @JsonProperty("total_guaranteed")
  public void setTotalGuaranteed(Integer totalGuaranteed) {
    this.totalGuaranteed = totalGuaranteed;
  }

  public Restrictions withTotalGuaranteed(Integer totalGuaranteed) {
    this.totalGuaranteed = totalGuaranteed;
    return this;
  }

  @JsonProperty("total-limit")
  public Integer getTotalLimit() {
    return totalLimit;
  }

  @JsonProperty("total-limit")
  public void setTotalLimit(Integer totalLimit) {
    this.totalLimit = totalLimit;
  }

  public Restrictions withTotalLimit(Integer totalLimit) {
    this.totalLimit = totalLimit;
    return this;
  }

  @JsonProperty("instance-guaranteed")
  public Integer getInstanceGuaranteed() {
    return instanceGuaranteed;
  }

  @JsonProperty("instance-guaranteed")
  public void setInstanceGuaranteed(Integer instanceGuaranteed) {
    this.instanceGuaranteed = instanceGuaranteed;
  }

  public Restrictions withInstanceGuaranteed(Integer instanceGuaranteed) {
    this.instanceGuaranteed = instanceGuaranteed;
    return this;
  }

  @JsonProperty("instance-limit")
  public Integer getInstanceLimit() {
    return instanceLimit;
  }

  @JsonProperty("instance-limit")
  public void setInstanceLimit(Integer instanceLimit) {
    this.instanceLimit = instanceLimit;
  }

  public Restrictions withInstanceLimit(Integer instanceLimit) {
    this.instanceLimit = instanceLimit;
    return this;
  }

  @JsonProperty("user-guaranteed")
  public Integer getUserGuaranteed() {
    return userGuaranteed;
  }

  @JsonProperty("user-guaranteed")
  public void setUserGuaranteed(Integer userGuaranteed) {
    this.userGuaranteed = userGuaranteed;
  }

  public Restrictions withUserGuaranteed(Integer userGuaranteed) {
    this.userGuaranteed = userGuaranteed;
    return this;
  }

  @JsonProperty("user-limit")
  public Integer getUserLimit() {
    return userLimit;
  }

  @JsonProperty("user-limit")
  public void setUserLimit(Integer userLimit) {
    this.userLimit = userLimit;
  }

  public Restrictions withUserLimit(Integer userLimit) {
    this.userLimit = userLimit;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(totalGuaranteed).append(totalLimit)
        .append(instanceGuaranteed).append(instanceLimit).append(userGuaranteed).append(userLimit)
        .toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof Restrictions) == false) {
      return false;
    }
    Restrictions rhs = ((Restrictions) other);
    return new EqualsBuilder().append(totalGuaranteed, rhs.totalGuaranteed)
        .append(totalLimit, rhs.totalLimit).append(instanceGuaranteed, rhs.instanceGuaranteed)
        .append(instanceLimit, rhs.instanceLimit).append(userGuaranteed, rhs.userGuaranteed)
        .append(userLimit, rhs.userLimit).isEquals();
  }

}
