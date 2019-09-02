/*
 * Copyright © 2019 I.N.F.N.
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

import feign.Feign;
import feign.Logger.Level;
import feign.RequestInterceptor;
//import feign.gson.GsonDecoder;
//import feign.gson.GsonEncoder;
import feign.slf4j.Slf4jLogger;

import it.infn.ba.deep.qcg.client.Qcg;
import it.infn.ba.deep.qcg.client.utils.QcgDecoder;
import it.infn.ba.deep.qcg.client.utils.QcgEncoder;
import it.infn.ba.deep.qcg.client.utils.QcgException;
import it.reply.orchestrator.dto.cmdb.QcgServiceData;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;


import feign.auth.BasicAuthRequestInterceptor;

import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.exception.service.DeploymentException;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;


import org.checkerframework.checker.nullness.qual.NonNull;


@Slf4j
@Service
public class QcgClientFactory {

	/**
	 * 
	 * @return
	 */
	protected String getFrameworkName() {
	    return "Qcg";
	}

	/**
	 * 
	 * @param qcgEndpoint
	 * @param authInterceptor
	 * @return
	 */
	public Qcg build(String qcgEndpoint, RequestInterceptor authInterceptor) {
		LOG.info("Generating Qcg client with endpoint {}", qcgEndpoint);
    
		return Feign
		    .builder()
		    .encoder(new QcgEncoder())
		    .decoder(new QcgDecoder())
		    .logger(new Slf4jLogger(Qcg.class))
		    .logLevel(Level.FULL)
		    .errorDecoder(
		        (methodKey, response) -> new QcgException(response.status(), response.reason()))
		    .requestInterceptor(authInterceptor)
		    .requestInterceptor(template -> {
		      template.header(HttpHeaders.ACCEPT, "application/json");
		      template.header(HttpHeaders.CONTENT_TYPE, "application/json");
		     })
		    .target(Qcg.class, qcgEndpoint);
	}

	/**
	 * 
	 * @param cloudProviderEndpoint
	 * @param accessToken
	 * @return
	 */
	public Qcg build(CloudProviderEndpoint cloudProviderEndpoint, String accessToken) {
	  final RequestInterceptor requestInterceptor;
	  if (cloudProviderEndpoint.getUsername() != null
	      || cloudProviderEndpoint.getPassword() != null) {
	    Objects.requireNonNull(cloudProviderEndpoint.getUsername(), "Username must be provided");
	    Objects.requireNonNull(cloudProviderEndpoint.getPassword(), "Password must be provided");
	    requestInterceptor = new BasicAuthRequestInterceptor(cloudProviderEndpoint.getUsername(),
	        cloudProviderEndpoint.getPassword());
	  } else {
	    Objects.requireNonNull(accessToken, "Access Token must not be null");
	    requestInterceptor = requestTemplate -> {
	    	//TODO  remove test token ad resume standard implementation
	      requestTemplate
	      	.header(HttpHeaders.AUTHORIZATION, "Bearer " +  "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6ImRlZmF1bHQta2lkIn0.eyJzdWIiOiJkZWZhdWx0LXVzZSIsImlzcyI6ImRlZmF1bHQtaXNzdWVyIiwiaWF0IjoxNTYxNTQ0MTA3LCJleHAiOjE1NzAxODQxMDcsImF0dHJzIjp7ImxvY2FsX3VzZXIiOiJ1bmtub3duIn0sInBlcm1zIjp7Im93bmVyIjpbInN0YXR1cyIsImNhbmNlbCJdLCJzeXN0ZW0iOlsic3VibWl0IiwicmVzb3VyY2VzIiwiYWRtaW4iLCJzY2hlbWEiXSwiYWRtaW4iOlsidXBkYXRlIiwic3RhdHVzIiwiY2FuY2VsIl19fQ.jW3k5jOALPOlxWf7FFjYds0Fs4-bfTdBvBYJxvcgowG-IQbq3KWEX5mrT-8-wc5SzZ9Bl7XDr-xx45bxcc0dziGtf05X5MrpxKnQu5q2b9_7w75zM4ijOXnNHLupPSx--rS2Z5kctN77oIe3_2S0E5L_ewClKWltO6k7SQIZBmDWPk4TFfbIehYXaOsHsA0oR7hdRHKlhMN-hfjZjcmik0jwJR0_oIf7ZS-psVOOCXJ1HK-2UWxj4Hj-P0-FCRAKldMAfgChiwcAWJ-Fdp5rWCSg9ZLdI918-gxNvfofrHevKu4XXhlyYsAzm2PNaJSH0TMV3T5oSNBWa0iw5r-flA");
	      //    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
	    };
	  }
	  return build(cloudProviderEndpoint.getCpEndpoint(), requestInterceptor);
	}

    /**
     * Get the framework properties.
     *
     * @param deploymentMessage
     *     the deploymentMessage
     * @return the framework properties
     */
    @NonNull
    public QcgServiceData getFrameworkProperties(DeploymentMessage deploymentMessage) {
      String computeServiceId = deploymentMessage.getChosenCloudProviderEndpoint()
          .getCpComputeServiceId();
      Map<String, CloudService> cmdbProviderServices = deploymentMessage
          .getCloudProvidersOrderedIterator().current().getCmdbProviderServices();
      return (QcgServiceData) Optional.ofNullable(cmdbProviderServices.get(computeServiceId))
          .map(CloudService::getData)
          .orElseThrow(() -> new DeploymentException(String
              .format("No %s instance available for cloud provider service %s", getFrameworkName(),
                  computeServiceId)));
    }
 
}
