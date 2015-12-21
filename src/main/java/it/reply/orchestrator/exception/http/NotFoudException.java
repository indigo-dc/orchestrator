package it.reply.orchestrator.exception.http;

/**
 * Exception thrown when a resource doesn't exist.
 * 
 * @author m.bassi
 *
 */
public class NotFoudException extends OrchestratorApiException {

  private static final long serialVersionUID = 1L;

  public NotFoudException(String message) {
    super(message);
  }

}
