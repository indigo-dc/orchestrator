package it.reply.orchestrator.service;

public interface TemplateService {

  /**
   * Returns the template by deploymentId.
   *
   * @param uuid
   *          the uuid of the deployment
   * @return the template
   */
  public String getTemplate(String uuid);

}
