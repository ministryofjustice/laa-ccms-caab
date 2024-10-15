package uk.gov.laa.ccms.caab.mapper;

import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.APPLICATION_CASE_REF;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.APP_AMEND_TYPE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.CATEGORY_OF_LAW;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.CLIENT_INVOLVEMENT_TYPE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.CLIENT_VULNERABLE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.COST_LIMIT_CHANGED_FLAG;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.COUNTRY;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.COUNTY;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.DATE_ASSESSMENT_STARTED;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.DATE_OF_BIRTH;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.DEFAULT_COST_LIMITATION;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.DELEGATED_FUNCTIONS_DATE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.DEVOLVED_POWERS_CONTRACT_FLAG;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.ECF_FLAG;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.FIRST_NAME;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.HIGH_PROFILE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.HOME_OFFICE_NO;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.LAR_SCOPE_FLAG;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.LEAD_PROCEEDING;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.LEAD_PROCEEDING_CHANGED;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.LEVEL_OF_SERVICE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.MARITIAL_STATUS;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.MATTER_TYPE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.NEW_APPL_OR_AMENDMENT;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.NEW_OR_EXISTING;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.NI_NO;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.OPPONENT_DOB;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.OTHER_PARTY_ID;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.OTHER_PARTY_NAME;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.OTHER_PARTY_TYPE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.POA_OR_BILL_FLAG;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.POST_CODE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.PROCEEDING_ID;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.PROCEEDING_NAME;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.PROCEEDING_ORDER_TYPE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.PROVIDER_CASE_REFERENCE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.PROVIDER_HAS_CONTRACT;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.RELATIONSHIP_TO_CASE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.RELATIONSHIP_TO_CLIENT;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.REQUESTED_SCOPE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.REQ_COST_LIMITATION;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.SCOPE_LIMIT_IS_DEFAULT;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.SURNAME;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.SURNAME_AT_BIRTH;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.USER_PROVIDER_FIRM_ID;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.USER_TYPE;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType.GLOBAL;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType.OPPONENT;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType.PROCEEDING;
import static uk.gov.laa.ccms.caab.util.ApplicationUtil.getAppAmendTypeAssessmentInput;
import static uk.gov.laa.ccms.caab.util.ApplicationUtil.getEcfFlagAssessmentInput;
import static uk.gov.laa.ccms.caab.util.ApplicationUtil.getLarScopeFlagAssessmentInput;
import static uk.gov.laa.ccms.caab.util.ApplicationUtil.getNewApplicationOrAmendment;
import static uk.gov.laa.ccms.caab.util.ApplicationUtil.getProviderHasContractAssessmentInput;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getAssessmentEntityType;
import static uk.gov.laa.ccms.caab.util.OpponentUtil.getPartyName;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentRelationshipDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentRelationshipTargetDetail;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute;
import uk.gov.laa.ccms.caab.mapper.context.AssessmentMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.AssessmentOpponentMappingContext;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.util.OpponentUtil;
import uk.gov.laa.ccms.caab.util.ProceedingUtil;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;

/**
 * Mapper for converting assessment mapping contexts to assessment details.
 */
