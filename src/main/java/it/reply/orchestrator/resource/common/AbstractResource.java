package it.reply.orchestrator.resource.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.reply.orchestrator.enums.Status;

import org.springframework.hateoas.ResourceSupport;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

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
    if (creationTime == null) {
      return;
    }

    this.creationTime = convertDate(creationTime);
  }

  public String getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(Date updateTime) {
    if (updateTime == null) {
      return;
    }

    this.updateTime = convertDate(updateTime);
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

  private String convertDate(Date date) {
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
    df.setTimeZone(tz);
    return df.format(date);
  }
}
