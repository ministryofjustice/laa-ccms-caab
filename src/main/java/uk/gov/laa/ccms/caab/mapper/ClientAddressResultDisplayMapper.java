package uk.gov.laa.ccms.caab.mapper;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.util.StringUtils;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.model.os.DeliveryPointAddress;
import uk.gov.laa.ccms.caab.model.os.OrdinanceSurveyResponse;
import uk.gov.laa.ccms.caab.model.os.OrdinanceSurveyResult;

/**
 * Maps between Ordinance survey models and CAAB models. Requires the
 * {@link uk.gov.laa.ccms.caab.mapper.IgnoreUnmappedMapperConfig}.
 */
@Mapper(componentModel = "spring", config = IgnoreUnmappedMapperConfig.class)
public interface ClientAddressResultDisplayMapper {

  /**
   * Updates the ClientDetails object with information from the ClientAddressResultRowDisplay
   * object.
   *
   * @param addressDetails Target address details object to update.
   * @param addressResultRowDisplay Source object containing the address details.
   */
  void updateClientFormDataAddressDetails(
      @MappingTarget ClientFormDataAddressDetails addressDetails,
      AddressResultRowDisplay addressResultRowDisplay);

  /**
   * Transforms an OrdinanceSurveyResponse into a ClientAddressResultsDisplay object.
   *
   * @param ordinanceSurveyResponse The response from the Ordinance Survey API.
   * @return Mapped ClientAddressResultsDisplay object.
   */
  @Mapping(target = "content", source = "results")
  ResultsDisplay<AddressResultRowDisplay> toClientAddressResultsDisplay(
      OrdinanceSurveyResponse ordinanceSurveyResponse);

  /**
   * Maps individual OrdinanceSurveyResult to ClientAddressResultRowDisplay.
   *
   * @param ordinanceSurveyResult Source object containing ordinance survey result.
   * @return Mapped ClientAddressResultRowDisplay object.
   */
  @Mapping(target = "fullAddress", source = "deliveryPointAddress.address")
  @Mapping(target = "uprn", source = "deliveryPointAddress.uprn")
  @Mapping(target = "addressLine1",
      source = "deliveryPointAddress",
      qualifiedByName = "addressLine1Translator")
  @Mapping(target = "addressLine2",
      source = "deliveryPointAddress",
      qualifiedByName = "addressLine2Translator")
  @Mapping(target = "houseNameNumber",
      source = "deliveryPointAddress",
      qualifiedByName = "houseNameNumberTranslator")
  @Mapping(target = "postcode", source = "deliveryPointAddress.postcode")
  @Mapping(target = "cityTown", source = "deliveryPointAddress.postTown")
  @Mapping(target = "country", constant = "GBR")
  AddressResultRowDisplay toClientAddressResultRowDisplay(
      OrdinanceSurveyResult ordinanceSurveyResult);

  /**
   * Translates DeliveryPointAddress into address line 1 format.
   *
   * @param deliveryPointAddress The DeliveryPointAddress to translate.
   * @return Translated address line 1 string.
   */
  @Named("addressLine1Translator")
  default String toAddressLine1(DeliveryPointAddress deliveryPointAddress) {

    if (StringUtils.hasText(deliveryPointAddress.getSubBuildingName())
        || StringUtils.hasText(deliveryPointAddress.getOrganisationName())) {

      return Stream.of(deliveryPointAddress.getOrganisationName(),
          deliveryPointAddress.getSubBuildingName()).filter(StringUtils::hasText).collect(
          Collectors.joining(","));

    } else {
      return Stream.of(deliveryPointAddress.getBuildingNumber(),
          deliveryPointAddress.getBuildingName(),
          deliveryPointAddress.getThoroughfareName()).filter(StringUtils::hasText).collect(
          Collectors.joining(","));
    }
  }

  /**
   * Translates DeliveryPointAddress into address line 2 format.
   *
   * @param deliveryPointAddress The DeliveryPointAddress to translate.
   * @return Translated address line 2 string or null if not applicable.
   */
  @Named("addressLine2Translator")
  default String toAddressLine2(DeliveryPointAddress deliveryPointAddress) {
    if (StringUtils.hasText(deliveryPointAddress.getSubBuildingName())
        || StringUtils.hasText(deliveryPointAddress.getOrganisationName())) {
      return Stream.of(deliveryPointAddress.getBuildingNumber(),
          deliveryPointAddress.getBuildingName(),
          deliveryPointAddress.getThoroughfareName()).filter(StringUtils::hasText).collect(
          Collectors.joining(","));
    } else {
      return null;
    }
  }

  /**
   * Translates DeliveryPointAddress into house name or number format.
   *
   * @param deliveryPointAddress The DeliveryPointAddress to translate.
   * @return Translated house name or number string.
   */
  @Named("houseNameNumberTranslator")
  default String toHouseNameNumber(DeliveryPointAddress deliveryPointAddress) {
    return Stream.of(deliveryPointAddress.getBuildingNumber(),
        deliveryPointAddress.getBuildingName())
        .filter(StringUtils::hasText)
        .collect(Collectors.joining(","));
  }
}
