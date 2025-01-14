package uk.gov.laa.ccms.caab.controller.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.DelegatedFunctionsValidator;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class DelegatedFunctionsControllerTest {

  @Mock
  private DelegatedFunctionsValidator delegatedFunctionsValidator;

  @InjectMocks
  private DelegatedFunctionsController delegatedFunctionsController;

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext webApplicationContext;

  private ApplicationFormData applicationFormData;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(delegatedFunctionsController).build();
    applicationFormData = new ApplicationFormData();
  }

  @Test
  public void testGetDelegatedFunctions() throws Exception {
    this.mockMvc.perform(get("/application/delegated-functions")
            .sessionAttr(APPLICATION_FORM_DATA, applicationFormData))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/select-delegated-functions"))
        .andExpect(model().attribute(APPLICATION_FORM_DATA, applicationFormData));
  }

  @Test
  public void testPostDelegatedFunctionsHandlesValidationError() throws Exception {

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];

      errors.rejectValue("delegatedFunctionUsedDate", "invalid.format",
          "Please enter the date.");
      return null;
    }).when(delegatedFunctionsValidator).validate(any(), any());
    this.mockMvc.perform(post("/application/delegated-functions")
            .flashAttr(APPLICATION_FORM_DATA, applicationFormData))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/select-delegated-functions"));
  }

  @ParameterizedTest
  @CsvSource({"SUB, true, SUBDP",
      "SUB, false, SUB",
      "EMER, true, DP",
      "EMER, false, EMER"})
  public void testPostDelegatedFunctionsIsSuccessful(String category, boolean delegatedFunctions,
                                                     String expectedApplicationType)
      throws Exception {
    applicationFormData.setApplicationTypeCategory(category);
    applicationFormData.setDelegatedFunctions(delegatedFunctions);

    this.mockMvc.perform(post("/application/delegated-functions")
            .flashAttr(APPLICATION_FORM_DATA, applicationFormData))
        .andDo(print())
        .andExpect(redirectedUrl("/application/client/search"))
        .andReturn();
  }


}
