/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
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

package it.reply.orchestrator.dto.common;

import it.reply.orchestrator.utils.CommonUtils;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.Optional;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Error implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  @Nullable
  private Integer code;

  @Nullable
  private String title;

  @Nullable
  private String message;

  /**
   * Generate an Error from an exception and a {@link HttpStatus}.
   * 
   * @param ex
   *          the exception
   * @param status
   *          the HttpStatus
   */
  @Builder
  protected Error(@Nullable Exception exception, @Nullable String message,
      @Nullable HttpStatus status) {
    Optional.ofNullable(status).map(CommonUtils::checkNotNull).ifPresent(safeStatus -> {
      code = safeStatus.value();
      title = safeStatus.getReasonPhrase();
    });
    this.message = Optional
        .ofNullable(message)
        .orElseGet(() -> Optional
            .ofNullable(exception)
            .map(Exception::getMessage)
            .orElse(null));
  }

}
