package it.reply.orchestrator.controller;

import it.reply.orchestrator.service.TemplateService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/deployments/{deploymentId}/template")
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
  // @ResponseStatus(HttpStatus.OK)
  @RequestMapping(method = RequestMethod.GET)
  public ResponseEntity<String> getOrchestrator(@PathVariable("deploymentId") String uuid) {
    String template = templateService.getTemplate(uuid);
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(MediaType.TEXT_PLAIN);
    return new ResponseEntity<String>(template, responseHeaders, HttpStatus.OK);
  }

}
