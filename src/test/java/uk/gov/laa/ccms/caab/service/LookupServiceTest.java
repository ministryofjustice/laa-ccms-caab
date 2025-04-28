package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.laa.ccms.caab.constants.ClientActionConstants.ACTION_VIEW;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CASE_ADDRESS_OPTION;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_LANGUAGE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_METHOD;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_COURTS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DISABILITY;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ETHNIC_ORIGIN;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_GENDER;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_MARITAL_STATUS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_PROCEEDING_ORDER_TYPE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_RELATIONSHIP_TO_CLIENT;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataContactDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataMonitoringDetails;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.caab.mapper.context.submission.GeneralDetailsSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.submission.OpponentSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.caab.mapper.context.submission.ProceedingSubmissionSummaryMappingContext;
import uk.gov.laa.ccms.data.model.AssessmentSummaryEntityLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupDetail;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupValueDetail;
import uk.gov.laa.ccms.data.model.ClientInvolvementTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.DeclarationLookupDetail;
import uk.gov.laa.ccms.data.model.LevelOfServiceLookupDetail;
import uk.gov.laa.ccms.data.model.MatterTypeLookupDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetails;
import uk.gov.laa.ccms.data.model.ProceedingDetail;
import uk.gov.laa.ccms.data.model.ProceedingDetails;
import uk.gov.laa.ccms.data.model.ProviderRequestTypeLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetails;
import uk.gov.laa.ccms.data.model.StageEndLookupDetail;

@ExtendWith(MockitoExtension.class)
class LookupServiceTest {
  @Mock
  private EbsApiClient ebsApiClient;

  @InjectMocks
  private LookupService lookupService;

  @Test
  @DisplayName("getMatterTypes returns data successfully")
  void getMatterTypes_success() {
    final String categoryOfLaw = "LAW1";
    final MatterTypeLookupDetail mockMatterTypeDetail = new MatterTypeLookupDetail(); // Populate as needed

    when(ebsApiClient.getMatterTypes(categoryOfLaw)).thenReturn(Mono.just(mockMatterTypeDetail));

    final Mono<MatterTypeLookupDetail> result = lookupService.getMatterTypes(categoryOfLaw);

    StepVerifier.create(result)
        .expectNext(mockMatterTypeDetail)
        .verifyComplete();

    verify(ebsApiClient).getMatterTypes(categoryOfLaw);
  }

  @Test
  @DisplayName("getProceedings returns data successfully")
  void getProceedings_success() {
    final ProceedingDetail searchCriteria = new ProceedingDetail(); // Populate as needed
    final Boolean larScopeFlag = true;
    final String applicationType = "APP_TYPE";
    final Boolean isLead = true;
    final ProceedingDetails mockProceedingDetails = new ProceedingDetails(); // Populate as needed

    when(ebsApiClient.getProceedings(searchCriteria, larScopeFlag, applicationType, isLead))
        .thenReturn(Mono.just(mockProceedingDetails));

    final Mono<ProceedingDetails> result = lookupService.getProceedings(searchCriteria, larScopeFlag, applicationType, isLead);

    StepVerifier.create(result)
        .expectNext(mockProceedingDetails)
        .verifyComplete();

    verify(ebsApiClient).getProceedings(searchCriteria, larScopeFlag, applicationType, isLead);
  }

  @Test
  @DisplayName("getProceedingClientInvolvementTypes returns data successfully")
  void getProceedingClientInvolvementTypes_success() {
    final String proceedingCode = "PROC_CODE";
    final ClientInvolvementTypeLookupDetail mockClientInvolvementDetail = new ClientInvolvementTypeLookupDetail(); // Populate as needed

    when(ebsApiClient.getClientInvolvementTypes(proceedingCode))
        .thenReturn(Mono.just(mockClientInvolvementDetail));

    final Mono<ClientInvolvementTypeLookupDetail> result = lookupService.getProceedingClientInvolvementTypes(proceedingCode);

    StepVerifier.create(result)
        .expectNext(mockClientInvolvementDetail)
        .verifyComplete();

    verify(ebsApiClient).getClientInvolvementTypes(proceedingCode);
  }

