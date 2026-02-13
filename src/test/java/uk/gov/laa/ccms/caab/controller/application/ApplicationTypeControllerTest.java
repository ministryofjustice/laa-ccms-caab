package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_TYPE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.util.ConversionServiceUtils.getConversionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.ApplicationTypeValidator;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.service.AmendmentService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration
@WebAppConfiguration
class ApplicationTypeControllerTest {

  @Mock private LookupService lookupService;

  @Mock private AmendmentService amendmentService;

  @Mock private ApplicationTypeValidator applicationTypeValidator;

  @InjectMocks private ApplicationTypeController applicationTypeController;

  private MockMvcTester mockMvc;

  @Autowired private WebApplicationContext webApplicationContext;

  @BeforeEach
  void setup() {
    mockMvc =
        MockMvcTester.create(
            standaloneSetup(applicationTypeController)
                .setConversionService(getConversionService())
                .build());
  }

  @Nested
  @DisplayName("GET: /{caseContext}/application-type")
  class GetApplicationTypeTests {

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should return view with application types on model")
    void testGetApplicationTypeAddsApplicationTypesToModel(String caseContext) {
      final CommonLookupDetail applicationTypes = new CommonLookupDetail();
      applicationTypes.addContentItem(new CommonLookupValueDetail().type("Type 1").code("Code 1"));

      when(lookupService.getCommonValues(COMMON_VALUE_APPLICATION_TYPE))
          .thenReturn(Mono.just(applicationTypes));

      assertThat(
              mockMvc.perform(
                  get("/%s/application-type".formatted(caseContext))
                      .sessionAttr(APPLICATION_FORM_DATA, new ApplicationFormData())))
          .hasStatusOk()
          .hasViewName("application/select-application-type")
          .model()
          .containsEntry(APPLICATION_FORM_DATA, new ApplicationFormData())
          .containsEntry("applicationTypes", applicationTypes.getContent());

      verify(lookupService, times(1)).getCommonValues(COMMON_VALUE_APPLICATION_TYPE);
    }

    @Test
    @DisplayName("Should handle exceptional funding when application")
    void testGetApplicationTypeHandlesExceptionalFundingWhenApplication() {
      final ApplicationFormData applicationFormData = new ApplicationFormData();
      applicationFormData.setApplicationTypeCategory("ECF");
      applicationFormData.setExceptionalFunding(true);

      assertThat(
              mockMvc.perform(
                  get("/application/application-type")
                      .flashAttr(APPLICATION_FORM_DATA, applicationFormData)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/application/client/search");

      verifyNoInteractions(lookupService);
    }

    @Test
    @DisplayName("Should not handle exceptional funding when amendment")
    void testGetApplicationTypeHandlesExceptionalFundingWhenAmendment() {
      final CommonLookupDetail applicationTypes = new CommonLookupDetail();
      applicationTypes.addContentItem(new CommonLookupValueDetail().type("Type 1").code("Code 1"));

      when(lookupService.getCommonValues(COMMON_VALUE_APPLICATION_TYPE))
          .thenReturn(Mono.just(applicationTypes));

      final ApplicationFormData applicationFormData = new ApplicationFormData();
      applicationFormData.setApplicationTypeCategory("ECF");
      applicationFormData.setExceptionalFunding(true);

      assertThat(
              mockMvc.perform(
                  get("/amendments/application-type")
                      .sessionAttr(APPLICATION_FORM_DATA, new ApplicationFormData())))
          .hasStatusOk()
          .hasViewName("application/select-application-type")
          .model()
          .containsEntry(APPLICATION_FORM_DATA, new ApplicationFormData())
          .containsEntry("applicationTypes", applicationTypes.getContent());

      verify(lookupService, times(1)).getCommonValues(COMMON_VALUE_APPLICATION_TYPE);
    }
  }

  @Nested
  @DisplayName("POST: /{caseContext}/application-type")
  class PostApplicationTypeTests {

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should having validation error")
    void testPostApplicationTypeHandlesValidationError(String caseContext) {
      final ApplicationFormData applicationFormData = new ApplicationFormData();
      final UserDetail userDetail = new UserDetail();
      final ApplicationDetail ebsCase = new ApplicationDetail();
      final CommonLookupDetail applicationTypes = new CommonLookupDetail();
      applicationTypes.addContentItem(new CommonLookupValueDetail().type("Type 1").code("Code 1"));

      when(lookupService.getCommonValues(COMMON_VALUE_APPLICATION_TYPE))
          .thenReturn(Mono.just(applicationTypes));

      doAnswer(
              invocation -> {
                Errors errors = (Errors) invocation.getArguments()[1];
                errors.rejectValue(
                    "applicationTypeCategory",
                    "required.applicationTypeCategory",
                    "Please select an application type.");
                return null;
              })
          .when(applicationTypeValidator)
          .validate(any(), any());

      assertThat(
              mockMvc.perform(
                  post("/%s/application-type".formatted(caseContext))
                      .flashAttr(APPLICATION_FORM_DATA, applicationFormData)
                      .sessionAttr(USER_DETAILS, userDetail)
                      .sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/select-application-type");
    }

    @ParameterizedTest
    @ValueSource(strings = {"application", "amendments"})
    @DisplayName("Should be successful whilst emergency")
    void testPostApplicationTypeIsSuccessful(String caseContext) {

      final UserDetail userDetail = new UserDetail();
      final ApplicationDetail ebsCase = new ApplicationDetail();
      final ApplicationFormData applicationFormData = new ApplicationFormData();
      applicationFormData.setApplicationTypeCategory(APP_TYPE_EMERGENCY);

      assertThat(
              mockMvc.perform(
                  post("/%s/application-type".formatted(caseContext))
                      .flashAttr(APPLICATION_FORM_DATA, applicationFormData)
                      .sessionAttr(USER_DETAILS, userDetail)
                      .sessionAttr(CASE, ebsCase)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/%s/delegated-functions".formatted(caseContext));

      verifyNoInteractions(lookupService);
    }

    @Test
    @DisplayName("Should be successful whilst substantive application")
    void testPostApplicationTypeIsSuccessfulSubstantiveApplication() {

      final UserDetail userDetail = new UserDetail();
      final ApplicationDetail ebsCase = new ApplicationDetail();
      final ApplicationFormData applicationFormData = new ApplicationFormData();
      applicationFormData.setApplicationTypeCategory(APP_TYPE_SUBSTANTIVE);

      assertThat(
              mockMvc.perform(
                  post("/application/application-type")
                      .flashAttr(APPLICATION_FORM_DATA, applicationFormData)
                      .sessionAttr(USER_DETAILS, userDetail)
                      .sessionAttr(CASE, ebsCase)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/application/delegated-functions");

      verifyNoInteractions(lookupService);
    }

    @Test
    @DisplayName("Should be successful whilst substantive amendment")
    void testPostApplicationTypeIsSuccessfulSubstantiveAmendment() {

      final UserDetail userDetail = new UserDetail();
      final ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setCaseReferenceNumber("123");
      final ApplicationFormData applicationFormData = new ApplicationFormData();
      applicationFormData.setApplicationTypeCategory(APP_TYPE_SUBSTANTIVE);

      assertThat(
              mockMvc.perform(
                  post("/amendments/application-type")
                      .flashAttr(APPLICATION_FORM_DATA, applicationFormData)
                      .sessionAttr(USER_DETAILS, userDetail)
                      .sessionAttr(CASE, ebsCase)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/amendments/create");

      verifyNoInteractions(lookupService);
    }
  }
}
