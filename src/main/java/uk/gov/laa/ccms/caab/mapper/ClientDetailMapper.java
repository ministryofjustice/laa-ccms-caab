package uk.gov.laa.ccms.caab.mapper;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
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
 * Maps the client details form bean to the soa-api client detail sent on create and update, and the
 * ebs-api client detail to the form bean. Requires the {@link
 * uk.gov.laa.ccms.caab.mapper.IgnoreUnmappedMapperConfig}.
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

  @Mapping(target = "basicDetails", source = ".")
  @Mapping(target = "contactDetails", source = "contacts")
  @Mapping(target = "addressDetails", source = "address")
  @Mapping(target = "monitoringDetails.ethnicOrigin", source = "ethnicMonitoring")
  @Mapping(
      target = "monitoringDetails.disability",
      source = "disabilityMonitoring.disabilityType",
      qualifiedByName = "mapListToString")
  @Mapping(target = "monitoringDetails.specialConsiderations", source = "specialConsiderations")
  ClientFlowFormData toClientFlowFormData(uk.gov.laa.ccms.data.model.ClientDetail clientDetail);

  @Mapping(target = "title", source = "name.title")
  @Mapping(target = "firstName", source = "name.firstName")
  @Mapping(target = "middleNames", source = "name.middleName")
  @Mapping(target = "surname", source = "name.surname")
  @Mapping(target = "surnameAtBirth", source = "name.surnameAtBirth")
  @Mapping(
      target = "dateOfBirth",
      source = "dateOfBirth",
      qualifiedByName = "mapComponentDateFromLocalDate")
  @Mapping(target = "countryOfOrigin", source = "countryOfOriginCode")
  @Mapping(target = "nationalInsuranceNumber", source = "niNumber")
  @Mapping(target = "mentalIncapacity", source = "mentalCapacityInd")
  ClientFormDataBasicDetails toClientFormDataBasicDetails(
      uk.gov.laa.ccms.data.model.ClientDetail clientDetail);

  /**
   * Adds address details to the client flow form data, overriding the vulnerable client state.
   *
   * @param clientFlowFormData The client flow form data with basic details to be amended.
   */
  @AfterMapping
  default void setAddressDetailsIfNull(@MappingTarget final ClientFlowFormData clientFlowFormData) {
    if (clientFlowFormData.getAddressDetails() == null) {
      final ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();
      addressDetails.setVulnerableClient(
          clientFlowFormData.getBasicDetails().getVulnerableClient());
      clientFlowFormData.setAddressDetails(addressDetails);
    }
  }

  @Mapping(target = "telephoneMobile", source = "mobileNumber")
  @Mapping(
      target = "telephoneHomePresent",
      expression =
          "java(contacts.getTelephoneHome() != null && !contacts.getTelephoneHome().isEmpty())")
  @Mapping(
      target = "telephoneWorkPresent",
      expression =
          "java(contacts.getTelephoneWork() != null && !contacts.getTelephoneWork().isEmpty())")
  @Mapping(
      target = "telephoneMobilePresent",
      expression =
          "java(contacts.getMobileNumber() != null && !contacts.getMobileNumber().isEmpty())")
  ClientFormDataContactDetails toClientFormDataContactDetails(
      uk.gov.laa.ccms.data.model.ContactDetails contacts);

  @Mapping(target = "houseNameNumber", source = "house")
  @Mapping(target = "cityTown", source = "city")
  @Mapping(target = "postcode", source = "postalCode")
  ClientFormDataAddressDetails toClientFormDataAddressDetails(
      uk.gov.laa.ccms.data.model.AddressDetail address);

  /**
   * Translates a list to a sting, using the first element.
   *
   * @param values The list of strings to convert
   * @return Translated String value.
   */
  @Named("mapListToString")
  default String mapListToString(List<String> values) {
    if (values != null && !values.isEmpty()) {
      return values.getFirst();
    }
    return null;
  }

  /**
   * Translates a LocalDate into a component date string.
   *
   * @param date The date to convert
   * @return Translated String value.
   */
  @Named("mapComponentDateFromLocalDate")
  default String mapComponentDateFromLocalDate(LocalDate date) {
    if (date == null) {
      return null;
    }
    return DateUtils.convertToComponentDate(date);
  }
}
