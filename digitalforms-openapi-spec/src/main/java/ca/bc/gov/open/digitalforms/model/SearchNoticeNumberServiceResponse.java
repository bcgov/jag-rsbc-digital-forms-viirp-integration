package ca.bc.gov.open.digitalforms.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * SearchNoticeNumberServiceResponse
 */
@lombok.NoArgsConstructor @lombok.AllArgsConstructor

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-06-28T11:23:40.309485400-04:00[America/Toronto]")
public class SearchNoticeNumberServiceResponse   {

  @JsonProperty("impoundmentId")
  private Long impoundmentId;

  @JsonProperty("prohibitionId")
  private Long prohibitionId;

  public SearchNoticeNumberServiceResponse impoundmentId(Long impoundmentId) {
    this.impoundmentId = impoundmentId;
    return this;
  }

  /**
   * Get impoundmentId
   * @return impoundmentId
  */
  
  @Schema(name = "impoundmentId", required = false)
  public Long getImpoundmentId() {
    return impoundmentId;
  }

  public void setImpoundmentId(Long impoundmentId) {
    this.impoundmentId = impoundmentId;
  }

  public SearchNoticeNumberServiceResponse prohibitionId(Long prohibitionId) {
    this.prohibitionId = prohibitionId;
    return this;
  }

  /**
   * Get prohibitionId
   * @return prohibitionId
  */
  
  @Schema(name = "prohibitionId", required = false)
  public Long getProhibitionId() {
    return prohibitionId;
  }

  public void setProhibitionId(Long prohibitionId) {
    this.prohibitionId = prohibitionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SearchNoticeNumberServiceResponse searchNoticeNumberServiceResponse = (SearchNoticeNumberServiceResponse) o;
    return Objects.equals(this.impoundmentId, searchNoticeNumberServiceResponse.impoundmentId) &&
        Objects.equals(this.prohibitionId, searchNoticeNumberServiceResponse.prohibitionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(impoundmentId, prohibitionId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SearchNoticeNumberServiceResponse {\n");
    sb.append("    impoundmentId: ").append(toIndentedString(impoundmentId)).append("\n");
    sb.append("    prohibitionId: ").append(toIndentedString(prohibitionId)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

