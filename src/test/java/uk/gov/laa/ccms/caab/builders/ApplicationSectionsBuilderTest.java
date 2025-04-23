package uk.gov.laa.ccms.caab.builders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_INDIVIDUAL;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.OPPONENT_TYPE_ORGANISATION;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.SECTION_STATUS_COMPLETE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.SECTION_STATUS_NOT_AVAILABLE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.SECTION_STATUS_STARTED;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildApplicationProviderDetails;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildOpponent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.AuditDetail;
import uk.gov.laa.ccms.caab.model.ClientDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.DevolvedPowersDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.OpponentSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.ProceedingSectionDisplay;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;

class ApplicationSectionsBuilderTest {

  private ApplicationSectionsBuilder builder;

  @BeforeEach
  void setUp() {
    // Mock an AuditDetail for testing
    AuditDetail auditDetail = new AuditDetail();
    auditDetail.setLastSaved(Date.from(Instant.now()));
    auditDetail.setLastSavedBy("TestUser");

    // Create a new builder for each test
    builder = new ApplicationSectionsBuilder(auditDetail);
  }

  @Test
  void caseReferenceNumber() {
    builder.caseReferenceNumber("REF123");
    ApplicationSectionDisplay result = builder.build();
    assertEquals("REF123", result.getCaseReferenceNumber());
  }

  @Test
  void applicationType() {
    ApplicationType applicationType = new ApplicationType();
    applicationType.setId("TEST");
    applicationType.setDisplayValue("TEST DISPLAY");
    applicationType.setDevolvedPowers(new DevolvedPowersDetail()
        .dateUsed(new Date())
        .used(true));

    builder.applicationType(applicationType);

    ApplicationSectionDisplay result = builder.build();

    assertNotNull(result.getApplicationType());
    assertEquals(applicationType.getDisplayValue(), result.getApplicationType().getDescription());
    assertEquals(applicationType.getDevolvedPowers().getDateUsed(), result.getApplicationType().getDevolvedPowersDate());
    assertEquals(applicationType.getDevolvedPowers().getUsed(), result.getApplicationType().getDevolvedPowersUsed());
    assertTrue(result.getApplicationType().isEnabled());
  }

  @Test
  void applicationTypeECFStatusDisabled() {
    ApplicationType applicationType = new ApplicationType()
        .id(APP_TYPE_EXCEPTIONAL_CASE_FUNDING)
        .displayValue(APP_TYPE_EXCEPTIONAL_CASE_FUNDING_DISPLAY)
        .devolvedPowers(new DevolvedPowersDetail().used(true).dateUsed(new Date()));

    builder.applicationType(applicationType);

    ApplicationSectionDisplay result = builder.build();

    assertNotNull(result.getApplicationType());
    assertEquals(applicationType.getDisplayValue(), result.getApplicationType().getDescription());
    assertEquals(applicationType.getDevolvedPowers().getDateUsed(), result.getApplicationType().getDevolvedPowersDate());
    assertEquals(applicationType.getDevolvedPowers().getUsed(), result.getApplicationType().getDevolvedPowersUsed());

    assertFalse(result.getApplicationType().isEnabled());
  }

  @Test
  void generalDetailsHasPreferredAddressStatusComplete() {
    StringDisplayValue applicationStatus = new StringDisplayValue().id("1").displayValue("appStat");
    StringDisplayValue categoryOfLaw = new StringDisplayValue().id("1=2").displayValue("cat");

    // Create a non-empty AddressDetail
    String correspondenceMethod = "Send to provider";

    builder.generalDetails(applicationStatus, categoryOfLaw, correspondenceMethod);
    ApplicationSectionDisplay result = builder.build();

    assertEquals(applicationStatus.getDisplayValue(), result.getGeneralDetails().getApplicationStatus());
    assertEquals(categoryOfLaw.getDisplayValue(), result.getGeneralDetails().getCategoryOfLaw());
    assertEquals(correspondenceMethod, result.getGeneralDetails().getCorrespondenceMethod());

    assertEquals(SECTION_STATUS_COMPLETE, result.getGeneralDetails().getStatus());
  }

  @Test
  void generalDetailsNoPreferredAddressStatusStarted() {
    StringDisplayValue applicationStatus = new StringDisplayValue().id("1").displayValue("appStat");
    StringDisplayValue categoryOfLaw = new StringDisplayValue().id("1=2").displayValue("cat");

    builder.generalDetails(applicationStatus, categoryOfLaw, null);
    ApplicationSectionDisplay result = builder.build();

    assertEquals(SECTION_STATUS_STARTED, result.getGeneralDetails().getStatus());
  }

  @Test
  void client() {
    ClientDetail client = new ClientDetail()
        .firstName("first")
        .surname("second")
        .reference("ref");

    builder.client(client);
    ApplicationSectionDisplay result = builder.build();

    assertEquals("first second", result.getClient().getClientFullName());
    assertEquals(client.getReference(), result.getClient().getClientReferenceNumber());
  }

