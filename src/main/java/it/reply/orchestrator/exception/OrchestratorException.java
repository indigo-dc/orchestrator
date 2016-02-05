package it.reply.orchestrator.exception;

/**
 * Base orchestrator exception
 * 
 * @author m.bassi
 *
 */
public class OrchestratorException extends RuntimeException {

  private static final long serialVersionUID = -8879317682949851699L;

  public OrchestratorException() {
    super();
  }

  public OrchestratorException(String message) {
    super(message);
  }

  public OrchestratorException(Throwable cause) {
    super(cause);
  }

  public OrchestratorException(String message, Throwable cause) {
    super(message, cause);
  }

}
