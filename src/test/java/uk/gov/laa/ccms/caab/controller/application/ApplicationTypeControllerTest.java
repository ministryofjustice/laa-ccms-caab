package uk.gov.laa.ccms.caab.controller.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_TYPE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;

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
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.ApplicationTypeValidator;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
class ApplicationTypeControllerTest {
  @Mock
  private LookupService lookupService;

  @Mock
  private ApplicationTypeValidator applicationTypeValidator;

  @InjectMocks
  private ApplicationTypeController applicationTypeController;

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  void setup() {
    mockMvc = standaloneSetup(applicationTypeController).build();
  }

  @Test
  void getApplicationTypeAddsApplicationTypesToModel() throws Exception {
    final CommonLookupDetail applicationTypes = new CommonLookupDetail();
    applicationTypes.addContentItem(
        new CommonLookupValueDetail().type("Type 1").code("Code 1"));

    when(lookupService.getCommonValues(COMMON_VALUE_APPLICATION_TYPE)).thenReturn(
        Mono.just(applicationTypes));

    this.mockMvc.perform(get("/application/application-type")
            .sessionAttr(APPLICATION_FORM_DATA, new ApplicationFormData()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/select-application-type"))
        .andExpect(model().attribute(APPLICATION_FORM_DATA, new ApplicationFormData()))
        .andExpect(model().attribute("applicationTypes", applicationTypes.getContent()));

    verify(lookupService, times(1))
        .getCommonValues(COMMON_VALUE_APPLICATION_TYPE);
  }

  @Test
  void getApplicationTypeHandlesExceptionalFunding() throws Exception {
    final ApplicationFormData applicationFormData = new ApplicationFormData();
    applicationFormData.setApplicationTypeCategory("ECF");
    applicationFormData.setExceptionalFunding(true);

    this.mockMvc.perform(get("/application/application-type")
            .flashAttr(APPLICATION_FORM_DATA, applicationFormData))
        .andDo(print())
        .andExpect(redirectedUrl("/application/client/search"));

    verifyNoInteractions(lookupService);
  }

  @Test
  void postApplicationTypeHandlesValidationError() throws Exception {
    final ApplicationFormData applicationFormData = new ApplicationFormData();

    final CommonLookupDetail applicationTypes = new CommonLookupDetail();
    applicationTypes.addContentItem(
        new CommonLookupValueDetail().type("Type 1").code("Code 1"));

    when(lookupService.getCommonValues(COMMON_VALUE_APPLICATION_TYPE)).thenReturn(
        Mono.just(applicationTypes));

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("applicationTypeCategory", "required.applicationTypeCategory",
          "Please select an application type.");
      return null;
    }).when(applicationTypeValidator).validate(any(), any());

    this.mockMvc.perform(post("/application/application-type")
            .flashAttr(APPLICATION_FORM_DATA, applicationFormData))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/select-application-type"));
  }

  @Test
  void postApplicationTypeIsSuccessful() throws Exception {
    final ApplicationFormData applicationFormData = new ApplicationFormData();
    applicationFormData.setApplicationTypeCategory("ECF");

    this.mockMvc.perform(post("/application/application-type")
            .flashAttr(APPLICATION_FORM_DATA, applicationFormData))
        .andDo(print())
        .andExpect(redirectedUrl("/application/delegated-functions"));

    verifyNoInteractions(lookupService);
  }


}