  @Test
  void providerStatusComplete() {
    ApplicationProviderDetails applicationProviderDetails = buildApplicationProviderDetails(1);

    builder.provider(applicationProviderDetails);
    ApplicationSectionDisplay result = builder.build();

    assertEquals(applicationProviderDetails.getProvider().getDisplayValue(), result.getProvider().getProviderName());
    assertEquals(applicationProviderDetails.getFeeEarner().getDisplayValue(), result.getProvider().getFeeEarner());
    assertEquals(applicationProviderDetails.getOffice().getDisplayValue(), result.getProvider().getOfficeName());
    assertEquals(applicationProviderDetails.getProviderCaseReference(), result.getProvider().getProviderCaseReferenceNumber());
    assertEquals(applicationProviderDetails.getProviderContact().getDisplayValue(), result.getProvider().getProviderContactName());
    assertEquals(applicationProviderDetails.getSupervisor().getDisplayValue(), result.getProvider().getSupervisorName());

    assertEquals(SECTION_STATUS_COMPLETE, result.getProvider().getStatus());
  }

  @Test
  void providerNoProviderContactStatusStarted() {
    ApplicationProviderDetails applicationProviderDetails = new ApplicationProviderDetails();

    builder.provider(applicationProviderDetails);
    ApplicationSectionDisplay result = builder.build();

    assertEquals(SECTION_STATUS_STARTED, result.getProvider().getStatus());
  }

  @Test
  void proceedingsAndCostsStatusComplete() {
    AuditDetail auditDetail = new AuditDetail();
    auditDetail.setLastSaved(Date.from(Instant.now()));
    auditDetail.setLastSavedBy("TestUser");

    ProceedingDetail proceeding1 = new ProceedingDetail()
        .auditTrail(auditDetail)
        .matterType(new StringDisplayValue().id("mat1").displayValue("matter type 1"))
        .levelOfService(new StringDisplayValue().id("lev1").displayValue("Level 1"))
        .clientInvolvement(new StringDisplayValue().id("inv1").displayValue("Involvement 1"))
        .proceedingType(new StringDisplayValue().id("proc1").displayValue("proceeding type 1"))
        .stage("Stage 1")
        .addScopeLimitationsItem(new ScopeLimitationDetail()
            .scopeLimitation(new StringDisplayValue().id("scope1").displayValue("Scope 1")));

    ProceedingDetail proceeding2 = new ProceedingDetail()
        .auditTrail(auditDetail)
        .matterType(new StringDisplayValue().id("mat2").displayValue("matter type 2"))
        .proceedingType(new StringDisplayValue().id("proc2").displayValue("proceeding type 2"))
        .levelOfService(new StringDisplayValue().id("lev2").displayValue("Level 2"))
        .clientInvolvement(new StringDisplayValue().id("inv2").displayValue("Involvement 2"));

    ProceedingDetail proceeding3 = new ProceedingDetail()
        .auditTrail(auditDetail)
        .matterType(new StringDisplayValue().id("mat3").displayValue("matter type 3"))
        .proceedingType(new StringDisplayValue().id("proc3").displayValue("proceeding type 3"))
        .levelOfService(new StringDisplayValue().id("lev3").displayValue("Level 3"))
        .clientInvolvement(new StringDisplayValue().id("inv3").displayValue("Involvement 3"));

    PriorAuthorityDetail priorAuthority = new PriorAuthorityDetail()
        .amountRequested(BigDecimal.ONE)
        .auditTrail(new AuditDetail())
        .status("the stat")
        .summary("a summary")
        .type(new StringDisplayValue().id("type1").displayValue("prior auth type 1"));

    CostStructureDetail costStructure = new CostStructureDetail()
        .grantedCostLimitation(BigDecimal.TWO)
        .requestedCostLimitation(BigDecimal.TEN)
        .auditTrail(new AuditDetail());

    builder.proceedingsPriorAuthsAndCosts(
        List.of(proceeding1, proceeding2, proceeding3),
        List.of(priorAuthority),
        costStructure);

    ApplicationSectionDisplay result = builder.build();

    assertEquals(costStructure.getGrantedCostLimitation(), result.getProceedingsAndCosts().getGrantedCostLimitation());
    assertEquals(costStructure.getRequestedCostLimitation(), result.getProceedingsAndCosts().getRequestedCostLimitation());
    assertNotNull(result.getProceedingsAndCosts().getProceedings());
    assertEquals(3, result.getProceedingsAndCosts().getProceedings().size());

    ProceedingSectionDisplay procSummary = result.getProceedingsAndCosts().getProceedings().getFirst();
    assertEquals(proceeding1.getClientInvolvement().getDisplayValue(), procSummary.getClientInvolvement());
    assertEquals(proceeding1.getLevelOfService().getDisplayValue(), procSummary.getLevelOfService());
    assertEquals(proceeding1.getMatterType().getDisplayValue(), procSummary.getMatterType());
    assertEquals(proceeding1.getProceedingType().getDisplayValue(), procSummary.getProceedingType());
    assertNotNull(procSummary.getScopeLimitations());
    assertEquals(1, procSummary.getScopeLimitations().size());
    assertEquals(proceeding1.getScopeLimitations().getFirst().getScopeLimitation().getDisplayValue(),
        procSummary.getScopeLimitations().getFirst().getScopeLimitation());
    assertEquals(proceeding1.getScopeLimitations().getFirst().getScopeLimitationWording(),
        procSummary.getScopeLimitations().getFirst().getWording());

    assertEquals(SECTION_STATUS_COMPLETE, result.getProceedingsAndCosts().getStatus());
  }

