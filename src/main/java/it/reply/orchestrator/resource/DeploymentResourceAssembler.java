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

package it.reply.orchestrator.resource;

import it.reply.orchestrator.controller.DeploymentController;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.service.utils.MyLinkBuilder;

import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

import javax.servlet.http.HttpServletRequest;

@Component
public class DeploymentResourceAssembler
    extends ResourceAssemblerSupport<Deployment, DeploymentResource> {

  private URI uri;

  StampedLock lock = new StampedLock();

  public DeploymentResourceAssembler() {
    super(DeploymentController.class, DeploymentResource.class);
  }

  @Override
  public DeploymentResource toResource(Deployment entity) {
    return getDeploymentResource(entity);
  }

  private DeploymentResource getDeploymentResource(Deployment entity) {

    DeploymentResource resource = new DeploymentResource();
    resource.setUuid(entity.getId());
    resource.setCreationTime(entity.getCreated());
    resource.setUpdateTime(entity.getUpdated());
    resource.setStatus(entity.getStatus());
    resource.setStatusReason(entity.getStatusReason());
    resource.setCloudProviderName(entity.getCloudProviderName());

    resource.setTask(entity.getTask());

    resource.setOutputs(entity.getOutputs());

    if (entity.getCallback() != null) {
      resource.setCallback(entity.getCallback());
    }

    // Optional.ofNullable(entity.getOwner())
    // .map(owner -> owner.getOidcEntityId())
    // .ifPresent(owner -> resource.setCreatedBy(owner));

    // TODO Use ControllerLinkBuilder when
    // https://github.com/spring-projects/spring-hateoas/issues/408 will be resolved
    URI ctrlUri = null;
    if (isInHttpRequest()) {
      ctrlUri = ControllerLinkBuilder.linkTo(DeploymentController.class).toUri();
      if (uri == null) {
        long writeStamp = lock.tryWriteLock();
        if (writeStamp != 0) {
          uri = ctrlUri;
          lock.unlockWrite(writeStamp);
        }
      }
    } else {
      if (uri != null || lock.isWriteLocked()) {
        try {
          long readStamp = lock.tryReadLock(30, TimeUnit.SECONDS);
          if (readStamp != 0) {
            ctrlUri = uri;
            lock.unlockRead(readStamp);
          }
        } catch (InterruptedException ex) {
          // DO NOTHING
        }
      }
    }
    if (ctrlUri == null) {
      ctrlUri = URI.create("");
    }

    resource
        .add(MyLinkBuilder.getNewBuilder(ctrlUri).slash("deployments").slash(entity).withSelfRel());
    resource.add(MyLinkBuilder.getNewBuilder(ctrlUri)
        .slash("deployments")
        .slash(entity)
        .slash("resources")
        .withRel("resources"));
    resource.add(MyLinkBuilder.getNewBuilder(ctrlUri)
        .slash("deployments")
        .slash(entity)
        .slash("template")
        .withRel("template"));
    /////////////////////////////////////////////////////////////////////////////////
    return resource;
  }

  // Dirty hack due to Spring Hateoas being unable to generate links outside of HTTP sessions
  // https://github.com/spring-projects/spring-hateoas/issues/408
  private boolean isInHttpRequest() {
    Optional<HttpServletRequest> request =
        Optional.ofNullable(RequestContextHolder.getRequestAttributes())
            .filter(ServletRequestAttributes.class::isInstance)
            .map(ServletRequestAttributes.class::cast)
            .map(ServletRequestAttributes::getRequest);
    return request.isPresent();
  }

}
