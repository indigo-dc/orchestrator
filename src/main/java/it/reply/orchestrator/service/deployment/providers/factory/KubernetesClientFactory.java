package it.reply.orchestrator.service.deployment.providers.factory;

import feign.Feign;
import feign.RequestInterceptor;
import feign.Logger.Level;
import feign.slf4j.Slf4jLogger;

import it.infn.ba.deep.qcg.client.Qcg;
import it.infn.ba.deep.qcg.client.utils.QcgDecoder;
import it.infn.ba.deep.qcg.client.utils.QcgEncoder;
import it.infn.ba.deep.qcg.client.utils.QcgException;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.cmdb.KubernetesService;

import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import io.kubernetes.client.openapi.ApiClient;

@Service
@Slf4j
public class KubernetesClientFactory {

  /**
   * Build a Kubernetes Api client object.
   * @param cloudProviderEndpoint the service endpoint.
   * @param accessToken the input accesstoken.
   * @return the Kubernetes Api client object.
   */
  public ApiClient build(CloudProviderEndpoint cloudProviderEndpoint, String accessToken) {
    final RequestInterceptor requestInterceptor;
    Objects.requireNonNull(accessToken, "Access Token must not be null");
      requestInterceptor = requestTemplate ->
        requestTemplate.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

    return build(cloudProviderEndpoint.getCpEndpoint(), requestInterceptor);
  }

  /**
   * Build a Qcg client object.
   * @param qcgEndpoint the input qcg service endpoint.
   * @param authInterceptor the input request interceptor.
   * @return the Qcg client object.
   */
  public ApiClient build(String apiClientEndpoint, RequestInterceptor authInterceptor) {
    LOG.info("Generating Qcg client with endpoint {}", apiClientEndpoint);

    //TODO * new ApiEncoder()).decoder(new QcgDecoder()
    return Feign.builder().encoder(null/* * */)
        .logger(new Slf4jLogger(ApiClient.class))
        .logLevel(Level.FULL)
        .errorDecoder((methodKey, response) -> new QcgException(response.status(),
            response.reason()))
        .requestInterceptor(authInterceptor).requestInterceptor(template -> {
          template.header(HttpHeaders.ACCEPT, "application/json");
          template.header(HttpHeaders.CONTENT_TYPE, "application/json");
        }).target(ApiClient.class, apiClientEndpoint);
  }

}
