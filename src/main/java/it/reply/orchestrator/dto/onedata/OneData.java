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

package it.reply.orchestrator.dto.onedata;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
@ToString(exclude = "token")
public class OneData implements Serializable {

  private static final long serialVersionUID = 8590316308119399053L;

  @Data
  public static class OneDataProviderInfo implements Serializable {

    private static final long serialVersionUID = -4904767929269221557L;

    private String id;

    private String endpoint;

    private String cloudProviderId;

    private String cloudServiceId;

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
  private List<OneDataProviderInfo> providers = new ArrayList<>();
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
      this.providers.addAll(Arrays.asList(providers.split(","))
          .stream()
          .map(prov -> new OneDataProviderInfo(prov))
          .collect(Collectors.toList()));
    }
  }

  public OneData(String token, String space, String path, String providers) {
    this(token, space, path, providers, null);
  }

}
