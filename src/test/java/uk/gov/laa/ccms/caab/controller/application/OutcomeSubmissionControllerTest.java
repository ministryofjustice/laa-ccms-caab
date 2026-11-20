package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.bean.CaseDischargeFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.CaseDischargeValidator;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;
import uk.gov.laa.ccms.caab.service.CaseOutcomeService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.OutcomeSubmissionService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
class OutcomeSubmissionControllerTest {
  @Mock private OutcomeSubmissionService service;
  @Mock private CaseOutcomeService outcomes;
  @Mock private LookupService lookup;
  private MockMvc mvc;
  private MockHttpSession session;
  private ApplicationDetail currentCase;
  private UserDetail user;
  private CaseOutcomeDetail draft;
  private OutcomeSubmissionController controller;

  @BeforeEach
  void setup() {
    controller =
        new OutcomeSubmissionController(service, outcomes, lookup, new CaseDischargeValidator());
    org.mockito.Mockito.doCallRealMethod().when(service).requireSubmissionPermission(any(), any());
    mvc = MockMvcBuilders.standaloneSetup(controller).build();
    user = ApplicationTestUtils.buildUser();
    user.setFunctions(List.of("POD", "SUBOUT"));
    currentCase =
        new ApplicationDetail().caseReferenceNumber("case").availableFunctions(List.of("POD"));
    draft = new CaseOutcomeDetail().id(42);
    session = new MockHttpSession();
    session.setAttribute(CASE, currentCase);
    session.setAttribute(USER_DETAILS, user);
  }

  @AfterEach
  void dischargeRequestsNeverRepeatSubmissionPreparation() {
    verify(service, never()).prepareSubmission(any(), any());
  }

  @Test
  void submissionPermissionIsRequiredOnEveryRoute() throws Exception {
    user.setFunctions(List.of("POD"));
    mvc.perform(get("/case/outcome-and-awards/discharge").session(session))
        .andExpect(status().isForbidden());
    mvc.perform(post("/case/outcome-and-awards/discharge").session(session))
        .andExpect(status().isForbidden());
    verifyNoInteractions(outcomes);
  }

  @Test
  void dischargeGetAndPostRecheckEligibility() throws Exception {
    when(outcomes.requireCaseOutcome("case", user.getProvider().getId().intValue()))
        .thenReturn(draft);
    mvc.perform(get("/case/outcome-and-awards/discharge").session(session))
        .andExpect(status().isForbidden());
    mvc.perform(post("/case/outcome-and-awards/discharge").session(session))
        .andExpect(status().isForbidden());
    verify(service, never()).saveDischarge(any(), any(), any());
  }

  @Test
  void loadsSavedDischargeIncludingNoValues() throws Exception {
    eligibleDischarge();
    draft.setDischargeCaseInd(false);
    draft.setClientContinueInd(false);
    draft.setDischargeReason("CD");
    draft.setOtherDetails("Saved details");
    mvc.perform(get("/case/outcome-and-awards/discharge").session(session))
        .andExpect(view().name("application/request-case-discharge"))
        .andExpect(model().attribute("discharge", form(false, "CD", false, "Saved details")));
    verify(outcomes).requireCaseOutcome("case", user.getProvider().getId().intValue());
    verify(service, never()).saveDischarge(any(), any(), any());
  }

  @Test
  void yesSavesTypedBooleansAndAdvancesToSummary() throws Exception {
    eligibleDischarge();
    mvc.perform(
            post("/case/outcome-and-awards/discharge")
                .session(session)
                .param("dischargeCaseInd", "true")
                .param("dischargeReason", "CD")
                .param("clientContinueInd", "false")
                .param("otherDetails", "Details"))
        .andExpect(redirectedUrl("/case/outcome-and-awards/summary"));
    verify(service).saveDischarge(draft, form(true, "CD", false, "Details"), user.getLoginId());
    verify(outcomes).requireCaseOutcome("case", user.getProvider().getId().intValue());
  }

  @Test
  void noRetainsDependentFieldsAsInLegacy() throws Exception {
    eligibleDischarge();
    mvc.perform(
            post("/case/outcome-and-awards/discharge")
                .session(session)
                .param("dischargeCaseInd", "false")
                .param("dischargeReason", "CD")
                .param("clientContinueInd", "true"))
        .andExpect(redirectedUrl("/case/outcome-and-awards/summary"));
    final var captor = ArgumentCaptor.forClass(CaseDischargeFormData.class);
    verify(service).saveDischarge(eq(draft), captor.capture(), eq(user.getLoginId()));
    assertThat(captor.getValue().getDischargeReason()).isEqualTo("CD");
    assertThat(captor.getValue().getClientContinueInd()).isTrue();
  }

