package uk.gov.laa.ccms.caab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.util.ApplicationDetailUtils.buildFullApplicationDetail;
import static uk.gov.laa.ccms.caab.util.ApplicationDetailUtils.expectedApplicationSectionDisplay;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.client.CaabApiClient;
import uk.gov.laa.ccms.caab.client.SoaApiClient;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.PriorAuthoritySectionDisplay;
import uk.gov.laa.ccms.caab.util.DateUtils;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseTransactionResponse;

@DisplayName("Amendment service test")
@ExtendWith(MockitoExtension.class)
class AmendmentServiceTest {

  @Mock private ApplicationService applicationService;
  @Mock private CaabApiClient caabApiClient;
  @Mock private SoaApiClient soaApiClient;

  private AmendmentService amendmentService;

  @BeforeEach
  void beforeEach() {
    amendmentService = new AmendmentService(applicationService, caabApiClient, soaApiClient);
  }

  @Nested
  @DisplayName("createAndSubmitAmendmentForCase() tests")
  class CreateAndSubmitAmendmentForCaseTests {

    @Test
    @DisplayName("Should create and submit amendment for a case")
    void shouldCreateAndSubmitAmendmentForACase() {
      // Given
      ApplicationFormData applicationFormData = new ApplicationFormData();
      applicationFormData.setApplicationTypeCategory(APP_TYPE_SUBSTANTIVE);
      applicationFormData.setDelegatedFunctions(true);
      applicationFormData.setDelegatedFunctionUsedDate("01/01/2025");
      String caseRef = "12345";
      UserDetail userDetails = new UserDetail().loginId("123").provider(new BaseProvider().id(10));
      userDetails.setProvider(new BaseProvider().id(1001));
      userDetails.setLoginId("LoginID");
      ApplicationDetail amendment = buildFullApplicationDetail();

      when(applicationService.getTdsApplications(any(), any(), any(), any()))
          .thenReturn(new ApplicationDetails().content(Collections.emptyList()));
      when(applicationService.getCase(any(), anyLong(), any())).thenReturn(amendment);
      when(caabApiClient.createApplication(any(), any())).thenReturn(Mono.just("123"));
      // When
      ApplicationDetail result =
          amendmentService.createAndSubmitAmendmentForCase(
              applicationFormData, caseRef, userDetails);
      // Then
      assertNotNull(result);
      assertThat(result.getAmendment()).isTrue();
      assertThat(result.getApplicationType().getId())
          .isEqualTo(APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS);
      assertThat(result.getApplicationType().getDevolvedPowers().getUsed()).isTrue();
      assertThat(result.getApplicationType().getDevolvedPowers().getDateUsed())
          .isEqualTo(DateUtils.convertToDate("01/01/2025"));
    }

    @Test
    @DisplayName("Should throw exception if application already exists for case reference")
    void shouldThrowExceptionIfApplicationAlreadyExistsForCaseReference() {
      // Given
      ApplicationFormData applicationFormData = new ApplicationFormData();
      applicationFormData.setApplicationTypeCategory(APP_TYPE_SUBSTANTIVE);
      applicationFormData.setDelegatedFunctions(true);
      applicationFormData.setDelegatedFunctionUsedDate("01/01/2025");
      String caseRef = "12345";
      UserDetail userDetails = new UserDetail().loginId("123").provider(new BaseProvider().id(10));
      userDetails.setProvider(new BaseProvider().id(1001));
      userDetails.setLoginId("LoginID");

      when(applicationService.getTdsApplications(any(), any(), any(), any()))
          .thenReturn(
              new ApplicationDetails()
                  .content(Collections.singletonList(new BaseApplicationDetail())));
      // When / Then
      assertThatThrownBy(
              () ->
                  amendmentService.createAndSubmitAmendmentForCase(
                      applicationFormData, caseRef, userDetails))
          .isInstanceOf(CaabApplicationException.class)
          .hasMessageContaining("Application already exists for case reference: 12345");
    }
  }

  @Nested
  @DisplayName("getAmendmentSections() tests")
  class GetAmendmentSectionsTests {

    @Test
    @DisplayName("Should return amendment sections with document upload disabled")
    void shouldReturnAmendmentSectionsWithDocumentUploadDisabled() {
      // Given
      ApplicationDetail amendment = buildFullApplicationDetail();
      UserDetail userDetails = new UserDetail().loginId("123");
      ApplicationSectionDisplay originalDisplay = expectedApplicationSectionDisplay();
      amendment.setMeritsAssessmentAmended(false);
      amendment.setMeansAssessmentAmended(false);
      when(applicationService.getApplicationSections(amendment, userDetails))
          .thenReturn(originalDisplay);
      // When
      ApplicationSectionDisplay result =
          amendmentService.getAmendmentSections(amendment, userDetails);
      // Then
      assertThat(result).isNotNull();
      assertThat(result.getDocumentUpload()).isNotNull();
      assertThat(result.getDocumentUpload().isEnabled()).isFalse();
    }

