package it.reply.orchestrator.dto.cmdb;

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
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "total_rows", "offset", "rows" })
public class CmdbHasManyList<ROWT> implements Serializable {

  private static final long serialVersionUID = -7214527741922419947L;

  @JsonProperty("total_rows")
  private Long totalRows;
  @JsonProperty("offset")
  private Long offset;
  @JsonProperty("rows")
  private List<ROWT> rows = new ArrayList<>();
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<String, Object>();

  @JsonProperty("total_rows")
  public Long getTotalRows() {
    return totalRows;
  }

  @JsonProperty("total_rows")
  public void setTotalRows(Long totalRows) {
    this.totalRows = totalRows;
  }

  public CmdbHasManyList<ROWT> withTotalRows(Long totalRows) {
    this.totalRows = totalRows;
    return this;
  }

  @JsonProperty("offset")
  public Long getOffset() {
    return offset;
  }

  @JsonProperty("offset")
  public void setOffset(Long offset) {
    this.offset = offset;
  }

  public CmdbHasManyList<ROWT> withOffset(Long offset) {
    this.offset = offset;
    return this;
  }

  @JsonProperty("rows")
  public List<ROWT> getRows() {
    return rows;
  }

  @JsonProperty("rows")
  public void setRows(List<ROWT> rows) {
    this.rows = rows;
  }

  public CmdbHasManyList<ROWT> withRows(List<ROWT> rows) {
    this.rows = rows;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  public CmdbHasManyList<ROWT> withAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
    return this;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(totalRows).append(offset).append(rows)
        .append(additionalProperties).toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (other == null) {
      return false;
    }
    if ((other instanceof CmdbHasManyList) == false) {
      return false;
    }

    CmdbHasManyList<?> rhs = ((CmdbHasManyList<?>) other);
    return new EqualsBuilder().append(totalRows, rhs.totalRows).append(offset, rhs.offset)
        .append(rows, rhs.rows).append(additionalProperties, rhs.additionalProperties).isEquals();
  }

}