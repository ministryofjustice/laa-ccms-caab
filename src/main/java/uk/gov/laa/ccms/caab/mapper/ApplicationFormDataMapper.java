package uk.gov.laa.ccms.caab.mapper;

import java.util.Date;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.util.DateUtils;

/**
 * Maps between ApplicationFormData and ApplicationType models. Requires the {@link
 * uk.gov.laa.ccms.caab.mapper.IgnoreUnmappedMapperConfig}.
 */
@Mapper(componentModel = "spring", config = IgnoreUnmappedMapperConfig.class)
public interface ApplicationFormDataMapper {

  @Mapping(target = "applicationTypeCategory", source = "id")
  @Mapping(target = "delegatedFunctions", source = "devolvedPowers.used")
  @Mapping(
      target = "delegatedFunctionUsedDate",
      source = "devolvedPowers.dateUsed",
      qualifiedByName = "mapDevolvedPowersDate")
  @Mapping(target = "devolvedPowersContractFlag", source = "devolvedPowers.contractFlag")
  ApplicationFormData toApplicationTypeFormData(ApplicationType applicationType);

  @Mapping(target = "officeId", source = "office.id")
  @Mapping(target = "officeName", source = "office.displayValue")
  @Mapping(target = "feeEarnerId", source = "feeEarner.id")
  @Mapping(target = "supervisorId", source = "supervisor.id")
  @Mapping(target = "contactNameId", source = "providerContact.id")
  ApplicationFormData toApplicationProviderDetailsFormData(
      ApplicationProviderDetails providerDetails);

  /**
   * Translates Date into a day string.
   *
   * @param dateUsed The Date to translate.
   * @return Translated day string.
   */
  @Named("mapDevolvedPowersDate")
  default String mapDevolvedPowersDate(Date dateUsed) {
    if (dateUsed == null) {
      return null;
    }
    return DateUtils.convertToComponentDate(dateUsed);
  }
}
