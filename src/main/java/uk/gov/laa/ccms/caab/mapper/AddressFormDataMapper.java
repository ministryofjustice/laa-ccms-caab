package uk.gov.laa.ccms.caab.mapper;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.model.Address;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;

/**
 * Maps between AddressFormData and Address models.
 */
@Mapper(componentModel = "spring")
public interface AddressFormDataMapper {

  @Mapping(target = "houseNameNumber", source = "houseNameOrNumber")
  @Mapping(target = "cityTown", source = "city")
  AddressFormData toAddressFormData(Address address);

  @InheritInverseConfiguration
  @Mapping(target = "noFixedAbode", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  Address toAddress(AddressFormData addressFormData);

  /**
   * Updates the AddressFormData object with information from the AddressResultRowDisplay
   * object.
   *
   * @param addressDetails Target address details object to update.
   * @param addressResultRowDisplay Source object containing the address details.
   */
  @Mapping(target = "preferredAddress", ignore = true)
  @Mapping(target = "careOf", ignore = true)
  void updateAddressFormData(
      @MappingTarget AddressFormData addressDetails,
      AddressResultRowDisplay addressResultRowDisplay);

}
