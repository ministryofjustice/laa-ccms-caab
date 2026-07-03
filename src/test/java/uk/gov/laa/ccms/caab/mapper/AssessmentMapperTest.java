package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildApplicationDetail;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildOpponent;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildProceeding;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildClientDetail;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentRelationshipDetail;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute;
import uk.gov.laa.ccms.caab.mapper.context.AssessmentMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.AssessmentOpponentMappingContext;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.LinkedCaseDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

class AssessmentMapperTest {

  final String dateStr = new SimpleDateFormat("dd-MM-yyyy").format(new Date());

  private final AssessmentMapper assessmentMapper = new AssessmentMapperImpl() {};

  private AssessmentMappingContext context;

  @BeforeEach
  public void setUp() {
    final AssessmentOpponentMappingContext opponentMappingContext =
        AssessmentOpponentMappingContext.builder()
            .opponent(buildOpponent(new Date()))
            .titleCommonLookupValue(new CommonLookupValueDetail().code("MR").description("Mr"))
            .build();

    final ApplicationDetail application = buildApplicationDetail(1234567890, true, new Date());
    final ClientDetail client = buildClientDetail();
    final UserDetail user = buildUserDetail();

    this.context =
        AssessmentMappingContext.builder()
            .application(application)
            .user(user)
            .client(client)
            .opponentContext(List.of(opponentMappingContext))
            .build();
  }

  @Test
  public void testToAssessmentEntityTypeList_whenContextIsNull() {
    final List<AssessmentEntityTypeDetail> result =
        assessmentMapper.toAssessmentEntityTypeList(null);
    assertNull(result);
  }

  @Test
  void testToAssessmentEntityTypeList_whenContextIsNotNull() {
    final List<AssessmentEntityTypeDetail> result =
        assessmentMapper.toAssessmentEntityTypeList(context);
    assertEquals(4, result.size());
  }

  @Test
  void toAssessmentEntityTypeList_buildsLinkedCaseEntityAndRelationship() {
    final LinkedCaseDetail linkedCase = new LinkedCaseDetail();
    linkedCase.setLscCaseReference("LINK-1");
    context.getApplication().setLinkedCases(List.of(linkedCase));

    final List<AssessmentEntityTypeDetail> result =
        assessmentMapper.toAssessmentEntityTypeList(context);

    // the LINKED_CASES entity type carries an instance per linked case, keyed by its case reference
    final AssessmentEntityTypeDetail linkedCaseType =
        result.stream()
            .filter(entityType -> "LINKED_CASES".equals(entityType.getName()))
            .findFirst()
            .orElseThrow();
    assertEquals(1, linkedCaseType.getEntities().size());
    final AssessmentEntityDetail linkedCaseEntity = linkedCaseType.getEntities().get(0);
    assertEquals("LINK-1", linkedCaseEntity.getName());
    assertContainsAttribute(
        linkedCaseEntity.getAttributes(), AssessmentAttribute.LINKED_CASE_ID, "LINK-1");

    // the global entity declares the linkedcases containment relationship to it
    final AssessmentEntityDetail globalEntity =
        result.stream()
            .filter(entityType -> "global".equals(entityType.getName()))
            .findFirst()
            .orElseThrow()
            .getEntities()
            .get(0);
    final AssessmentRelationshipDetail linkedCaseRelationship =
        globalEntity.getRelations().stream()
            .filter(relationship -> "linkedcases".equals(relationship.getName()))
            .findFirst()
            .orElseThrow();
    assertEquals(1, linkedCaseRelationship.getRelationshipTargets().size());
    assertEquals(
        "LINK-1", linkedCaseRelationship.getRelationshipTargets().get(0).getTargetEntityId());
  }

  @Test
  void linkedCaseWithoutLscRef_usesNumericIdForLinkedCaseIdValue() {
    // LINKED_CASE_ID is numeric in the rulebase, so with no LSC reference its value falls back to
    // the bare numeric id, not the "LC_<id>" instance identifier.
    final LinkedCaseDetail linkedCase = new LinkedCaseDetail();
    linkedCase.setId(42);
    context.getApplication().setLinkedCases(List.of(linkedCase));

    final List<AssessmentEntityTypeDetail> result =
        assessmentMapper.toAssessmentEntityTypeList(context);

    final AssessmentEntityDetail linkedCaseEntity =
        result.stream()
            .filter(entityType -> "LINKED_CASES".equals(entityType.getName()))
            .findFirst()
            .orElseThrow()
            .getEntities()
            .get(0);
    // Entity instance name keeps the "LC_<id>" identifier.
    assertEquals("LC_42", linkedCaseEntity.getName());
    // But the numeric LINKED_CASE_ID attribute value is the bare id, not "LC_42".
    assertContainsAttribute(
        linkedCaseEntity.getAttributes(), AssessmentAttribute.LINKED_CASE_ID, "42");
  }

