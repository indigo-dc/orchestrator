package it.reply.orchestrator.exception;

import it.reply.orchestrator.dto.common.Error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class GlobalControllerExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public @ResponseBody
  Error handleException(NotFoudException e) {

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
}
