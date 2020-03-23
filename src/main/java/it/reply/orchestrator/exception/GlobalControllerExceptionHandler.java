/*
 * Copyright © 2015-2020 Santer Reply S.p.A.
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
import it.reply.orchestrator.exception.http.OrchestratorApiException;
import it.reply.orchestrator.utils.CommonUtils;

import java.sql.SQLTransientException;

import lombok.extern.slf4j.Slf4j;

import org.apache.ibatis.exceptions.PersistenceException;
import org.flowable.engine.common.api.FlowableOptimisticLockingException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.error.AbstractOAuth2SecurityExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalControllerExceptionHandler extends ResponseEntityExceptionHandler {

  /**
   * {@link OrchestratorApiException} handler.
   *
   * @param ex
   *          the exception
   * @return a {@code ResponseEntity} instance
   */
  @ExceptionHandler
  public ResponseEntity<Object> handleException(OrchestratorApiException ex, WebRequest request) {
    return handleExceptionInternal(ex, ex.getHttpStatus(), request);
  }

  /**
   * OAuth2Exception exception handler. This handler will just re-throw the exception and to let
   * the {@link AbstractOAuth2SecurityExceptionHandler} handle it.
   *
   * @param ex
   *          the exception
   * @return a {@code ResponseEntity} instance
   */
  @ExceptionHandler
  public ResponseEntity<Object> handleException(OAuth2Exception ex, WebRequest request) {
    // NOTE: is not a requirement to re-throw the same exception,
    // whichever unchecked exception would fulfill the scope
    throw ex;
  }

  /**
   * {@link TransientDataAccessException} and {@link FlowableOptimisticLockingException} handler.
   *
   * @param ex
   *     the exception
   * @return a {@code ResponseEntity} instance
   */
  @ExceptionHandler({TransientDataAccessException.class, FlowableOptimisticLockingException.class})
  public ResponseEntity<Object> handleTransientDataException(Exception ex, WebRequest request) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.RETRY_AFTER, "0");
    Error bodyToWrite = Error
        .builder()
        .message("The request couldn't be fulfilled because of a concurrent update."
            + " Please retry later")
        .exception(ex)
        .status(HttpStatus.CONFLICT)
        .build();
    return handleExceptionInternal(ex, bodyToWrite, headers, HttpStatus.CONFLICT, request);
  }

  /**
   * {@link PersistenceException} handler.
   *
   * @param ex
   *     the exception
   * @return a {@code ResponseEntity} instance
   */
  @ExceptionHandler
  public ResponseEntity<Object> handleIbatisPersistenceException(PersistenceException ex,
      WebRequest request) {
    if (ex.getCause() instanceof SQLTransientException) {
      return handleTransientDataException(ex, request);
    } else {
      return handleGenericException(ex, request);
    }
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
    if (status != HttpStatus.NOT_FOUND && status != HttpStatus.METHOD_NOT_ALLOWED) {
      LOG.error("Error handling request {}", request, ex);
    }
    final HttpHeaders headersToWrite = CommonUtils.notNullOrDefaultValue(headers, HttpHeaders::new);
    final Object bodyToWrite = CommonUtils.notNullOrDefaultValue(body,
        () -> CommonUtils.checkNotNull(Error.builder().exception(ex).status(status).build()));
    return super.handleExceptionInternal(ex, bodyToWrite, headersToWrite, status, request);
  }

  protected ResponseEntity<Object> handleExceptionInternal(Exception ex, HttpStatus status,
      WebRequest request) {
    return handleExceptionInternal(ex, null, null, status, request);
  }

  protected ResponseEntity<Object> handleExceptionInternal(Exception ex, HttpStatus status,
      HttpHeaders headers, WebRequest request) {
    return handleExceptionInternal(ex, null, headers, status, request);
  }

}
