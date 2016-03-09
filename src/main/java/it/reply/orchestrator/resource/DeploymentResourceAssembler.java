package it.reply.orchestrator.resource;

import it.reply.orchestrator.controller.DeploymentController;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.service.utils.MyLinkBuilder;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

import javax.servlet.http.HttpServletRequest;

import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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

    resource.setTask(entity.getTask());

    resource.setOutputs((Map) entity.getOutputs());

    if (entity.getCallback() != null) {
      resource.setCallback(entity.getCallback());
    }

    // TODO Use ControllerLinkBuilder when
    // https://github.com/spring-projects/spring-hateoas/issues/408 will be resolved
    URI _uri = null;
    if (isInHttpRequest()) {
      _uri = ControllerLinkBuilder.linkTo(DeploymentController.class).toUri();
      if (uri == null) {
        long writeStamp = lock.tryWriteLock();
        if (writeStamp != 0) {
          uri = _uri;
          lock.unlockWrite(writeStamp);
        }
      }
    } else {
      if (uri != null || lock.isWriteLocked()) {
        try {
          long readStamp = lock.tryReadLock(30, TimeUnit.SECONDS);
          if (readStamp != 0) {
            _uri = uri;
            lock.unlockRead(readStamp);
          }
        } catch (InterruptedException e) {
          // DO NOTHING
        }
      }
    }
    if (_uri == null) {
      _uri = URI.create("");
    }

    resource
        .add(MyLinkBuilder.getNewBuilder(_uri).slash("deployments").slash(entity).withSelfRel());
    resource.add(MyLinkBuilder.getNewBuilder(_uri).slash("deployments").slash(entity)
        .slash("resources").withRel("resources"));
    resource.add(MyLinkBuilder.getNewBuilder(_uri).slash("deployments").slash(entity)
        .slash("template").withRel("template"));
    /////////////////////////////////////////////////////////////////////////////////
    return resource;
  }

  // Dirty hack due to Spring Hateoas being unable to generate links outside of HTTP sessions
  // https://github.com/spring-projects/spring-hateoas/issues/408
  private boolean isInHttpRequest() {
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes != null && requestAttributes instanceof ServletRequestAttributes) {
      HttpServletRequest servletRequest = ((ServletRequestAttributes) requestAttributes)
          .getRequest();
      return servletRequest != null;
    }
    return false;
  }

}
