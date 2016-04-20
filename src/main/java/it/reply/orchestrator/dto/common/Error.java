package it.reply.orchestrator.dto.common;

public class Error {

  private Integer code;
  private String title;
  private String message;

  public Integer getCode() {
    return code;
  }

  public Error withCode(Integer code) {
    this.code = code;
    return this;
  }

  public String getTitle() {
    return title;
  }

  public Error withTitle(String title) {
    this.title = title;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public Error withMessage(String message) {
    this.message = message;
    return this;
  }

}