  @Test
  @DisplayName("getProceedingLevelOfServiceTypes returns data successfully")
  void getProceedingLevelOfServiceTypes_success() {
    final String categoryOfLaw = "LAW1";
    final String proceedingCode = "PROC_CODE";
    final String matterType = "MATTER_TYPE";
    final LevelOfServiceLookupDetail mockLevelOfServiceDetail = new LevelOfServiceLookupDetail(); // Populate as needed

    when(ebsApiClient.getLevelOfServiceTypes(proceedingCode, categoryOfLaw, matterType))
        .thenReturn(Mono.just(mockLevelOfServiceDetail));

    final Mono<LevelOfServiceLookupDetail> result = lookupService.getProceedingLevelOfServiceTypes(categoryOfLaw, proceedingCode, matterType);

    StepVerifier.create(result)
        .expectNext(mockLevelOfServiceDetail)
        .verifyComplete();

    verify(ebsApiClient).getLevelOfServiceTypes(proceedingCode, categoryOfLaw, matterType);
  }

  @Test
  @DisplayName("getScopeLimitationDetails returns data successfully")
  void getScopeLimitationDetails_success() {
    final ScopeLimitationDetail searchCriteria = new ScopeLimitationDetail(); // Populate as needed
    final ScopeLimitationDetails mockScopeDetails = new ScopeLimitationDetails(); // Populate as needed

    when(ebsApiClient.getScopeLimitations(searchCriteria)).thenReturn(Mono.just(mockScopeDetails));

    final Mono<ScopeLimitationDetails> result = lookupService.getScopeLimitationDetails(searchCriteria);

    StepVerifier.create(result)
        .expectNext(mockScopeDetails)
        .verifyComplete();

    verify(ebsApiClient).getScopeLimitations(searchCriteria);
  }

  @Test
  @DisplayName("getCourts with courtCode and description returns data successfully")
  void getCourts_withCourtCodeAndDescription_success() {
    final String courtCode = "COURT123";
    final String description = "High Court";
    final CommonLookupDetail mockCourtDetails = new CommonLookupDetail(); // Populate as needed

    when(ebsApiClient.getCommonValues(COMMON_VALUE_COURTS, "*" + courtCode + "*", "*" + description.toUpperCase() + "*"))
        .thenReturn(Mono.just(mockCourtDetails));

    final Mono<CommonLookupDetail> result = lookupService.getCourts(courtCode, description);

    StepVerifier.create(result)
        .expectNext(mockCourtDetails)
        .verifyComplete();

    verify(ebsApiClient).getCommonValues(COMMON_VALUE_COURTS, "*" + courtCode + "*", "*" + description.toUpperCase() + "*");
  }

  @Test
  @DisplayName("getCourts with only courtCode returns data successfully")
  void getCourts_withCourtCodeOnly_success() {
    final String courtCode = "COURT123";
    final CommonLookupDetail mockCourtDetails = new CommonLookupDetail(); // Populate as needed

    when(ebsApiClient.getCommonValues(COMMON_VALUE_COURTS, "*" + courtCode + "*", null))
        .thenReturn(Mono.just(mockCourtDetails));

    final Mono<CommonLookupDetail> result = lookupService.getCourts(courtCode);

    StepVerifier.create(result)
        .expectNext(mockCourtDetails)
        .verifyComplete();

    verify(ebsApiClient).getCommonValues(COMMON_VALUE_COURTS, "*" + courtCode + "*", null);
  }

  @Test
  @DisplayName("getOutcomeResults returns data successfully")
  void getOutcomeResults_success() {
    final String proceedingCode = "PRO123";
    final String outcomeResult = "OUTCOME";
    final OutcomeResultLookupDetail mockOutcomeResultDetail = new OutcomeResultLookupDetail(); // Populate as needed

    when(ebsApiClient.getOutcomeResults(proceedingCode, outcomeResult)).thenReturn(Mono.just(mockOutcomeResultDetail));

    final Mono<OutcomeResultLookupDetail> result = lookupService.getOutcomeResults(proceedingCode, outcomeResult);

    StepVerifier.create(result)
        .expectNext(mockOutcomeResultDetail)
        .verifyComplete();

    verify(ebsApiClient).getOutcomeResults(proceedingCode, outcomeResult);
  }

  @Test
  @DisplayName("getStageEnds returns data successfully")
  void getStageEnds_success() {
    final String proceedingCode = "PRO123";
    final String stageEnd = "STAGE_END";
    final StageEndLookupDetail mockStageEndDetail = new StageEndLookupDetail(); // Populate as needed

    when(ebsApiClient.getStageEnds(proceedingCode, stageEnd)).thenReturn(Mono.just(mockStageEndDetail));

    final Mono<StageEndLookupDetail> result = lookupService.getStageEnds(proceedingCode, stageEnd);

    StepVerifier.create(result)
        .expectNext(mockStageEndDetail)
        .verifyComplete();

    verify(ebsApiClient).getStageEnds(proceedingCode, stageEnd);
  }

