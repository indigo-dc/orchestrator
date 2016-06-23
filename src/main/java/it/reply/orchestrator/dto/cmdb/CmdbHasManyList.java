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
public class CmdbHasManyList<ROW_TYPE> {

  @JsonProperty("total_rows")
  private Long totalRows;
  @JsonProperty("offset")
  private Long offset;
  @JsonProperty("rows")
  private List<ROW_TYPE> rows = new ArrayList<>();
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<String, Object>();

  /**
   * 
   * @return The totalRows
   */
  @JsonProperty("total_rows")
  public Long getTotalRows() {
    return totalRows;
  }

  /**
   * 
   * @param totalRows
   *          The total_rows
   */
  @JsonProperty("total_rows")
  public void setTotalRows(Long totalRows) {
    this.totalRows = totalRows;
  }

  public CmdbHasManyList<ROW_TYPE> withTotalRows(Long totalRows) {
    this.totalRows = totalRows;
    return this;
  }

  /**
   * 
   * @return The offset
   */
  @JsonProperty("offset")
  public Long getOffset() {
    return offset;
  }

  /**
   * 
   * @param offset
   *          The offset
   */
  @JsonProperty("offset")
  public void setOffset(Long offset) {
    this.offset = offset;
  }

  public CmdbHasManyList<ROW_TYPE> withOffset(Long offset) {
    this.offset = offset;
    return this;
  }

  /**
   * 
   * @return The rows
   */
  @JsonProperty("rows")
  public List<ROW_TYPE> getRows() {
    return rows;
  }

  /**
   * 
   * @param rows
   *          The rows
   */
  @JsonProperty("rows")
  public void setRows(List<ROW_TYPE> rows) {
    this.rows = rows;
  }

  public CmdbHasManyList<ROW_TYPE> withRows(List<ROW_TYPE> rows) {
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

  public CmdbHasManyList<ROW_TYPE> withAdditionalProperty(String name, Object value) {
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
    CmdbHasManyList<ROW_TYPE> rhs = ((CmdbHasManyList<ROW_TYPE>) other);
    return new EqualsBuilder().append(totalRows, rhs.totalRows).append(offset, rhs.offset)
        .append(rows, rhs.rows).append(additionalProperties, rhs.additionalProperties).isEquals();
  }

}