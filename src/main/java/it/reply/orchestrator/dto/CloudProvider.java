package it.reply.orchestrator.dto;

import it.reply.orchestrator.dto.cmdb.Image;
import it.reply.orchestrator.dto.cmdb.Provider;
import it.reply.orchestrator.dto.cmdb.Service;
import it.reply.orchestrator.dto.cmdb.Type;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal CloudProvider representation (to contain joined data about the provider - from
 * SLAM/CMDB/etc)
 * 
 * @author l.biava
 *
 */
public class CloudProvider implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  private String name;
  private String id;

  private Provider cmdbProviderData;
  private Map<String, Service> cmdbProviderServices = new HashMap<>();
  private List<Image> cmdbProviderImages = new ArrayList<>();

  public CloudProvider() {
  }

  public CloudProvider(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Provider getCmdbProviderData() {
    return cmdbProviderData;
  }

  public void setCmdbProviderData(Provider cmdbProviderData) {
    this.cmdbProviderData = cmdbProviderData;
  }

  public Map<String, Service> getCmdbProviderServices() {
    return cmdbProviderServices;
  }

  public void setCmdbProviderServices(Map<String, Service> cmdbProviderServices) {
    this.cmdbProviderServices = cmdbProviderServices;
  }

  public List<Image> getCmdbProviderImages() {
    return cmdbProviderImages;
  }

  public void setCmdbProviderImages(List<Image> cmdbProviderImages) {
    this.cmdbProviderImages = cmdbProviderImages;
  }

  /**
   * Look for a Service in the current Provider of the given Type.
   * 
   * @param type
   *          the type.
   * @return the Service if found, <tt>null</tt> otherwise.
   */
  public Service getCmbdProviderServiceByType(Type type) {
    for (Service service : cmdbProviderServices.values()) {
      if (service.getData().getType().equals(type)) {
        return service;
      }
    }
    return null;
  }

}
