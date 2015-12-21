package it.reply.orchestrator.exception;

import it.reply.orchestrator.dto.common.Error;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class GlobalControllerExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public Error handleException(NotFoudException e) {

    return new Error().withCode(HttpStatus.NOT_FOUND.value())
        .withTitle(HttpStatus.NOT_FOUND.getReasonPhrase()).withMessage(e.getMessage());
  }

  @ExceptionHandler
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  Error handleException(Exception e) {

    return new Error().withCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .withTitle(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()).withMessage(e.getMessage());
  }

  /**
   * Customize the response for METHOD_NOT_ALLOWED.
   */
  @Override
  protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
      HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatus status,
      WebRequest request) {

    return handleResponse(ex, headers, status);
  }

  /**
   * Customize the response when there are not handler.
   */
  @Override
  protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex,
      HttpHeaders headers, HttpStatus status, WebRequest request) {

    return handleResponse(ex, headers, status);
  }

  /**
   * 
   * Convert the exception into {@link Error} object.
   * 
   * @param ex
   * @param headers
   * @param status
   * @return
   */
  private ResponseEntity<Object> handleResponse(Exception ex, HttpHeaders headers,
      HttpStatus status) {
    Error e = new Error().withCode(status.value()).withTitle(status.getReasonPhrase())
        .withMessage(ex.getMessage());

    return new ResponseEntity<Object>(e, headers, status);

  }
}
