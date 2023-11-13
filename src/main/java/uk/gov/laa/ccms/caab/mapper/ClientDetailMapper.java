package uk.gov.laa.ccms.caab.mapper;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.soa.gateway.model.AddressDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientPersonalDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContactDetail;
import uk.gov.laa.ccms.soa.gateway.model.NameDetail;

/**
 * Maps between Client details form bean and the soa-api client detail. Requires the
 * {@link uk.gov.laa.ccms.caab.mapper.IgnoreUnmappedMapperConfig}.
 */
@Mapper(componentModel = "spring", config = IgnoreUnmappedMapperConfig.class)
public interface ClientDetailMapper {

  @Mapping(target = "details.name", source = "clientFormData")
  @Mapping(target = "details.personalInformation", source = "clientFormData")
  @Mapping(target = "details.contacts", source = "clientFormData")
  @Mapping(target = "details.address", source = "clientFormData")
  @Mapping(target = "details.disabilityMonitoring.disabilityType",
      source = "disability",
      qualifiedByName = "mapStringToList")
  @Mapping(target = "details.ethnicMonitoring",
      source = "ethnicOrigin")
  @Mapping(target = "details.specialConsiderations",
      source = "specialConsiderations")
  @Mapping(target = "details.noFixedAbode",
      source = "noFixedAbode")
  ClientDetail toSoaClientDetail(ClientDetails clientFormData);

  @Mapping(target = "middleName", source = "middleNames")
  @Mapping(target = "fullName", source = "clientFormData", qualifiedByName = "mapFullName")
  NameDetail mapNameDetail(ClientDetails clientFormData);

  @Mapping(target = "dateOfBirth",
      source = ".",
      qualifiedByName = "mapDateOfBirth")
  @Mapping(target = "dateOfDeath",
      ignore = true)
  @Mapping(target = "mentalCapacityInd",
      source = "mentalIncapacity")
  ClientPersonalDetail mapPersonalInformationDetail(ClientDetails clientFormData);

  @Mapping(target = "mobileNumber",
      source = "telephoneMobile")
  @Mapping(target = "fax",
      ignore = true)
  ContactDetail mapContactDetail(ClientDetails clientFormData);

  @Mapping(target = "addressId",
      ignore = true)
  @Mapping(target = "house",
      source = "houseNameNumber")
  @Mapping(target = "careOfName",
      ignore = true)
  @Mapping(target = "addressLine3",
      ignore = true)
  @Mapping(target = "addressLine4",
      ignore = true)
  @Mapping(target = "city",
      source = "cityTown")
  @Mapping(target = "province",
      ignore = true)
  @Mapping(target = "state",
      ignore = true)
  @Mapping(target = "postalCode",
      source = "postcode")
  AddressDetail mapAddressDetail(ClientDetails clientFormData);

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
   * @param clientFormData The client details containing all name components
   * @return Translated String of full name details.
   */
  @Named("mapFullName")
  default String mapFullName(ClientDetails clientFormData) {
    String fullName = Stream.of(
            clientFormData.getFirstName(),
            clientFormData.getMiddleNames(),
            clientFormData.getSurname()
        )
        .filter(name -> name != null && !name.isEmpty())
        .collect(Collectors.joining(" "));

    return fullName.isEmpty() ? null : fullName;
  }

  /**
   * Translates the clients dob name, month, year into a Date object.
   *
   * @param clientFormData The client details containing all dob components
   * @return Translated Date for date of birth.
   */
  @Named("mapDateOfBirth")
  default Date mapDateOfBirth(ClientDetails clientFormData) {
    if (clientFormData != null) {
      int day = Integer.parseInt(clientFormData.getDobDay());

      // Subtract 1 since Calendar months are zero-based
      int month = Integer.parseInt(clientFormData.getDobMonth()) - 1;
      int year = Integer.parseInt(clientFormData.getDobYear());

      Calendar calendar = Calendar.getInstance();
      calendar.set(year, month, day);

      return calendar.getTime();
    }
    return null;
  }

  @Mapping(target = "title", source = "details.name.title")
  @Mapping(target = "firstName", source = "details.name.firstName")
  @Mapping(target = "middleNames", source = "details.name.middleName")
  @Mapping(target = "surname", source = "details.name.surname")
  @Mapping(target = "surnameAtBirth", source = "details.name.surnameAtBirth")
  @Mapping(target = "dobDay", source = "details.personalInformation.dateOfBirth",
      qualifiedByName = "mapDayFromDate")
  @Mapping(target = "dobMonth", source = "details.personalInformation.dateOfBirth",
      qualifiedByName = "mapMonthFromDate")
  @Mapping(target = "dobYear", source = "details.personalInformation.dateOfBirth",
      qualifiedByName = "mapYearFromDate")
  @Mapping(target = "countryOfOrigin", source = "details.personalInformation.countryOfOrigin")
  @Mapping(target = "nationalInsuranceNumber",
      source = "details.personalInformation.nationalInsuranceNumber")
  @Mapping(target = "homeOfficeNumber",
      source = "details.personalInformation.homeOfficeNumber")
  @Mapping(target = "gender",
      source = "details.personalInformation.gender")
  @Mapping(target = "maritalStatus",
      source = "details.personalInformation.maritalStatus")
  @Mapping(target = "vulnerableClient",
      source = "details.personalInformation.vulnerableClient")
  @Mapping(target = "highProfileClient",
      source = "details.personalInformation.highProfileClient")
  @Mapping(target = "vexatiousLitigant",
      source = "details.personalInformation.vexatiousLitigant")
  @Mapping(target = "mentalIncapacity",
      source = "details.personalInformation.mentalCapacityInd")
  @Mapping(target = "telephoneHome",
      source = "details.contacts.telephoneHome")
  @Mapping(target = "telephoneWork",
      source = "details.contacts.telephoneWork")
  @Mapping(target = "telephoneMobile",
      source = "details.contacts.mobileNumber")
  @Mapping(target = "emailAddress",
      source = "details.contacts.emailAddress")
  @Mapping(target = "passwordReminder",
      source = "details.contacts.passwordReminder")
  @Mapping(target = "correspondenceMethod",
      source = "details.contacts.correspondenceMethod")
  @Mapping(target = "correspondenceLanguage",
      source = "details.contacts.correspondenceLanguage")
  @Mapping(target = "noFixedAbode",
      source = "details.noFixedAbode")
  @Mapping(target = "country",
      source = "details.address.country")
  @Mapping(target = "ethnicOrigin",
      source = "details.ethnicMonitoring")
  @Mapping(target = "disability",
      source = "details.disabilityMonitoring.disabilityType",
      qualifiedByName = "mapListToString")
  @Mapping(target = "specialConsiderations",
      source = "details.specialConsiderations")
  @Mapping(target = "houseNameNumber", source = "details.address.house")
  @Mapping(target = "addressLine1", source = "details.address.addressLine1")
  @Mapping(target = "addressLine2", source = "details.address.addressLine2")
  @Mapping(target = "cityTown", source = "details.address.city")
  @Mapping(target = "postcode", source = "details.address.postalCode")
  @Mapping(target = "county", source = "details.address.county")
  ClientDetails toClientDetails(ClientDetail soaClientDetail);

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
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(date);
      return Integer.toString(calendar.get(Calendar.DAY_OF_MONTH));
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
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(date);
      // Add 1 since Calendar months are zero-based
      return Integer.toString(calendar.get(Calendar.MONTH) + 1);
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
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(date);
      return Integer.toString(calendar.get(Calendar.YEAR));
    }
    return null;
  }
}
