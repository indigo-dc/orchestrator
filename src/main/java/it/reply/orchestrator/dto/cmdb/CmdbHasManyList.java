package it.reply.orchestrator.dto.cmdb;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({ "total_rows", "offset", "rows" })
public class CmdbHasManyList<ROWT> {

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
    if ((other instanceof CmdbHasManyList<?>) == false) {
      return false;
    }
    @SuppressWarnings("unchecked")
    CmdbHasManyList<ROWT> rhs = ((CmdbHasManyList<ROWT>) other);
    return new EqualsBuilder().append(totalRows, rhs.totalRows).append(offset, rhs.offset)
        .append(rows, rhs.rows).append(additionalProperties, rhs.additionalProperties).isEquals();
  }

}