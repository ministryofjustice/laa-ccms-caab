package uk.gov.laa.ccms.caab.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.laa.ccms.caab.bean.declaration.DynamicCheckbox;
import uk.gov.laa.ccms.caab.mapper.context.submission.GeneralDetailsSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.submission.OpponentSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.submission.ProceedingSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.DevolvedPowersDetail;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.model.summary.GeneralDetailsSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.OpponentSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.OpponentsAndOtherPartiesSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ProceedingAndCostSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ProviderSubmissionSummaryDisplay;
import uk.gov.laa.ccms.caab.model.summary.ScopeLimitationSubmissionSummaryDisplay;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.DeclarationLookupDetail;
import uk.gov.laa.ccms.data.model.DeclarationLookupValueDetail;

@ExtendWith(SpringExtension.class)
class SubmissionSummaryDisplayMapperTest {

  @InjectMocks
  SubmissionSummaryDisplayMapper mapper = new SubmissionSummaryDisplayMapperImpl();

  @Test
  @DisplayName("toProviderSummaryDisplay should return null when ApplicationDetail is null")
  void toProviderSummaryDisplay_nullApplicationDetail_returnsNull() {
    final ProviderSubmissionSummaryDisplay result = mapper.toProviderSummaryDisplay(null);
    assertNull(result, "Expected result to be null when application detail is null.");
  }

  @Test
  @DisplayName("toProviderSummaryDisplay should correctly map all fields when ApplicationDetail is valid")
  void toProviderSummaryDisplay_validApplicationDetail_returnsMappedProviderSummary() {
    final ApplicationDetail applicationDetail = createApplicationDetailWithProviderDetails();

    final ProviderSubmissionSummaryDisplay result = mapper.toProviderSummaryDisplay(applicationDetail);

    assertMappedProviderSummary(result);
  }

  @Test
  @DisplayName("toProviderSummaryDisplay should return null fields when ApplicationDetail fields are not set")
  void toProviderSummaryDisplay_emptyApplicationDetail_returnsMappedProviderSummary() {
    final ApplicationDetail applicationDetail = new ApplicationDetail();

    final ProviderSubmissionSummaryDisplay result = mapper.toProviderSummaryDisplay(applicationDetail);

    assertNull(result.getOffice(), "Office should be null when not set.");
    assertNull(result.getFeeEarner(), "Fee Earner should be null when not set.");
    assertNull(result.getSupervisor(), "Supervisor should be null when not set.");
    assertNull(result.getProviderCaseReference(), "Provider Case Reference should be null when not set.");
    assertNull(result.getContactName(), "Contact Name should be null when not set.");
  }

  @Test
  @DisplayName("toGeneralDetailsSummaryDisplay should return null when ApplicationDetail is null")
  void toGeneralDetailsSummaryDisplay_nullApplicationDetail_returnsNull() {
    final GeneralDetailsSubmissionSummaryDisplay result = mapper.toGeneralDetailsSummaryDisplay(null, null);
    assertNull(result, "Expected result to be null when application detail is null.");
  }

  @Test
  @DisplayName("toGeneralDetailsSummaryDisplay should correctly map all fields when ApplicationDetail is valid")
  void toGeneralDetailsSummaryDisplay_validApplicationDetail_returnsMappedGeneralSummary() {
    final ApplicationDetail applicationDetail = createApplicationDetailWithGeneralDetails();
    final GeneralDetailsSubmissionSummaryMappingContext context = buildGeneralDetailsSubmissionSummaryMappingContext();

    final GeneralDetailsSubmissionSummaryDisplay result = mapper.toGeneralDetailsSummaryDisplay(applicationDetail, context);

    assertMappedGeneralSummary(result, context);
  }

