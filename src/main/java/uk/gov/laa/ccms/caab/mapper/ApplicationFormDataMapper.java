package uk.gov.laa.ccms.caab.mapper;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;

/**
 * Maps between ApplicationFormData and ApplicationType models. Requires the
 * {@link uk.gov.laa.ccms.caab.mapper.IgnoreUnmappedMapperConfig}.
 */
@Mapper(componentModel = "spring", config = IgnoreUnmappedMapperConfig.class)
public interface ApplicationFormDataMapper {

  @Mapping(target = "applicationTypeCategory", source = "id")
  @Mapping(target = "delegatedFunctions", source = "devolvedPowers.used")
  @Mapping(target = "delegatedFunctionUsedDay",
      source = "devolvedPowers.dateUsed",
      qualifiedByName = "mapDevolvedPowersDay")
  @Mapping(target = "delegatedFunctionUsedMonth",
      source = "devolvedPowers.dateUsed",
      qualifiedByName = "mapDevolvedPowersMonth")
  @Mapping(target = "delegatedFunctionUsedYear",
      source = "devolvedPowers.dateUsed",
      qualifiedByName = "mapDevolvedPowersYear")
  @Mapping(target = "devolvedPowersContractFlag",
      source = "devolvedPowers.contractFlag")
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
  @Named("mapDevolvedPowersDay")
  default String mapDevolvedPowersDay(Date dateUsed) {
    if (dateUsed != null) {
      LocalDate localDate = dateUsed.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
      return Integer.toString(localDate.getDayOfMonth());
    }
    return null;
  }

  /**
   * Translates Date into a month string.
   *
   * @param dateUsed The Date to translate.
   * @return Translated month string.
   */
  @Named("mapDevolvedPowersMonth")
  default String mapDevolvedPowersMonth(Date dateUsed) {
    if (dateUsed != null) {
      LocalDate localDate = dateUsed.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
      return Integer.toString(localDate.getMonthValue());
    }
    return null;
  }

  /**
   * Translates Date into a year string.
   *
   * @param dateUsed The Date to translate.
   * @return Translated year string.
   */
  @Named("mapDevolvedPowersYear")
  default String mapDevolvedPowersYear(Date dateUsed) {
    if (dateUsed != null) {
      LocalDate localDate = dateUsed.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
      return Integer.toString(localDate.getYear());
    }
    return null;
  }
}