  @Test
  public void shouldConvertProceedingToAttributeList() {
    // Given
    final ProceedingDetail proceeding = context.getApplication().getProceedings().getFirst();

    // When
    final List<AssessmentAttributeDetail> result =
        assessmentMapper.proceedingToAttributeList(proceeding);

    // Then
    assertProceedingAttributes(result);
  }

  @Test
  public void shouldConvertOpponentToAttributeList() {
    // Given
    final AssessmentOpponentMappingContext opponentMappingContext =
        context.getOpponentContext().getFirst();

    // When
    final List<AssessmentAttributeDetail> result =
        assessmentMapper.opponentToAttributeList(opponentMappingContext);

    // Then
    assertOpponentAttributes(result);
  }

  @Test
  public void shouldConvertGlobalToAttributeList() {
    // When
    final List<AssessmentAttributeDetail> result = assessmentMapper.globalToAttributeList(context);

    // Then
    assertGlobalAttributes(result);
  }

  @Test
  public void shouldReturnCorrectOpponentOpaInstanceMappingId() {
    final OpponentDetail opponent = new OpponentDetail().id(123);
    final String expectedOpaInstanceMappingId = "OPPONENT_123";

    final String result = assessmentMapper.getOpponentOpaInstanceMappingId(opponent);

    assertEquals(expectedOpaInstanceMappingId, result);
  }

  @Test
  public void shouldReturnCorrectProceedingOpaInstanceMappingId() {
    final ProceedingDetail proceeding = new ProceedingDetail().id(123);
    final String expectedOpaInstanceMappingId = "P_123";

    final String result = assessmentMapper.getProceedingOpaInstanceMappingId(proceeding);

    assertEquals(expectedOpaInstanceMappingId, result);
  }

