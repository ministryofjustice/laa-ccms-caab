package uk.gov.laa.ccms.caab.mapper;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;

/** Maps between AddressFormData and AddressDetail models. */
@Mapper(componentModel = "spring")
public interface AddressFormDataMapper {

  @Mapping(target = "houseNameNumber", source = "houseNameOrNumber")
  @Mapping(target = "cityTown", source = "city")
  AddressFormData toAddressFormData(AddressDetail address);

  @InheritInverseConfiguration
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "noFixedAbode", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  AddressDetail toAddress(AddressFormData addressFormData);

  /**
   * Updates the AddressFormData object with information from the AddressResultRowDisplay object.
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
