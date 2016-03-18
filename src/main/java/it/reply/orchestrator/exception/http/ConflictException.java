package it.reply.orchestrator.exception.http;

/**
 * Exception thrown when the request could not be completed due to a conflict with the current state
 * of the resource.
 * 
 * @author m.bassi
 *
 */
public class ConflictException extends OrchestratorApiException {

  private static final long serialVersionUID = 1L;

  public ConflictException(String message) {
    super(message);
  }

}
