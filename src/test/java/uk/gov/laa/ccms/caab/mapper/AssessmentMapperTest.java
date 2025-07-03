package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildApplicationDetail;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildOpponent;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildClientDetail;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute;
import uk.gov.laa.ccms.caab.mapper.context.AssessmentMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.AssessmentOpponentMappingContext;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
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
    assertEquals(3, result.size());
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

    assertEquals(30, attributes.size());
    assertContainsAttribute(attributes, AssessmentAttribute.APPLICATION_CASE_REF, "1234567890");
    assertContainsAttribute(attributes, AssessmentAttribute.APP_AMEND_TYPE, "type1234567890");
    assertContainsAttribute(attributes, AssessmentAttribute.CATEGORY_OF_LAW, "1234567890cat1");
    assertContainsAttribute(attributes, AssessmentAttribute.CLIENT_VULNERABLE, "false");
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
    assertEquals(10, attributes.size());
    assertContainsAttribute(attributes, AssessmentAttribute.CLIENT_INVOLVEMENT_TYPE, "clientInv");
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
    assertEquals(3, assessment.getEntityTypes().size());
  }

  @Test
  void testToAssessmentDetail_whenContextIsNotNullAndEntityTypesIsNotNullAndEmpty() {
    final AssessmentDetail assessment = new AssessmentDetail();
    assessment.setEntityTypes(new ArrayList<>());

    assessmentMapper.toAssessmentDetail(assessment, context);

    assertNotNull(assessment.getEntityTypes());
    assertEquals(3, assessment.getEntityTypes().size());
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
}
