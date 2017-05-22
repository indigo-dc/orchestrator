/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.reply.orchestrator.validator;

import com.google.common.base.Strings;

import it.reply.orchestrator.dto.request.DeploymentRequest;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.net.URI;

public class DeploymentRequestValidator implements Validator {

  @Override
  public boolean supports(Class<?> clazz) {
    return DeploymentRequest.class.isAssignableFrom(clazz);
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
