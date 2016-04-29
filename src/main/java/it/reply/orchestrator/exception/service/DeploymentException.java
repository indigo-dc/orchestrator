package it.reply.orchestrator.exception.service;

import it.reply.orchestrator.exception.http.OrchestratorApiException;

/**
 * Exception thrown when error occurred during the process deployment.
 * 
 * @author m.bassi
 *
 */
public class DeploymentException extends OrchestratorApiException {

  private static final long serialVersionUID = 1L;

  public DeploymentException(String message) {
    super(message);
  }

  public DeploymentException(String message, Exception ex) {
    super(message, ex);
  }

}
