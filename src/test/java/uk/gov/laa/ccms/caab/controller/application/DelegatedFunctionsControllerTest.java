package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.util.ConversionServiceUtils.getConversionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.DelegatedFunctionsValidator;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
class DelegatedFunctionsControllerTest {

  @Mock
  private DelegatedFunctionsValidator delegatedFunctionsValidator;

  @InjectMocks
  private DelegatedFunctionsController delegatedFunctionsController;

  private MockMvcTester mockMvc;

  @Autowired
  private WebApplicationContext webApplicationContext;

  private ApplicationFormData applicationFormData;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcTester.create(standaloneSetup(delegatedFunctionsController)
        .setConversionService(getConversionService()).build());
    applicationFormData = new ApplicationFormData();
  }

  @Nested
  @DisplayName("GET: /{caseContext}/delegated-functions")
  class GetDelegatedFunctionsTests {

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should return expected view with application form data")
    void testGetDelegatedFunctions(String caseContext) {
      assertThat(mockMvc.perform(get("/%s/delegated-functions".formatted(caseContext))
          .sessionAttr(APPLICATION_FORM_DATA, applicationFormData)))
          .hasStatusOk()
          .hasViewName("application/select-delegated-functions")
          .model()
          .containsEntry(APPLICATION_FORM_DATA, applicationFormData);
    }
  }

  @Nested
  @DisplayName("POST: /{caseContext}/delegated-functions")
  class PostDelegatedFunctionsTests {

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should handle validation error")
    void testPostDelegatedFunctionsHandlesValidationError(String caseContext) {

      final UserDetail userDetail = new UserDetail();
      final ApplicationDetail applicationDetail = new ApplicationDetail();
      doAnswer(invocation -> {
        Errors errors = (Errors) invocation.getArguments()[1];

        errors.rejectValue("delegatedFunctionUsedDate", "invalid.format",
            "Please enter the date.");
        return null;
      }).when(delegatedFunctionsValidator).validate(any(), any());

      assertThat(
          mockMvc.perform(post("/%s/delegated-functions".formatted(caseContext))
              .flashAttr(APPLICATION_FORM_DATA, applicationFormData)
              .sessionAttr(USER_DETAILS, userDetail)
              .sessionAttr(CASE, applicationDetail)))
          .hasStatusOk()
          .hasViewName("application/select-delegated-functions");
    }

    @ParameterizedTest
    @CsvSource({"SUB, true, SUBDP",
        "SUB, false, SUB",
        "EMER, true, DP",
        "EMER, false, EMER"})
    @DisplayName("Should redirect to client search when new application")
    void testPostApplicationDelegatedFunctionsIsSuccessful(String category,
        boolean delegatedFunctions,
        String expectedApplicationType) {
      applicationFormData.setApplicationTypeCategory(category);
      applicationFormData.setDelegatedFunctions(delegatedFunctions);
      final ApplicationDetail applicationDetail = new ApplicationDetail();
      final UserDetail userDetail = new UserDetail();

      assertThat(
          mockMvc.perform(post("/application/delegated-functions")
              .flashAttr(APPLICATION_FORM_DATA, applicationFormData)
              .sessionAttr(CASE, applicationDetail)
              .sessionAttr(USER_DETAILS, userDetail)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/application/client/search");
    }
  }


}
