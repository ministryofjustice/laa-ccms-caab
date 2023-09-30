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
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

/**
 * Maps between Client details form bean and the soa-api client detail. Requires the
 * {@link uk.gov.laa.ccms.caab.mapper.IgnoreUnmappedMapperConfig}.
 */
@Mapper(componentModel = "spring", config = IgnoreUnmappedMapperConfig.class)
public interface ClientDetailMapper {

  @Mapping(target = "details.name.title",
      source = "title")
  @Mapping(target = "details.name.surname",
      source = "surname")
  @Mapping(target = "details.name.firstName",
      source = "firstName")
  @Mapping(target = "details.name.middleName",
      source = "middleNames")
  @Mapping(target = "details.name.surnameAtBirth",
      source = "surnameAtBirth")
  @Mapping(target = "details.name.fullName",
      source = "clientFormData",
      qualifiedByName = "mapFullName")
  @Mapping(target = "details.personalInformation.dateOfBirth",
      source = ".",
      qualifiedByName = "mapDateOfBirth")
  @Mapping(target = "details.personalInformation.dateOfDeath",
      ignore = true)
  @Mapping(target = "details.personalInformation.gender",
      source = "gender")
  @Mapping(target = "details.personalInformation.maritalStatus",
      source = "maritalStatus")
  @Mapping(target = "details.personalInformation.nationalInsuranceNumber",
      source = "nationalInsuranceNumber")
  @Mapping(target = "details.personalInformation.homeOfficeNumber",
      source = "homeOfficeNumber")
  @Mapping(target = "details.personalInformation.vulnerableClient",
      source = "vulnerableClient")
  @Mapping(target = "details.personalInformation.highProfileClient",
      source = "highProfileClient")
  @Mapping(target = "details.personalInformation.vexatiousLitigant",
      source = "vexatiousLitigant")
  @Mapping(target = "details.personalInformation.countryOfOrigin",
      source = "countryOfOrigin")
  @Mapping(target = "details.personalInformation.mentalCapacityInd",
      source = "mentalIncapacity")
  @Mapping(target = "details.contacts.telephoneHome",
      source = "telephoneHome")
  @Mapping(target = "details.contacts.telephoneWork",
      source = "telephoneWork")
  @Mapping(target = "details.contacts.mobileNumber",
      source = "telephoneMobile")
  @Mapping(target = "details.contacts.emailAddress",
      source = "emailAddress")
  @Mapping(target = "details.contacts.fax",
      ignore = true)
  @Mapping(target = "details.contacts.password",
      source = "password")
  @Mapping(target = "details.contacts.passwordReminder",
      source = "passwordReminder")
  @Mapping(target = "details.contacts.correspondenceMethod",
      source = "correspondenceMethod")
  @Mapping(target = "details.contacts.correspondenceLanguage",
      source = "correspondenceLanguage")
  @Mapping(target = "details.disabilityMonitoring.disabilityType",
      source = "disability",
      qualifiedByName = "mapStringToList")
  @Mapping(target = "details.ethnicMonitoring",
      source = "ethnicOrigin")
  @Mapping(target = "details.specialConsiderations",
      source = "specialConsiderations")
  @Mapping(target = "details.noFixedAbode",
      source = "noFixedAbode")
  @Mapping(target = "details.address.addressId",
      ignore = true)
  @Mapping(target = "details.address.house",
      source = "houseNameNumber")
  @Mapping(target = "details.address.careOfName",
      ignore = true)
  @Mapping(target = "details.address.addressLine1",
      source = "addressLine1")
  @Mapping(target = "details.address.addressLine2",
      source = "addressLine2")
  @Mapping(target = "details.address.addressLine3",
      ignore = true)
  @Mapping(target = "details.address.addressLine4",
      ignore = true)
  @Mapping(target = "details.address.city",
      source = "cityTown")
  @Mapping(target = "details.address.country",
      source = "country")
  @Mapping(target = "details.address.county",
      source = "county")
  @Mapping(target = "details.address.province",
      ignore = true)
  @Mapping(target = "details.address.state",
      ignore = true)
  @Mapping(target = "details.address.postalCode",
      source = "postcode")
  ClientDetail toSoaClientDetail(ClientDetails clientFormData);

  /**
   * Translates a string into a singleton list.
   *
   * @param value The String to translate.
   * @return Translated singleton list.
   */
  @Named("mapStringToList")
  default List<String> map(String value) {
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
}
