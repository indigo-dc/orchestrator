/*
 * Copyright © 2015-2019 Santer Reply S.p.A.
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

package it.reply.orchestrator.service.deployment.providers.factory;

import com.google.gson.annotations.SerializedName;

import feign.Feign;
import feign.Logger.Level;
import feign.RequestInterceptor;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.slf4j.Slf4jLogger;

import it.reply.orchestrator.dto.cmdb.MarathonService;
import it.reply.orchestrator.service.deployment.providers.CredentialProviderService;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import mesosphere.client.common.ModelUtils;
import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonException;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MarathonClientFactory extends
    MesosFrameworkClientFactory<MarathonService, Marathon> {

  public MarathonClientFactory(CredentialProviderService credProvServ) {
    super(credProvServ);
  }

  private static class DeserializingMarathonErrorDecoder implements ErrorDecoder {

    @Data
    @AllArgsConstructor
    private static class ErrorResponse {

      @Nullable
      @SerializedName("message")
      private String message;

      @NonNull
      @NotNull
      @SerializedName("details")
      private List<ErrorResponseDetails> details = new ArrayList<>();

      @Override
      public String toString() {
        return ModelUtils.toString(this);
      }
    }

    @Data
    @AllArgsConstructor
    private static class ErrorResponseDetails {

      @Nullable
      @SerializedName("path")
      private String path;

      @NonNull
      @NotNull
      @SerializedName("errors")
      private List<String> errors = new ArrayList<>();

      @Override
      public String toString() {
        return ModelUtils.toString(this);
      }
    }

    @Override
    public Exception decode(String methodKey, Response response) {

      String message = Optional
          .ofNullable(response.body())
          .map(body -> {
            Reader reader = null;
            try {
              reader = body.asReader();
              return ModelUtils.GSON.fromJson(reader, ErrorResponse.class);
            } catch (IOException | RuntimeException ex) {
              LOG.trace("Error deserializing error response", ex);
              return null;
            } finally {
              Util.ensureClosed(reader);
            }
          })
          .filter(errorResponse -> errorResponse.getMessage() != null)
          .map(errorResponse -> errorResponse.toString())
          .orElseGet(response::reason);
      return new MarathonException(response.status(), message);
    }
  }

  @Override
  public Marathon build(String marathonEndpoint, RequestInterceptor authInterceptor) {
    LOG.info("Generating Marathon client with endpoint {}", marathonEndpoint);

    return Feign
        .builder()
        .encoder(new GsonEncoder(ModelUtils.GSON))
        .decoder(new GsonDecoder(ModelUtils.GSON))
        .logger(new Slf4jLogger(Marathon.class))
        .logLevel(Level.FULL)
        .errorDecoder(new DeserializingMarathonErrorDecoder())
        .requestInterceptor(authInterceptor)
        .requestInterceptor(template -> {
          template.header(HttpHeaders.ACCEPT, "application/json");
          template.header(HttpHeaders.CONTENT_TYPE, "application/json");
        })
        .target(Marathon.class, marathonEndpoint);
  }
}
