package it.reply.orchestrator.exception.http;

/**
 * Exception thrown when a resource doesn't exist.
 * 
 * @author m.bassi
 *
 */
public class BadRequestException extends OrchestratorApiException {

  private static final long serialVersionUID = 1L;

  public BadRequestException(String message) {
    super(message);
  }

  public BadRequestException(String message, Throwable ex) {
    super(message, ex);
  }

}
