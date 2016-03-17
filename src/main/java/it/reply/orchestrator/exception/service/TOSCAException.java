package it.reply.orchestrator.exception.service;

import it.reply.orchestrator.exception.OrchestratorException;

/**
 * Exception thrown when error occurred during the tosca parsing.
 * 
 * @author m.bassi
 *
 */
public class TOSCAException extends OrchestratorException {

  private static final long serialVersionUID = 1L;

  public TOSCAException(String message) {
    super(message);
  }

  public TOSCAException(Throwable cause) {
    super(cause);
  }

  public TOSCAException(String message, Throwable cause) {
    super(message, cause);
  }

}