  @Test
  @DisplayName("getPriorAuthorityTypes with no parameters returns data successfully")
  void getPriorAuthorityTypes_noParameters_success() {
    final PriorAuthorityTypeDetails mockPriorAuthorityDetails = new PriorAuthorityTypeDetails(); // Populate as needed

    when(ebsApiClient.getPriorAuthorityTypes(null, null)).thenReturn(Mono.just(mockPriorAuthorityDetails));

    final Mono<PriorAuthorityTypeDetails> result = lookupService.getPriorAuthorityTypes();

    StepVerifier.create(result)
        .expectNext(mockPriorAuthorityDetails)
        .verifyComplete();

    verify(ebsApiClient).getPriorAuthorityTypes(null, null);
  }

  @Test
  @DisplayName("getPriorAuthorityTypes with code and valueRequired returns data successfully")
  void getPriorAuthorityTypes_withParameters_success() {
    final String code = "AUTH123";
    final Boolean valueRequired = true;
    final PriorAuthorityTypeDetails mockPriorAuthorityDetails = new PriorAuthorityTypeDetails(); // Populate as needed

    when(ebsApiClient.getPriorAuthorityTypes(code, valueRequired)).thenReturn(Mono.just(mockPriorAuthorityDetails));

    final Mono<PriorAuthorityTypeDetails> result = lookupService.getPriorAuthorityTypes(code, valueRequired);

    StepVerifier.create(result)
        .expectNext(mockPriorAuthorityDetails)
        .verifyComplete();

    verify(ebsApiClient).getPriorAuthorityTypes(code, valueRequired);
  }

  @Test
  @DisplayName("getPriorAuthorityType returns the correct data successfully")
  void getPriorAuthorityType_success() {
    final String code = "AUTH123";
    final PriorAuthorityTypeDetail mockPriorAuthorityDetail = new PriorAuthorityTypeDetail(); // Populate as needed
    final PriorAuthorityTypeDetails mockPriorAuthorityDetails = new PriorAuthorityTypeDetails().addContentItem(mockPriorAuthorityDetail);

    when(ebsApiClient.getPriorAuthorityTypes(code, null)).thenReturn(Mono.just(mockPriorAuthorityDetails));

    final Mono<Optional<PriorAuthorityTypeDetail>> result = lookupService.getPriorAuthorityType(code);

    StepVerifier.create(result)
        .expectNext(Optional.of(mockPriorAuthorityDetail))
        .verifyComplete();

    verify(ebsApiClient).getPriorAuthorityTypes(code, null);
  }

  @Test
  @DisplayName("getPriorAuthorityType returns empty when no match is found")
  void getPriorAuthorityType_noMatch_returnsEmpty() {
    final String code = "AUTH123";
    final PriorAuthorityTypeDetails mockPriorAuthorityDetails = new PriorAuthorityTypeDetails(); // No content

    when(ebsApiClient.getPriorAuthorityTypes(code, null)).thenReturn(Mono.just(mockPriorAuthorityDetails));

    final Mono<Optional<PriorAuthorityTypeDetail>> result = lookupService.getPriorAuthorityType(code);

    StepVerifier.create(result)
        .expectNext(Optional.empty())
        .verifyComplete();

    verify(ebsApiClient).getPriorAuthorityTypes(code, null);
  }

  @Test
  void getCountries_returnsData() {
    final CommonLookupValueDetail commonValue = new CommonLookupValueDetail().code("GBR");
    final CommonLookupDetail commonValues = new CommonLookupDetail().addContentItem(commonValue);

    when(ebsApiClient.getCountries()).thenReturn(Mono.just(commonValues));

    final Mono<CommonLookupDetail> commonLookupDetailMono = lookupService.getCountries();
    StepVerifier.create(commonLookupDetailMono)
        .expectNext(commonValues)
        .verifyComplete();
  }

  @Test
  void getCountry_returnsData() {
    final CommonLookupValueDetail commonValue = new CommonLookupValueDetail().code("GBR");
    final CommonLookupDetail commonValues = new CommonLookupDetail().addContentItem(commonValue);

    when(ebsApiClient.getCountries()).thenReturn(Mono.just(commonValues));

    final Mono<Optional<CommonLookupValueDetail>> commonLookupDetailMono =
        lookupService.getCountry("GBR");
    StepVerifier.create(commonLookupDetailMono)
        .expectNextMatches(result -> "GBR".equals(result.get().getCode()))
        .verifyComplete();
  }

