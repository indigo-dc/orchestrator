package it.reply.orchestrator.exception.http;

/**
 * Exception thrown when a resource doesn't exist.
 * 
 * @author m.bassi
 *
 */
public class NotFoundException extends OrchestratorApiException {

  private static final long serialVersionUID = 1L;

  public NotFoundException(String message) {
    super(message);
  }

}