  private void assertGlobalAttributes(final List<AssessmentAttributeDetail> attributes) {

    assertEquals(54, attributes.size());
    assertContainsAttribute(attributes, AssessmentAttribute.APPLICATION_CASE_REF, "1234567890");
    assertContainsAttribute(attributes, AssessmentAttribute.RNON_MAND_EVIDENCE_AMD_CORR, "false");
    assertContainsAttribute(
        attributes, AssessmentAttribute.PDECLARATION_WILL_BE_SIGNED_EM, "false");
    assertContainsAttribute(attributes, AssessmentAttribute.APP_AMEND_TYPE, "type1234567890");
    assertContainsAttribute(attributes, AssessmentAttribute.CATEGORY_OF_LAW, "1234567890cat1");
    assertContainsAttribute(attributes, AssessmentAttribute.CERTIFICATE_TYPE, "type1234567890");
    assertContainsAttribute(attributes, AssessmentAttribute.CLIENT_VULNERABLE, "false");
    assertContainsAttribute(attributes, AssessmentAttribute.ACTION_CLIENTS_UK_STATUS, "false");
    assertContainsAttribute(
        attributes, AssessmentAttribute.CLIENT_IMM_ASY_CLAIM_DETENTION, "false");
    assertContainsAttribute(attributes, AssessmentAttribute.HRA_ISSUES_SIGNIFICANT, "false");
    assertContainsAttribute(attributes, AssessmentAttribute.COST_LIMIT_CHANGED_FLAG, "true");
    assertContainsAttribute(attributes, AssessmentAttribute.COUNTRY, "clientthecountry");
    assertContainsAttribute(attributes, AssessmentAttribute.COUNTY, "clientthecounty");
    assertContainsAttribute(attributes, AssessmentAttribute.DATE_ASSESSMENT_STARTED, dateStr);
    assertContainsAttribute(attributes, AssessmentAttribute.DATE_OF_BIRTH, dateStr);
    assertContainsAttribute(attributes, AssessmentAttribute.DEFAULT_COST_LIMITATION, "1");
    assertContainsAttribute(attributes, AssessmentAttribute.DELEGATED_FUNCTIONS_DATE, dateStr);
    assertContainsAttribute(
        attributes, AssessmentAttribute.DEVOLVED_POWERS_CONTRACT_FLAG, "flag1234567890");
    assertContainsAttribute(attributes, AssessmentAttribute.ECF_FLAG, "false");
    assertContainsAttribute(attributes, AssessmentAttribute.FIRST_NAME, "firstname");
    assertContainsAttribute(attributes, AssessmentAttribute.HIGH_PROFILE, "false");
    assertContainsAttribute(attributes, AssessmentAttribute.HOME_OFFICE_NO, "123");
    assertContainsAttribute(attributes, AssessmentAttribute.LAR_SCOPE_FLAG, "true");
    assertContainsAttribute(attributes, AssessmentAttribute.LEAD_PROCEEDING_CHANGED, "true");
    assertContainsAttribute(attributes, AssessmentAttribute.MARITIAL_STATUS, "status");
    assertContainsAttribute(attributes, AssessmentAttribute.NEW_APPL_OR_AMENDMENT, "AMENDMENT");
    assertContainsAttribute(attributes, AssessmentAttribute.NI_NO, "nino");
    assertContainsAttribute(attributes, AssessmentAttribute.POA_OR_BILL_FLAG, "N/A");
    assertContainsAttribute(attributes, AssessmentAttribute.POST_CODE, "clientpc");
    assertContainsAttribute(
        attributes, AssessmentAttribute.PROVIDER_CASE_REFERENCE, "provcaseref1234567890");
    assertContainsAttribute(attributes, AssessmentAttribute.PROVIDER_HAS_CONTRACT, "false");
    assertContainsAttribute(attributes, AssessmentAttribute.REQ_COST_LIMITATION, "10");
    assertContainsAttribute(attributes, AssessmentAttribute.SURNAME, "surname");
    assertContainsAttribute(attributes, AssessmentAttribute.SURNAME_AT_BIRTH, "birth");
    assertContainsAttribute(attributes, AssessmentAttribute.USER_PROVIDER_FIRM_ID, "123");
    assertContainsAttribute(attributes, AssessmentAttribute.USER_TYPE, "testUserType");
  }

  private void assertProceedingAttributes(final List<AssessmentAttributeDetail> attributes) {
    assertEquals(11, attributes.size());
    assertContainsAttribute(attributes, AssessmentAttribute.CLIENT_INVOLVEMENT_TYPE, "clientInv");
    assertContainsAttribute(attributes, AssessmentAttribute.PROC_OUTCOME_STATUS, "true");
    assertContainsAttribute(attributes, AssessmentAttribute.LEAD_PROCEEDING, "true");
    assertContainsAttribute(attributes, AssessmentAttribute.LEVEL_OF_SERVICE, "los");
    assertContainsAttribute(attributes, AssessmentAttribute.MATTER_TYPE, "mat");
    assertContainsAttribute(attributes, AssessmentAttribute.NEW_OR_EXISTING, "CHANGED");
    assertContainsAttribute(attributes, AssessmentAttribute.PROCEEDING_ID, "theebsid");
    assertContainsAttribute(attributes, AssessmentAttribute.PROCEEDING_NAME, "type");
    assertContainsAttribute(attributes, AssessmentAttribute.PROCEEDING_ORDER_TYPE, "too");
    assertContainsAttribute(attributes, AssessmentAttribute.REQUESTED_SCOPE, "scopelim");
    assertContainsAttribute(attributes, AssessmentAttribute.SCOPE_LIMIT_IS_DEFAULT, "true");
  }

  private void assertOpponentAttributes(final List<AssessmentAttributeDetail> attributes) {
    assertEquals(6, attributes.size());
    assertContainsAttribute(attributes, AssessmentAttribute.OPPONENT_DOB, dateStr);
    assertContainsAttribute(attributes, AssessmentAttribute.OTHER_PARTY_ID, "ebsid");
    assertContainsAttribute(
        attributes, AssessmentAttribute.OTHER_PARTY_NAME, "Mr firstname surname");
    assertContainsAttribute(attributes, AssessmentAttribute.OTHER_PARTY_TYPE, "ORGANISATION");
    assertContainsAttribute(attributes, AssessmentAttribute.RELATIONSHIP_TO_CASE, "relToCase");
    assertContainsAttribute(attributes, AssessmentAttribute.RELATIONSHIP_TO_CLIENT, "relToClient");
  }