  @Test
  void getCategoriesOfLaw_returnsData() {
    final CategoryOfLawLookupValueDetail commonValue =
        new CategoryOfLawLookupValueDetail().code("CAT1");
    final CategoryOfLawLookupDetail commonValues =
        new CategoryOfLawLookupDetail().addContentItem(commonValue);

    when(ebsApiClient.getCategoriesOfLaw(null, null, null))
        .thenReturn(Mono.just(commonValues));

    final Mono<CategoryOfLawLookupDetail> commonLookupDetailMono =
        lookupService.getCategoriesOfLaw();
    StepVerifier.create(commonLookupDetailMono)
        .expectNextMatches(result -> result == commonValues)
        .verifyComplete();
  }

  @Test
  void getCategoryOfLaw_returnsData() {
    final CategoryOfLawLookupValueDetail commonValue = new CategoryOfLawLookupValueDetail()
        .code("CAT1")
        .matterTypeDescription("DESC")
        .copyCostLimit(Boolean.TRUE);
    final CategoryOfLawLookupDetail commonValues =
        new CategoryOfLawLookupDetail().addContentItem(commonValue);

    when(ebsApiClient.getCategoriesOfLaw(commonValue.getCode(),
        null,
        null))
        .thenReturn(Mono.just(commonValues));

    final Mono<Optional<CategoryOfLawLookupValueDetail>> commonLookupDetailMono =
        lookupService.getCategoryOfLaw(commonValue.getCode());
    StepVerifier.create(commonLookupDetailMono)
        .expectNextMatches(result -> result.get() == commonValue)
        .verifyComplete();
  }

  @Test
  @DisplayName("getAwardTypes with no parameters returns data successfully")
  void getAwardTypes_noParameters_success() {
    final AwardTypeLookupDetail mockAwardTypeDetails = new AwardTypeLookupDetail(); // Populate as needed

    when(ebsApiClient.getAwardTypes(null, null)).thenReturn(Mono.just(mockAwardTypeDetails));

    final Mono<AwardTypeLookupDetail> result = lookupService.getAwardTypes();

    StepVerifier.create(result)
        .expectNext(mockAwardTypeDetails)
        .verifyComplete();

    verify(ebsApiClient).getAwardTypes(null, null);
  }

  @Test
  @DisplayName("getAwardTypes with parameters returns data successfully")
  void getAwardTypes_withParameters_success() {
    final String code = "AWARD123";
    final String awardType = "TYPE1";
    final AwardTypeLookupDetail mockAwardTypeDetails = new AwardTypeLookupDetail(); // Populate as needed

    when(ebsApiClient.getAwardTypes(code, awardType)).thenReturn(Mono.just(mockAwardTypeDetails));

    final Mono<AwardTypeLookupDetail> result = lookupService.getAwardTypes(code, awardType);

    StepVerifier.create(result)
        .expectNext(mockAwardTypeDetails)
        .verifyComplete();

    verify(ebsApiClient).getAwardTypes(code, awardType);
  }

  @Test
  @DisplayName("getPersonToCaseRelationships returns data successfully")
  void getPersonToCaseRelationships_success() {
    final RelationshipToCaseLookupDetail mockRelationshipDetail = new RelationshipToCaseLookupDetail(); // Populate as needed

    when(ebsApiClient.getPersonToCaseRelationships(null, null)).thenReturn(Mono.just(mockRelationshipDetail));

    final Mono<RelationshipToCaseLookupDetail> result = lookupService.getPersonToCaseRelationships();

    StepVerifier.create(result)
        .expectNext(mockRelationshipDetail)
        .verifyComplete();

    verify(ebsApiClient).getPersonToCaseRelationships(null, null);
  }

  @Test
  @DisplayName("getPersonToCaseRelationship returns the correct data successfully")
  void getPersonToCaseRelationship_success() {
    final String code = "REL123";
    final RelationshipToCaseLookupValueDetail mockRelationshipValueDetail = new RelationshipToCaseLookupValueDetail(); // Populate as needed
    final RelationshipToCaseLookupDetail mockRelationshipDetail = new RelationshipToCaseLookupDetail().addContentItem(mockRelationshipValueDetail);

    when(ebsApiClient.getPersonToCaseRelationships(code, null)).thenReturn(Mono.just(mockRelationshipDetail));

    final Mono<Optional<RelationshipToCaseLookupValueDetail>> result = lookupService.getPersonToCaseRelationship(code);

    StepVerifier.create(result)
        .expectNext(Optional.of(mockRelationshipValueDetail))
        .verifyComplete();

    verify(ebsApiClient).getPersonToCaseRelationships(code, null);
  }

