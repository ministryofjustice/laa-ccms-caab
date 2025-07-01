package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.util.ConversionServiceUtils.getConversionService;

import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.DelegatedFunctionsValidator;
import uk.gov.laa.ccms.caab.constants.CaseContext;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.DevolvedPowersDetail;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.util.DateUtils;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
class DelegatedFunctionsControllerTest {

  @Mock
  private DelegatedFunctionsValidator delegatedFunctionsValidator;

  @Mock
  private ApplicationService applicationService;

  @InjectMocks
  private DelegatedFunctionsController delegatedFunctionsController;

  private MockMvcTester mockMvc;

  @Autowired
  private WebApplicationContext webApplicationContext;

  private ApplicationFormData applicationFormData;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcTester.create(standaloneSetup(delegatedFunctionsController)
        .setControllerAdvice(new GlobalExceptionHandler())
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


  @Nested
  @DisplayName("GET: /amendments/edit-delegated-functions")
  class GetEditDelegatedFunctionsTests {


    @Test
    @DisplayName("Should use existing applicationFormData if present in session")
    void testEditDelegatedFunction_FormDataPresent() {
      ApplicationType applicationType = new ApplicationType();
      DevolvedPowersDetail devolvedPowers = new DevolvedPowersDetail();
      devolvedPowers.setUsed(true);
      Date dateUsed = DateUtils.convertToDate("1/6/2024");
      devolvedPowers.setDateUsed(dateUsed);
      applicationType.setDevolvedPowers(devolvedPowers);
      ApplicationDetail tdsApplication = new ApplicationDetail();
      tdsApplication.setApplicationType(applicationType);

      ApplicationFormData formData = new ApplicationFormData();
      formData.setDelegatedFunctions(true);
      formData.setDelegatedFunctionUsedDate(DateUtils.convertToComponentDate(dateUsed));

      assertThat(mockMvc.perform(get("/amendments/edit-delegated-functions")
          .sessionAttr(APPLICATION, tdsApplication)
          .sessionAttr(APPLICATION_FORM_DATA, new ApplicationFormData())))
          .hasStatusOk()
          .hasViewName("application/select-delegated-functions")
          .model()
          .containsEntry(APPLICATION_FORM_DATA, formData)
          .containsEntry("caseContext", CaseContext.AMENDMENTS)
          .containsEntry("edit", true);
    }

    @Test
    @DisplayName("Should set delegatedFunctionUsedDate to null if devolved powers dateUsed is null")
    void testEditDelegatedFunction_DevolvedPowersDateNull() {
      ApplicationType applicationType = new ApplicationType();
      DevolvedPowersDetail devolvedPowers = new DevolvedPowersDetail();
      devolvedPowers.setUsed(Boolean.FALSE);
      devolvedPowers.setDateUsed(null);
      applicationType.setDevolvedPowers(devolvedPowers);
      ApplicationDetail tdsApplication = new ApplicationDetail();
      tdsApplication.setApplicationType(applicationType);


      ApplicationFormData formData = new ApplicationFormData();
      formData.setDelegatedFunctions(false);
      formData.setDelegatedFunctionUsedDate(null);

      assertThat(mockMvc.perform(get("/amendments/edit-delegated-functions")
          .sessionAttr(APPLICATION, tdsApplication)))
          .hasStatusOk()
          .hasViewName("application/select-delegated-functions")
          .model()
          .containsEntry(APPLICATION_FORM_DATA, formData)
          .containsEntry("caseContext", CaseContext.AMENDMENTS)
          .containsEntry("edit", true);
    }

    @Test
    @DisplayName("Should throw exception if applicationType is null")
    void testEditDelegatedFunction_ApplicationTypeNull() {
      ApplicationDetail tdsApplication = new ApplicationDetail();

      assertThat(mockMvc.perform(get("/amendments/edit-delegated-functions")
          .sessionAttr(APPLICATION, tdsApplication)))
          .hasViewName("error");
    }
  }


  @Nested
  @DisplayName("POST: /amendments/edit-delegated-functions")
  class EditDelegatedFunctionsTests {

    @Test
    @DisplayName("Should handle validation errors on edit delegated functions POST")
    void testPostEditDelegatedFunctionsValidationError() {
      ApplicationDetail tdsApplication = new ApplicationDetail();
      ApplicationType applicationType = new ApplicationType();
      applicationType.setDevolvedPowers(new DevolvedPowersDetail());
      tdsApplication.setApplicationType(applicationType);
      UserDetail user = new UserDetail();
      doAnswer(invocation -> {
        Errors errors = (Errors) invocation.getArguments()[1];
        errors.rejectValue("delegatedFunctionUsedDate", "invalid.format", "Please enter the date.");
        return null;
      }).when(delegatedFunctionsValidator).validate(any(), any());

      assertThat(
          mockMvc.perform(post("/amendments/edit-delegated-functions")
              .sessionAttr(APPLICATION, tdsApplication)
              .sessionAttr(USER_DETAILS, user)
              .flashAttr(APPLICATION_FORM_DATA, applicationFormData)))
          .hasStatusOk()
          .hasViewName("application/select-delegated-functions")
          .model()
          .containsEntry("caseContext", CaseContext.AMENDMENTS)
          .containsEntry("edit", true);
    }

    @Test
    @DisplayName("Should update application and redirect on successful POST")
    void testPostEditDelegatedFunctionsSuccess() {
      ApplicationDetail tdsApplication = new ApplicationDetail();
      tdsApplication.setId(123);
      ApplicationType applicationType = new ApplicationType();
      DevolvedPowersDetail powers = new DevolvedPowersDetail();
      applicationType.setDevolvedPowers(powers);
      tdsApplication.setApplicationType(applicationType);
      UserDetail user = new UserDetail();
      applicationFormData.setDelegatedFunctions(true);
      applicationFormData.setDelegatedFunctionUsedDate("1/7/2025");

      assertThat(
          mockMvc.perform(post("/amendments/edit-delegated-functions")
              .sessionAttr(APPLICATION, tdsApplication)
              .sessionAttr(USER_DETAILS, user)
              .flashAttr(APPLICATION_FORM_DATA, applicationFormData)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/amendments/sections/linked-cases");

      verify(applicationService).putApplicationTypeFormData(123, applicationType, user);
    }

  }


}
