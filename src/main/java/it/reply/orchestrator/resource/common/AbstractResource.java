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

package it.reply.orchestrator.resource.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springframework.hateoas.ResourceSupport;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@JsonInclude(Include.NON_NULL)
@Data
@EqualsAndHashCode(callSuper = true)
public class AbstractResource extends ResourceSupport {

  private String uuid;
  private String creationTime;
  private String updateTime;

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

  public void setUpdateTime(String updateTime) {
    this.updateTime = updateTime;
  }

  private String convertDate(Date date) {
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
    df.setTimeZone(tz);
    return df.format(date);
  }
}
