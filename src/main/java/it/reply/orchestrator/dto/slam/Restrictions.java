/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
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

package it.reply.orchestrator.dto.slam;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.reply.orchestrator.dto.AdditionalPropertiesAwareDto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Restrictions extends AdditionalPropertiesAwareDto implements Serializable {

  private static final long serialVersionUID = 6559999818418491070L;

  @JsonProperty("total_guaranteed")
  @Nullable
  private Integer totalGuaranteed;

  @JsonProperty("total_limit")
  @Nullable
  private Integer totalLimit;

  @JsonProperty("instance_guaranteed")
  @Nullable
  private Integer instanceGuaranteed;

  @JsonProperty("instance_limit")
  @Nullable
  private Integer instanceLimit;

  @JsonProperty("user_guaranteed")
  @Nullable
  private Integer userGuaranteed;

  @JsonProperty("user_limit")
  @Nullable
  private Integer userLimit;

}
