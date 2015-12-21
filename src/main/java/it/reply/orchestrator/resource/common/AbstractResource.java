package it.reply.orchestrator.resource.common;

import it.reply.orchestrator.enums.Status;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class AbstractResource extends ResourceSupport {

  private String uuid;
  private String creationTime;
  private String updateTime;

  private Status status;

  public AbstractResource() {
  }

  public AbstractResource(String uuid, Date creationTime, Status status) {
    super();
    this.uuid = uuid;
    setCreationTime(creationTime);
    this.status = status;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(Date creationTime) {
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
    df.setTimeZone(tz);
    this.creationTime = df.format(creationTime);
  }

  public String getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(String updateTime) {
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
    df.setTimeZone(tz);
    this.updateTime = df.format(updateTime);
  }

  public void setCreationTime(String creationTime) {
    this.creationTime = creationTime;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

}