  private void assertContainsAttribute(
      final List<AssessmentAttributeDetail> attributeDetails,
      final AssessmentAttribute expectedAttribute,
      final String expectedValue) {
    boolean found = false;
    for (final AssessmentAttributeDetail attributeDetail : attributeDetails) {
      if (attributeDetail.getName().equals(expectedAttribute.name())) {
        assertEquals(
            expectedValue,
            attributeDetail.getValue(),
            "Value for attribute " + expectedAttribute.name() + " does not match expected value.");
        found = true;
        break;
      }
    }
    assertTrue(found, "Attribute " + expectedAttribute.name() + " not found in the list.");
  }

  @Test
  void testToAssessmentDetail_whenContextIsNull() {
    final AssessmentDetail assessment = new AssessmentDetail();

    assessmentMapper.toAssessmentDetail(assessment, null);

    assertNotNull(assessment.getEntityTypes());
    assertEquals(0, assessment.getEntityTypes().size());
  }

  @Test
  void testToAssessmentDetail_whenContextIsNotNullAndEntityTypesIsNull() {
    final AssessmentDetail assessment = new AssessmentDetail();
    assessment.setEntityTypes(null);

    assessmentMapper.toAssessmentDetail(assessment, context);

    assertNotNull(assessment.getEntityTypes());
    assertEquals(4, assessment.getEntityTypes().size());
  }

  @Test
  void testToAssessmentDetail_whenContextIsNotNullAndEntityTypesIsNotNullAndEmpty() {
    final AssessmentDetail assessment = new AssessmentDetail();
    assessment.setEntityTypes(new ArrayList<>());

    assessmentMapper.toAssessmentDetail(assessment, context);

    assertNotNull(assessment.getEntityTypes());
    assertEquals(4, assessment.getEntityTypes().size());
  }

  @Test
  void toAssessmentDetail_applicationAndAmendmentUseConsistentEntityAndRelationshipShape() {
    final Date date = new Date();
    final AssessmentMappingContext applicationContext = buildAssessmentMappingContext(false, date);
    final AssessmentMappingContext amendmentContext = buildAssessmentMappingContext(true, date);
    final AssessmentDetail applicationAssessment = new AssessmentDetail();
    final AssessmentDetail amendmentAssessment = new AssessmentDetail();

    assessmentMapper.toAssessmentDetail(applicationAssessment, applicationContext);
    assessmentMapper.toAssessmentDetail(amendmentAssessment, amendmentContext);

    assertEquals(
        getEntityTypeNames(applicationAssessment), getEntityTypeNames(amendmentAssessment));
    assertEquals(
        getGlobalRelationshipNames(applicationAssessment),
        getGlobalRelationshipNames(amendmentAssessment));
    assertEquals(
        Set.of("linkedcases", "opponentotherparties", "proceeding"),
        getGlobalRelationshipNames(applicationAssessment));
  }

  @Test
  void toAssessmentDetail_includesAmendmentDraftProceedings() {
    final Date date = new Date();
    final ProceedingDetail liveProceeding =
        buildProceeding(date, java.math.BigDecimal.ONE)
            .ebsId("LIVE_EBS_ID")
            .proceedingType(new StringDisplayValue().id("LIVE_TYPE").displayValue("Live type"));
    final ProceedingDetail draftProceeding =
        buildProceeding(date, java.math.BigDecimal.TEN)
            .id(777)
            .ebsId(null)
            .proceedingType(new StringDisplayValue().id("DRAFT_TYPE").displayValue("Draft type"));
    final ApplicationDetail application = buildApplicationDetail(1234567890, true, date);
    application.setProceedings(List.of(liveProceeding));
    application.setAmendmentProceedingsInEbs(List.of(draftProceeding));
    final AssessmentDetail assessment = new AssessmentDetail();

    assessmentMapper.toAssessmentDetail(
        assessment,
        AssessmentMappingContext.builder()
            .application(application)
            .client(buildClientDetail())
            .user(buildUserDetail())
            .opponentContext(context.getOpponentContext())
            .build());

    assertEquals(Set.of("LIVE_EBS_ID", "P_777"), getProceedingEntityNames(assessment));
    assertEquals(Set.of("LIVE_EBS_ID", "P_777"), getProceedingRelationshipTargetIds(assessment));
  }

