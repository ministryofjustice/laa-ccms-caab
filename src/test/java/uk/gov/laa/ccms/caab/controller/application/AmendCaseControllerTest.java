package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

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
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.BaseApplicationDetail;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.service.AmendmentService;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
@DisplayName("Amend case controller tests")
class AmendCaseControllerTest {

  @Mock
  private ApplicationService applicationService;
  @Mock
  private AmendmentService amendmentService;

  @InjectMocks
  AmendCaseController amendCaseController;

  private MockMvcTester mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcTester.create(MockMvcBuilders.standaloneSetup(amendCaseController)
        .setControllerAdvice(new GlobalExceptionHandler()).build());
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

      assertThat(mockMvc.perform(get("/amendments/create")
          .sessionAttr(CASE, ebsCase)
          .sessionAttr(USER_DETAILS, userDetail)
          .sessionAttr(APPLICATION_FORM_DATA, applicationFormData)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/amendments/summary");

      verify(amendmentService, times(1))
          .createAndSubmitAmendmentForCase(applicationFormData, "123", userDetail);

    }
  }

  @Nested
  @DisplayName("GET: /amendments/summary")
  class GetAmendCaseSummaryTests {

    @Test
    @DisplayName("Should return expected view")
    void shouldReturnExpectedView() {
      UserDetail userDetail = new UserDetail();
      BaseApplicationDetail tdsApplication = new BaseApplicationDetail();
      ApplicationDetail amendment = new ApplicationDetail().id(123).caseReferenceNumber("123");
      ApplicationSectionDisplay applicationSectionDisplay = ApplicationSectionDisplay.builder().build();

      when(applicationService.getApplication(any())).thenReturn(Mono.just(amendment));
      when(amendmentService.getAmendmentSections(amendment, userDetail))
          .thenReturn(applicationSectionDisplay);
      assertThat(mockMvc.perform(get("/amendments/summary")
          .sessionAttr(USER_DETAILS, userDetail)
          .sessionAttr(APPLICATION, tdsApplication)
          .sessionAttr(ACTIVE_CASE, ActiveCase.builder().build())
      ))
          .hasStatusOk()
          .hasViewName("application/amendment-summary")
          .model()
          .containsEntry("summary", applicationSectionDisplay);
    }
  }
}