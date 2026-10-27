package uk.gov.laa.ccms.caab.mapper;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataContactDetails;
import uk.gov.laa.ccms.caab.model.BaseClientDetail;
import uk.gov.laa.ccms.caab.util.DateUtils;
import uk.gov.laa.ccms.soa.gateway.model.AddressDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientPersonalDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContactDetail;
import uk.gov.laa.ccms.soa.gateway.model.NameDetail;

/**
 * Maps the client details form bean to the soa-api client detail sent on create and update.
 * Requires the {@link uk.gov.laa.ccms.caab.mapper.IgnoreUnmappedMapperConfig}.
 */
@Mapper(componentModel = "spring", config = IgnoreUnmappedMapperConfig.class)
public interface ClientDetailMapper {

  @Mapping(target = "firstName", source = "basicDetails.firstName")
  @Mapping(target = "surname", source = "basicDetails.surname")
  BaseClientDetail toBaseClient(ClientFlowFormData clientFlowFormData);

  @Mapping(target = "details", source = ".")
  ClientDetail toClientDetail(ClientFlowFormData clientFlowFormData);

  @Mapping(
      target = "disabilityMonitoring.disabilityType",
      source = "monitoringDetails.disability",
      qualifiedByName = "mapStringToList")
  @Mapping(target = "ethnicMonitoring", source = "monitoringDetails.ethnicOrigin")
  @Mapping(target = "specialConsiderations", source = "monitoringDetails.specialConsiderations")
  @Mapping(target = "name", source = "basicDetails")
  @Mapping(target = "personalInformation", source = "basicDetails")
  @Mapping(target = "contacts", source = "contactDetails")
  @Mapping(target = "address", source = "addressDetails")
  @Mapping(target = "noFixedAbode", source = "addressDetails.noFixedAbode")
  ClientDetailDetails toClientDetailDetails(ClientFlowFormData clientFlowFormData);

  @Mapping(target = "middleName", source = "middleNames")
  @Mapping(target = "fullName", source = ".", qualifiedByName = "mapFullName")
  NameDetail toNameDetail(ClientFormDataBasicDetails basicDetails);

  @Mapping(target = "dateOfBirth", source = ".", qualifiedByName = "mapDateOfBirth")
  @Mapping(target = "dateOfDeath", ignore = true)
  @Mapping(target = "mentalCapacityInd", source = "mentalIncapacity")
  ClientPersonalDetail toClientPersonalDetail(ClientFormDataBasicDetails basicDetails);

  @Mapping(target = "mobileNumber", source = "telephoneMobile")
  ContactDetail toContactDetails(ClientFormDataContactDetails contactDetails);

  @Mapping(target = "addressId", ignore = true)
  @Mapping(target = "house", source = "houseNameNumber")
  @Mapping(target = "city", source = "cityTown")
  @Mapping(target = "postalCode", source = "postcode")
  AddressDetail toAddressDetail(ClientFormDataAddressDetails addressDetails);

  /**
   * Translates a string into a singleton list.
   *
   * @param value The String to translate.
   * @return Translated singleton list.
   */
  @Named("mapStringToList")
  default List<String> mapStringToList(final String value) {
    if (value != null) {
      return Collections.singletonList(value);
    }
    return null;
  }

  /**
   * Translates the client name details into a single full name String.
   *
   * @param basicDetails The basic details containing all name components
   * @return Translated String of full name details.
   */
  @Named("mapFullName")
  default String mapFullName(final ClientFormDataBasicDetails basicDetails) {
    final String fullName =
        Stream.of(
                basicDetails.getFirstName(),
                basicDetails.getMiddleNames(),
                basicDetails.getSurname())
            .filter(name -> name != null && !name.isEmpty())
            .collect(Collectors.joining(" "));

    return fullName.isEmpty() ? null : fullName;
  }

  /**
   * Translates the clients dob name, month, year into a Date object.
   *
   * @param basicDetails The client basic details containing all dob components
   * @return Translated Date for date of birth.
   */
  @Named("mapDateOfBirth")
  default Date mapDateOfBirth(final ClientFormDataBasicDetails basicDetails) {
    if (basicDetails == null) {
      return null;
    }
    return DateUtils.convertToDate(basicDetails.getDateOfBirth());
  }
}
