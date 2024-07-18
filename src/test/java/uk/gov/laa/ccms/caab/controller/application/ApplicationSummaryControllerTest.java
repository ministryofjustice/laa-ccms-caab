package uk.gov.laa.ccms.caab.controller.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildApplicationDetail;

import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.controller.application.summary.ApplicationSummaryController;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.summary.ApplicationSummaryDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
class ApplicationSummaryControllerTest {

  @Mock
  private ApplicationService applicationService;

  @InjectMocks
  private ApplicationSummaryController applicationSummaryController;

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(applicationSummaryController).build();
  }

  @Test
  public void testGetApplicationTypeAddsApplicationTypesToModel() throws Exception {
    final String id = "123";

    final ApplicationSummaryDisplay applicationSummaryDisplay =
        ApplicationSummaryDisplay.builder()
            .build();

    final UserDetail user = new UserDetail();
    user.setLoginId("testLogin");
    user.setUserType("testUserType");

    final ApplicationDetail application = buildApplicationDetail(1, true, new Date());

    when(applicationService.getApplication(anyString())).thenReturn(Mono.just(application));
    when(applicationService.getApplicationSummary(any(ApplicationDetail.class), any(UserDetail.class)))
        .thenReturn(applicationSummaryDisplay);

    this.mockMvc.perform(get("/application/summary")
            .sessionAttr("applicationId", id)
            .sessionAttr("user", user))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/summary/summary-task-page"))
        .andExpect(model().attributeExists("activeCase"))
        .andExpect(model().attribute("summary", applicationSummaryDisplay));

    verify(applicationService, times(1)).getApplicationSummary(
        any(ApplicationDetail.class), any(UserDetail.class));
  }

}