  @Test
  @DisplayName("toGeneralDetailsSummaryDisplay should return null fields when ApplicationDetail fields are not set")
  void toGeneralDetailsSummaryDisplay_emptyApplicationDetail_returnsMappedGeneralSummary() {
    final ApplicationDetail applicationDetail = new ApplicationDetail();
    final GeneralDetailsSubmissionSummaryMappingContext context = buildGeneralDetailsSubmissionSummaryMappingContext();

    final GeneralDetailsSubmissionSummaryDisplay result = mapper.toGeneralDetailsSummaryDisplay(applicationDetail, context);

    assertNull(result.getCategoryOfLaw(), "Category of Law should be null when not set.");
    assertNull(result.getApplicationType(), "Application Type should be null when not set.");
    assertNull(result.getDelegatedFunctionsDate(), "Delegated Functions Date should be null when not set.");
    assertNull(result.getPreferredAddress(), "Preferred Address should be null when not set.");
    assertNull(result.getCountry(), "Country should be null when not set.");
    assertNull(result.getHouseNameOrNumber(), "House Name or Number should be null when not set.");
    assertNull(result.getAddressLine1(), "Address Line 1 should be null when not set.");
    assertNull(result.getAddressLine2(), "Address Line 2 should be null when not set.");
    assertNull(result.getCity(), "City should be null when not set.");
    assertNull(result.getCounty(), "County should be null when not set.");
    assertNull(result.getPostcode(), "Postcode should be null when not set.");
    assertNull(result.getCareOf(), "Care Of should be null when not set.");
  }

  @Test
  @DisplayName("toProceedingAndCostSummaryDisplay should return null when ApplicationDetail is null")
  void toProceedingAndCostSummaryDisplay_nullApplicationDetail_returnsNull() {
    final ProceedingAndCostSubmissionSummaryDisplay result = mapper.toProceedingAndCostSummaryDisplay(null, null);
    assertNull(result, "Expected result to be null when application detail is null.");
  }

  @Test
  @DisplayName("toProceedingAndCostSummaryDisplay should correctly map all fields when ApplicationDetail is valid")
  void toProceedingAndCostSummaryDisplay_validApplicationDetail_returnsMappedProceedingAndCostSummary() {
    final ApplicationDetail applicationDetail = createApplicationDetailWithProceedingDetails();
    final ProceedingSubmissionSummaryMappingContext context = buildProceedingSubmissionSummaryMappingContext();

    final ProceedingAndCostSubmissionSummaryDisplay result = mapper.toProceedingAndCostSummaryDisplay(applicationDetail, context);

    assertMappedProceedingAndCostSummary(result, context);
  }

  @Test
  @DisplayName("toProceedingAndCostSummaryDisplay should return null fields when ApplicationDetail fields are not set")
  void toProceedingAndCostSummaryDisplay_emptyApplicationDetail_returnsMappedProceedingAndCostSummary() {
    final ApplicationDetail applicationDetail = new ApplicationDetail();
    final ProceedingSubmissionSummaryMappingContext context = buildProceedingSubmissionSummaryMappingContext();

    final ProceedingAndCostSubmissionSummaryDisplay result = mapper.toProceedingAndCostSummaryDisplay(applicationDetail, context);

    assertNull(result.getCaseCostLimitation(), "Case Cost Limitation should be null when not set.");
    assertThat(result.getProceedings()).as("Proceedings should be an empty list when not set.").isEmpty();
  }

  @Test
  @DisplayName("toScopeLimitationSummaryDisplayList should return null when scope limitations list is null")
  void toScopeLimitationSummaryDisplayList_nullScopeLimitations_returnsNull() {
    final List<ScopeLimitationSubmissionSummaryDisplay> result = mapper.toScopeLimitationSummaryDisplayList(null);
    assertNull(result, "Expected result to be null when scope limitations list is null.");
  }

  @Test
  @DisplayName("toScopeLimitationSummaryDisplayList should return empty list when scope limitations list is empty")
  void toScopeLimitationSummaryDisplayList_emptyScopeLimitations_returnsEmptyList() {
    final List<ScopeLimitationDetail> scopeLimitations = new ArrayList<>();
    final List<ScopeLimitationSubmissionSummaryDisplay> result = mapper.toScopeLimitationSummaryDisplayList(scopeLimitations);
    assertNotNull(result, "Result should not be null when scope limitations list is empty.");
    assertTrue(result.isEmpty(), "Result list should be empty when scope limitations list is empty.");
  }