  @Test
  void toAssessmentDetail_whenContextIsNull() {
    final AssessmentDetail assessment = new AssessmentDetail();

    assessmentMapper.toAssessmentDetail(assessment, null);
    assertNotNull(assessment.getEntityTypes());
    assertEquals(0, assessment.getEntityTypes().size());
  }

  @Test
  void toAssessmentEntityDetailListProceeding_whenProceedingsIsNull() {
    final List<AssessmentEntityDetail> result =
        assessmentMapper.toAssessmentEntityDetailListProceeding(null);
    assertNull(result);
  }

  @Test
  void toAssessmentEntityDetailListProceeding_whenProceedingsIsNotNull() {
    final List<AssessmentEntityDetail> result =
        assessmentMapper.toAssessmentEntityDetailListProceeding(
            context.getApplication().getProceedings());

    assertEquals(2, result.size());
  }

  @Test
  void toAssessmentEntityDetail_whenProceedingIsNull() {
    final AssessmentEntityDetail result =
        assessmentMapper.toAssessmentEntityDetail((ProceedingDetail) null);
    assertNull(result);
  }

  @Test
  void toAssessmentEntityDetail_whenOpponentContextIsNull() {
    final AssessmentEntityDetail result =
        assessmentMapper.toAssessmentEntityDetail((AssessmentOpponentMappingContext) null);
    assertNull(result);
  }

  @Test
  void toAssessmentEntityDetail_whenOpponentContextIsNotNull() {
    final AssessmentOpponentMappingContext opponentContext =
        context.getOpponentContext().getFirst();
    final AssessmentEntityDetail result =
        assessmentMapper.toAssessmentEntityDetail(opponentContext);

    assertNotNull(result);
    assertEquals("ebsid", result.getName());
    assertEquals(6, result.getAttributes().size());
    assertTrue(result.getPrepopulated());
  }

  private AssessmentMappingContext buildAssessmentMappingContext(
      final boolean amendment, final Date date) {
    final AssessmentOpponentMappingContext opponentMappingContext =
        AssessmentOpponentMappingContext.builder()
            .opponent(buildOpponent(date))
            .titleCommonLookupValue(new CommonLookupValueDetail().code("MR").description("Mr"))
            .build();

    final ApplicationDetail application = buildApplicationDetail(1234567890, amendment, date);
    final ClientDetail client = buildClientDetail();
    final UserDetail user = buildUserDetail();

    return AssessmentMappingContext.builder()
        .application(application)
        .user(user)
        .client(client)
        .opponentContext(List.of(opponentMappingContext))
        .build();
  }

  private Set<String> getEntityTypeNames(final AssessmentDetail assessment) {
    return assessment.getEntityTypes().stream()
        .map(AssessmentEntityTypeDetail::getName)
        .collect(java.util.stream.Collectors.toSet());
  }

  private Set<String> getGlobalRelationshipNames(final AssessmentDetail assessment) {
    return assessment.getEntityTypes().stream()
        .filter(entityType -> "global".equals(entityType.getName()))
        .flatMap(entityType -> entityType.getEntities().stream())
        .flatMap(entity -> entity.getRelations().stream())
        .map(AssessmentRelationshipDetail::getName)
        .collect(java.util.stream.Collectors.toSet());
  }

  private Set<String> getProceedingEntityNames(final AssessmentDetail assessment) {
    return assessment.getEntityTypes().stream()
        .filter(entityType -> "PROCEEDING".equals(entityType.getName()))
        .flatMap(entityType -> entityType.getEntities().stream())
        .map(AssessmentEntityDetail::getName)
        .collect(java.util.stream.Collectors.toSet());
  }

  private Set<String> getProceedingRelationshipTargetIds(final AssessmentDetail assessment) {
    return assessment.getEntityTypes().stream()
        .filter(entityType -> "global".equals(entityType.getName()))
        .flatMap(entityType -> entityType.getEntities().stream())
        .flatMap(entity -> entity.getRelations().stream())
        .filter(relationship -> "proceeding".equals(relationship.getName()))
        .flatMap(relationship -> relationship.getRelationshipTargets().stream())
        .map(target -> target.getTargetEntityId())
        .collect(java.util.stream.Collectors.toSet());
  }
}
