package uk.gov.laa.ccms.caab.mapper;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataContactDetails;
import uk.gov.laa.ccms.soa.gateway.model.AddressDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientPersonalDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContactDetail;
import uk.gov.laa.ccms.soa.gateway.model.NameDetail;

/**
 * Maps between Client details form bean and the soa-api client detail. Requires the
 * {@link uk.gov.laa.ccms.caab.mapper.IgnoreUnmappedMapperConfig}.
 */
@Mapper(componentModel = "spring", config = IgnoreUnmappedMapperConfig.class)
public interface ClientDetailMapper {

  @Mapping(target = "details", source = ".")
  ClientDetail toClientDetail(ClientFlowFormData clientFlowFormData);

  @Mapping(target = "disabilityMonitoring.disabilityType",
      source = "monitoringDetails.disability",
      qualifiedByName = "mapStringToList")
  @Mapping(target = "ethnicMonitoring",
      source = "monitoringDetails.ethnicOrigin")
  @Mapping(target = "specialConsiderations",
      source = "monitoringDetails.specialConsiderations")
  @Mapping(target = "name", source = "basicDetails")
  @Mapping(target = "personalInformation", source = "basicDetails")
  @Mapping(target = "contacts", source = "contactDetails")
  @Mapping(target = "address", source = "addressDetails")
  ClientDetailDetails toClientDetailDetails(ClientFlowFormData clientFlowFormData);

  @Mapping(target = "middleName", source = "middleNames")
  @Mapping(target = "fullName", source = ".", qualifiedByName = "mapFullName")
  NameDetail toNameDetail(ClientFormDataBasicDetails basicDetails);

  @Mapping(target = "dateOfBirth",
      source = ".",
      qualifiedByName = "mapDateOfBirth")
  @Mapping(target = "dateOfDeath",
      ignore = true)
  @Mapping(target = "mentalCapacityInd",
      source = "mentalIncapacity")
  ClientPersonalDetail toClientPersonalDetail(ClientFormDataBasicDetails basicDetails);

  @Mapping(target = "mobileNumber",
      source = "telephoneMobile")
  ContactDetail toContactDetails(ClientFormDataContactDetails contactDetails);

  @Mapping(target = "addressId",
      ignore = true)
  @Mapping(target = "house",
      source = "houseNameNumber")
  @Mapping(target = "city",
      source = "cityTown")
  @Mapping(target = "postalCode",
      source = "postcode")
  AddressDetail toAddressDetail(ClientFormDataAddressDetails addressDetails);