  @Test
  @DisplayName("toScopeLimitationSummaryDisplayList should correctly map scope limitations")
  void toScopeLimitationSummaryDisplayList_validScopeLimitations_returnsMappedScopeLimitationSummaryList() {
    final ScopeLimitationDetail scopeLimitationDetail = new ScopeLimitationDetail();
    scopeLimitationDetail.setScopeLimitation(new StringDisplayValue().displayValue("Scope Limitation 1"));
    scopeLimitationDetail.setScopeLimitationWording("Scope Limitation Wording 1");

    final List<ScopeLimitationDetail> scopeLimitations = List.of(scopeLimitationDetail);
    final List<ScopeLimitationSubmissionSummaryDisplay> result = mapper.toScopeLimitationSummaryDisplayList(scopeLimitations);

    assertNotNull(result, "Result should not be null.");
    assertEquals(1, result.size(), "Scope limitations list should have correct size.");
    assertEquals("Scope Limitation 1", result.getFirst().getScopeLimitation(), "Scope Limitation should be mapped correctly.");
    assertEquals("Scope Limitation Wording 1", result.getFirst().getScopeLimitationWording(), "Scope Limitation Wording should be mapped correctly.");
  }

  @Test
  @DisplayName("toScopeLimitationSummaryDisplay should return null when ScopeLimitationDetail is null")
  void toScopeLimitationSummaryDisplay_nullScopeLimitation_returnsNull() {
    final ScopeLimitationSubmissionSummaryDisplay result = mapper.toScopeLimitationSummaryDisplay(null);
    assertNull(result, "Expected result to be null when scope limitation detail is null.");
  }

  @Test
  @DisplayName("toScopeLimitationSummaryDisplay should correctly map fields when ScopeLimitationDetail is valid")
  void toScopeLimitationSummaryDisplay_validScopeLimitation_returnsMappedScopeLimitationSummary() {
    final ScopeLimitationDetail scopeLimitationDetail = new ScopeLimitationDetail();
    scopeLimitationDetail.setScopeLimitation(new StringDisplayValue().displayValue("Scope Limitation 1"));
    scopeLimitationDetail.setScopeLimitationWording("Scope Limitation Wording 1");

    final ScopeLimitationSubmissionSummaryDisplay result = mapper.toScopeLimitationSummaryDisplay(scopeLimitationDetail);

    assertEquals("Scope Limitation 1", result.getScopeLimitation(), "Scope Limitation should be mapped correctly.");
    assertEquals("Scope Limitation Wording 1", result.getScopeLimitationWording(), "Scope Limitation Wording should be mapped correctly.");
  }

  @Test
  @DisplayName("toOpponentsAndOtherPartiesSummaryDisplay should return null when ApplicationDetail is null")
  void toOpponentsAndOtherPartiesSummaryDisplay_nullApplicationDetail_returnsNull() {
    final OpponentsAndOtherPartiesSubmissionSummaryDisplay
        result = mapper.toOpponentsAndOtherPartiesSummaryDisplay(null, null);
    assertNull(result, "Expected result to be null when application detail is null.");
  }

  @Test
  @DisplayName("toOpponentsAndOtherPartiesSummaryDisplay should correctly map opponents when ApplicationDetail is valid")
  void toOpponentsAndOtherPartiesSummaryDisplay_validApplicationDetail_returnsMappedOpponentsSummary() {
    final ApplicationDetail applicationDetail = new ApplicationDetail().opponents(List.of(createOpponentDetail()));
    final OpponentSubmissionSummaryMappingContext context = buildOpponentSubmissionSummaryMappingContext();

    final OpponentsAndOtherPartiesSubmissionSummaryDisplay result = mapper.toOpponentsAndOtherPartiesSummaryDisplay(applicationDetail, context);

    assertNotNull(result, "Result should not be null.");
    assertEquals(1, result.getOpponents().size(), "Opponents list should have correct size.");
    assertMappedOpponentSummary(result.getOpponents().getFirst(), context);
  }

