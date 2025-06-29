package uk.gov.laa.ccms.caab.model.os;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/** Represents the response returned by the ordinance survey api. */
@Data
@RequiredArgsConstructor
public class OrdinanceSurveyResponse {

  @JsonProperty("results")
  private List<OrdinanceSurveyResult> results;
}
