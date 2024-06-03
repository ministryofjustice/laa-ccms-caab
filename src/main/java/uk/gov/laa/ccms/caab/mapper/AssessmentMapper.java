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
import static uk.gov.laa.ccms.caab.util.ApplicationUtil.getAppAmendTypeOpaInput;
import static uk.gov.laa.ccms.caab.util.ApplicationUtil.getEcfFlagOpaInput;
import static uk.gov.laa.ccms.caab.util.ApplicationUtil.getLarScopeFlagOpaInput;
import static uk.gov.laa.ccms.caab.util.ApplicationUtil.getNewApplicationOrAmendment;
import static uk.gov.laa.ccms.caab.util.ApplicationUtil.getProviderHasContractOpaInput;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getAssessmentEntityType;
import static uk.gov.laa.ccms.caab.util.OpponentUtil.getDisplayName;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRelationship;
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
          new ArrayList<>();
      for (final OpponentDetail opponent : context.getApplication().getOpponents()) {
        final AssessmentRelationshipTargetDetail relationshipTarget =
            new AssessmentRelationshipTargetDetail()
                .targetEntityId(getOpponentOpaInstanceMappingId(opponent));
        opponentRelationshipTargets.add(relationshipTarget);
      }
      final AssessmentRelationshipDetail opponentRelationship =
          new AssessmentRelationshipDetail()
          .name(OPPONENT.getType().toLowerCase().replace("_", ""))
          .relationshipTargets(opponentRelationshipTargets)
          .prepopulated(true);

      //proceeding relationships
      final List<AssessmentRelationshipTargetDetail> proceedingRelationshipTargets =
          new ArrayList<>();
      for (final ProceedingDetail proceeding : context.getApplication().getProceedings()) {
        final AssessmentRelationshipTargetDetail relationshipTarget =
            new AssessmentRelationshipTargetDetail()
                .targetEntityId(getProceedingOpaInstanceMappingId(proceeding));
        proceedingRelationshipTargets.add(relationshipTarget);
      }
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
        toAttribute(CLIENT_INVOLVEMENT_TYPE,
            proceeding.getClientInvolvement().getId()),
        toAttribute(LEAD_PROCEEDING,
            proceeding.getLeadProceedingInd()),
        toAttribute(LEVEL_OF_SERVICE,
            proceeding.getLevelOfService().getId()),
        toAttribute(MATTER_TYPE,
            proceeding.getMatterType().getId()),
        toAttribute(NEW_OR_EXISTING,
            ProceedingUtil.getNewOrExisting(proceeding)),
        toAttribute(PROCEEDING_ID,
            ProceedingUtil.getOpaInstanceMappingId(proceeding)),
        toAttribute(PROCEEDING_NAME,
            proceeding.getProceedingType().getId()),
        toAttribute(PROCEEDING_ORDER_TYPE,
            proceeding.getTypeOfOrder().getId()),
        toAttribute(REQUESTED_SCOPE,
            ProceedingUtil.getRequestedScopeForAssessmentInput(proceeding)),
        toAttribute(SCOPE_LIMIT_IS_DEFAULT,
            ProceedingUtil.isScopeLimitDefault(proceeding)));
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
        toAttribute(OPPONENT_DOB,
            opponent.getDateOfBirth()),
        toAttribute(OTHER_PARTY_ID,
            getOpponentOpaInstanceMappingId(opponent)),
        toAttribute(OTHER_PARTY_NAME,
            getDisplayName(opponent,
                opponentContext.getTitleCommonLookupValue())),
        toAttribute(OTHER_PARTY_TYPE,
            OpponentUtil.getOpaOpponentType(opponent.getType())),
        toAttribute(RELATIONSHIP_TO_CASE,
            opponent.getRelationshipToCase()),
        toAttribute(RELATIONSHIP_TO_CLIENT,
            opponent.getRelationshipToClient())
    );
  }

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
        toAttribute(APPLICATION_CASE_REF,
            application.getCaseReferenceNumber()),
        toAttribute(APP_AMEND_TYPE,
            getAppAmendTypeOpaInput(application)),
        toAttribute(CATEGORY_OF_LAW,
            application.getCategoryOfLaw().getId()),
        toAttribute(CLIENT_VULNERABLE,
            client.getPersonalInformation().isVulnerableClient()),
        toAttribute(COST_LIMIT_CHANGED_FLAG,
            application.getCostLimit().getChanged()),
        toAttribute(COUNTRY,
            client.getAddress().getCountry()),
        toAttribute(COUNTY,
            client.getAddress().getCounty()),

        //When the mapper is used the created date will match that of the assessment
        toAttribute(DATE_ASSESSMENT_STARTED,
            new Date()),

        toAttribute(DATE_OF_BIRTH,
            client.getPersonalInformation().getDateOfBirth()),
        toAttribute(DEFAULT_COST_LIMITATION,
            application.getCosts().getDefaultCostLimitation()),
        toAttribute(DELEGATED_FUNCTIONS_DATE,
            application.getApplicationType().getDevolvedPowers().getDateUsed()),
        toAttribute(DEVOLVED_POWERS_CONTRACT_FLAG,
            application.getApplicationType().getDevolvedPowers().getContractFlag()),
        toAttribute(ECF_FLAG,
            getEcfFlagOpaInput(application)),
        toAttribute(FIRST_NAME,
            client.getName().getFirstName()),
        toAttribute(HIGH_PROFILE,
            client.getPersonalInformation().isHighProfileClient()),
        toAttribute(HOME_OFFICE_NO,
            client.getPersonalInformation().getHomeOfficeNumber()),
        toAttribute(LAR_SCOPE_FLAG,
            getLarScopeFlagOpaInput(application)),
        toAttribute(LEAD_PROCEEDING_CHANGED,
            application.getLeadProceedingChanged()),
        toAttribute(MARITIAL_STATUS,
            client.getPersonalInformation().getMaritalStatus()),
        toAttribute(NEW_APPL_OR_AMENDMENT,
            getNewApplicationOrAmendment(application)),
        toAttribute(NI_NO,
            client.getPersonalInformation().getNationalInsuranceNumber()),
        toAttribute(POA_OR_BILL_FLAG, "N/A"),
        toAttribute(POST_CODE,
            client.getAddress().getPostalCode()),
        toAttribute(PROVIDER_CASE_REFERENCE,
            application.getProviderDetails().getProviderCaseReference()),
        toAttribute(PROVIDER_HAS_CONTRACT,
            getProviderHasContractOpaInput(application)),
        toAttribute(REQ_COST_LIMITATION,
            application.getCosts().getRequestedCostLimitation()),
        toAttribute(SURNAME,
            client.getName().getSurname()),
        toAttribute(SURNAME_AT_BIRTH,
            client.getName().getSurnameAtBirth()),
        toAttribute(USER_PROVIDER_FIRM_ID,
            user.getProvider().getId()),
        toAttribute(USER_TYPE,
            user.getUserType())
    );
  }

  @Named("getOpponentOpaInstanceMappingId")
  default String getOpponentOpaInstanceMappingId(final OpponentDetail opponent) {
    return OpponentUtil.getOpaInstanceMappingId(opponent);
  }

  @Named("getProceedingOpaInstanceMappingId")
  default String getProceedingOpaInstanceMappingId(final ProceedingDetail proceeding) {
    return ProceedingUtil.getOpaInstanceMappingId(proceeding);
  }

  /**
   * Converts the given assessment attribute and value to an {@link AssessmentAttributeDetail}.
   *
   * @param attribute the assessment attribute to convert
   * @param value the value of the attribute
   * @return the converted {@link AssessmentAttributeDetail}
   */
  default AssessmentAttributeDetail toAttribute(
      final AssessmentAttribute attribute,
      final Object value) {
    final String valueString = switch (value) {
      case final Date date
          when attribute.getType().equals("date") ->
          new SimpleDateFormat("dd-MM-yyyy").format(date);
      case final Boolean b
          when attribute.getType().equals("boolean") -> b ? "true" : "false";
      case final BigDecimal bigDecimal
          when attribute.getType().equals("currency") -> value.toString();
      case final Integer i
          when attribute.getType().equals("number") -> value.toString();
      case null,
          default -> value != null ? value.toString() : null;
    };

    return new AssessmentAttributeDetail()
        .name(attribute.getName())
        .type(attribute.getType())
        .value(valueString)
        .prepopulated(attribute.isPrepopulated())
        .asked(attribute.isAsked());
  }




}