  @Test
  void missingFieldsInvalidReasonAndInvalidRadioValuesDoNotSave() throws Exception {
    eligibleDischarge();
    mvc.perform(
            post("/case/outcome-and-awards/discharge")
                .session(session)
                .param("dischargeCaseInd", "true"))
        .andExpect(view().name("application/request-case-discharge"))
        .andExpect(
            model().attributeHasFieldErrors("discharge", "dischargeReason", "clientContinueInd"));
    mvc.perform(
            post("/case/outcome-and-awards/discharge")
                .session(session)
                .param("dischargeCaseInd", "false")
                .param("dischargeReason", "FORGED"))
        .andExpect(model().attributeHasFieldErrors("discharge", "dischargeReason"));
    mvc.perform(
            post("/case/outcome-and-awards/discharge")
                .session(session)
                .param("dischargeCaseInd", "not-a-boolean"))
        .andExpect(model().attributeHasFieldErrors("discharge", "dischargeCaseInd"));
    verify(service, never()).saveDischarge(any(), any(), any());
  }

  @Test
  void dischargeSaveFailureStaysOnFormWithEnteredValues() throws Exception {
    eligibleDischarge();
    doThrow(new CaabApiClientException("failed", HttpStatus.BAD_REQUEST))
        .when(service)
        .saveDischarge(eq(draft), any(), eq(user.getLoginId()));
    mvc.perform(
            post("/case/outcome-and-awards/discharge")
                .session(session)
                .param("dischargeCaseInd", "false")
                .param("otherDetails", "Retain me"))
        .andExpect(view().name("application/request-case-discharge"))
        .andExpect(model().attributeHasErrors("discharge"));
  }

  @Test
  void deliberateErrorsUseTheSharedExceptionHandler() throws Exception {
    user.setFunctions(List.of("POD"));
    mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    mvc.perform(get("/case/outcome-and-awards/discharge").session(session))
        .andExpect(view().name("error"))
        .andExpect(model().attributeExists("error"));
  }

  @Test
  void rendersBothRadioGroupsSelectedValuesAndCancelWithoutPosting() throws Exception {
    eligibleDischarge();
    draft.setDischargeCaseInd(true);
    draft.setClientContinueInd(false);
    draft.setDischargeReason("CD");
    renderTemplates();
    final String html =
        mvc.perform(get("/case/outcome-and-awards/discharge").session(session))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(html)
        .contains(
            "Request Case Discharge",
            "Client Death",
            "action=\"/case/outcome-and-awards/discharge\"",
            "href=\"/case/outcome-and-awards\"",
            "maxlength=\"950\"")
        .containsPattern("(?s)<input[^>]*type=\"radio\"[^>]*value=\"true\"[^>]*checked=\"checked\"")
        .containsPattern(
            "(?s)<input[^>]*type=\"radio\"[^>]*value=\"false\"[^>]*checked=\"checked\"");
    assertThat(
            Pattern.compile("<input(?=[^>]*type=\"radio\")(?=[^>]*name=\"dischargeCaseInd\")[^>]*>")
                .matcher(html)
                .results()
                .count())
        .isEqualTo(2);
    assertThat(
            Pattern.compile(
                    "<input(?=[^>]*type=\"radio\")(?=[^>]*name=\"clientContinueInd\")[^>]*>")
                .matcher(html)
                .results()
                .count())
        .isEqualTo(2);
    verify(service, never()).saveDischarge(any(), any(), any());
  }

  @Test
  void rendersConditionalErrorsAndPreservesDetails() throws Exception {
    eligibleDischarge();
    renderTemplates();
    final String html =
        mvc.perform(
                post("/case/outcome-and-awards/discharge")
                    .session(session)
                    .param("dischargeCaseInd", "true")
                    .param("otherDetails", "Keep these details"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(html)
        .contains(
            "govuk-error-summary",
            "Keep these details",
            "Client Continuing Privately",
            "Discharge Reason");
  }

  private void renderTemplates() {
    final var resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    final var messages = new ResourceBundleMessageSource();
    messages.setBasename("messages");
    final var engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setTemplateEngineMessageSource(messages);
    final var views = new ThymeleafViewResolver();
    views.setTemplateEngine(engine);
    mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setViewResolvers(views)
            .setControllerAdvice(new CaseControllerTest.TestUserModelAdvice())
            .build();
  }

  private void eligibleDischarge() {
    when(outcomes.requireCaseOutcome("case", user.getProvider().getId().intValue()))
        .thenReturn(draft);
    when(service.canDischarge(currentCase, user, draft)).thenReturn(true);
    when(lookup.getCommonValues("XXCCMS_DISCHARGE_REASON"))
        .thenReturn(
            Mono.just(
                new CommonLookupDetail()
                    .content(
                        List.of(
                            new CommonLookupValueDetail()
                                .code("CD")
                                .description("Client Death")))));
  }

  private CaseDischargeFormData form(
      Boolean discharge, String reason, Boolean continuing, String details) {
    final var form = new CaseDischargeFormData();
    form.setDischargeCaseInd(discharge);
    form.setDischargeReason(reason);
    form.setClientContinueInd(continuing);
    form.setOtherDetails(details);
    return form;
  }
}
