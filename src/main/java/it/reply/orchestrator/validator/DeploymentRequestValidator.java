package it.reply.orchestrator.validator;

import it.reply.orchestrator.dto.request.DeploymentRequest;

import org.apache.logging.log4j.util.Strings;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.net.URI;

public class DeploymentRequestValidator implements Validator {

  @Override
  public boolean supports(Class<?> clazz) {
    return DeploymentRequest.class.equals(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    if (target == null) {
      errors.reject("request.null", "Deployment request is null");
      return;
    }

    DeploymentRequest deploymentRequest = (DeploymentRequest) target;
    String callbackUrl = deploymentRequest.getCallback();
    if (callbackUrl != null) {
      if (Strings.isBlank(callbackUrl)) {
        errors.rejectValue("callback", "callback.blank", "Callback URL is blank");
      } else {
        try {
          URI.create(callbackUrl).toURL();
        } catch (Exception e) {
          errors.rejectValue("callback", "callback.malformed", "Callback URL is malformed");
        }
      }
    }
  }
}
