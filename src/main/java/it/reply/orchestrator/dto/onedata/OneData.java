package it.reply.orchestrator.dto.onedata;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OneData implements Serializable {

  private static final long serialVersionUID = 8590316308119399053L;

  public static class OneDataProviderInfo implements Serializable {

    private static final long serialVersionUID = -4904767929269221557L;

    public String id;

    public String endpoint;

    public String cloudProviderId;

    public String cloudServiceId;

    public OneDataProviderInfo() {
    }

    public OneDataProviderInfo(String endpoint) {
      this.endpoint = endpoint;
    }
  }

  private String token;
  private String space;
  private String path;
  private String zone;
  private List<OneDataProviderInfo> providers = Lists.newArrayList();
  private boolean smartScheduling;

  /**
   * Construct OneData settings with providers as list.
   * 
   * @param token
   *          .
   * @param space
   *          .
   * @param path
   *          .
   * @param providers
   *          .
   */
  public OneData(String token, String space, String path, List<OneDataProviderInfo> providers,
      String zone) {
    this.token = token;
    this.space = space;
    this.path = path;
    this.zone = zone;
    this.providers = providers;
  }

  public OneData(String token, String space, String path, List<OneDataProviderInfo> providers) {
    this(token, space, path, providers, null);
  }

  /**
   * Construct OneData settings with providers as CSV.
   * 
   * @param token
   *          .
   * @param space
   *          .
   * @param path
   *          .
   * @param providers
   *          .
   * @param zone
   *          The zone
   */
  public OneData(String token, String space, String path, String providers, String zone) {
    this(token, space, path, Lists.newArrayList(), zone);
    if (!Strings.isNullOrEmpty(providers)) {
      this.providers.addAll(Arrays.asList(providers.split(",")).stream()
          .map(prov -> new OneDataProviderInfo(prov)).collect(Collectors.toList()));
    }
  }

  public OneData(String token, String space, String path, String providers) {
    this(token, space, path, providers, null);
  }

  public String getToken() {
    return token;
  }

  public String getSpace() {
    return space;
  }

  public String getPath() {
    return path;
  }

  public String getZone() {
    return zone;
  }

  public List<OneDataProviderInfo> getProviders() {
    return providers;
  }

  public void setProviders(List<OneDataProviderInfo> providers) {
    this.providers = providers;
  }

  // /**
  // * Generate the provider list as CSV.
  // *
  // * @return the provider list as CSV
  // */
  // public String getProvidersAsList() {
  // return providers != null ? StringUtils.join(providers, ",") : "";
  // }

  /**
   * @return the smartScheduling
   */
  public boolean isSmartScheduling() {
    return smartScheduling;
  }

  /**
   * @param smartScheduling
   *          the smartScheduling to set
   */
  public void setSmartScheduling(boolean smartScheduling) {
    this.smartScheduling = smartScheduling;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

}
