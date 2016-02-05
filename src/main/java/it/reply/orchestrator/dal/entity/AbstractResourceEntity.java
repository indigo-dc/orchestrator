package it.reply.orchestrator.dal.entity;

import it.reply.orchestrator.enums.Status;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.hateoas.Identifiable;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Version;

@MappedSuperclass
public abstract class AbstractResourceEntity implements Identifiable<String> {

  @Id
  @GeneratedValue(generator = "uuid")
  @GenericGenerator(name = "uuid", strategy = "uuid2")
  @Column(name = "uuid", unique = true)
  private String id;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 500)
  private Status status;

  @Column(name = "statusReason")
  private String statusReason;

  private Date created;
  private Date updated;

  @Version
  private Long version;

  protected AbstractResourceEntity() {
    this.id = null;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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

  public void setCreated(Date created) {
    this.created = created;
  }

  public void setUpdated(Date updated) {
    this.updated = updated;
  }

  public Date getCreated() {
    return created;
  }

  public Date getUpdated() {
    return updated;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  @PrePersist
  protected void onCreate() {
    this.created = new Date();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updated = new Date();
  }

  @Override
  public String toString() {
    return "AbstractResourceEntity [id=" + id + ", status=" + status + ", created=" + created
        + ", updated=" + updated + "]";
  }

}