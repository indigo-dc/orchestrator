package it.reply.orchestrator.exception;

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

import it.reply.orchestrator.dto.common.Error;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.error.DefaultOAuth2ExceptionRenderer;
import org.springframework.web.context.request.ServletWebRequest;

public class CustomOAuth2ExceptionRenderer extends DefaultOAuth2ExceptionRenderer {

  @Override
  public void handleHttpEntityResponse(HttpEntity<?> responseEntity, ServletWebRequest webRequest)
      throws Exception {
    final HttpEntity<?> responseEntityToWrite;
    if (responseEntity.getBody() instanceof OAuth2Exception) {
      // write an Error as response body instead of a JSON serialized OAuth2Exception
      OAuth2Exception oauth2Exception = (OAuth2Exception) responseEntity.getBody();
      HttpStatus status = HttpStatus.valueOf(oauth2Exception.getHttpErrorCode());
      responseEntityToWrite = new ResponseEntity<>(new Error(oauth2Exception, status),
          responseEntity.getHeaders(), status);
    } else {
      responseEntityToWrite = responseEntity;
    }
    super.handleHttpEntityResponse(responseEntityToWrite, webRequest);
  }

}
