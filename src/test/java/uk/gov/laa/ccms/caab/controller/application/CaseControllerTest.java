package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;

@ExtendWith(MockitoExtension.class)
class CaseControllerTest {

  @Mock
  private ApplicationService applicationService;

  @InjectMocks
  private CaseController caseController;

  private MockMvcTester mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcTester.create(MockMvcBuilders.standaloneSetup(caseController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build());
  }

  @Test
  void caseDetailsReturnsViewAndModelWhenCaseDetailsExist() {
    ApplicationDetail ebsCase = new ApplicationDetail();
    ApplicationSectionDisplay display = ApplicationSectionDisplay.builder().build();
    when(applicationService.getCaseDetailsDisplay(ebsCase)).thenReturn(display);

    assertThat(mockMvc.perform(get("/cases/details").sessionAttr(CASE, ebsCase)))
        .hasStatusOk()
        .hasViewName("application/case-details")
        .model()
        .containsEntry("summary", display);
  }

  @Test
  void caseDetailsThrowsExceptionWhenCaseDetailsMissing() {
    ApplicationDetail ebsCase = new ApplicationDetail();
    when(applicationService.getCaseDetailsDisplay(ebsCase)).thenReturn(null);

    assertThat(mockMvc.perform(get("/cases/details").sessionAttr(CASE, ebsCase)))
        .hasStatusOk()
        .hasViewName("error");
  }
}