  /**
   * Translates a string into a singleton list.
   *
   * @param value The String to translate.
   * @return Translated singleton list.
   */
  @Named("mapStringToList")
  default List<String> mapStringToList(String value) {
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
  default String mapFullName(ClientFormDataBasicDetails basicDetails) {
    String fullName = Stream.of(
            basicDetails.getFirstName(),
            basicDetails.getMiddleNames(),
            basicDetails.getSurname()
        )
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
  default Date mapDateOfBirth(ClientFormDataBasicDetails basicDetails) {
    if (basicDetails != null) {
      int day = Integer.parseInt(basicDetails.getDobDay());
      int month = Integer.parseInt(basicDetails.getDobMonth());
      int year = Integer.parseInt(basicDetails.getDobYear());

      LocalDate localDate = LocalDate.of(year, month, day);
      return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
    return null;
  }

  @Mapping(target = "contactDetails", source = "contacts")
  @Mapping(target = "addressDetails", source = "address")
  @Mapping(target = "monitoringDetails.ethnicOrigin",
      source = "ethnicMonitoring")
  @Mapping(target = "monitoringDetails.disability",
      source = "disabilityMonitoring.disabilityType",
      qualifiedByName = "mapListToString")
  @Mapping(target = "monitoringDetails.specialConsiderations",
      source = "specialConsiderations")
  ClientFlowFormData toClientFlowFormData(ClientDetailDetails clientDetailDetails);

  /**
   * Adds the basic details to the client flow form data from soa client details, using nameDetails
   * and personal information.
   *
   * @param clientFlowFormData The client flow form data with basic details to be amended.
   * @param clientDetailDetails The returned soa client details to map from.
   */
  @BeforeMapping
  default void addClientFormDataBasicDetails(
      @MappingTarget ClientFlowFormData clientFlowFormData,
      ClientDetailDetails clientDetailDetails) {
    clientFlowFormData.setBasicDetails(new ClientFormDataBasicDetails());
    addClientFormDataBasicDetailsFromNameDetail(clientFlowFormData.getBasicDetails(),
        clientDetailDetails.getName());
    addClientFormDataBasicDetailsFromClientPersonalDetail(clientFlowFormData.getBasicDetails(),
        clientDetailDetails.getPersonalInformation());
  }

  /**
   * Adds address details to the client flow form data, overriding the vulnerable client state.
   *
   * @param clientFlowFormData The client flow form data with basic details to be amended.
   * @param clientDetailDetails The returned soa client details to map from.
   */
  @AfterMapping
  default void setAddressDetailsIfNull(
      @MappingTarget ClientFlowFormData clientFlowFormData,
      ClientDetailDetails clientDetailDetails) {
    if (clientFlowFormData.getAddressDetails() == null) {
      ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();
      addressDetails.setVulnerableClient(
          clientFlowFormData.getBasicDetails().getVulnerableClient());
      clientFlowFormData.setAddressDetails(addressDetails);
    }
  }

  @Mapping(target = "basicDetails.dobDay", source = "personalInformation.dateOfBirth",
      qualifiedByName = "mapDayFromDate")
  @Mapping(target = "basicDetails.dobMonth", source = "personalInformation.dateOfBirth",
      qualifiedByName = "mapMonthFromDate")
  @Mapping(target = "basicDetails.dobYear", source = "personalInformation.dateOfBirth",
      qualifiedByName = "mapYearFromDate")
  @Mapping(target = "basicDetails.mentalIncapacity",
      source = "personalInformation.mentalCapacityInd")
  void addClientFormDataBasicDetailsFromClientPersonalDetail(
      @MappingTarget ClientFormDataBasicDetails basicDetails,
      ClientPersonalDetail personalInformation);

  @Mapping(target = "middleNames", source = "name.middleName")
  void addClientFormDataBasicDetailsFromNameDetail(
      @MappingTarget ClientFormDataBasicDetails basicDetails,  NameDetail name);

  @Mapping(target = "telephoneMobile",
      source = "mobileNumber")
  ClientFormDataContactDetails toClientFormDataContactDetails(ContactDetail contacts);

  @Mapping(target = "houseNameNumber", source = "house")
  @Mapping(target = "cityTown", source = "city")
  @Mapping(target = "postcode", source = "postalCode")
  ClientFormDataAddressDetails toClientFormDataAddressDetails(AddressDetail address);

  /**
   * Translates a list to a sting, using the first element.
   *
   * @param values The list of strings to convert
   * @return Translated String value.
   */
  @Named("mapListToString")
  default String mapListToString(List<String> values) {
    if (values != null && !values.isEmpty()) {
      return values.get(0);
    }
    return null;
  }

  /**
   * Translates a Date and extracts the day.
   *
   * @param date The date to convert
   * @return Translated String value.
   */
  @Named("mapDayFromDate")
  default String mapDayFromDate(Date date) {
    if (date != null) {
      LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
      return Integer.toString(localDate.getDayOfMonth());
    }
    return null;
  }

  /**
   * Translates a Date and extracts the month.
   *
   * @param date The date to convert
   * @return Translated String value.
   */
  @Named("mapMonthFromDate")
  default String mapMonthFromDate(Date date) {
    if (date != null) {
      LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
      return Integer.toString(localDate.getMonthValue());
    }
    return null;
  }

  /**
   * Translates a Date and extracts the year.
   *
   * @param date The date to convert
   * @return Translated String value.
   */
  @Named("mapYearFromDate")
  default String mapYearFromDate(Date date) {
    if (date != null) {
      LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
      return Integer.toString(localDate.getYear());
    }
    return null;
  }
}