    @Test
    @DisplayName(
        "Should return amendment sections with document upload enabled when draft prior authority")
    void shouldReturnAmendmentSectionsWithDocumentUploadEnabledWhenDraftPriorAuthority() {
      // Given
      ApplicationDetail amendment = buildFullApplicationDetail();
      UserDetail userDetails = new UserDetail().loginId("123");
      ApplicationSectionDisplay originalDisplay = expectedApplicationSectionDisplay();
      originalDisplay.setPriorAuthorities(
          Collections.singletonList(
              PriorAuthoritySectionDisplay.builder().status("Draft").build()));
      amendment.setMeritsAssessmentAmended(false);
      amendment.setMeansAssessmentAmended(false);
      when(applicationService.getApplicationSections(amendment, userDetails))
          .thenReturn(originalDisplay);
      // When
      ApplicationSectionDisplay result =
          amendmentService.getAmendmentSections(amendment, userDetails);
      // Then
      assertThat(result).isNotNull();
      assertThat(result.getDocumentUpload()).isNotNull();
      assertThat(result.getDocumentUpload().isEnabled()).isTrue();
    }

    @Test
    @DisplayName(
        "Should return amendment sections with document upload enabled when merits " + "modified")
    void shouldReturnAmendmentSectionsWithDocumentUploadEnabledWhenMeritsModified() {
      // Given
      ApplicationDetail amendment = buildFullApplicationDetail();
      UserDetail userDetails = new UserDetail().loginId("123");
      ApplicationSectionDisplay originalDisplay = expectedApplicationSectionDisplay();
      amendment.setMeritsAssessmentAmended(true);
      amendment.setMeansAssessmentAmended(false);
      when(applicationService.getApplicationSections(amendment, userDetails))
          .thenReturn(originalDisplay);
      // When
      ApplicationSectionDisplay result =
          amendmentService.getAmendmentSections(amendment, userDetails);
      // Then
      assertThat(result).isNotNull();
      assertThat(result.getDocumentUpload()).isNotNull();
      assertThat(result.getDocumentUpload().isEnabled()).isTrue();
    }

    @Test
    @DisplayName(
        "Should return amendment sections with document upload enabled when means " + "modified")
    void shouldReturnAmendmentSectionsWithDocumentUploadEnabledWhenMeansModified() {
      // Given
      ApplicationDetail amendment = buildFullApplicationDetail();
      UserDetail userDetails = new UserDetail().loginId("123");
      ApplicationSectionDisplay originalDisplay = expectedApplicationSectionDisplay();
      amendment.setMeritsAssessmentAmended(false);
      amendment.setMeansAssessmentAmended(true);
      when(applicationService.getApplicationSections(amendment, userDetails))
          .thenReturn(originalDisplay);
      // When
      ApplicationSectionDisplay result =
          amendmentService.getAmendmentSections(amendment, userDetails);
      // Then
      assertThat(result).isNotNull();
      assertThat(result.getDocumentUpload()).isNotNull();
      assertThat(result.getDocumentUpload().isEnabled()).isTrue();
    }
  }

  @Nested
  @DisplayName("submitQuickAmendmentCorrespondenceAddress() tests")
  class SubmitQuickAmendmentCorrespondenceAddressTests {

    @Test
    @DisplayName("Should submit quick amend correspondence address")
    void shouldSubmitQuickAmendmentCorrespondenceAddress() {
      // Given
      AddressFormData addressFormData = new AddressFormData();
      addressFormData.setAddressLine1("Line 1");
      addressFormData.setAddressLine2("Line 2");
      addressFormData.setCareOf("CO");
      addressFormData.setCityTown("Town");
      addressFormData.setCountry("Country");
      addressFormData.setCounty("County");
      addressFormData.setHouseNameNumber("123");
      addressFormData.setPostcode("NE1 2BC");
      addressFormData.setPreferredAddress("Preferred Address");

      String caseRef = "12345";

      UserDetail userDetails =
          new UserDetail().loginId("123").userType("Type").provider(new BaseProvider().id(10));
      when(applicationService.getCase(any(), anyLong(), any()))
          .thenReturn(buildFullApplicationDetail());
      when(soaApiClient.updateCase(any(), any(), any()))
          .thenReturn(Mono.just(new CaseTransactionResponse().transactionId("12345")));
      // When
      String transactionId =
          amendmentService.submitQuickAmendmentCorrespondenceAddress(
              addressFormData, caseRef, userDetails);
      // Then
      verify(caabApiClient, times(1)).createApplication(eq("123"), any(ApplicationDetail.class));
      verify(soaApiClient, times(1)).updateCase(eq("123"), eq("Type"), any());
      assertThat(transactionId).isNotNull();
      assertThat(transactionId).isEqualTo("12345");
    }
  }
}
