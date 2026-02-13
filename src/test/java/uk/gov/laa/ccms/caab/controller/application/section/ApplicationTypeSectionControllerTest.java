package uk.gov.laa.ccms.caab.controller.application.section;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.DelegatedFunctionsValidator;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class ApplicationTypeSectionControllerTest {

  @Mock private ApplicationService applicationService;

  @Mock private DelegatedFunctionsValidator delegatedFunctionsValidator;

  @InjectMocks private ApplicationTypeSectionController applicationTypeSectionController;

  private MockMvc mockMvc;

  @Autowired private WebApplicationContext webApplicationContext;

  private UserDetail user;
  private ActiveCase activeCase;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(applicationTypeSectionController).build();
    this.user = buildUser();
    this.activeCase = buildActiveCase();
  }

  @Test
  public void testApplicationSummaryApplicationType() throws Exception {
    final ApplicationFormData applicationFormData = new ApplicationFormData();
    applicationFormData.setApplicationTypeCategory("Category A"); // Updated field

    when(applicationService.getApplicationTypeFormData("123")).thenReturn(applicationFormData);

    this.mockMvc
        .perform(
            get("/application/sections/application-type")
                .sessionAttr(APPLICATION_ID, "123")
                .sessionAttr(ACTIVE_CASE, activeCase)
                .sessionAttr(USER_DETAILS, user)
                .flashAttr(APPLICATION_FORM_DATA, applicationFormData))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/application-type-section"))
        .andExpect(model().attribute("applicationFormData", applicationFormData));
  }

  @Test
  public void testDelegatedFunctionIsSuccessful() throws Exception {
    final ApplicationFormData applicationFormData = new ApplicationFormData();
    applicationFormData.setDelegatedFunctions(true);

    this.mockMvc
        .perform(
            post("/application/sections/application-type")
                .sessionAttr(APPLICATION_ID, "123")
                .sessionAttr(ACTIVE_CASE, activeCase)
                .sessionAttr(USER_DETAILS, user)
                .flashAttr(APPLICATION_FORM_DATA, applicationFormData))
        .andDo(print())
        .andExpect(redirectedUrl("/application/sections"));

    verify(applicationService)
        .updateApplicationType(eq("123"), any(ApplicationFormData.class), any(UserDetail.class));
    verify(applicationService)
        .updateApplicationType(eq("123"), any(ApplicationFormData.class), any(UserDetail.class));
  }

  @Test
  public void testDelegatedFunctionHandlesValidationError() throws Exception {
    final ApplicationFormData applicationFormData = new ApplicationFormData();
    applicationFormData.setDelegatedFunctions(true);

    doAnswer(
            invocation -> {
              Errors errors = (Errors) invocation.getArguments()[1];
              errors.rejectValue(
                  "delegatedFunctions",
                  "required.delegatedFunctions",
                  "Delegated functions required.");
              return null;
            })
        .when(delegatedFunctionsValidator)
        .validate(any(), any());

    this.mockMvc
        .perform(
            post("/application/sections/application-type")
                .sessionAttr(APPLICATION_ID, "123")
                .sessionAttr(ACTIVE_CASE, activeCase)
                .sessionAttr(USER_DETAILS, user)
                .flashAttr(APPLICATION_FORM_DATA, applicationFormData))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/sections/application-type-section"))
        .andExpect(model().hasErrors());

    verify(applicationService, never())
        .updateApplicationType(eq("123"), any(ApplicationFormData.class), any(UserDetail.class));
  }

  private UserDetail buildUser() {
    return new UserDetail().userId(1).userType("testUserType").loginId("testLoginId");
  }

  private ActiveCase buildActiveCase() {
    return ActiveCase.builder().caseReferenceNumber("12345").build();
  }
}