  @Test
  @DisplayName("toOpponentsAndOtherPartiesSummaryDisplay should return null fields when ApplicationDetail has no opponents")
  void toOpponentsAndOtherPartiesSummaryDisplay_emptyApplicationDetail_returnsMappedOpponentsSummary() {
    final ApplicationDetail applicationDetail = new ApplicationDetail();
    final OpponentSubmissionSummaryMappingContext context = buildOpponentSubmissionSummaryMappingContext();

    final OpponentsAndOtherPartiesSubmissionSummaryDisplay result = mapper.toOpponentsAndOtherPartiesSummaryDisplay(applicationDetail, context);

    assertThat(result.getOpponents()).as("Opponents should be an empty list when not set.").isEmpty();
  }

  @Test
  @DisplayName("toOpponentSummaryDisplayList should return null when opponents list is null")
  void toOpponentSummaryDisplayList_nullOpponents_returnsNull() {
    final List<OpponentSubmissionSummaryDisplay> result = mapper.toOpponentSummaryDisplayList(null, null);
    assertNull(result, "Expected result to be null when opponents list is null.");
  }

  @Test
  @DisplayName("toOpponentSummaryDisplayList should return empty list when opponents list is empty")
  void toOpponentSummaryDisplayList_emptyOpponents_returnsEmptyList() {
    final List<OpponentDetail> opponents = new ArrayList<>();
    final OpponentSubmissionSummaryMappingContext context = buildOpponentSubmissionSummaryMappingContext();

    final List<OpponentSubmissionSummaryDisplay> result = mapper.toOpponentSummaryDisplayList(opponents, context);

    assertNotNull(result, "Result should not be null when opponents list is empty.");
    assertTrue(result.isEmpty(), "Result list should be empty when opponents list is empty.");
  }

  @Test
  @DisplayName("toOpponentSummaryDisplayList should correctly map opponents")
  void toOpponentSummaryDisplayList_validOpponents_returnsMappedOpponentSummaryList() {
    final OpponentDetail opponentDetail = createOpponentDetail();
    final List<OpponentDetail> opponents = List.of(opponentDetail);
    final OpponentSubmissionSummaryMappingContext context = buildOpponentSubmissionSummaryMappingContext();

    final List<OpponentSubmissionSummaryDisplay> result = mapper.toOpponentSummaryDisplayList(opponents, context);

    assertNotNull(result, "Result should not be null.");
    assertEquals(1, result.size(), "Opponents list should have correct size.");
    assertMappedOpponentSummary(result.getFirst(), context);
  }

  @Test
  @DisplayName("toOpponentSummaryDisplay should return null when OpponentDetail is null")
  void toOpponentSummaryDisplay_nullOpponentDetail_returnsNull() {
    final OpponentSubmissionSummaryDisplay result = mapper.toOpponentSummaryDisplay(null, null);
    assertNull(result, "Expected result to be null when opponent detail is null.");
  }

  @Test
  @DisplayName("toOpponentSummaryDisplay should correctly map fields when OpponentDetail is valid")
  void toOpponentSummaryDisplay_validOpponentDetail_returnsMappedOpponentSummary() {
    final OpponentDetail opponentDetail = createOpponentDetail();
    final OpponentSubmissionSummaryMappingContext context = buildOpponentSubmissionSummaryMappingContext();

    final OpponentSubmissionSummaryDisplay result = mapper.toOpponentSummaryDisplay(opponentDetail, context);

    assertMappedOpponentSummary(result, context);
  }

  private ApplicationDetail createApplicationDetailWithProviderDetails() {
    final ApplicationProviderDetails providerDetails = new ApplicationProviderDetails();
    providerDetails.setOffice(new IntDisplayValue().displayValue("Test Office"));
    providerDetails.setFeeEarner(new StringDisplayValue().displayValue("Test Fee Earner"));
    providerDetails.setSupervisor(new StringDisplayValue().displayValue("Test Supervisor"));
    providerDetails.setProviderCaseReference("Test Case Reference");
    providerDetails.setProviderContact(new StringDisplayValue().displayValue("Test Contact"));

    final ApplicationDetail applicationDetail = new ApplicationDetail();
    applicationDetail.setProviderDetails(providerDetails);
    return applicationDetail;
  }

