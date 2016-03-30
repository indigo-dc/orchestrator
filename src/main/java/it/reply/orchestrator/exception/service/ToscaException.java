package it.reply.orchestrator.exception.service;

import it.reply.orchestrator.exception.OrchestratorException;

/**
 * Exception thrown when error occurred during the tosca parsing.
 * 
 * @author m.bassi
 *
 */
public class ToscaException extends OrchestratorException {

  private static final long serialVersionUID = 1L;

  public ToscaException(String message) {
    super(message);
  }

  public ToscaException(Throwable cause) {
    super(cause);
  }

  public ToscaException(String message, Throwable cause) {
    super(message, cause);
  }

}
