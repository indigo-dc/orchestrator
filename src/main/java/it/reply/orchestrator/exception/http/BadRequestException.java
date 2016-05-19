package it.reply.orchestrator.exception.http;

/**
 * Exception thrown when the client request is bad (i.e. wrong information, wrong resource status,
 * etc)
 * 
 * @author m.bassi
 *
 */
public class BadRequestException extends OrchestratorApiException {

  private static final long serialVersionUID = 1L;

  public BadRequestException(String message) {
    super(message);
  }

}
