package uk.gov.laa.ccms.caab.model.os;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/** Represents the result returned by the ordinance survey api. */
@Data
@RequiredArgsConstructor
public class OrdinanceSurveyResult {

  @JsonProperty("DPA")
  private DeliveryPointAddress deliveryPointAddress;
}
