package uk.gov.laa.ccms.caab.model.os;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/** Pagination metadata returned by the OS Places API. */
@Data
@RequiredArgsConstructor
public class OrdinanceSurveyHeader {

  @JsonProperty("totalresults")
  private Integer totalResults;
}
