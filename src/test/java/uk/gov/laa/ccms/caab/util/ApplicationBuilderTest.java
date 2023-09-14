package uk.gov.laa.ccms.caab.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
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
  void testApplicationTypeForSubstantiveWithoutDelegatedFunctions() {
    ApplicationDetail detail =
        builder.applicationType(APP_TYPE_SUBSTANTIVE, false).build();
    assertEquals(APP_TYPE_SUBSTANTIVE, detail.getApplicationType().getId());
    assertEquals(APP_TYPE_SUBSTANTIVE_DISPLAY,
        detail.getApplicationType().getDisplayValue());
  }

  @Test
  void testApplicationTypeForSubstantiveWithDelegatedFunctions() {
    ApplicationDetail detail =
        builder.applicationType(APP_TYPE_SUBSTANTIVE, true).build();
    assertEquals(APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS,
        detail.getApplicationType().getId());
    assertEquals(APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS_DISPLAY,
        detail.getApplicationType().getDisplayValue());
  }

  @Test
  void testApplicationTypeForEmergencyWithoutDelegatedFunctions() {
    ApplicationDetail detail =
        builder.applicationType(APP_TYPE_EMERGENCY, false).build();
    assertEquals(APP_TYPE_EMERGENCY, detail.getApplicationType().getId());
    assertEquals(APP_TYPE_EMERGENCY_DISPLAY,
        detail.getApplicationType().getDisplayValue());
  }

  @Test
  void testApplicationTypeForEmergencyWithDelegatedFunctions() {
    ApplicationDetail detail =
        builder.applicationType(APP_TYPE_EMERGENCY, true).build();
    assertEquals(APP_TYPE_EMERGENCY_DEVOLVED_POWERS,
        detail.getApplicationType().getId());
    assertEquals(APP_TYPE_EMERGENCY_DEVOLVED_POWERS_DISPLAY,
        detail.getApplicationType().getDisplayValue());
  }

  @Test
  void testApplicationTypeForExceptionalCaseFunding() {
    // Here I am passing an unknown category to force the code into the "else" branch.
    // However, for more clarity, you could also directly use APP_TYPE_EXCEPTIONAL_CASE_FUNDING.
    ApplicationDetail detail = builder.applicationType("ECF", false).build();
    assertEquals(APP_TYPE_EXCEPTIONAL_CASE_FUNDING,
        detail.getApplicationType().getId());
    assertEquals(APP_TYPE_EXCEPTIONAL_CASE_FUNDING_DISPLAY,
        detail.getApplicationType().getDisplayValue());
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
  void testDevolvedPowers() throws Exception {
    List<ContractDetail> contractDetails = Collections.singletonList(new ContractDetail());
    ApplicationDetails appDetails = new ApplicationDetails();
    appDetails.setCategoryOfLawId("CATEGORY1");
    appDetails.setDelegatedFunctionUsedDay("01");
    appDetails.setDelegatedFunctionUsedMonth("01");
    appDetails.setDelegatedFunctionUsedYear("2022");
    appDetails.setDelegatedFunctions(true);

    ApplicationDetail detail = builder.devolvedPowers(contractDetails, appDetails).build();
    assertNotNull(detail.getDevolvedPowers());
  }

  @Test
  void testLarScopeFlag() {
    AmendmentTypeLookupValueDetail lookupValueDetail = new AmendmentTypeLookupValueDetail();
    lookupValueDetail.setDefaultLarScopeFlag("FLAG1");
    AmendmentTypeLookupDetail lookupDetail = new AmendmentTypeLookupDetail();
    lookupDetail.setContent(Collections.singletonList(lookupValueDetail));

    ApplicationDetail detail = builder.larScopeFlag(lookupDetail).build();
    assertEquals("FLAG1", detail.getLarScopeFlag());
  }

  @Test
  void testStatus() {
    ApplicationDetail detail = builder.status().build();
    assertEquals(STATUS_UNSUBMITTED_ACTUAL_VALUE, detail.getStatus().getId());
    assertEquals(STATUS_UNSUBMITTED_ACTUAL_VALUE_DISPLAY, detail.getStatus().getDisplayValue());
  }


}