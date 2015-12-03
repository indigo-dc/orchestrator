package it.reply.orchestrator.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@JsonPropertyOrder({ "code", "title", "message" })
public class Error {

  @JsonProperty("code")
  private Integer code;
  @JsonProperty("title")
  private String title;
  @JsonProperty("message")
  private String message;

  /**
   * 
   * @return The code.
   */
  @JsonProperty("code")
  public Integer getCode() {
    return code;
  }

  /**
   * 
   * @param code
   *          The code.
   */
  @JsonProperty("code")
  public void setCode(Integer code) {
    this.code = code;
  }

  public Error withCode(Integer code) {
    this.code = code;
    return this;
  }

  /**
   * 
   * @return The title.
   */
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  /**
   * 
   * @param title
   *          The title.
   */
  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  public Error withTitle(String title) {
    this.title = title;
    return this;
  }

  /**
   * 
   * @return The message.
   */
  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  /**
   * 
   * @param message
   *          The message.
   */
  @JsonProperty("message")
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