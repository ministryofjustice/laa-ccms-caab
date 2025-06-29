package uk.gov.laa.ccms.caab.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import uk.gov.laa.ccms.caab.bean.common.DynamicOptionFormData;
import uk.gov.laa.ccms.caab.bean.costs.CostsFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityDetailsFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityFlowFormData;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFlowFormData;
import uk.gov.laa.ccms.caab.bean.scopelimitation.ScopeLimitationFlowFormData;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ReferenceDataItemDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;

/**
 * This interface provides methods for mapping between {@link ProceedingFlowFormData} and {@link
 * ProceedingDetail} objects. It uses MapStruct for the mapping, with the Spring framework providing
 * the implementation at runtime.
 */
@Mapper(componentModel = "spring")
public interface ProceedingAndCostsMapper {

  @Mapping(target = "matterType.id", source = "proceedingFlowFormData.matterTypeDetails.matterType")
  @Mapping(
      target = "matterType.displayValue",
      source = "proceedingFlowFormData.matterTypeDetails.matterTypeDisplayValue")
  @Mapping(
      target = "proceedingType.id",
      source = "proceedingFlowFormData.proceedingDetails.proceedingType")
  @Mapping(
      target = "proceedingType.displayValue",
      source = "proceedingFlowFormData.proceedingDetails.proceedingTypeDisplayValue")
  @Mapping(
      target = "description",
      source = "proceedingFlowFormData.proceedingDetails.proceedingDescription")
  @Mapping(target = "larScope", source = "proceedingFlowFormData.proceedingDetails.larScope")
  @Mapping(
      target = "clientInvolvement.id",
      source = "proceedingFlowFormData.furtherDetails.clientInvolvementType")
  @Mapping(
      target = "clientInvolvement.displayValue",
      source = "proceedingFlowFormData.furtherDetails.clientInvolvementTypeDisplayValue")
  @Mapping(
      target = "levelOfService.id",
      source = "proceedingFlowFormData.furtherDetails.levelOfService")
  @Mapping(
      target = "levelOfService.displayValue",
      source = "proceedingFlowFormData.furtherDetails.levelOfServiceDisplayValue")
  @Mapping(target = "typeOfOrder.id", source = "proceedingFlowFormData.furtherDetails.typeOfOrder")
  @Mapping(target = "stage", source = "stage")
  @Mapping(target = "costLimitation", source = "costLimitation")
  @Mapping(target = "id", source = "proceedingFlowFormData.existingProceedingId")
  @Mapping(target = "typeOfOrder.displayValue", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "ebsId", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "dateGranted", ignore = true)
  @Mapping(target = "dateCostsValid", ignore = true)
  @Mapping(target = "edited", ignore = true)
  @Mapping(target = "scopeLimitations", ignore = true)
  @Mapping(target = "defaultScopeLimitation", ignore = true)
  @Mapping(target = "leadProceedingInd", ignore = true)
  @Mapping(target = "grantedUsingDevolvedPowers", ignore = true)
  @Mapping(target = "dateDevolvedPowersUsed", ignore = true)
  @Mapping(target = "proceedingCaseId", ignore = true)
  @Mapping(target = "outcome", ignore = true)
  @Mapping(target = "orderTypeReqFlag", ignore = true)
  @Mapping(target = "orderTypeDisplayFlag", ignore = true)
  @Mapping(target = "deleteScopeLimitationFlag", ignore = true)
  @Mapping(target = "availableFunctions", ignore = true)
  // used for mapping a new proceeding
  ProceedingDetail toProceeding(
      ProceedingFlowFormData proceedingFlowFormData, BigDecimal costLimitation, String stage);

