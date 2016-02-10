package it.reply.orchestrator.validator;

import java.net.URI;

import org.apache.logging.log4j.util.Strings;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import it.reply.orchestrator.dto.request.DeploymentRequest;

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

    DeploymentRequest dr = (DeploymentRequest) target;
    String callbackURL = dr.getCallback();
    if (callbackURL != null) {
      if (Strings.isBlank(callbackURL)) {
        errors.rejectValue("callback", "callback.blank", "Callback URL is blank");
      } else {
        try {
          URI.create(callbackURL).toURL();
        } catch (Exception e) {
          errors.rejectValue("callback", "callback.malformed", "Callback URL is malformed");
        }
      }
    }
  }
}