  private ApplicationDetail createApplicationDetailWithGeneralDetails() {
    final ApplicationDetail applicationDetail = new ApplicationDetail();

    final StringDisplayValue categoryOfLaw = new StringDisplayValue().displayValue("Criminal Law");
    applicationDetail.setCategoryOfLaw(categoryOfLaw);

    final ApplicationType applicationType = new ApplicationType();
    applicationType.setDisplayValue("Full Application");
    final DevolvedPowersDetail devolvedPowers = new DevolvedPowersDetail();
    final Date dateUsed = new Date();
    devolvedPowers.setDateUsed(dateUsed);
    applicationType.setDevolvedPowers(devolvedPowers);
    applicationDetail.setApplicationType(applicationType);

    final AddressDetail correspondenceAddress = new AddressDetail();
    correspondenceAddress.setPreferredAddress("pAddrCode");
    correspondenceAddress.setCountry("countryCode");
    correspondenceAddress.setHouseNameOrNumber("12A");
    correspondenceAddress.setAddressLine1("Main Road");
    correspondenceAddress.setAddressLine2("Second Floor");
    correspondenceAddress.setCity("London");
    correspondenceAddress.setCounty("Greater London");
    correspondenceAddress.setPostcode("NW1 6XE");
    correspondenceAddress.setCareOf("John Doe");
    applicationDetail.setCorrespondenceAddress(correspondenceAddress);

    return applicationDetail;
  }

  private ApplicationDetail createApplicationDetailWithProceedingDetails() {
    final ProceedingDetail proceedingDetail = new ProceedingDetail();
    proceedingDetail.setMatterType(new StringDisplayValue().displayValue("Matter Type 1"));
    proceedingDetail.setProceedingType(new StringDisplayValue().displayValue("Proceeding Type 1"));
    proceedingDetail.setClientInvolvement(new StringDisplayValue().displayValue("Client Involvement 1"));
    proceedingDetail.setLevelOfService(new StringDisplayValue().displayValue("Level of Service 1"));
    proceedingDetail.setTypeOfOrder(new StringDisplayValue().id("orderCode"));

    final List<ProceedingDetail> proceedings = List.of(proceedingDetail);

    final ApplicationDetail applicationDetail = new ApplicationDetail();
    applicationDetail.setProceedings(proceedings);

    final CostStructureDetail costStructureDetail = new CostStructureDetail();
    costStructureDetail.setRequestedCostLimitation(BigDecimal.valueOf(1000));
    applicationDetail.setCosts(costStructureDetail);

    return applicationDetail;
  }

  private OpponentDetail createOpponentDetail() {
    return new OpponentDetail()
        .title("titleCode")
        .surname("Doe")
        .firstName("John")
        .type("Individual")
        .address(new AddressDetail()
            .houseNameOrNumber("12A")
            .addressLine1("Main Road")
            .addressLine2("Second Floor")
            .city("London")
            .county("Greater London")
            .country("countryCode")
            .postcode("NW1 6XE"))
        .relationshipToClient("relCode");
  }

  // Assertions

  private void assertMappedProviderSummary(final ProviderSubmissionSummaryDisplay result) {
    assertEquals("Test Office", result.getOffice(), "Office should be mapped correctly.");
    assertEquals("Test Fee Earner", result.getFeeEarner(), "Fee Earner should be mapped correctly.");
    assertEquals("Test Supervisor", result.getSupervisor(), "Supervisor should be mapped correctly.");
    assertEquals("Test Case Reference", result.getProviderCaseReference(), "Provider Case Reference should be mapped correctly.");
    assertEquals("Test Contact", result.getContactName(), "Contact Name should be mapped correctly.");
  }

