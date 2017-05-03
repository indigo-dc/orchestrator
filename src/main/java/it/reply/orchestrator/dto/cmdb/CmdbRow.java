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

package it.reply.orchestrator.dto.cmdb;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "id", "key", "value", "doc" })
public class CmdbRow<T> implements Serializable {

  private static final long serialVersionUID = 559476054523810413L;

  @JsonProperty("id")
  private String id;
  @JsonProperty("key")
  private List<String> key = new ArrayList<String>();
  @JsonProperty("doc")
  private T doc;

  @JsonProperty("id")
  public String getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  public CmdbRow<T> withId(String id) {
    this.id = id;
    return this;
  }

  @JsonProperty("key")
  public List<String> getKey() {
    return key;
  }

  @JsonProperty("key")
  public void setKey(List<String> key) {
    this.key = key;
  }

  public CmdbRow<T> withKey(List<String> key) {
    this.key = key;
    return this;
  }

  @JsonProperty("doc")
  public T getDoc() {
    return doc;
  }

  @JsonProperty("doc")
  public void setDoc(T doc) {
    this.doc = doc;
  }

  public CmdbRow<T> withDoc(T doc) {
    this.doc = doc;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(id).append(key).append(doc).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (other == null) {
      return false;
    }
    if ((other instanceof CmdbRow) == false) {
      return false;
    }

    CmdbRow<?> rhs = ((CmdbRow<?>) other);
    return new EqualsBuilder().append(id, rhs.id).append(key, rhs.key).append(doc, rhs.doc)
        .isEquals();
  }
}