@Mapper(componentModel = "spring")
public interface AssessmentMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "name", ignore = true)
  @Mapping(target = "providerId", ignore = true)
  @Mapping(target = "caseReferenceNumber", ignore = true)
  @Mapping(target = "checkpoint", ignore = true)
  @Mapping(target = "auditDetail", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "entityTypes", source = "context",
      qualifiedByName = "toAssessmentEntityTypeList")
  void toAssessmentDetail(
      @MappingTarget AssessmentDetail assessment,
      AssessmentMappingContext context);

  /**
   * Converts the given assessment mapping context to a list of
   * {@link AssessmentEntityTypeDetail}.
   *
   * @param context the assessment mapping context containing application, client, and user details
   * @return a list of assessment entity type details
   */
  @Named("toAssessmentEntityTypeList")
  default List<AssessmentEntityTypeDetail> toAssessmentEntityTypeList(
      final AssessmentMappingContext context) {

    if (context == null) {
      return null;
    }

    return List.of(
        toAssessmentEntityTypeDetailGlobal(context),
        toAssessmentEntityTypeDetailProceeding(context),
        toAssessmentEntityTypeDetailOpponent(context)
    );
  }

  /**
   * Converts the global context within the given assessment mapping context to an
   * {@link AssessmentEntityTypeDetail}.
   *
   * @param context the assessment mapping context containing application, client, and user details
   * @return the assessment entity type detail for global entities
   */
  default AssessmentEntityTypeDetail toAssessmentEntityTypeDetailGlobal(
      final AssessmentMappingContext context) {

    if (context == null) {
      return null;
    }

    final AssessmentEntityTypeDetail existingEntityType =
        getAssessmentEntityType(context.getAssessment(), GLOBAL);

    if (existingEntityType != null) {
      return existingEntityType;
    } else {

      //opponent relationships
      final List<AssessmentRelationshipTargetDetail> opponentRelationshipTargets =
          context.getApplication().getOpponents().stream()
              .map(opponent -> new AssessmentRelationshipTargetDetail()
                  .targetEntityId(getOpponentOpaInstanceMappingId(opponent)))
              .collect(Collectors.toList());

      final AssessmentRelationshipDetail opponentRelationship = new AssessmentRelationshipDetail()
          .name(OPPONENT.getType().toLowerCase().replace("_", ""))
          .relationshipTargets(opponentRelationshipTargets)
          .prepopulated(true);


      //proceeding relationships
      final List<AssessmentRelationshipTargetDetail> proceedingRelationshipTargets =
          context.getApplication().getProceedings().stream()
              .map(proceeding -> new AssessmentRelationshipTargetDetail()
                  .targetEntityId(getProceedingOpaInstanceMappingId(proceeding)))
              .collect(Collectors.toList());

      final AssessmentRelationshipDetail proceedingRelationship = new AssessmentRelationshipDetail()
          .name(PROCEEDING.getType().toLowerCase())
          .relationshipTargets(proceedingRelationshipTargets)
          .prepopulated(true);

      //global entities
      final List<AssessmentEntityDetail> globalEntityList = List.of(new AssessmentEntityDetail()
          .name(context.getApplication().getCaseReferenceNumber())
          .attributes(globalToAttributeList(context))
          .relations(List.of(
              opponentRelationship,
              proceedingRelationship))
          .prepopulated(false));

      return new AssessmentEntityTypeDetail()
          .name(GLOBAL.getType())
          .entities(globalEntityList);
    }
  }

  /**
   * Converts the proceeding context within the given assessment mapping context to an
   * {@link AssessmentEntityTypeDetail}.
   *
   * @param context the assessment mapping context containing proceeding details
   * @return the assessment entity type detail for proceedings
   */
  default AssessmentEntityTypeDetail toAssessmentEntityTypeDetailProceeding(
      final AssessmentMappingContext context) {

    if (context == null) {
      return null;
    }

    final List<ProceedingDetail> proceedings = context.getApplication().getProceedings();

    final AssessmentEntityTypeDetail existingEntityType =
        getAssessmentEntityType(context.getAssessment(), PROCEEDING);

    if (existingEntityType != null) {
      return existingEntityType;
    } else {
      final List<AssessmentEntityDetail> proceedingEntityList = new ArrayList<>();

      if (proceedings != null && !proceedings.isEmpty()) {
        proceedingEntityList.addAll(
            toAssessmentEntityDetailListProceeding(proceedings));
      }

      return new AssessmentEntityTypeDetail()
          .name(PROCEEDING.getType())
          .entities(proceedingEntityList);
    }
  }

  /**
   * Converts the opponent context within the given assessment mapping context to an
   * {@link AssessmentEntityTypeDetail}.
   *
   * @param context the assessment mapping context containing opponent contexts
   * @return the assessment entity type detail for opponents
   */
  default AssessmentEntityTypeDetail toAssessmentEntityTypeDetailOpponent(
      final AssessmentMappingContext context) {

    if (context == null) {
      return null;
    }

    final List<AssessmentOpponentMappingContext> opponentContextList =
        context.getOpponentContext();

    final AssessmentEntityTypeDetail existingEntityType =
        getAssessmentEntityType(context.getAssessment(), OPPONENT);

    if (existingEntityType != null) {
      return existingEntityType;
    } else {
      final List<AssessmentEntityDetail> opponentsEntityList = new ArrayList<>();

      if (opponentContextList != null && !opponentContextList.isEmpty()) {
        opponentsEntityList.addAll(
            toAssessmentEntityDetailListOpponent(opponentContextList));
      }

      return new AssessmentEntityTypeDetail()
          .name(OPPONENT.getType())
          .entities(opponentsEntityList);
    }
  }

  List<AssessmentEntityDetail> toAssessmentEntityDetailListProceeding(
      final List<ProceedingDetail> proceedings);


  /**
   * Converts proceeding data to a list of {@link AssessmentAttributeDetail} objects.
   *
   * @param proceeding the proceeding detail containing various attributes
   * @return a list of assessment attribute details for the proceeding
   */
  @Named("proceedingToAttributeList")
  default List<AssessmentAttributeDetail> proceedingToAttributeList(
      final ProceedingDetail proceeding) {
    return List.of(
        toClientInvolvementTypeAttribute(proceeding, CLIENT_INVOLVEMENT_TYPE),
        toLeadProceedingAttribute(proceeding, LEAD_PROCEEDING),
        toLevelOfServiceAttribute(proceeding, LEVEL_OF_SERVICE),
        toMatterTypeAttribute(proceeding, MATTER_TYPE),
        toNewOrExistingAttribute(proceeding, NEW_OR_EXISTING),
        toProceedingIdAttribute(proceeding, PROCEEDING_ID),
        toProceedingNameAttribute(proceeding, PROCEEDING_NAME),
        toProceedingOrderTypeAttribute(proceeding, PROCEEDING_ORDER_TYPE),
        toRequestedScopeAttribute(proceeding, REQUESTED_SCOPE),
        toScopeLimitIsDefaultAttribute(proceeding, SCOPE_LIMIT_IS_DEFAULT));
  }

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "proceeding.clientInvolvement.id")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toClientInvolvementTypeAttribute(
      ProceedingDetail proceeding, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "proceeding.leadProceedingInd")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toLeadProceedingAttribute(
      ProceedingDetail proceeding, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "proceeding.levelOfService.id")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toLevelOfServiceAttribute(
      ProceedingDetail proceeding, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "proceeding.matterType.id")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toMatterTypeAttribute(
      ProceedingDetail proceeding, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "proceeding",
      qualifiedByName = "mapNewOrExistingAttribute")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toNewOrExistingAttribute(
      ProceedingDetail proceeding, AssessmentAttribute attribute);

  @Named("mapNewOrExistingAttribute")
  default String mapNewOrExistingAttribute(final ProceedingDetail proceeding) {
    return ProceedingUtil.getNewOrExisting(proceeding);
  }

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "proceeding",
      qualifiedByName = "mapProceedingIdAttribute")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toProceedingIdAttribute(
      ProceedingDetail proceeding, AssessmentAttribute attribute);

  @Named("mapProceedingIdAttribute")
  default String mapProceedingIdAttribute(
      final ProceedingDetail proceeding) {
    return ProceedingUtil.getAssessmentMappingId(proceeding);
  }

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "proceeding.proceedingType.id")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toProceedingNameAttribute(
      ProceedingDetail proceeding, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "proceeding.typeOfOrder.id")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toProceedingOrderTypeAttribute(
      ProceedingDetail proceeding, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "proceeding",
      qualifiedByName = "mapRequestedScopeAttribute")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toRequestedScopeAttribute(
      ProceedingDetail proceeding, AssessmentAttribute attribute);

  @Named("mapRequestedScopeAttribute")
  default String mapRequestedScopeAttribute(
      final ProceedingDetail proceeding) {
    return ProceedingUtil.getRequestedScopeForAssessmentInput(proceeding);
  }

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "proceeding",
      qualifiedByName = "mapScopeLimitIsDefaultAttribute")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toScopeLimitIsDefaultAttribute(
      ProceedingDetail proceeding, AssessmentAttribute attribute);

  @Named("mapScopeLimitIsDefaultAttribute")
  default boolean mapScopeLimitIsDefaultAttribute(
      final ProceedingDetail proceeding) {
    return ProceedingUtil.isScopeLimitDefault(proceeding);
  }

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "name", source = "proceeding",
      qualifiedByName = "getProceedingOpaInstanceMappingId")
  @Mapping(target = "attributes", source = "proceeding",
      qualifiedByName = "proceedingToAttributeList")
  @Mapping(target = "prepopulated", constant = "true")
  @Mapping(target = "relations", ignore = true)
  @Mapping(target = "completed", ignore = true)
  AssessmentEntityDetail toAssessmentEntityDetail(ProceedingDetail proceeding);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "name", source = "opponentContext.opponent",
      qualifiedByName = "getOpponentOpaInstanceMappingId")
  @Mapping(target = "attributes", source = "opponentContext",
      qualifiedByName = "opponentToAttributeList")
  @Mapping(target = "prepopulated", constant = "true")
  @Mapping(target = "relations", ignore = true)
  @Mapping(target = "completed", ignore = true)
  AssessmentEntityDetail toAssessmentEntityDetail(AssessmentOpponentMappingContext opponentContext);

  /**
   * Converts a list of opponent mapping contexts to a list of {@link AssessmentEntityDetail}
   * objects.
   *
   * @param opponentContextList the list of opponent mapping contexts
   * @return a list of assessment entity details, or {@code null} if the input list is null
   */
  default List<AssessmentEntityDetail> toAssessmentEntityDetailListOpponent(
      final List<AssessmentOpponentMappingContext> opponentContextList) {

    if (opponentContextList == null) {
      return null;
    }

    final List<AssessmentEntityDetail> list = new ArrayList<>(opponentContextList.size());

    for (final AssessmentOpponentMappingContext opponentContext : opponentContextList) {
      list.add(toAssessmentEntityDetail(opponentContext));
    }

    return list;
  }

  /**
   * Converts opponent data to a list of {@link AssessmentAttributeDetail} objects.
   *
   * @param opponentContext the opponent mapping context containing opponent details
   * @return a list of assessment attribute details for the opponent
   */
  @Named("opponentToAttributeList")
  default List<AssessmentAttributeDetail> opponentToAttributeList(
      final AssessmentOpponentMappingContext opponentContext) {

    final OpponentDetail opponent = opponentContext.getOpponent();
    return List.of(
        toOpponentDobAttribute(opponent, OPPONENT_DOB),
        toOtherPartyIdAttribute(opponent, OTHER_PARTY_ID),
        toOtherPartyNameAttribute(opponentContext, OTHER_PARTY_NAME),
        toOtherPartyTypeAttribute(opponent, OTHER_PARTY_TYPE),
        toRelationshipToCaseAttribute(opponent, RELATIONSHIP_TO_CASE),
        toRelationshipToClientAttribute(opponent, RELATIONSHIP_TO_CLIENT)
    );
  }

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "opponent.dateOfBirth", dateFormat = "dd-MM-yyyy")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toOpponentDobAttribute(
      OpponentDetail opponent, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "opponent",
      qualifiedByName = "mapOpponentOpaInstanceMappingId")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toOtherPartyIdAttribute(
      OpponentDetail opponent, AssessmentAttribute attribute);

  @Named("mapOpponentOpaInstanceMappingId")
  default String mapOpponentOpaInstanceMappingId(final OpponentDetail opponent) {
    return getOpponentOpaInstanceMappingId(opponent);
  }

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "opponentContext", qualifiedByName = "mapPartyName")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toOtherPartyNameAttribute(
      AssessmentOpponentMappingContext opponentContext, AssessmentAttribute attribute);

  @Named("mapPartyName")
  default String mapPartyName(final AssessmentOpponentMappingContext opponentContext) {
    return getPartyName(opponentContext.getOpponent(), opponentContext.getTitleCommonLookupValue());
  }

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "opponent", qualifiedByName = "mapOpponentType")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toOtherPartyTypeAttribute(
      OpponentDetail opponent, AssessmentAttribute attribute);

  @Named("mapOpponentType")
  default String mapOpponentType(final OpponentDetail opponent) {
    return OpponentUtil.getAssessmentOpponentType(opponent.getType());
  }

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "opponent.relationshipToCase")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toRelationshipToCaseAttribute(
      OpponentDetail opponent, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "opponent.relationshipToClient")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toRelationshipToClientAttribute(
      OpponentDetail opponent, AssessmentAttribute attribute);

  /**
   * Converts global assessment data to a list of {@link AssessmentAttributeDetail} objects.
   *
   * @param context the assessment mapping context containing application, client, and user details
   * @return a list of assessment attribute details
   */
  @Named("globalToAttributeList")
  default List<AssessmentAttributeDetail> globalToAttributeList(
      final AssessmentMappingContext context) {

    final ApplicationDetail application = context.getApplication();
    final ClientDetailDetails client = context.getClient().getDetails();
    final UserDetail user = context.getUser();

    return List.of(
        toApplicationCaseRefAttribute(application, APPLICATION_CASE_REF),
        toAppAmendTypeAttribute(application, APP_AMEND_TYPE),
        toCategoryOfLawAttribute(application, CATEGORY_OF_LAW),
        toClientVulnerableAttribute(client, CLIENT_VULNERABLE),
        toCostLimitChangedFlagAttribute(application, COST_LIMIT_CHANGED_FLAG),
        toCountryAttribute(client, COUNTRY),
        toCountyAttribute(client, COUNTY),
        toDateAssessmentStartedAttribute(DATE_ASSESSMENT_STARTED),
        toDateOfBirthAttribute(client, DATE_OF_BIRTH),
        toDefaultCostLimitationAttribute(application, DEFAULT_COST_LIMITATION),
        toDelegatedFunctionsDateAttribute(application, DELEGATED_FUNCTIONS_DATE),
        toDevolvedPowersContractFlagAttribute(application, DEVOLVED_POWERS_CONTRACT_FLAG),
        toEcfFlagAttribute(application, ECF_FLAG),
        toFirstNameAttribute(client, FIRST_NAME),
        toHighProfileAttribute(client, HIGH_PROFILE),
        toHomeOfficeNoAttribute(client, HOME_OFFICE_NO),
        toLarScopeFlagAttribute(application, LAR_SCOPE_FLAG),
        toLeadProceedingChangedAttribute(application, LEAD_PROCEEDING_CHANGED),
        toMaritalStatusAttribute(client, MARITIAL_STATUS),
        toNewApplOrAmendmentAttribute(application, NEW_APPL_OR_AMENDMENT),
        toNiNoAttribute(client, NI_NO),
        toPoaOrBillFlagAttribute(POA_OR_BILL_FLAG),
        toPostCodeAttribute(client, POST_CODE),
        toProviderCaseReferenceAttribute(application, PROVIDER_CASE_REFERENCE),
        toProviderHasContractAttribute(application, PROVIDER_HAS_CONTRACT),
        toReqCostLimitationAttribute(application, REQ_COST_LIMITATION),
        toSurnameAttribute(client, SURNAME),
        toSurnameAtBirthAttribute(client, SURNAME_AT_BIRTH),
        toUserProviderFirmIdAttribute(user, USER_PROVIDER_FIRM_ID),
        toUserTypeAttribute(user, USER_TYPE)
    );
  }

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "application.caseReferenceNumber")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toApplicationCaseRefAttribute(
      ApplicationDetail application, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "application", qualifiedByName = "mapAppAmendType")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toAppAmendTypeAttribute(
      ApplicationDetail application, AssessmentAttribute attribute);

  @Named("mapAppAmendType")
  default String mapAppAmendType(final ApplicationDetail application) {
    return getAppAmendTypeAssessmentInput(application);
  }

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "application.categoryOfLaw.id")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toCategoryOfLawAttribute(
      ApplicationDetail application, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "client.personalInformation.vulnerableClient")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toClientVulnerableAttribute(
      ClientDetailDetails client, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "application.costLimit.changed")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toCostLimitChangedFlagAttribute(
      ApplicationDetail application, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "client.address.country")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toCountryAttribute(
      ClientDetailDetails client, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "client.address.county")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toCountyAttribute(
      ClientDetailDetails client, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "attribute",
      qualifiedByName = "mapDateAssessmentStarted",
      dateFormat = "dd-MM-yyyy")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toDateAssessmentStartedAttribute(AssessmentAttribute attribute);

  @Named("mapDateAssessmentStarted")
  default Date mapDateAssessmentStarted(final AssessmentAttribute attribute) {
    return new Date();
  }

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value",
      source = "client.personalInformation.dateOfBirth", dateFormat = "dd-MM-yyyy")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toDateOfBirthAttribute(
      ClientDetailDetails client, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "application.costs.defaultCostLimitation")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toDefaultCostLimitationAttribute(
      ApplicationDetail application, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value",
      source = "application.applicationType.devolvedPowers.dateUsed", dateFormat = "dd-MM-yyyy")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toDelegatedFunctionsDateAttribute(
      ApplicationDetail application, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "application.applicationType.devolvedPowers.contractFlag")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toDevolvedPowersContractFlagAttribute(
      ApplicationDetail application, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "application", qualifiedByName = "mapEcfFlag")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toEcfFlagAttribute(
      ApplicationDetail application, AssessmentAttribute attribute);

  @Named("mapEcfFlag")
  default boolean mapEcfFlag(final ApplicationDetail application) {
    return getEcfFlagAssessmentInput(application);
  }

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "client.name.firstName")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toFirstNameAttribute(
      ClientDetailDetails client, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "client.personalInformation.highProfileClient")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toHighProfileAttribute(
      ClientDetailDetails client, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "client.personalInformation.homeOfficeNumber")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toHomeOfficeNoAttribute(
      ClientDetailDetails client, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "application", qualifiedByName = "mapLarScopeFlag")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toLarScopeFlagAttribute(
      ApplicationDetail application, AssessmentAttribute attribute);

  @Named("mapLarScopeFlag")
  default boolean mapLarScopeFlag(final ApplicationDetail application) {
    return getLarScopeFlagAssessmentInput(application);
  }

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "application.leadProceedingChanged")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toLeadProceedingChangedAttribute(
      ApplicationDetail application, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "client.personalInformation.maritalStatus")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toMaritalStatusAttribute(
      ClientDetailDetails client, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "application", qualifiedByName = "mapNewApplOrAmendment")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toNewApplOrAmendmentAttribute(
      ApplicationDetail application, AssessmentAttribute attribute);

  @Named("mapNewApplOrAmendment")
  default String mapNewApplOrAmendment(final ApplicationDetail application) {
    return getNewApplicationOrAmendment(application);
  }

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "client.personalInformation.nationalInsuranceNumber")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toNiNoAttribute(
      ClientDetailDetails client, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", constant = "N/A")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toPoaOrBillFlagAttribute(
      AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "client.address.postalCode")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toPostCodeAttribute(
      ClientDetailDetails client, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "application.providerDetails.providerCaseReference")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toProviderCaseReferenceAttribute(
      ApplicationDetail application, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "application", qualifiedByName = "mapProviderHasContract")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toProviderHasContractAttribute(
      ApplicationDetail application, AssessmentAttribute attribute);

  @Named("mapProviderHasContract")
  default boolean mapProviderHasContract(final ApplicationDetail application) {
    return getProviderHasContractAssessmentInput(application);
  }

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "application.costs.requestedCostLimitation")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toReqCostLimitationAttribute(
      ApplicationDetail application, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "client.name.surname")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toSurnameAttribute(
      ClientDetailDetails client, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "client.name.surnameAtBirth")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toSurnameAtBirthAttribute(
      ClientDetailDetails client, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "user.provider.id")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toUserProviderFirmIdAttribute(
      UserDetail user, AssessmentAttribute attribute);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "value", source = "user.userType")
  @Mapping(target = "name", source = "attribute")
  @Mapping(target = "type", source = "attribute.type")
  @Mapping(target = "inferencingType", ignore = true)
  AssessmentAttributeDetail toUserTypeAttribute(
      UserDetail user, AssessmentAttribute attribute);

  @Named("getOpponentOpaInstanceMappingId")
  default String getOpponentOpaInstanceMappingId(final OpponentDetail opponent) {
    return OpponentUtil.getAssessmentMappingId(opponent);
  }

  @Named("getProceedingOpaInstanceMappingId")
  default String getProceedingOpaInstanceMappingId(final ProceedingDetail proceeding) {
    return ProceedingUtil.getAssessmentMappingId(proceeding);
  }

}
