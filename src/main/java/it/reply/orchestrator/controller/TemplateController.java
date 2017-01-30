package it.reply.orchestrator.controller;

import it.reply.orchestrator.service.TemplateService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TemplateController {

  @Autowired
  private TemplateService templateService;

  /**
   * Get the template by deploymentId.
   * 
   * @param uuid
   *          the uuid of the deployment
   * @return the template
   */
  @RequestMapping(value = "/deployments/{deploymentId}/template", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public String getOrchestrator(@PathVariable("deploymentId") String uuid) {
    return templateService.getTemplate(uuid);
  }

}
