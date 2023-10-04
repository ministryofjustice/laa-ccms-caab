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

  @Mapping(target = "details.name", source = "clientFormData",
      qualifiedByName = "mapNameDetail")
  @Mapping(target = "details.personalInformation", source = "clientFormData",
      qualifiedByName = "mapPersonalInformationDetail")
  @Mapping(target = "details.contacts", source = "clientFormData",
      qualifiedByName = "mapContactDetail")
  @Mapping(target = "details.address", source = "clientFormData",
      qualifiedByName = "mapAddressDetail")
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

  @Mapping(target = "title", source = "title")
  @Mapping(target = "surname", source = "surname")
  @Mapping(target = "firstName", source = "firstName")
  @Mapping(target = "middleName", source = "middleNames")
  @Mapping(target = "surnameAtBirth", source = "surnameAtBirth")
  @Mapping(target = "fullName", source = "clientFormData", qualifiedByName = "mapFullName")
  @Named("mapNameDetail")
  NameDetail mapNameDetail(ClientDetails clientFormData);

  @Mapping(target = "dateOfBirth",
      source = ".",
      qualifiedByName = "mapDateOfBirth")
  @Mapping(target = "dateOfDeath",
      ignore = true)
  @Mapping(target = "mentalCapacityInd",
      source = "mentalIncapacity")
  @Named("mapPersonalInformationDetail")
  ClientPersonalDetail mapPersonalInformationDetail(ClientDetails clientFormData);

  @Mapping(target = "mobileNumber",
      source = "telephoneMobile")
  @Mapping(target = "fax",
      ignore = true)
  @Named("mapContactDetail")
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
  @Named("mapAddressDetail")
  AddressDetail mapAddressDetail(ClientDetails clientFormData);

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
