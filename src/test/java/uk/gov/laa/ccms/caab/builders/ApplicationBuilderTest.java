package uk.gov.laa.ccms.caab.builders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY_DEVOLVED_POWERS_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE_DISPLAY;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.DevolvedPowers;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.BaseOffice;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetail;

class ApplicationBuilderTest {

  private ApplicationBuilder builder;

  @BeforeEach
  void setUp() {
    builder = new ApplicationBuilder();
  }

  @Test
  void testCaseReference() {
    CaseReferenceSummary summary = new CaseReferenceSummary();
    summary.setCaseReferenceNumber("REF123");
    ApplicationDetail detail = builder.caseReference(summary).build();
    assertEquals("REF123", detail.getCaseReferenceNumber());
  }

  @Test
  void testProvider() {
    UserDetail userDetail = new UserDetail();
    BaseProvider provider = new BaseProvider();
    provider.setId(123);
    provider.setName("PROVIDERNAME");
    userDetail.setProvider(provider);

    ApplicationDetail detail = builder.provider(userDetail).build();
    assertEquals("PROVIDERNAME", detail.getProvider().getDisplayValue());
  }

  @Test
  void testCategoryOfLaw() {
    String categoryId = "CATEGORY1";
    CommonLookupDetail lookupDetail = new CommonLookupDetail();
    lookupDetail.addContentItem(new CommonLookupValueDetail().code(categoryId).description("Description"));

    ApplicationDetail detail = builder.categoryOfLaw(categoryId, lookupDetail).build();
    assertEquals(categoryId, detail.getCategoryOfLaw().getId());
    assertEquals("Description", detail.getCategoryOfLaw().getDisplayValue());
  }

  @Test
  void testContractualDevolvedPower() throws ParseException {

    ApplicationType applicationType = new ApplicationType();
    // Create a mock ContractDetail object
    ContractDetail contractDetail = new ContractDetail();
    contractDetail.setCategoryofLaw("CATEGORY1");
    contractDetail.setContractualDevolvedPowers("CONTRACTUAL_DEVOLVED_POWERS");

    // Create a list of ContractDetail containing the mock contractDetail
    List<ContractDetail> contractDetails = Collections.singletonList(contractDetail);

    // Perform the contractualDevolvedPower operation
    ApplicationDetail application = builder.contractualDevolvedPower(contractDetails, "CATEGORY1").build();

    // Verify that application.getApplicationType() and applicationType.getDevolvedPowers() were called
    // Verify that the contractFlag was set in the DevolvedPowers object
    assertEquals(
        "CONTRACTUAL_DEVOLVED_POWERS",
        application.getApplicationType().getDevolvedPowers().getContractFlag());
  }

  @Test
  void testOffice() {
    Integer officeId = 1;
    BaseOffice baseOffice = new BaseOffice();
    baseOffice.setId(officeId);
    baseOffice.setName("OfficeName");
    List<BaseOffice> baseOffices = Collections.singletonList(baseOffice);

    ApplicationDetail detail = builder.office(officeId, baseOffices).build();
    assertEquals(officeId, detail.getOffice().getId());
    assertEquals("OfficeName", detail.getOffice().getDisplayValue());
  }

  @Test
  void testLarScopeFlag() {
    AmendmentTypeLookupValueDetail lookupValueDetail = new AmendmentTypeLookupValueDetail();
    lookupValueDetail.setDefaultLarScopeFlag("Y");
    AmendmentTypeLookupDetail lookupDetail = new AmendmentTypeLookupDetail();
    lookupDetail.setContent(Collections.singletonList(lookupValueDetail));

    ApplicationDetail detail = builder.larScopeFlag(lookupDetail).build();
    assertTrue(detail.getLarScopeFlag());
  }

  @Test
  void testStatus() {
    ApplicationDetail detail = builder.status().build();
    assertEquals(STATUS_UNSUBMITTED_ACTUAL_VALUE, detail.getStatus().getId());
    assertEquals(STATUS_UNSUBMITTED_ACTUAL_VALUE_DISPLAY, detail.getStatus().getDisplayValue());
  }


}