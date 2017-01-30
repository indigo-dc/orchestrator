package it.reply.orchestrator.dto.onedata;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSpaces implements Serializable {

  private static final long serialVersionUID = 2242273425591647283L;

  @JsonProperty("spaces")
  private List<String> spaces = Lists.newArrayList();
  @JsonProperty("defaultSpace")
  private String defaultSpace;

  public List<String> getSpaces() {
    return spaces;
  }

  public String getDefaultSpace() {
    return defaultSpace;
  }

  public void setSpaces(List<String> spaces) {
    this.spaces = spaces;
  }

  public void setDefaultSpace(String defaultSpace) {
    this.defaultSpace = defaultSpace;
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