  @Test
  void proceedingsAndCostsNoStageStatusStarted() {
    AuditDetail auditDetail = new AuditDetail();
    auditDetail.setLastSaved(Date.from(Instant.now()));
    auditDetail.setLastSavedBy("TestUser");

    ProceedingDetail proceeding1 = new ProceedingDetail()
        .auditTrail(auditDetail);

    PriorAuthorityDetail priorAuthority = new PriorAuthorityDetail()
        .amountRequested(BigDecimal.ONE)
        .auditTrail(new AuditDetail())
        .status("the stat")
        .summary("a summary")
        .type(new StringDisplayValue().id("type1").displayValue("prior auth type 1"));

    CostStructureDetail costStructure = new CostStructureDetail()
        .grantedCostLimitation(BigDecimal.TWO)
        .requestedCostLimitation(BigDecimal.TEN)
        .auditTrail(new AuditDetail());

    builder.proceedingsPriorAuthsAndCosts(
        List.of(proceeding1),
        List.of(priorAuthority),
        costStructure);

    ApplicationSectionDisplay result = builder.build();

    assertEquals(SECTION_STATUS_STARTED, result.getProceedingsAndCosts().getStatus());
  }

  @Test
  void opponentsAndOtherParties() {
    AuditDetail auditDetail = new AuditDetail();
    auditDetail.setLastSaved(new Date());
    auditDetail.setLastSavedBy("TestUser");

    // Create some sample data for opponents
    OpponentDetail opponent1 = buildOpponent(new Date());
    opponent1.setType(OPPONENT_TYPE_ORGANISATION);
    opponent1.setAuditTrail(auditDetail);

    OpponentDetail opponent2 = buildOpponent(new Date());
    opponent2.setType(OPPONENT_TYPE_INDIVIDUAL);
    opponent2.setAuditTrail(auditDetail);

    // Create some sample data for organization relationships
    RelationshipToCaseLookupValueDetail orgRelationshipToCase = new RelationshipToCaseLookupValueDetail();
    orgRelationshipToCase.setCode(opponent1.getRelationshipToCase());
    orgRelationshipToCase.setDescription("an org relationship");
    orgRelationshipToCase.setOpponentInd(true);

    builder.opponentsAndOtherParties(
        List.of(opponent1, opponent2),
        Collections.emptyList(),
        List.of(orgRelationshipToCase),
        Collections.emptyList(),
        Collections.emptyList());
    ApplicationSectionDisplay result = builder.build();

    assertNotNull(result.getOpponentsAndOtherParties());
    assertNotNull(result.getOpponentsAndOtherParties().getOpponents());
    assertEquals(2, result.getOpponentsAndOtherParties().getOpponents().size());

    OpponentSectionDisplay opponentSectionDisplay =
        result.getOpponentsAndOtherParties().getOpponents().getFirst();
    assertEquals(opponent1.getOrganisationName(), opponentSectionDisplay.getPartyName());
    assertEquals(opponent1.getType(), opponentSectionDisplay.getPartyType());
    assertEquals(orgRelationshipToCase.getDescription(), opponentSectionDisplay.getRelationshipToCase());
    assertEquals(opponent1.getRelationshipToClient(), opponentSectionDisplay.getRelationshipToClient());

    assertEquals(SECTION_STATUS_COMPLETE, result.getOpponentsAndOtherParties().getStatus());
  }

  @ParameterizedTest
  @CsvSource({"true,true," + SECTION_STATUS_COMPLETE + ",true",
      "true,false," + SECTION_STATUS_STARTED + ",true",
      "false,false," + SECTION_STATUS_NOT_AVAILABLE + ",false",
      "false,true," + SECTION_STATUS_NOT_AVAILABLE + ",false"})
  void documentUploadDisabled(
      final boolean evidenceRequired,
      final boolean allEvidenceProvided,
      final String expectedStatus,
      final boolean expectedEnabled) {

    builder.documentUpload(evidenceRequired, allEvidenceProvided);
    ApplicationSectionDisplay result = builder.build();

    assertNotNull(result.getDocumentUpload());
    assertEquals(expectedStatus, result.getDocumentUpload().getStatus());
    assertEquals(expectedEnabled, result.getDocumentUpload().isEnabled());
  }
}