  @Test
  @DisplayName("getPersonToCaseRelationship returns empty when no match is found")
  void getPersonToCaseRelationship_noMatch_returnsEmpty() {
    final String code = "REL123";
    final RelationshipToCaseLookupDetail mockRelationshipDetail = new RelationshipToCaseLookupDetail(); // No content

    when(ebsApiClient.getPersonToCaseRelationships(code, null)).thenReturn(Mono.just(mockRelationshipDetail));

    final Mono<Optional<RelationshipToCaseLookupValueDetail>> result = lookupService.getPersonToCaseRelationship(code);

    StepVerifier.create(result)
        .expectNext(Optional.empty())
        .verifyComplete();

    verify(ebsApiClient).getPersonToCaseRelationships(code, null);
  }

  @Test
  @DisplayName("getOrganisationToCaseRelationships returns data successfully")
  void getOrganisationToCaseRelationships_success() {
    final RelationshipToCaseLookupDetail mockRelationshipDetail = new RelationshipToCaseLookupDetail(); // Populate as needed

    when(ebsApiClient.getOrganisationToCaseRelationshipValues(null, null)).thenReturn(Mono.just(mockRelationshipDetail));

    final Mono<RelationshipToCaseLookupDetail> result = lookupService.getOrganisationToCaseRelationships();

    StepVerifier.create(result)
        .expectNext(mockRelationshipDetail)
        .verifyComplete();

    verify(ebsApiClient).getOrganisationToCaseRelationshipValues(null, null);
  }

  @Test
  @DisplayName("getOrganisationToCaseRelationship returns the correct data successfully")
  void getOrganisationToCaseRelationship_success() {
    final String code = "ORG123";
    final RelationshipToCaseLookupValueDetail mockRelationshipValueDetail = new RelationshipToCaseLookupValueDetail(); // Populate as needed
    final RelationshipToCaseLookupDetail mockRelationshipDetail = new RelationshipToCaseLookupDetail().addContentItem(mockRelationshipValueDetail);

    when(ebsApiClient.getOrganisationToCaseRelationshipValues(code, null)).thenReturn(Mono.just(mockRelationshipDetail));

    final Mono<Optional<RelationshipToCaseLookupValueDetail>> result = lookupService.getOrganisationToCaseRelationship(code);

    StepVerifier.create(result)
        .expectNext(Optional.of(mockRelationshipValueDetail))
        .verifyComplete();

    verify(ebsApiClient).getOrganisationToCaseRelationshipValues(code, null);
  }

  @Test
  @DisplayName("getOrganisationToCaseRelationship returns empty when no match is found")
  void getOrganisationToCaseRelationship_noMatch_returnsEmpty() {
    final String code = "ORG123";
    final RelationshipToCaseLookupDetail mockRelationshipDetail = new RelationshipToCaseLookupDetail(); // No content

    when(ebsApiClient.getOrganisationToCaseRelationshipValues(code, null)).thenReturn(Mono.just(mockRelationshipDetail));

    final Mono<Optional<RelationshipToCaseLookupValueDetail>> result = lookupService.getOrganisationToCaseRelationship(code);

    StepVerifier.create(result)
        .expectNext(Optional.empty())
        .verifyComplete();

    verify(ebsApiClient).getOrganisationToCaseRelationshipValues(code, null);
  }

  @Test
  @DisplayName("getCaseStatusValues with no parameters returns data successfully")
  void getCaseStatusValues_noParameters_success() {
    final CaseStatusLookupDetail mockCaseStatusDetail = new CaseStatusLookupDetail(); // Populate as needed

    when(ebsApiClient.getCaseStatusValues(null)).thenReturn(Mono.just(mockCaseStatusDetail));

    final Mono<CaseStatusLookupDetail> result = lookupService.getCaseStatusValues();

    StepVerifier.create(result)
        .expectNext(mockCaseStatusDetail)
        .verifyComplete();

    verify(ebsApiClient).getCaseStatusValues(null);
  }

