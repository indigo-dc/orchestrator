package it.reply.orchestrator.dto.common;

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
import org.springframework.http.HttpStatus;

import java.io.Serializable;

public class Error implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  private Integer code;
  private String title;
  private String message;

  public Error() {
    // default constructor
  }

  /**
   * Generate an Error from an exception and a {@link HttpStatus}.
   * 
   * @param ex
   *          the exception
   * @param status
   *          the HttpStatus
   */
  public Error(Exception ex, HttpStatus status) {
    code = status.value();
    title = status.getReasonPhrase();
    message = ex.getMessage();
  }

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