  private void assertMappedGeneralSummary(final GeneralDetailsSubmissionSummaryDisplay result, final GeneralDetailsSubmissionSummaryMappingContext context) {
    assertEquals("Criminal Law", result.getCategoryOfLaw(), "Category of Law should be mapped correctly.");
    assertEquals("Full Application", result.getApplicationType(), "Application Type should be mapped correctly.");
    assertNotNull(result.getDelegatedFunctionsDate(), "Delegated Functions Date should be set.");
    assertEquals("pAddrDesc", result.getPreferredAddress(), "Preferred Address should be mapped correctly.");
    assertEquals("countryDesc", result.getCountry(), "Country should be mapped correctly.");
    assertEquals("12A", result.getHouseNameOrNumber(), "House Name or Number should be mapped correctly.");
    assertEquals("Main Road", result.getAddressLine1(), "Address Line 1 should be mapped correctly.");
    assertEquals("Second Floor", result.getAddressLine2(), "Address Line 2 should be mapped correctly.");
    assertEquals("London", result.getCity(), "City should be mapped correctly.");
    assertEquals("Greater London", result.getCounty(), "County should be mapped correctly.");
    assertEquals("NW1 6XE", result.getPostcode(), "Postcode should be mapped correctly.");
    assertEquals("John Doe", result.getCareOf(), "Care Of should be mapped correctly.");
  }

  private void assertMappedProceedingAndCostSummary(final ProceedingAndCostSubmissionSummaryDisplay result, final ProceedingSubmissionSummaryMappingContext context) {
    assertEquals("1000", result.getCaseCostLimitation(), "Case Cost Limitation should be mapped correctly.");
    assertEquals(1, result.getProceedings().size(), "Proceedings list should have correct size.");
    assertEquals("Matter Type 1", result.getProceedings().getFirst().getMatterType(), "Matter Type should be mapped correctly.");
    assertEquals("Proceeding Type 1", result.getProceedings().getFirst().getProceeding(), "Proceeding Type should be mapped correctly.");
    assertEquals("Client Involvement 1", result.getProceedings().getFirst().getClientInvolvementType(), "Client Involvement Type should be mapped correctly.");
    assertEquals("Level of Service 1", result.getProceedings().getFirst().getFormOfCivilLegalService(), "Level of Service should be mapped correctly.");
    assertEquals("orderDesc", result.getProceedings().getFirst().getTypeOfOrder(), "Type of Order should be mapped correctly.");
  }

  private void assertMappedOpponentSummary(final OpponentSubmissionSummaryDisplay result, final OpponentSubmissionSummaryMappingContext context) {
    assertEquals("titleDesc", result.getTitle(), "Title should be mapped correctly.");
    assertEquals("Doe", result.getSurname(), "Surname should be mapped correctly.");
    assertEquals("John", result.getFirstName(), "First Name should be mapped correctly.");
    assertEquals("12A", result.getHouseNameOrNumber(), "House Name or Number should be mapped correctly.");
    assertEquals("Main Road", result.getAddressLine1(), "Address Line 1 should be mapped correctly.");
    assertEquals("Second Floor", result.getAddressLine2(), "Address Line 2 should be mapped correctly.");
    assertEquals("London", result.getCity(), "City should be mapped correctly.");
    assertEquals("Greater London", result.getCounty(), "County should be mapped correctly.");
    assertEquals("countryDesc", result.getCountry(), "Country should be mapped correctly.");
    assertEquals("NW1 6XE", result.getPostcode(), "Postcode should be mapped correctly.");
    assertEquals("relDesc", result.getRelationshipToClient(), "Relationship to Client should be mapped correctly.");
  }

  // Context building methods