  @Test
  @DisplayName("getCaseStatusValues with copyAllowed parameter returns data successfully")
  void getCaseStatusValues_withCopyAllowed_success() {
    final Boolean copyAllowed = true;
    final CaseStatusLookupDetail mockCaseStatusDetail = new CaseStatusLookupDetail();

    when(ebsApiClient.getCaseStatusValues(copyAllowed)).thenReturn(Mono.just(mockCaseStatusDetail));

    final Mono<CaseStatusLookupDetail> result = lookupService.getCaseStatusValues(copyAllowed);

    StepVerifier.create(result)
        .expectNext(mockCaseStatusDetail)
        .verifyComplete();

    verify(ebsApiClient).getCaseStatusValues(copyAllowed);
  }

  @Test
  @DisplayName("getAssessmentSummaryAttributes returns data successfully")
  void getAssessmentSummaryAttributes_success() {
    final String summaryType = "SUMMARY_TYPE";
    final AssessmentSummaryEntityLookupDetail mockAssessmentSummaryDetail = new AssessmentSummaryEntityLookupDetail();

    when(ebsApiClient.getAssessmentSummaryAttributes(summaryType)).thenReturn(Mono.just(mockAssessmentSummaryDetail));

    final Mono<AssessmentSummaryEntityLookupDetail> result = lookupService.getAssessmentSummaryAttributes(summaryType);

    StepVerifier.create(result)
        .expectNext(mockAssessmentSummaryDetail)
        .verifyComplete();

    verify(ebsApiClient).getAssessmentSummaryAttributes(summaryType);
  }

  @Test
  @DisplayName("Test getClientLookups method")
  void getClientLookups() {
    final String TITLE = "Mr.";
    final String COUNTRY_OF_ORIGIN = "UK";
    final String MARITAL_STATUS = "Single";
    final String GENDER = "Male";
    final String CORRESPONDENCE_METHOD = "Email";
    final String ETHNIC_ORIGIN = "Asian";
    final String DISABILITY = "None";
    final String COUNTRY = "USA";
    final String CORRESPONDENCE_LANGUAGE = "English";

    final ClientFlowFormData clientFlowFormData = new ClientFlowFormData(ACTION_VIEW);
    final ClientFormDataBasicDetails basicDetails = new ClientFormDataBasicDetails();
    basicDetails.setTitle(TITLE);
    basicDetails.setCountryOfOrigin(COUNTRY_OF_ORIGIN);
    basicDetails.setMaritalStatus(MARITAL_STATUS);
    basicDetails.setGender(GENDER);
    clientFlowFormData.setBasicDetails(basicDetails);

    final ClientFormDataContactDetails contactDetails = new ClientFormDataContactDetails();
    contactDetails.setCorrespondenceMethod(CORRESPONDENCE_METHOD);
    contactDetails.setCorrespondenceLanguage(CORRESPONDENCE_LANGUAGE);
    clientFlowFormData.setContactDetails(contactDetails);

    final ClientFormDataMonitoringDetails monitoringDetails = new ClientFormDataMonitoringDetails();
    monitoringDetails.setEthnicOrigin(ETHNIC_ORIGIN);
    monitoringDetails.setDisability(DISABILITY);
    clientFlowFormData.setMonitoringDetails(monitoringDetails);

    final ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();
    addressDetails.setCountry(COUNTRY);
    clientFlowFormData.setAddressDetails(addressDetails);

    final CommonLookupValueDetail titleLookupValueDetail = new CommonLookupValueDetail();
    final CommonLookupValueDetail countryLookupValueDetail = new CommonLookupValueDetail();
    final CommonLookupValueDetail maritalStatusLookupValueDetail = new CommonLookupValueDetail();
    final CommonLookupValueDetail genderLookupValueDetail = new CommonLookupValueDetail();
    final CommonLookupValueDetail correspondenceMethodLookupValueDetail = new CommonLookupValueDetail();
    final CommonLookupValueDetail ethnicityLookupValueDetail = new CommonLookupValueDetail();
    final CommonLookupValueDetail disabilityLookupValueDetail = new CommonLookupValueDetail();
    final CommonLookupValueDetail correspondenceLanguageLookupValueDetail = new CommonLookupValueDetail();

    final CommonLookupDetail commonLookupDetailWithCountry = new CommonLookupDetail();
    commonLookupDetailWithCountry.setContent(List.of(countryLookupValueDetail));

    final CommonLookupDetail commonLookupDetailWithValues = new CommonLookupDetail();
    commonLookupDetailWithValues.setContent(List.of(
        titleLookupValueDetail,
        maritalStatusLookupValueDetail,
        genderLookupValueDetail,
        correspondenceMethodLookupValueDetail,
        ethnicityLookupValueDetail,
        disabilityLookupValueDetail,
        correspondenceLanguageLookupValueDetail
    ));

    when(ebsApiClient.getCommonValues(COMMON_VALUE_CONTACT_TITLE, TITLE)).thenReturn(Mono.just(commonLookupDetailWithCountry));
    when(ebsApiClient.getCommonValues(COMMON_VALUE_MARITAL_STATUS, MARITAL_STATUS)).thenReturn(Mono.just(commonLookupDetailWithCountry));
    when(ebsApiClient.getCommonValues(COMMON_VALUE_GENDER, GENDER)).thenReturn(Mono.just(commonLookupDetailWithCountry));
    when(ebsApiClient.getCommonValues(COMMON_VALUE_CORRESPONDENCE_METHOD, CORRESPONDENCE_METHOD)).thenReturn(Mono.just(commonLookupDetailWithCountry));
    when(ebsApiClient.getCommonValues(COMMON_VALUE_ETHNIC_ORIGIN, ETHNIC_ORIGIN)).thenReturn(Mono.just(commonLookupDetailWithCountry));
    when(ebsApiClient.getCommonValues(COMMON_VALUE_DISABILITY, DISABILITY)).thenReturn(Mono.just(commonLookupDetailWithCountry));
    when(ebsApiClient.getCommonValues(COMMON_VALUE_CORRESPONDENCE_LANGUAGE, CORRESPONDENCE_LANGUAGE)).thenReturn(Mono.just(commonLookupDetailWithCountry));

    when(ebsApiClient.getCountries()).thenReturn(Mono.just(commonLookupDetailWithCountry));

    List<Pair<String, Mono<Optional<CommonLookupValueDetail>>>> lookups =
        lookupService.getClientLookups(clientFlowFormData);

    assertEquals(9, lookups.size());

    assertEquals("contactTitle", lookups.getFirst().getLeft());
    assertEquals(titleLookupValueDetail, lookups.getFirst().getRight().block().get());

    assertEquals("countryOfOrigin", lookups.get(1).getLeft());

    assertEquals("maritalStatus", lookups.get(2).getLeft());
    assertEquals(maritalStatusLookupValueDetail, lookups.get(2).getRight().block().get());

    assertEquals("gender", lookups.get(3).getLeft());
    assertEquals(genderLookupValueDetail, lookups.get(3).getRight().block().get());

    assertEquals("correspondenceMethod", lookups.get(4).getLeft());
    assertEquals(correspondenceMethodLookupValueDetail, lookups.get(4).getRight().block().get());

    assertEquals("ethnicity", lookups.get(5).getLeft());
    assertEquals(ethnicityLookupValueDetail, lookups.get(5).getRight().block().get());

    assertEquals("disability", lookups.get(6).getLeft());
    assertEquals(disabilityLookupValueDetail, lookups.get(6).getRight().block().get());

    assertEquals("country", lookups.get(7).getLeft());

    assertEquals("correspondenceLanguage", lookups.get(8).getLeft());
    assertEquals(correspondenceLanguageLookupValueDetail, lookups.get(8).getRight().block().get());
  }

