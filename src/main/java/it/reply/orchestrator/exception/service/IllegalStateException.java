package it.reply.orchestrator.exception.service;

import it.reply.orchestrator.exception.http.OrchestratorApiException;

/**
 * Exception thrown when the current state.
 * 
 * @author a.brigandi
 *
 */
public class IllegalStateException extends OrchestratorApiException {

  private static final long serialVersionUID = 1L;

  public IllegalStateException(String message) {
    super(message);
  }

}
