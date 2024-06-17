package uk.gov.laa.ccms.caab.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildApplicationDetail;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;

import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.opa.util.SecurityUtils;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.AssessmentService;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
public class AssessmentControllerTest {

  @Mock
  private AssessmentService assessmentService;

  @Mock
  private ApplicationService applicationService;

  @Mock
  private ClientService clientService;

  @Mock
  private SecurityUtils contextSecurityUtil;

  @InjectMocks
  private AssessmentController assessmentController;

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(assessmentController).build();
  }

  private static final UserDetail userDetails = buildUserDetail();

  private static final ActiveCase activeCase = ActiveCase.builder()
      .caseReferenceNumber("testCaseReferenceNumber")
      .build();

  @Test
  public void assessmentRemoveDisplaysCorrectView() throws Exception {
    this.mockMvc.perform(get("/assessments/testCategory/remove"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/assessments/assessment-remove"))
        .andExpect(model().attribute("assessment", "testCategory"));
  }

  @ParameterizedTest
  @CsvSource({
      "merits",
      "means"
  })
  public void assessmentRemovePostRedirectsToSummaryWhenAssessmentExists(
      final String category) throws Exception {
    when(assessmentService.deleteAssessments(any(), any(), any(), any())).thenReturn(Mono.empty());

    this.mockMvc.perform(post(String.format("/assessments/%s/remove", category))
            .sessionAttr(USER_DETAILS, userDetails)
            .sessionAttr(ACTIVE_CASE, activeCase))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/summary"));
  }
}