  @Test
  @DisplayName("addCommonLookupsToModel adds lookups to model and returns list of CommonLookupValueDetail")
  void addCommonLookupsToModel_success() {

    final Model model = mock(Model.class);

    final CommonLookupValueDetail detail1 = new CommonLookupValueDetail().code("CODE1");
    final CommonLookupValueDetail detail2 = new CommonLookupValueDetail().code("CODE2");
    final List<Pair<String, Mono<Optional<CommonLookupValueDetail>>>> lookups = List.of(
        Pair.of("lookup1", Mono.just(Optional.of(detail1))),
        Pair.of("lookup2", Mono.just(Optional.of(detail2)))
    );

    final Mono<Void> result = lookupService.addCommonLookupsToModel(lookups, model);

    StepVerifier.create(result)
        .verifyComplete();

    verify(model).addAttribute("lookup1", detail1);
    verify(model).addAttribute("lookup2", detail2);
  }

  @Test
  @DisplayName("getCommonLookupsMap returns map of lookup details")
  void getCommonLookupsMap_success() {
    final CommonLookupValueDetail detail1 = new CommonLookupValueDetail().code("CODE1");
    final CommonLookupValueDetail detail2 = new CommonLookupValueDetail().code("CODE2");
    final List<Pair<String, Mono<Optional<CommonLookupValueDetail>>>> lookups = List.of(
        Pair.of("lookup1", Mono.just(Optional.of(detail1))),
        Pair.of("lookup2", Mono.just(Optional.of(detail2)))
    );

    final Mono<HashMap<String, CommonLookupValueDetail>> result = lookupService.getCommonLookupsMap(lookups);

    StepVerifier.create(result)
        .expectNextMatches(map ->
            map.size() == 2 &&
                map.get("lookup1").equals(detail1) &&
                map.get("lookup2").equals(detail2))
        .verifyComplete();
  }


