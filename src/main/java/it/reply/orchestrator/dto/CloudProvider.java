package it.reply.orchestrator.dto;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.ImageData;
import it.reply.orchestrator.dto.cmdb.Provider;
import it.reply.orchestrator.dto.cmdb.Type;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
  private Map<String, CloudService> cmdbProviderServices = Maps.newHashMap();
  private Map<String, List<ImageData>> cmdbProviderImages = Maps.newHashMap();

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

  public Map<String, CloudService> getCmdbProviderServices() {
    return cmdbProviderServices;
  }

  public void setCmdbProviderServices(Map<String, CloudService> cmdbProviderServices) {
    this.cmdbProviderServices = cmdbProviderServices;
  }

  public Map<String, List<ImageData>> getCmdbProviderImages() {
    return cmdbProviderImages;
  }

  public void setCmdbProviderImages(Map<String, List<ImageData>> cmdbProviderImages) {
    this.cmdbProviderImages = cmdbProviderImages;
  }

  /**
   * Add the images of the cloud service.
   * 
   * @param cloudServiceId
   *          the cloud service Id
   * @param cmdbServiceImages
   *          the images of the compute cloud service
   */
  public void addCmdbCloudServiceImages(String cloudServiceId,
      Collection<ImageData> cmdbServiceImages) {
    List<ImageData> images = null;
    if ((images = cmdbProviderImages.get(cloudServiceId)) == null) {
      images = Lists.newArrayList();
      cmdbProviderImages.put(cloudServiceId, images);
    }
    images.addAll(cmdbServiceImages);
  }

  /**
   * Look for a Service in the current Provider of the given Type.
   * 
   * @param type
   *          the type.
   * @return the Service if found, <tt>null</tt> otherwise.
   */
  public List<CloudService> getCmbdProviderServicesByType(Type type) {
    return cmdbProviderServices.values().stream()
        .filter(service -> service.getData() != null && type == service.getData().getType())
        .collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(name).append(id).append(cmdbProviderData)
        .append(cmdbProviderServices).append(cmdbProviderImages).toHashCode();
  }

  @Override
  public boolean equals(Object other) {

    if (other == this) {
      return true;
    }

    if (other == null) {
      return false;
    }

    if ((other instanceof CloudProvider) == false) {
      return false;
    }

    CloudProvider rhs = (CloudProvider) other;

    return new EqualsBuilder().append(name, rhs.name).append(id, rhs.id)
        .append(cmdbProviderData, rhs.cmdbProviderData)
        .append(cmdbProviderServices, rhs.cmdbProviderServices)
        .append(cmdbProviderImages, rhs.cmdbProviderImages).isEquals();
  }

}
