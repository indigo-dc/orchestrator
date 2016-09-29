package it.reply.orchestrator.dal.entity;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.hateoas.Identifiable;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Version;

@MappedSuperclass
public abstract class AbstractResourceEntity implements Identifiable<String>, Serializable {

  private static final long serialVersionUID = 3797345592958668261L;

  public static final String ID_COLUMN_NAME = "uuid";
  public static final String CREATED_COLUMN_NAME = "created";

  @Id
  @GeneratedValue(generator = "uuid")
  @GenericGenerator(name = "uuid", strategy = "uuid2")
  @Column(name = ID_COLUMN_NAME, unique = true)
  private String id;

  @Column(name = CREATED_COLUMN_NAME)
  private Date created;
  private Date updated;

  @Version
  private Long version;

  protected AbstractResourceEntity() {
    this.id = null;
  }

  @Override
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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
    return "AbstractResourceEntity [id=" + id + ", created=" + created + ", updated=" + updated
        + "]";
  }

}