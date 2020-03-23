/*
 * Copyright © 2015-2020 Santer Reply S.p.A.
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

package it.reply.orchestrator.exception.http;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when the client request is bad (i.e. wrong information, wrong resource status,
 * etc)
 *
 * @author m.bassi
 *
 */
public class BadRequestException extends OrchestratorApiException {

  private static final long serialVersionUID = 1L;

  public BadRequestException(String message) {
    super(HttpStatus.BAD_REQUEST, message);
  }

  public BadRequestException(String message, Throwable ex) {
    super(HttpStatus.BAD_REQUEST, message, ex);
  }
}