  @Test
  @DisplayName("getProceedingSubmissionMappingContext returns correct ProceedingSubmissionSummaryMappingContext")
  void getProceedingSubmissionMappingContext_success() {
    final CommonLookupDetail mockTypeOfOrder = new CommonLookupDetail(); // Populate this as needed
    when(lookupService.getCommonValues(COMMON_VALUE_PROCEEDING_ORDER_TYPE)).thenReturn(Mono.just(mockTypeOfOrder));

    final Mono<ProceedingSubmissionSummaryMappingContext> result = lookupService.getProceedingSubmissionMappingContext();

    StepVerifier.create(result)
        .expectNextMatches(context -> context.getTypeOfOrder().equals(mockTypeOfOrder))
        .verifyComplete();
  }


  @Test
  @DisplayName("getOpponentSubmissionMappingContext returns correct OpponentSubmissionSummaryMappingContext")
  void getOpponentSubmissionMappingContext_success() {
    final CommonLookupDetail mockContactTitle = new CommonLookupDetail(); // Populate as needed
    final RelationshipToCaseLookupDetail mockOrganisationRelationshipsToCase = new RelationshipToCaseLookupDetail(); // Populate as needed
    final RelationshipToCaseLookupDetail mockIndividualRelationshipsToCase = new RelationshipToCaseLookupDetail(); // Populate as needed
    final CommonLookupDetail mockRelationshipToClient = new CommonLookupDetail(); // Populate as needed

    when(lookupService.getCommonValues(COMMON_VALUE_CONTACT_TITLE)).thenReturn(Mono.just(mockContactTitle));
    when(lookupService.getOrganisationToCaseRelationships()).thenReturn(Mono.just(mockOrganisationRelationshipsToCase));
    when(lookupService.getPersonToCaseRelationships()).thenReturn(Mono.just(mockIndividualRelationshipsToCase));
    when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT)).thenReturn(Mono.just(mockRelationshipToClient));

    final Mono<OpponentSubmissionSummaryMappingContext> result = lookupService.getOpponentSubmissionMappingContext();

    StepVerifier.create(result)
        .expectNextMatches(context ->
            context.getContactTitle().equals(mockContactTitle) &&
                context.getOrganisationRelationshipsToCase().equals(mockOrganisationRelationshipsToCase) &&
                context.getIndividualRelationshipsToCase().equals(mockIndividualRelationshipsToCase) &&
                context.getRelationshipToClient().equals(mockRelationshipToClient)
        )
        .verifyComplete();
  }

  @Test
  @DisplayName("getDeclarations with submissionType returns data successfully")
  void getDeclarations_success() {
    final String submissionType = "SUB_TYPE";
    final DeclarationLookupDetail mockDeclarationDetail = new DeclarationLookupDetail();

    when(ebsApiClient.getDeclarations(submissionType, null)).thenReturn(Mono.just(mockDeclarationDetail));

    final Mono<DeclarationLookupDetail> result = lookupService.getDeclarations(submissionType);

    StepVerifier.create(result)
        .expectNext(mockDeclarationDetail)
        .verifyComplete();

    verify(ebsApiClient).getDeclarations(submissionType, null);
  }

  @ParameterizedTest
  @DisplayName("getProviderRequestTypes with different parameters returns data successfully")
  @CsvSource({
      "true, TYPE1",
      "false, TYPE2",
      ", TYPE3",  // Case related is null
      "true, ",   // Type is null
      ", "        // Both are null
  })
  void getProviderRequestTypes_withParameters_success(final Boolean isCaseRelated, final String type) {
    final ProviderRequestTypeLookupDetail mockProviderRequestTypeDetail = new ProviderRequestTypeLookupDetail(); // Populate as needed

    when(ebsApiClient.getProviderRequestTypes(isCaseRelated, type))
        .thenReturn(Mono.just(mockProviderRequestTypeDetail));

    final Mono<ProviderRequestTypeLookupDetail> result = lookupService.getProviderRequestTypes(isCaseRelated, type);

    StepVerifier.create(result)
        .expectNext(mockProviderRequestTypeDetail)
        .verifyComplete();

    verify(ebsApiClient).getProviderRequestTypes(isCaseRelated, type);
  }




}