package uk.gov.laa.ccms.caab.mapper;

import java.time.LocalDate;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataContactDetails;
import uk.gov.laa.ccms.caab.util.DateUtils;
import uk.gov.laa.ccms.data.model.AddressDetail;
import uk.gov.laa.ccms.data.model.ClientDetail;
import uk.gov.laa.ccms.data.model.ContactDetails;

/**
 * Maps the ebs-api client detail to the client details form bean. Requires the {@link
 * uk.gov.laa.ccms.caab.mapper.IgnoreUnmappedMapperConfig}.
 */
@Mapper(componentModel = "spring", config = IgnoreUnmappedMapperConfig.class)
public interface EbsClientDetailMapper {

  @Mapping(target = "basicDetails", source = ".")
  @Mapping(target = "contactDetails", source = "contacts")
  @Mapping(target = "addressDetails", source = "address")
  @Mapping(target = "monitoringDetails.ethnicOrigin", source = "ethnicMonitoring")
  @Mapping(
      target = "monitoringDetails.disability",
      source = "disabilityMonitoring.disabilityType",
      qualifiedByName = "mapListToString")
  @Mapping(target = "monitoringDetails.specialConsiderations", source = "specialConsiderations")
  ClientFlowFormData toClientFlowFormData(ClientDetail clientDetail);

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
  ClientFormDataBasicDetails toClientFormDataBasicDetails(ClientDetail clientDetail);

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
  ClientFormDataContactDetails toClientFormDataContactDetails(ContactDetails contacts);

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
