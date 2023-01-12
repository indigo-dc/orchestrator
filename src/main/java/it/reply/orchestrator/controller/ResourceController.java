/*
 * Copyright © 2015-2021 I.N.F.N.
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

package it.reply.orchestrator.controller;

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dto.request.ActionRequest;
import it.reply.orchestrator.exception.http.BadRequestException;
import it.reply.orchestrator.resource.BaseResource;
import it.reply.orchestrator.resource.BaseResourceAssembler;
import it.reply.orchestrator.service.ResourceService;
import it.reply.orchestrator.service.security.OAuth2TokenService;

import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/deployments/{deploymentId}")
public class ResourceController {

  private static final String OFFLINE_ACCESS_REQUIRED_CONDITION =
      "#oauth2.throwOnError(#oauth2.hasAnyScope('offline_access'))";

  @Autowired
  private ResourceService resourceService;

  @Autowired
  BaseResourceAssembler baseResourceAssembler;

  @Autowired
  private OAuth2TokenService oauth2Tokenservice;

  @Autowired
  private OidcProperties oidcProperties;

  /**
   * Get all resources associated to a deployment.
   *
   *
   * @param deploymentId The deployment Id
   * @param type The resource tosca type to be used to filter the resource list
   * @param pageable The pageable object
   * @param pagedAssembler The PagedResourceAssembler object
   *
   * @return the paged resources
   */
  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/resources", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public PagedResources<BaseResource> getResources(
      @PathVariable("deploymentId") String deploymentId,
      @RequestParam("type") Optional<String> type,
      @PageableDefault(sort = "createdAt",
          direction = Direction.DESC) Pageable pageable,
      PagedResourcesAssembler<Resource> pagedAssembler) {

    String stype = type.orElse("");
    Page<Resource> page = null;

    if (stype.isEmpty()) {
      page = resourceService.getResources(deploymentId, pageable);
    } else {
      List<Resource> resources = resourceService.getResources(deploymentId, stype);
      page = new PageImpl<Resource>(resources, pageable, resources.size());

    }
    return pagedAssembler.toResource(page, baseResourceAssembler);
  }

  /**
   * Get resources associated to a deployment filtered by type.
   *
   *
   * @param deploymentId The deployment Id
   * @param type The node type to filter the resources
   *
   * @return the filtered resources
  */
  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/resources/filter", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public List<BaseResource> getFilteredResources(
      @PathVariable("deploymentId") String deploymentId,
      @RequestParam("type") String type) {

    if (type.isEmpty()) {
      throw new BadRequestException("Required String parameter \'type\' cannot be empty");
    }

    List<Resource> resources = resourceService.getResources(deploymentId, type);

    return baseResourceAssembler.toResources(resources);
  }

  /**
   * Get resource by id.
   *
   * @param deploymentId The deployment Id
   * @param resourceId The resource Id
   *
   * @return the BaseResource object
   */
  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/resources/{resourceId}", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public BaseResource getResource(@PathVariable("deploymentId") String deploymentId,
      @PathVariable("resourceId") String resourceId) {

    Resource resource = resourceService.getResource(resourceId, deploymentId);
    return baseResourceAssembler.toResource(resource);
  }


  /**
   * Perform action on the deployment resource.
   *
   * @param deploymentId The deployment Id
   * @param resourceId The resource Id
   */
  @ResponseStatus(HttpStatus.ACCEPTED)
  @RequestMapping(value = "/resources/{resourceId}/actions", method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(OFFLINE_ACCESS_REQUIRED_CONDITION)
  public void performAction(@PathVariable("deploymentId") String deploymentId,
      @PathVariable("resourceId") String resourceId,
      @Valid @RequestBody ActionRequest request) {
    OidcEntity owner = null;
    OidcTokenId requestedWithToken = null;
    if (oidcProperties.isEnabled()) {
      owner = oauth2Tokenservice.getOrGenerateOidcEntityFromCurrentAuth();
      requestedWithToken = oauth2Tokenservice.exchangeCurrentAccessToken();
    }
    resourceService.doAction(deploymentId, resourceId, request.getType(), requestedWithToken);

  }

}
