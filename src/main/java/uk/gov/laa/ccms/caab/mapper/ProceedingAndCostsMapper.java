package uk.gov.laa.ccms.caab.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFlowFormData;
import uk.gov.laa.ccms.caab.model.Proceeding;
import uk.gov.laa.ccms.caab.model.ScopeLimitation;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetail;

/**
 * This interface provides methods for mapping between {@link ProceedingFlowFormData} and
 * {@link Proceeding} objects. It uses MapStruct for the mapping, with the Spring framework
 * providing the implementation at runtime.
 */
@Mapper(componentModel = "spring")
public interface ProceedingAndCostsMapper {


  @Mapping(target = "matterType.id",
      source = "proceedingFlowFormData.matterTypeDetails.matterType")
  @Mapping(target = "matterType.displayValue",
      source = "proceedingFlowFormData.matterTypeDetails.matterTypeDisplayValue")
  @Mapping(target = "proceedingType.id",
      source = "proceedingFlowFormData.proceedingDetails.proceedingType")
  @Mapping(target = "proceedingType.displayValue",
      source = "proceedingFlowFormData.proceedingDetails.proceedingTypeDisplayValue")
  @Mapping(target = "description",
      source = "proceedingFlowFormData.proceedingDetails.proceedingTypeDisplayValue")
  @Mapping(target = "larScope",
      source = "proceedingFlowFormData.proceedingDetails.larScope")
  @Mapping(target = "clientInvolvement.id",
      source = "proceedingFlowFormData.furtherDetails.clientInvolvementType")
  @Mapping(target = "clientInvolvement.displayValue",
      source = "proceedingFlowFormData.furtherDetails.clientInvolvementTypeDisplayValue")
  @Mapping(target = "levelOfService.id",
      source = "proceedingFlowFormData.furtherDetails.levelOfService")
  @Mapping(target = "levelOfService.displayValue",
      source = "proceedingFlowFormData.furtherDetails.levelOfServiceDisplayValue")
  @Mapping(target = "typeOfOrder.id",
      source = "proceedingFlowFormData.furtherDetails.typeOfOrder")
  @Mapping(target = "stage", source = "stage")
  @Mapping(target = "costLimitation", source = "costLimitation")
  @Mapping(target = "id", source = "proceedingFlowFormData.existingProceedingId")
  @Mapping(target = "typeOfOrder.displayValue", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  //used for mapping a new proceeding
  Proceeding toProceeding(
      ProceedingFlowFormData proceedingFlowFormData,
      BigDecimal costLimitation,
      String stage);

  @Mapping(target = "matterType.id",
      source = "proceedingFlowFormData.matterTypeDetails.matterType")
  @Mapping(target = "matterType.displayValue",
      source = "proceedingFlowFormData.matterTypeDetails.matterTypeDisplayValue")
  @Mapping(target = "proceedingType.id",
      source = "proceedingFlowFormData.proceedingDetails.proceedingType")
  @Mapping(target = "proceedingType.displayValue",
      source = "proceedingFlowFormData.proceedingDetails.proceedingTypeDisplayValue")
  @Mapping(target = "clientInvolvement.id",
      source = "proceedingFlowFormData.furtherDetails.clientInvolvementType")
  @Mapping(target = "clientInvolvement.displayValue",
      source = "proceedingFlowFormData.furtherDetails.clientInvolvementTypeDisplayValue")
  @Mapping(target = "levelOfService.id",
      source = "proceedingFlowFormData.furtherDetails.levelOfService")
  @Mapping(target = "levelOfService.displayValue",
      source = "proceedingFlowFormData.furtherDetails.levelOfServiceDisplayValue")
  @Mapping(target = "typeOfOrder.id",
      source = "proceedingFlowFormData.furtherDetails.typeOfOrder")
  @Mapping(target = "stage", source = "stage")
  @Mapping(target = "costLimitation", source = "costLimitation")
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "description", ignore = true)
  @Mapping(target = "larScope", ignore = true)
  @Mapping(target = "typeOfOrder.displayValue", ignore = true)
  //used for mapping an existing proceeding
  //notice that some fields are ignored
  void toProceeding(
      @MappingTarget Proceeding proceeding,
      ProceedingFlowFormData proceedingFlowFormData,
      BigDecimal costLimitation,
      String stage);

  @Mapping(target = "matterTypeDetails.matterType",
      source = "proceeding.matterType.id")
  @Mapping(target = "matterTypeDetails.matterTypeDisplayValue",
      source = "proceeding.matterType.displayValue")
  @Mapping(target = "proceedingDetails.proceedingType",
      source = "proceeding.proceedingType.id")
  @Mapping(target = "proceedingDetails.proceedingTypeDisplayValue",
      source = "proceeding.proceedingType.displayValue")
  @Mapping(target = "proceedingDetails.larScope",
      source = "proceeding.larScope")
  @Mapping(target = "furtherDetails.clientInvolvementType",
      source = "proceeding.clientInvolvement.id")
  @Mapping(target = "furtherDetails.clientInvolvementTypeDisplayValue",
      source = "proceeding.clientInvolvement.displayValue")
  @Mapping(target = "furtherDetails.levelOfService",
      source = "proceeding.levelOfService.id")
  @Mapping(target = "furtherDetails.levelOfServiceDisplayValue",
      source = "proceeding.levelOfService.displayValue")
  @Mapping(target = "furtherDetails.typeOfOrder",
      source = "proceeding.typeOfOrder.id")
  @Mapping(target = "furtherDetails.typeOfOrderDisplayValue",
      source = "typeOfOrderDisplayValue")
  @Mapping(target = "action", constant = "edit")
  @Mapping(target = "amended", constant = "false")
  @Mapping(target = "editingScopeLimitations", constant = "false")
  @Mapping(target = "existingProceedingId", source = "proceeding.id")
  @Mapping(target = "leadProceeding", source = "proceeding.leadProceedingInd")
  ProceedingFlowFormData toProceedingFlow(Proceeding proceeding, String typeOfOrderDisplayValue);

  List<ScopeLimitation> toScopeLimitationList(
      List<ScopeLimitationDetail> scopeLimitationDetailList);

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
  ScopeLimitation toScopeLimitation(ScopeLimitationDetail scopeLimitationDetail);

}
