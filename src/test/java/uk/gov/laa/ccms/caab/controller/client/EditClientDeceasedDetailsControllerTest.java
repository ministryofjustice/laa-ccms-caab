package uk.gov.laa.ccms.caab.controller.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.ClientActionConstants.ACTION_EDIT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.util.ConversionServiceUtils.getConversionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataDeceasedDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientDeceasedDetailsValidator;

@ExtendWith(MockitoExtension.class)
class EditClientDeceasedDetailsControllerTest {

  @Mock private ClientDeceasedDetailsValidator clientDeceasedDetailsValidator;
  @InjectMocks private EditClientDeceasedDetailsController editClientDeceasedDetailsController;
  private MockMvc mockMvc;
  private ClientFlowFormData clientFlowFormData;
  private ClientFormDataDeceasedDetails deceasedDetails;

  @BeforeEach
  void setup() {
    mockMvc =
        standaloneSetup(editClientDeceasedDetailsController)
            .setConversionService(getConversionService())
            .build();

    deceasedDetails = new ClientFormDataDeceasedDetails();

    clientFlowFormData = new ClientFlowFormData(ACTION_EDIT);
    clientFlowFormData.setDeceasedDetails(deceasedDetails);
  }

  @Nested
  @DisplayName("Application tests")
  class ApplicationTests {

    @Test
    void testEditClientDetailsDeceased() throws Exception {
      mockMvc
          .perform(
              get("/application/sections/client/details/deceased")
                  .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/client-deceased-details"));
    }

    @Test
    void testEditClientDetailsDeceasedPost() throws Exception {

      mockMvc
          .perform(
              post("/application/sections/client/details/deceased")
                  .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                  .flashAttr("deceasedDetails", deceasedDetails))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/application/sections/client/details/summary"));
    }

    @Test
    void testEditClientDetailsDeceasedPostValidationError() throws Exception {

      doAnswer(
              invocation -> {
                Errors errors = (Errors) invocation.getArguments()[1];
                errors.rejectValue(
                    "dateOfDeath", "invalid.format", "Please enter the date of death");
                return null;
              })
          .when(clientDeceasedDetailsValidator)
          .validate(any(), any());

      mockMvc
          .perform(
              post("/application/sections/client/details/deceased")
                  .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                  .flashAttr("deceasedDetails", deceasedDetails))
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/client-deceased-details"));
    }
  }

  @Nested
  @DisplayName("Amendments tests")
  class AmendmentsTests {

    @Test
    void testEditClientDetailsDeceased() throws Exception {
      mockMvc
          .perform(
              get("/amendments/sections/client/details/deceased")
                  .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/client-deceased-details"));
    }

    @Test
    void testEditClientDetailsDeceasedPost() throws Exception {

      mockMvc
          .perform(
              post("/amendments/sections/client/details/deceased")
                  .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                  .flashAttr("deceasedDetails", deceasedDetails))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/amendments/sections/client/details/summary"));
    }

    @Test
    void testEditClientDetailsDeceasedPostValidationError() throws Exception {

      doAnswer(
              invocation -> {
                Errors errors = (Errors) invocation.getArguments()[1];
                errors.rejectValue(
                    "dateOfDeath", "invalid.format", "Please enter the date of death");
                return null;
              })
          .when(clientDeceasedDetailsValidator)
          .validate(any(), any());

      mockMvc
          .perform(
              post("/amendments/sections/client/details/deceased")
                  .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
                  .flashAttr("deceasedDetails", deceasedDetails))
          .andExpect(status().isOk())
          .andExpect(view().name("application/sections/client-deceased-details"));
    }
  }
}
