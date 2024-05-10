package uk.gov.laa.ccms.caab.builders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Date;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.ApplicationSummaryDisplay;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.AuditDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;

class ApplicationSummaryBuilderTest {

  private ApplicationSummaryBuilder builder;

  @BeforeEach
  void setUp() {
    // Mock an AuditDetail for testing
    AuditDetail auditDetail = new AuditDetail();
    auditDetail.setLastSaved(Date.from(Instant.now()));
    auditDetail.setLastSavedBy("TestUser");

    // Create a new builder for each test
    builder = new ApplicationSummaryBuilder(auditDetail);
  }

  @Test
  void testClientFullName() {
    builder.clientFullName("John", "Doe");
    ApplicationSummaryDisplay result = builder.build();
    assertEquals("John Doe", result.getClientFullName());
  }

  @Test
  void testCaseReferenceNumber() {
    builder.caseReferenceNumber("REF123");
    ApplicationSummaryDisplay result = builder.build();
    assertEquals("REF123", result.getCaseReferenceNumber());
  }

  @Test
  void testProviderCaseReferenceNumber() {
    builder.providerCaseReferenceNumber("PROV123");
    ApplicationSummaryDisplay result = builder.build();
    assertEquals("PROV123", result.getProviderCaseReferenceNumber());
  }

  @Test
  void testApplicationType() {
    ApplicationType applicationType = new ApplicationType();
    applicationType.setId("TEST");
    applicationType.setDisplayValue("TEST DISPLAY");
    builder.applicationType(applicationType);

    ApplicationSummaryDisplay result = builder.build();
    assertEquals("TEST DISPLAY", result.getApplicationType().getStatus());
  }

  @Test
  void testProviderDetails() {
    // Create a non-empty StringDisplayValue
    StringDisplayValue providerContact = new StringDisplayValue();
    providerContact.setDisplayValue("Provider Contact");

    builder.providerDetails(providerContact);
    ApplicationSummaryDisplay result = builder.build();
    assertEquals("Complete", result.getProviderDetails().getStatus());
  }

  @Test
  void testGeneralDetails() {
    // Create a non-empty AddressDetail
    AddressDetail address = new AddressDetail();
    address.setPreferredAddress("123 Main St");

    builder.generalDetails(address);
    ApplicationSummaryDisplay result = builder.build();
    assertEquals("Complete", result.getGeneralDetails().getStatus());
  }

  @Test
  void testProceedingsAndCosts() {
    AuditDetail auditDetail = new AuditDetail();
    auditDetail.setLastSaved(Date.from(Instant.now()));
    auditDetail.setLastSavedBy("TestUser");

    ProceedingDetail proceeding1 = new ProceedingDetail();
    proceeding1.setAuditTrail(auditDetail);
    proceeding1.setStage("Stage 1");

    ProceedingDetail proceeding2 = new ProceedingDetail();
    proceeding2.setAuditTrail(auditDetail);

    ProceedingDetail proceeding3 = new ProceedingDetail();
    proceeding3.setAuditTrail(auditDetail);

    List<ProceedingDetail> proceedings = Arrays.asList(proceeding1, proceeding2, proceeding3);

    PriorAuthorityDetail priorAuthority = new PriorAuthorityDetail();
    priorAuthority.setAuditTrail(new AuditDetail());

    CostStructureDetail costStructure = new CostStructureDetail();
    costStructure.setAuditTrail(new AuditDetail());

    builder.proceedingsAndCosts(proceedings, Collections.singletonList(priorAuthority), costStructure);
    ApplicationSummaryDisplay result = builder.build();
    assertEquals("Complete", result.getProceedingsAndCosts().getStatus());
  }

  @Test
  void testOpponentsAndOtherParties() {
    AuditDetail auditDetail = new AuditDetail();
    auditDetail.setLastSaved(Date.from(Instant.now()));
    auditDetail.setLastSavedBy("TestUser");

    // Create some sample data for opponents
    OpponentDetail opponent1 = new OpponentDetail();
    opponent1.setType("Organisation");
    opponent1.setRelationshipToCase("REL1");
    opponent1.setAuditTrail(auditDetail);

    List<OpponentDetail> opponents = Collections.singletonList(opponent1);

    // Create some sample data for organization relationships
    RelationshipToCaseLookupValueDetail relationship1 = new RelationshipToCaseLookupValueDetail();
    relationship1.setCode("REL1");
    relationship1.setOpponentInd(true);

    List<RelationshipToCaseLookupValueDetail> organizationRelationships = Collections.singletonList(relationship1);

    builder.opponentsAndOtherParties(opponents, organizationRelationships, Collections.emptyList());
    ApplicationSummaryDisplay result = builder.build();

    assertEquals("Complete", result.getOpponentsAndOtherParties().getStatus());
  }

}
