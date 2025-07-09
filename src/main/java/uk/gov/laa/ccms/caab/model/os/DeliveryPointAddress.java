package uk.gov.laa.ccms.caab.model.os;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/** Represents the delivery point address returned by the ordinance survey api. */
@Data
@RequiredArgsConstructor
public class DeliveryPointAddress {

  @JsonProperty("UPRN")
  private String uprn;

  @JsonProperty("ADDRESS")
  private String address;

  @JsonProperty("BUILDING_NUMBER")
  private String buildingNumber;

  @JsonProperty("BUILDING_NAME")
  private String buildingName;

  @JsonProperty("SUB_BUILDING_NAME")
  private String subBuildingName;

  @JsonProperty("ORGANISATION_NAME")
  private String organisationName;

  @JsonProperty("THOROUGHFARE_NAME")
  private String thoroughfareName;

  @JsonProperty("DEPENDENT_LOCALITY")
  private String dependentLocality;

  @JsonProperty("POST_TOWN")
  private String postTown;

  @JsonProperty("POSTCODE")
  private String postcode;
}
