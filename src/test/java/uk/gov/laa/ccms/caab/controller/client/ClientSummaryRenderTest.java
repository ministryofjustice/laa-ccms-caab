package uk.gov.laa.ccms.caab.controller.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.ClientActionConstants.ACTION_CREATE;
import static uk.gov.laa.ccms.caab.constants.ClientActionConstants.ACTION_EDIT;
import static uk.gov.laa.ccms.caab.util.ThymeleafTestUtils.createViewResolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataContactDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataDeceasedDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataMonitoringDetails;
import uk.gov.laa.ccms.caab.constants.CaseContext;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

class ClientSummaryRenderTest {

  private static final String DECEASED_ROW = "Deceased\\s*</dt>\\s*<dd[^>]*>\\s*";

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    mockMvc =
        standaloneSetup(new HarnessController()).setViewResolvers(createViewResolver()).build();
  }

  @Test
  @DisplayName("Client not deceased shows No without a date of death")
  void notDeceased() throws Exception {
    final String html = render(ACTION_EDIT, "false", null);

    assertThat(html).containsPattern(DECEASED_ROW + "No");
    assertThat(html).doesNotContain("Date of death");
  }

  @Test
  @DisplayName("Entered date of death shows Yes and the date, before the flag is saved")
  void enteredDateOfDeath() throws Exception {
    final String html = render(ACTION_EDIT, "false", "15/07/2024");

    assertThat(html).containsPattern(DECEASED_ROW + "Yes");
    assertThat(html).contains("Date of death").contains("15/07/2024");
  }

  @Test
  @DisplayName("Deceased flag without a date shows Yes and no date row")
  void deceasedFlagWithoutDate() throws Exception {
    final String html = render(ACTION_EDIT, "true", null);

    assertThat(html).containsPattern(DECEASED_ROW + "Yes");
    assertThat(html).doesNotContain("Date of death");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "  "})
  @DisplayName("Blank date of death is treated as no date")
  void blankDateOfDeath(String dateOfDeath) throws Exception {
    final String html = render(ACTION_EDIT, "false", dateOfDeath);

    assertThat(html).containsPattern(DECEASED_ROW + "No");
    assertThat(html).doesNotContain("Date of death");
  }

  @Test
  @DisplayName("Deceased rows are not shown when creating a client")
  void notShownWhenCreating() throws Exception {
    final String html = render(ACTION_CREATE, null, null);

    assertThat(html).doesNotContain(">Deceased<").doesNotContain("Date of death");
  }

  private String render(String action, String deceasedFlag, String dateOfDeath) throws Exception {
    final MockHttpServletRequestBuilder request =
        get("/test/client-summary").param("action", action);
    if (deceasedFlag != null) {
      request.param("deceasedFlag", deceasedFlag);
    }
    if (dateOfDeath != null) {
      request.param("dateOfDeath", dateOfDeath);
    }
    return mockMvc
        .perform(request)
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  @Controller
  static class HarnessController {

    @GetMapping("/test/client-summary")
    String render(
        @RequestParam String action,
        @RequestParam(required = false) Boolean deceasedFlag,
        @RequestParam(required = false) String dateOfDeath,
        Model model) {
      final ClientFlowFormData clientFlowFormData = new ClientFlowFormData(action);
      clientFlowFormData.setBasicDetails(new ClientFormDataBasicDetails());
      clientFlowFormData.setContactDetails(new ClientFormDataContactDetails());
      clientFlowFormData.setAddressDetails(new ClientFormDataAddressDetails());
      clientFlowFormData.setMonitoringDetails(new ClientFormDataMonitoringDetails());
      clientFlowFormData.setDeceasedFlag(deceasedFlag);
      if (dateOfDeath != null) {
        final ClientFormDataDeceasedDetails deceasedDetails = new ClientFormDataDeceasedDetails();
        deceasedDetails.setDateOfDeath(dateOfDeath);
        clientFlowFormData.setDeceasedDetails(deceasedDetails);
      }

      final CommonLookupValueDetail lookup = new CommonLookupValueDetail().description("Lookup");
      model.addAttribute("clientFlowFormData", clientFlowFormData);
      model.addAttribute("caseContext", CaseContext.AMENDMENTS);
      model.addAttribute("contactTitle", lookup);
      model.addAttribute("gender", lookup);
      model.addAttribute("maritalStatus", lookup);
      model.addAttribute("correspondenceMethod", lookup);
      model.addAttribute("country", lookup);
      model.addAttribute("ethnicity", lookup);
      model.addAttribute("disability", lookup);
      return "test/client-summary-harness";
    }
  }
}
