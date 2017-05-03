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

package it.reply.orchestrator.exception;

import it.reply.orchestrator.dto.common.Error;
import it.reply.orchestrator.exception.http.BadRequestException;
import it.reply.orchestrator.exception.http.ConflictException;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.exception.service.ToscaException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.error.AbstractOAuth2SecurityExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Optional;

@ControllerAdvice
public class GlobalControllerExceptionHandler extends ResponseEntityExceptionHandler {

  /**
   * Not Found exception handler.
   * 
   * @param ex
   *          the exception
   * @return a {@code ResponseEntity} instance
   */
  @ExceptionHandler
  public ResponseEntity<Object> handleException(NotFoundException ex, WebRequest request) {
    return handleExceptionInternal(ex, HttpStatus.NOT_FOUND, request);
  }

  /**
   * Conflict exception handler.
   * 
   * @param ex
   *          the exception
   * @return a {@code ResponseEntity} instance
   */
  @ExceptionHandler
  public ResponseEntity<Object> handleException(ConflictException ex, WebRequest request) {
    return handleExceptionInternal(ex, HttpStatus.CONFLICT, request);
  }

  /**
   * Bad Request exception handler.
   * 
   * @param ex
   *          the exception
   * @return a {@code ResponseEntity} instance
   */
  @ExceptionHandler
  public ResponseEntity<Object> handleException(BadRequestException ex, WebRequest request) {
    return handleExceptionInternal(ex, HttpStatus.BAD_REQUEST, request);
  }

  /**
   * Invalid TOSCA exception handler.
   * 
   * @param ex
   *          the exception
   * @return a {@code ResponseEntity} instance
   */
  @ExceptionHandler
  public ResponseEntity<Object> handleException(ToscaException ex, WebRequest request) {
    return handleExceptionInternal(ex, HttpStatus.BAD_REQUEST, request);
  }

  /**
   * OAuth2Exception exception handler. This handler will just re-throw the exception and will let
   * the {@link AbstractOAuth2SecurityExceptionHandler} to handle it.
   * 
   * @param ex
   *          the exception
   * @return a {@code ResponseEntity} instance
   */
  @ExceptionHandler
  public ResponseEntity<Object> handleException(OAuth2Exception ex, WebRequest request) {
    // NOTE: is not a requirement to re-thow the same exception,
    // whichever unchecked exception would fulfill the scope
    throw ex;
  }

  /**
   * Server Error exception handler.
   * 
   * @param ex
   *          the exception
   * @return a {@code ResponseEntity} instance
   */
  @ExceptionHandler
  public ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
    if (ex.getCause() instanceof OAuth2Exception) {
      return this.handleException((OAuth2Exception) ex.getCause(), request);
    } else {
      return handleExceptionInternal(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
      HttpHeaders headers, HttpStatus status, WebRequest request) {
    final HttpHeaders headersToWrite = Optional.ofNullable(headers).orElseGet(HttpHeaders::new);
    final Object bodyToWrite = Optional.ofNullable(body).orElse(new Error(ex, status));
    return super.handleExceptionInternal(ex, bodyToWrite, headersToWrite, status, request);
  }

  protected ResponseEntity<Object> handleExceptionInternal(Exception ex, HttpStatus status,
      WebRequest request) {
    return handleExceptionInternal(ex, null, null, status, request);
  }

}
