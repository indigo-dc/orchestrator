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
  private String statusReason;

  public AbstractResource() {
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

  /**
   * Set the creation time converting from Date.
   * 
   * @param creationTime
   *          {@code Date} of creation
   */
  public void setCreationTime(Date creationTime) {
    if (creationTime == null) {
      return;
    }

    this.creationTime = convertDate(creationTime);
  }

  public void setCreationTime(String creationTime) {
    this.creationTime = creationTime;
  }

  public String getUpdateTime() {
    return updateTime;
  }

  /**
   * Set the update time converting from Date.
   * 
   * @param updateTime
   *          {@code Date} of update
   */
  public void setUpdateTime(Date updateTime) {
    if (updateTime == null) {
      return;
    }

    this.updateTime = convertDate(updateTime);
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public String getStatusReason() {
    return statusReason;
  }

  public void setStatusReason(String statusReason) {
    this.statusReason = statusReason;
  }

  private String convertDate(Date date) {
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
    df.setTimeZone(tz);
    return df.format(date);
  }
}
