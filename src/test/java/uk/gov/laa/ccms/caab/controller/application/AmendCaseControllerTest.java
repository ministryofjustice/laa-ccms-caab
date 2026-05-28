package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_SUMMARY;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.math.BigDecimal;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.mapper.EvidenceMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.EvidenceDocumentDetails;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.service.AmendmentService;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.AssessmentService;
import uk.gov.laa.ccms.caab.service.EvidenceService;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
@DisplayName("Amend case controller tests")
class AmendCaseControllerTest {

  @Mock private ApplicationService applicationService;
  @Mock private AmendmentService amendmentService;
  @Mock private AssessmentService assessmentService;
  @Mock private EvidenceService evidenceService;
  @Mock private EvidenceMapper evidenceMapper;

  @InjectMocks AmendCaseController amendCaseController;

  private MockMvcTester mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcTester.create(
            MockMvcBuilders.standaloneSetup(amendCaseController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build());
  }

  @Nested
  @DisplayName("GET: /amendments/create")
  class GetStartAmendmentTests {

    @Test
    @DisplayName("Should redirect to summary")
    void shouldRedirectToSummary() {
      ApplicationFormData applicationFormData = new ApplicationFormData();
      ApplicationDetail ebsCase = new ApplicationDetail().caseReferenceNumber("123");
      UserDetail userDetail = new UserDetail();

      when(applicationService.getTdsApplications(any(), any(), eq(0), eq(1)))
          .thenReturn(new ApplicationDetails());

      assertThat(
              mockMvc.perform(
                  get("/amendments/create")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, userDetail)
                      .sessionAttr(APPLICATION_FORM_DATA, applicationFormData)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/amendments/summary");

      verify(amendmentService, times(1))
          .createAndSubmitAmendmentForCase(applicationFormData, ebsCase, userDetail);
    }
  }

  @Nested
  @DisplayName("GET: /amendments/summary")
  class GetAmendCaseSummaryTests {

    @Test
    @DisplayName("Should return expected view")
    void shouldReturnExpectedView() {
      UserDetail userDetail = new UserDetail();
      BaseApplicationDetail tdsApplication = new BaseApplicationDetail().id(123);
      CostStructureDetail costs =
          new CostStructureDetail()
              .addCostEntriesItem(
                  new CostEntryDetail().costCategory("Test cat").requestedCosts(BigDecimal.ONE));
      ApplicationDetail amendment =
          new ApplicationDetail().id(123).caseReferenceNumber("123").costs(costs);
      ApplicationDetail activeCase =
          new ApplicationDetail()
              .availableFunctions(
                  Collections.singletonList(FunctionConstants.MEANS_ASSESSMENT_LEGAL_AMENDMENT));
      ApplicationSectionDisplay applicationSectionDisplay =
          ApplicationSectionDisplay.builder().build();

      when(applicationService.getApplication(any())).thenReturn(Mono.just(amendment));
      when(amendmentService.getAmendmentSections(any(), eq(userDetail)))
          .thenReturn(applicationSectionDisplay);

      EvidenceDocumentDetails evidenceUploaded = new EvidenceDocumentDetails();

      when(evidenceService.getEvidenceDocumentsForCase(any(), any()))
          .thenReturn(Mono.just(evidenceUploaded));

      when(evidenceService.getDocumentsRequired(any(), any(), any()))
          .thenReturn(Mono.just(Collections.emptyList()));
      when(evidenceMapper.toEvidenceRequiredList(any(), any())).thenReturn(Collections.emptyList());

      assertThat(
              mockMvc.perform(
                  get("/amendments/summary")
                      .sessionAttr(APPLICATION_SUMMARY, tdsApplication)
                      .sessionAttr(CASE, activeCase)
                      .sessionAttr(
                          ACTIVE_CASE, ActiveCase.builder().caseReferenceNumber("123").build())
                      .sessionAttr(USER_DETAILS, userDetail)))
          .hasStatusOk()
          .hasViewName("application/amendment-summary")
          .model()
          .containsEntry("summary", applicationSectionDisplay)
          .containsEntry("evidenceUploaded", Collections.emptyList());

      verify(amendmentService)
          .getAmendmentSections(
              argThat(
                  application ->
                      application
                          .getAvailableFunctions()
                          .contains(FunctionConstants.MEANS_ASSESSMENT_LEGAL_AMENDMENT)),
              eq(userDetail));

      // The required evidence list is loaded so the upload screen can be reached directly
      // from the amend case summary.
      verify(evidenceService).getDocumentsRequired("123", "123", null);
      verify(evidenceMapper)
          .toEvidenceRequiredList(Collections.emptyList(), Collections.emptyList());
    }
  }
}
