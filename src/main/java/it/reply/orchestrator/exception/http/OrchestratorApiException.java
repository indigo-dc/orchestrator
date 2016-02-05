package it.reply.orchestrator.exception.http;

import it.reply.orchestrator.exception.OrchestratorException;

/**
 * Base exception used for HTTP errors.
 * 
 * @author m.bassi
 *
 */
public class OrchestratorApiException extends OrchestratorException {

  private static final long serialVersionUID = 1L;

  public OrchestratorApiException(String message) {
    super(message);
  }

}
