package it.reply.orchestrator.exception.service;


public class KubernetesException extends RuntimeException {

  /**
   * 
   */
  private static final long serialVersionUID = 908837224074715831L;
  
  private int status;
  private String message;
  
  public KubernetesException(String message) {
    super(message);
  }

  public KubernetesException(String message, Throwable cause) {
    super(message, cause);
  }
  
  protected KubernetesException(int status, String message) {
    this.status = status;
    this.message = message;
  }
  
  /**
   * Gets the status code of the failure, such as 404.
   *
   * @return status code
   */
  public int getStatus() {
      return status;
  }

  @Override
  public String getMessage() {
      return message + " (status: " + status + ")";
  }


}