  @Mapping(target = "matterType.id", source = "proceedingFlowFormData.matterTypeDetails.matterType")
  @Mapping(
      target = "matterType.displayValue",
      source = "proceedingFlowFormData.matterTypeDetails.matterTypeDisplayValue")
  @Mapping(
      target = "proceedingType.id",
      source = "proceedingFlowFormData.proceedingDetails.proceedingType")
  @Mapping(
      target = "proceedingType.displayValue",
      source = "proceedingFlowFormData.proceedingDetails.proceedingTypeDisplayValue")
  @Mapping(
      target = "clientInvolvement.id",
      source = "proceedingFlowFormData.furtherDetails.clientInvolvementType")
  @Mapping(
      target = "clientInvolvement.displayValue",
      source = "proceedingFlowFormData.furtherDetails.clientInvolvementTypeDisplayValue")
  @Mapping(
      target = "levelOfService.id",
      source = "proceedingFlowFormData.furtherDetails.levelOfService")
  @Mapping(
      target = "levelOfService.displayValue",
      source = "proceedingFlowFormData.furtherDetails.levelOfServiceDisplayValue")
  @Mapping(target = "typeOfOrder.id", source = "proceedingFlowFormData.furtherDetails.typeOfOrder")
  @Mapping(target = "stage", source = "stage")
  @Mapping(target = "costLimitation", source = "costLimitation")
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "description", ignore = true)
  @Mapping(target = "larScope", ignore = true)
  @Mapping(target = "typeOfOrder.displayValue", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "ebsId", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "dateGranted", ignore = true)
  @Mapping(target = "dateCostsValid", ignore = true)
  @Mapping(target = "edited", ignore = true)
  @Mapping(target = "scopeLimitations", ignore = true)
  @Mapping(target = "defaultScopeLimitation", ignore = true)
  @Mapping(target = "leadProceedingInd", ignore = true)
  @Mapping(target = "grantedUsingDevolvedPowers", ignore = true)
  @Mapping(target = "dateDevolvedPowersUsed", ignore = true)
  @Mapping(target = "proceedingCaseId", ignore = true)
  @Mapping(target = "outcome", ignore = true)
  @Mapping(target = "orderTypeReqFlag", ignore = true)
  @Mapping(target = "orderTypeDisplayFlag", ignore = true)
  @Mapping(target = "deleteScopeLimitationFlag", ignore = true)
  @Mapping(target = "availableFunctions", ignore = true)
  // used for mapping an existing proceeding
  // notice that some fields are ignored
  void toProceeding(
      @MappingTarget ProceedingDetail proceeding,
      ProceedingFlowFormData proceedingFlowFormData,
      BigDecimal costLimitation,
      String stage);

  @Mapping(target = "matterTypeDetails.matterType", source = "proceeding.matterType.id")
  @Mapping(
      target = "matterTypeDetails.matterTypeDisplayValue",
      source = "proceeding.matterType.displayValue")
  @Mapping(target = "proceedingDetails.proceedingType", source = "proceeding.proceedingType.id")
  @Mapping(
      target = "proceedingDetails.proceedingTypeDisplayValue",
      source = "proceeding.proceedingType.displayValue")
  @Mapping(target = "proceedingDetails.proceedingDescription", source = "proceeding.description")
  @Mapping(target = "proceedingDetails.larScope", source = "proceeding.larScope")
  @Mapping(
      target = "furtherDetails.clientInvolvementType",
      source = "proceeding.clientInvolvement.id")
  @Mapping(
      target = "furtherDetails.clientInvolvementTypeDisplayValue",
      source = "proceeding.clientInvolvement.displayValue")
  @Mapping(target = "furtherDetails.levelOfService", source = "proceeding.levelOfService.id")
  @Mapping(
      target = "furtherDetails.levelOfServiceDisplayValue",
      source = "proceeding.levelOfService.displayValue")
  @Mapping(target = "furtherDetails.typeOfOrder", source = "proceeding.typeOfOrder.id")
  @Mapping(target = "furtherDetails.typeOfOrderDisplayValue", source = "typeOfOrderDisplayValue")
  @Mapping(target = "action", constant = "edit")
  @Mapping(target = "amended", constant = "false")
  @Mapping(target = "editingScopeLimitations", constant = "false")
  @Mapping(target = "existingProceedingId", source = "proceeding.id")
  @Mapping(target = "leadProceeding", source = "proceeding.leadProceedingInd")
  ProceedingFlowFormData toProceedingFlow(
      ProceedingDetail proceeding, String typeOfOrderDisplayValue);

  List<ScopeLimitationDetail> toScopeLimitationList(
      List<uk.gov.laa.ccms.data.model.ScopeLimitationDetail> scopeLimitationDetailList);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "scopeLimitation.id", source = "scopeLimitations")
  @Mapping(target = "scopeLimitation.displayValue", source = "description")
  @Mapping(target = "scopeLimitationWording", source = "defaultWording")
  @Mapping(target = "defaultInd", source = "defaultCode")
  @Mapping(target = "delegatedFuncApplyInd.flag", source = "emergencyScopeDefault")
  @Mapping(target = "nonDefaultWordingReqd", source = "nonStandardWordingRequired")
  @Mapping(target = "stage", source = "stage")
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "ebsId", ignore = true)
  ScopeLimitationDetail toScopeLimitation(
      uk.gov.laa.ccms.data.model.ScopeLimitationDetail scopeLimitation);

  @Mapping(target = "action", constant = "edit")
  @Mapping(target = "scopeLimitationId", source = "id")
  @Mapping(target = "scopeLimitationDetails.scopeLimitation", source = "scopeLimitation.id")
  @Mapping(target = "scopeLimitationIndex", ignore = true)
  ScopeLimitationFlowFormData toScopeLimitationFlow(ScopeLimitationDetail scopeLimitation);

  @Mapping(target = "requestedCostLimitation", source = "requestedCostLimitation")
  @Mapping(target = "grantedCostLimitation", source = "grantedCostLimitation")
  CostsFormData toCostsFormData(CostStructureDetail costLimitation);

  @Mapping(target = "defaultCostLimitation", ignore = true)
  @Mapping(target = "grantedCostLimitation", ignore = true)
  @Mapping(target = "costEntries", ignore = true)
  @Mapping(target = "currentProviderBilledAmount", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  void toCostStructure(
      @MappingTarget CostStructureDetail costStructure, CostsFormData costsFormData);

  @Mapping(target = "action", constant = "edit")
  @Mapping(target = "priorAuthorityId", source = "priorAuthority.id")
  @Mapping(
      target = "priorAuthorityTypeFormData.priorAuthorityType",
      source = "priorAuthority.type.id")
  @Mapping(
      target = "priorAuthorityTypeFormData.priorAuthorityTypeDisplayValue",
      source = "priorAuthority.type.displayValue")
  @Mapping(target = "priorAuthorityDetailsFormData.summary", source = "priorAuthority.summary")
  @Mapping(
      target = "priorAuthorityDetailsFormData.justification",
      source = "priorAuthority.justification")
  @Mapping(
      target = "priorAuthorityDetailsFormData.valueRequired",
      source = "priorAuthority.valueRequired")
  @Mapping(
      target = "priorAuthorityDetailsFormData.amountRequested",
      source = "priorAuthority.amountRequested")
  @Mapping(
      target = "priorAuthorityDetailsFormData.dynamicOptions",
      source = "priorAuthority.items",
      qualifiedByName = "toDynamicOptions")
  PriorAuthorityFlowFormData toPriorAuthorityFlowFormData(
      final PriorAuthorityDetail priorAuthority);

  /**
   * Converts ReferenceDataItemDetail list to a map with dynamic options.
   *
   * @param items the list to convert; returns null if this is null.
   * @return a map of dynamic options or null if items is null.
   */
  @Named("toDynamicOptions")
  default Map<String, DynamicOptionFormData> toDynamicOptions(
      final List<ReferenceDataItemDetail> items) {

    if (items != null) {
      return items.stream()
          .collect(
              Collectors.toMap(
                  item -> item.getCode().getId(),
                  item -> {
                    final DynamicOptionFormData option = new DynamicOptionFormData();
                    option.setFieldDescription(item.getCode().getDisplayValue());
                    option.setFieldType(item.getType());
                    option.setMandatory(item.getMandatory());
                    option.setFieldValue(item.getValue().getId());
                    option.setFieldValueDisplayValue(item.getValue().getDisplayValue());
                    return option;
                  }));
    }
    return null;
  }

  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "justification", ignore = true)
  @Mapping(target = "amountRequested", ignore = true)
  @Mapping(target = "dynamicOptions", ignore = true)
  @Mapping(
      target = "valueRequired",
      source = "priorAuthorityFlowFormData.priorAuthorityDetailsFormData.valueRequired")
  void toPriorAuthorityDetailsFormData(
      @MappingTarget PriorAuthorityDetailsFormData priorAuthorityDetails,
      PriorAuthorityFlowFormData priorAuthorityFlowFormData);

  /**
   * Maps dynamic options from one form data to another.
   *
   * @param priorAuthorityDetails the target details to update.
   * @param priorAuthorityFlowFormData the source form data containing options.
   */
  @AfterMapping
  default void mapDynamicOptions(
      @MappingTarget final PriorAuthorityDetailsFormData priorAuthorityDetails,
      final PriorAuthorityFlowFormData priorAuthorityFlowFormData) {

    if (priorAuthorityFlowFormData.getPriorAuthorityDetailsFormData().getDynamicOptions() != null) {
      priorAuthorityFlowFormData
          .getPriorAuthorityDetailsFormData()
          .getDynamicOptions()
          .forEach(
              (key, value) -> {
                if (priorAuthorityDetails.getDynamicOptions().containsKey(key)) {
                  priorAuthorityDetails
                      .getDynamicOptions()
                      .get(key)
                      .setMandatory(value.isMandatory());
                  priorAuthorityDetails
                      .getDynamicOptions()
                      .get(key)
                      .setFieldDescription(value.getFieldDescription());
                  priorAuthorityDetails
                      .getDynamicOptions()
                      .get(key)
                      .setFieldType(value.getFieldType());
                }
              });
    }
  }

  @Mapping(target = "mandatory", source = "mandatoryFlag")
  @Mapping(target = "fieldDescription", source = "description")
  @Mapping(target = "fieldType", source = "dataType")
  @Mapping(target = "fieldValue", ignore = true)
  @Mapping(target = "fieldValueDisplayValue", ignore = true)
  DynamicOptionFormData toPriorAuthorityFormDataDynamicOption(
      uk.gov.laa.ccms.data.model.PriorAuthorityDetail formOption);

  /**
   * Populates dynamic options in priorAuthorityDetailsFormData.
   *
   * @param priorAuthorityDetails the target to populate.
   * @param priorAuthorityTypeDetail the source of dynamic options.
   */
  @AfterMapping
  default void populatePriorAuthorityDetailsForm(
      @MappingTarget final PriorAuthorityDetailsFormData priorAuthorityDetails,
      final PriorAuthorityTypeDetail priorAuthorityTypeDetail) {

    for (final uk.gov.laa.ccms.data.model.PriorAuthorityDetail formOption :
        priorAuthorityTypeDetail.getPriorAuthorities()) {
      final DynamicOptionFormData dynamicOption = toPriorAuthorityFormDataDynamicOption(formOption);
      priorAuthorityDetails.getDynamicOptions().put(formOption.getCode(), dynamicOption);
    }
  }

  @Mapping(target = "id", source = "priorAuthorityFlowFormData.priorAuthorityId")
  @Mapping(
      target = "type.id",
      source = "priorAuthorityFlowFormData.priorAuthorityTypeFormData.priorAuthorityType")
  @Mapping(
      target = "type.displayValue",
      source =
          "priorAuthorityFlowFormData.priorAuthorityTypeFormData"
              + ".priorAuthorityTypeDisplayValue")
  @Mapping(
      target = "summary",
      source = "priorAuthorityFlowFormData.priorAuthorityDetailsFormData.summary")
  @Mapping(
      target = "justification",
      source = "priorAuthorityFlowFormData.priorAuthorityDetailsFormData.justification")
  @Mapping(
      target = "valueRequired",
      source = "priorAuthorityFlowFormData.priorAuthorityDetailsFormData.valueRequired")
  @Mapping(
      target = "amountRequested",
      source = "priorAuthorityFlowFormData.priorAuthorityDetailsFormData.amountRequested")
  @Mapping(target = "status", constant = "Draft")
  @Mapping(
      target = "items",
      expression =
          "java(toReferenceDataItems("
              + "priorAuthorityFlowFormData.getPriorAuthorityDetailsFormData().getDynamicOptions(),"
              + "priorAuthorityDynamicForm))")
  @Mapping(target = "ebsId", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  PriorAuthorityDetail toPriorAuthority(
      PriorAuthorityFlowFormData priorAuthorityFlowFormData,
      PriorAuthorityTypeDetail priorAuthorityDynamicForm);

  /**
   * Converts dynamic options map to ReferenceDataItemDetail list.
   *
   * @param dynamicOptionsMap the map of dynamic options.
   * @param priorAuthorityDynamicForm the form details for LOV updates.
   * @return the list of converted ReferenceDataItems.
   */
  default List<ReferenceDataItemDetail> toReferenceDataItems(
      final Map<String, DynamicOptionFormData> dynamicOptionsMap,
      final PriorAuthorityTypeDetail priorAuthorityDynamicForm) {

    final List<ReferenceDataItemDetail> referenceDataItems = new ArrayList<>();

    for (final Map.Entry<String, DynamicOptionFormData> entry : dynamicOptionsMap.entrySet()) {

      final ReferenceDataItemDetail referenceDataItem =
          toReferenceDataItem(entry.getKey(), entry.getValue());

      referenceDataItems.add(referenceDataItem);

      // find the lov value from the prior authority dynamic form
      priorAuthorityDynamicForm.getPriorAuthorities().stream()
          .filter(priorAuthorityDetail -> priorAuthorityDetail.getCode().equals(entry.getKey()))
          .findFirst()
          .ifPresent(
              priorAuthorityDetail ->
                  referenceDataItem.lovLookUp(priorAuthorityDetail.getLovCode()));
    }
    return referenceDataItems;
  }

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "code.id", source = "key")
  @Mapping(target = "code.displayValue", source = "dynamicOption.fieldDescription")
  @Mapping(target = "value.id", source = "dynamicOption.fieldValue")
  @Mapping(target = "value.displayValue", source = "dynamicOption.fieldValueDisplayValue")
  @Mapping(target = "type", source = "dynamicOption.fieldType")
  @Mapping(target = "mandatory", source = "dynamicOption.mandatory")
  @Mapping(target = "lovLookUp", ignore = true)
  ReferenceDataItemDetail toReferenceDataItem(
      final String key, final DynamicOptionFormData dynamicOption);
}
