package it.reply.orchestrator.validator;

import com.google.common.base.Strings;

import it.reply.orchestrator.dto.request.DeploymentRequest;

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
      if (Strings.nullToEmpty(callbackUrl).trim().isEmpty()) {
        errors.rejectValue("callback", "callback.blank", "Callback URL is blank");
      } else {
        try {
          URI.create(callbackUrl).toURL();
        } catch (Exception ex) {
          errors.rejectValue("callback", "callback.malformed", "Callback URL is malformed");
        }
      }
    }
  }
}
