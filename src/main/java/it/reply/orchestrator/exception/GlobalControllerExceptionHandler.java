package it.reply.orchestrator.exception;

import it.reply.orchestrator.dto.common.Error;
import it.reply.orchestrator.exception.http.NotFoundException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Provide a centralized exception handling
 * 
 * @author m.bassi
 *
 */
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
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public Error handleException(NotFoundException ex) {

    return new Error().withCode(HttpStatus.NOT_FOUND.value())
        .withTitle(HttpStatus.NOT_FOUND.getReasonPhrase()).withMessage(ex.getMessage());
  }

  /**
   * Server Error exception handler.
   * 
   * @param ex
   *          the exception
   * @return a {@code ResponseEntity} instance
   */
  @ExceptionHandler
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  Error handleException(Exception ex) {

    return new Error().withCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .withTitle(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()).withMessage(ex.getMessage());
  }

  protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
      HttpHeaders headers, HttpStatus status, WebRequest request) {

    return handleResponse(ex, headers, status);
  }

  /**
   * METHOD_NOT_ALLOWED exception handler.
   * 
   * @param ex
   *          the exception
   * @return a {@code ResponseEntity} instance
   */
  @Override
  protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
      HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatus status,
      WebRequest request) {

    return handleResponse(ex, headers, status);
  }

  /**
   * UNSUPPORTED_MEDIA_TYPE exception handler.
   * 
   * @param ex
   *          {@code HttpMediaTypeNotSupportedException}
   * @return a {@code ResponseEntity} instance
   */
  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatus status,
      WebRequest request) {

    return handleResponse(ex, headers, status);
  }

  /**
   * Customize the response when there are not handler.
   * 
   * @param ex
   *          the exception
   * @param headers
   *          {@code HttpHeaders} instance
   * @param status
   *          {@code HttpStatus} instance
   * @param request
   *          {@code WebRequest} instance
   * @return a {@code ResponseEntity} instance
   */
  @Override
  protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex,
      HttpHeaders headers, HttpStatus status, WebRequest request) {

    return handleResponse(ex, headers, status);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
      HttpHeaders headers, HttpStatus status, WebRequest request) {
    return handleResponse(ex, headers, status);
  }

  /**
   * Convert the exception into {@link Error} object.
   * 
   * @param ex
   *          the exception to be handled
   * @param headers
   *          {@code HttpHeaders} instance
   * @param status
   *          {@code HttpStatus} instance
   * @return the error response
   */
  private ResponseEntity<Object> handleResponse(Exception ex, HttpHeaders headers,
      HttpStatus status) {
    Error error = new Error().withCode(status.value()).withTitle(status.getReasonPhrase())
        .withMessage(ex.getMessage());

    return new ResponseEntity<Object>(error, headers, status);

  }
}