  private GeneralDetailsSubmissionSummaryMappingContext buildGeneralDetailsSubmissionSummaryMappingContext() {
    final CommonLookupDetail preferredAddressLookup = new CommonLookupDetail();
    preferredAddressLookup.addContentItem(new CommonLookupValueDetail().code("pAddrCode").description("pAddrDesc"));

    final CommonLookupDetail countryLookup = new CommonLookupDetail();
    countryLookup.addContentItem(new CommonLookupValueDetail().code("countryCode").description("countryDesc"));

    return GeneralDetailsSubmissionSummaryMappingContext.builder()
        .preferredAddress(preferredAddressLookup)
        .country(countryLookup)
        .build();
  }

  private ProceedingSubmissionSummaryMappingContext buildProceedingSubmissionSummaryMappingContext() {
    final CommonLookupDetail typeOfOrderLookup = new CommonLookupDetail();
    typeOfOrderLookup.addContentItem(new CommonLookupValueDetail().code("orderCode").description("orderDesc"));

    return ProceedingSubmissionSummaryMappingContext.builder()
        .typeOfOrder(typeOfOrderLookup)
        .build();
  }

  private OpponentSubmissionSummaryMappingContext buildOpponentSubmissionSummaryMappingContext() {
    final CommonLookupDetail contactTitleLookup = new CommonLookupDetail();
    contactTitleLookup.addContentItem(new CommonLookupValueDetail().code("titleCode").description("titleDesc"));

    final CommonLookupDetail relationshipToClientLookup = new CommonLookupDetail();
    relationshipToClientLookup.addContentItem(new CommonLookupValueDetail().code("relCode").description("relDesc"));

    final CommonLookupDetail countryLookup = new CommonLookupDetail();
    countryLookup.addContentItem(new CommonLookupValueDetail().code("countryCode").description("countryDesc"));

    return OpponentSubmissionSummaryMappingContext.builder()
        .contactTitle(contactTitleLookup)
        .relationshipToClient(relationshipToClientLookup)
        .country(countryLookup)
        .build();
  }

  @Test
  @DisplayName("toDeclarationFormDataDynamicOptionList should return null when DeclarationLookupDetail is null")
  void toDeclarationFormDataDynamicOptionList_nullDeclarationLookupDetail_returnsNull() {
    final List<DynamicCheckbox> result = mapper.toDeclarationFormDataDynamicOptionList(null);
    assertNull(result, "Expected result to be null when DeclarationLookupDetail is null.");
  }

  @Test
  @DisplayName("toDeclarationFormDataDynamicOptionList should correctly map content when DeclarationLookupDetail is valid")
  void toDeclarationFormDataDynamicOptionList_validDeclarationLookupDetail_returnsMappedDynamicOptionList() {
    final DeclarationLookupValueDetail declarationValueDetail = new DeclarationLookupValueDetail();
    declarationValueDetail.setText("Checkbox Option 1");

    final DeclarationLookupDetail declarationLookupDetail = new DeclarationLookupDetail();
    declarationLookupDetail.setContent(List.of(declarationValueDetail));

    final List<DynamicCheckbox> result = mapper.toDeclarationFormDataDynamicOptionList(declarationLookupDetail);

    assertNotNull(result, "Result should not be null.");
    assertEquals(1, result.size(), "Result list should have correct size.");
    assertEquals("Checkbox Option 1", result.getFirst().getFieldValueDisplayValue(), "Field value display should be mapped correctly.");
    assertFalse(result.getFirst().isChecked(), "Checked should be false by default.");
  }

  @Test
  @DisplayName("toDeclarationFormDataDynamicOption should correctly map fields when DeclarationLookupValueDetail is valid")
  void toDeclarationFormDataDynamicOption_validDeclarationLookupValueDetail_returnsMappedDynamicCheckbox() {
    final DeclarationLookupValueDetail declarationValueDetail = new DeclarationLookupValueDetail();
    declarationValueDetail.setText("Checkbox Option 1");

    final DynamicCheckbox result = mapper.toDeclarationFormDataDynamicOption(declarationValueDetail);

    assertNotNull(result, "Result should not be null.");
    assertEquals("Checkbox Option 1", result.getFieldValueDisplayValue(), "Field value display should be mapped correctly.");
    assertFalse(result.isChecked(), "Checked should be false by default.");
  }


}
