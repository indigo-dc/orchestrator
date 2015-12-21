package it.reply.orchestrator.dto.common;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Error {

  private Integer code;
  private String title;
  private String message;

  public Integer getCode() {
    return code;
  }

  public void setCode(Integer code) {
    this.code = code;
  }

  public Error withCode(Integer code) {
    this.code = code;
    return this;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Error withTitle(String title) {
    this.title = title;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Error withMessage(String message) {
    this.message = message;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(code).append(title).append(message).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof Error) == false) {
      return false;
    }
    Error rhs = ((Error) other);
    return new EqualsBuilder().append(code, rhs.code).append(title, rhs.title)
        .append(message, rhs.message).isEquals();
  }
}