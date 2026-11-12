package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CURRENT_OPPONENT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.INDIVIDUAL_LIABLE_PARTY_FLOW;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.WebUtils;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.GlobalBinderAdvice;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualLiablePartyFlow;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentFormData;
import uk.gov.laa.ccms.caab.bean.validators.BaselineTextValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.IndividualOpponentOtherDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.IndividualOpponentValidator;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.service.CaseOutcomeService;
import uk.gov.laa.ccms.caab.service.IndividualOpponentFormService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;

@ExtendWith(MockitoExtension.class)
class LiablePartyControllerTest {
  private static final String BASE =
      "/case/outcome-and-awards/cost-award/7/liable-parties/individual/add";
  private static final String EDIT = BASE.replace("/individual/add", "/individual/11/edit");
  private static final String CONTACT_VIEW =
      "application/liable-parties/individual-contact-details";
  private static final String OTHER_VIEW = "application/liable-parties/individual-other-details";
  @Mock private CaseOutcomeService caseOutcomeService;
  @Mock private LookupService lookupService;
  private MockMvc mvc;
  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    mvc =
        standaloneSetup(
                new LiablePartyController(
                    caseOutcomeService,
                    new IndividualOpponentFormService(lookupService),
                    new IndividualOpponentValidator(),
                    new IndividualOpponentOtherDetailsValidator(),
                    lookupService))
            .setControllerAdvice(new GlobalBinderAdvice(new BaselineTextValidator()))
            .build();
    session = new MockHttpSession();
    session.setAttribute(
        CASE,
        new ApplicationDetail()
            .caseReferenceNumber("300000001")
            .availableFunctions(List.of(FunctionConstants.OUTCOME_WITH_DISCHARGE)));
    session.setAttribute(USER_DETAILS, ApplicationTestUtils.buildUser());
  }

  @Test
  void staleConfirmationCannotCombineOneJourneysOtherDetailsWithAnotherJourneysContacts()
      throws Exception {
    stubContactDropdowns();
    stubRelationship(false);
    mvc.perform(get(BASE).session(session));
    final String originalToken = journeyToken();
    mvc.perform(validContact());
    mvc.perform(get(BASE).session(session));
    mvc.perform(validContact().param("middleNames", "Replacement"));
    final var replacement = flow();
    assertThat(replacement.getJourneyToken()).isNotEqualTo(originalToken);

    assertThatThrownBy(
            () ->
                mvc.perform(
                    post(BASE + "/other-details")
                        .session(session)
                        .param("journeyToken", originalToken)
                        .param("employerName", "Original employer")))
        .hasRootCauseMessage("No individual party draft exists for this case and award");

    assertThat(flow()).isSameAs(replacement);
    assertThat(flow().getContactDetails().getMiddleNames()).isEqualTo("Replacement");
    assertThat(flow().getOtherDetails().getEmployerName()).isNull();
    assertThat(flow().isContactDetailsComplete()).isTrue();
    verifyNotSaved();
  }

  @ParameterizedTest
  @CsvSource({"false,false", "false,true", "true,false", "true,true"})
  void missingAndStaleTokensCannotAccessOrChangeReplacementDrafts(
      boolean editing, boolean missingToken) {
    final var original = editing ? editDraft() : draft();
    final String originalToken = original.getJourneyToken();
    final var replacement = editing ? editDraft() : draft();
    replacement.setContactDetailsComplete(true);
    replacement.getContactDetails().setFirstName("Replacement");
    replacement.getOtherDetails().setEmployerName("Replacement employer");
    final String base = editing ? EDIT : BASE;
    final var requests =
        List.of(
            get(base).param("back", "true"),
            post(base).param("firstName", "Stale contact"),
            get(base + "/other-details"),
            post(base + "/other-details").param("employerName", "Stale employer"),
            post(base + "/other-details").param("action", "back"),
            get(base + "/cancel"));

    for (final var request : requests) {
      request.session(session);
      if (!missingToken) {
        request.param("journeyToken", originalToken);
      }
      assertThatThrownBy(() -> mvc.perform(request))
          .hasRootCauseMessage("No individual party draft exists for this case and award");
      assertThat(flow()).isSameAs(replacement);
      assertThat(flow().getContactDetails().getFirstName()).isEqualTo("Replacement");
      assertThat(flow().getOtherDetails().getEmployerName()).isEqualTo("Replacement employer");
      assertThat(flow().isContactDetailsComplete()).isTrue();
    }
    verifyNotSaved();
  }

  @Test
  void concurrentConfirmationsSaveTheDraftOnlyOnce() throws Exception {
    draft().setContactDetailsComplete(true);
    final var saving = new CountDownLatch(1);
    final var releaseSave = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              assertThat(Thread.holdsLock(WebUtils.getSessionMutex(session))).isTrue();
              saving.countDown();
              assertThat(releaseSave.await(5, TimeUnit.SECONDS)).isTrue();
              return null;
            })
        .when(caseOutcomeService)
        .createIndividualLiableParty(any(), any(), any(), any(), any(), any(), any());

    try (final var pool = Executors.newFixedThreadPool(2)) {
      final var first =
          pool.submit(
              () ->
                  mvc.perform(
                      post(BASE + "/other-details")
                          .session(session)
                          .param("journeyToken", journeyToken())));
      try {
        assertThat(saving.await(5, TimeUnit.SECONDS)).isTrue();
        final var started = new CountDownLatch(1);
        final var second =
            pool.submit(
                () -> {
                  started.countDown();
                  return mvc.perform(
                      post(BASE + "/other-details")
                          .session(session)
                          .param("journeyToken", journeyToken()));
                });
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS))
            .isInstanceOf(TimeoutException.class);
        releaseSave.countDown();
        first
            .get(5, TimeUnit.SECONDS)
            .andExpect(redirectedUrl("/case/outcome-and-awards/cost-award/7/liable-parties"));
        assertThatThrownBy(() -> second.get(5, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class)
            .hasRootCauseMessage("No individual party draft exists for this case and award");
      } finally {
        releaseSave.countDown();
      }
    }
    verify(caseOutcomeService, times(1))
        .createIndividualLiableParty(any(), any(), any(), any(), any(), any(), any());
    assertThat(session.getAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW)).isNull();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void startingAnotherJourneyDuringSaveRetainsTheNewDraft(boolean editing) throws Exception {
    final var original = draft();
    original.setContactDetailsComplete(true);
    final var replacement = new IndividualLiablePartyFlow("300000001", 123, "cost-award", 7, 11);
    if (editing) {
      when(caseOutcomeService.getIndividualLiableParty("300000001", 123, "cost-award", 7, 11))
          .thenReturn(replacement);
    }
    stubContactDropdowns();
    final var saving = new CountDownLatch(1);
    final var releaseSave = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              saving.countDown();
              assertThat(releaseSave.await(5, TimeUnit.SECONDS)).isTrue();
              return null;
            })
        .when(caseOutcomeService)
        .createIndividualLiableParty(any(), any(), any(), any(), any(), any(), any());

    try (final var pool = Executors.newFixedThreadPool(2)) {
      final var confirmation =
          pool.submit(
              () ->
                  mvc.perform(
                      post(BASE + "/other-details")
                          .session(session)
                          .param("journeyToken", journeyToken())));
      try {
        assertThat(saving.await(5, TimeUnit.SECONDS)).isTrue();
        final var started = new CountDownLatch(1);
        final var newJourney =
            pool.submit(
                () -> {
                  started.countDown();
                  return mvc.perform(
                      get(editing ? EDIT : BASE)
                          .session(session)
                          .param("journeyToken", journeyToken()));
                });
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        assertThatThrownBy(() -> newJourney.get(200, TimeUnit.MILLISECONDS))
            .isInstanceOf(TimeoutException.class);
        releaseSave.countDown();
        confirmation
            .get(5, TimeUnit.SECONDS)
            .andExpect(redirectedUrl("/case/outcome-and-awards/cost-award/7/liable-parties"));
        newJourney.get(5, TimeUnit.SECONDS).andExpect(view().name(CONTACT_VIEW));
      } finally {
        releaseSave.countDown();
      }
    }
    assertThat(flow()).isNotSameAs(original);
    if (editing) {
      assertThat(flow()).isSameAs(replacement);
    }
    mvc.perform(
            get(editing ? EDIT : BASE)
                .session(session)
                .param("journeyToken", journeyToken())
                .param("back", "true"))
        .andExpect(view().name(CONTACT_VIEW))
        .andExpect(model().attribute("liablePartyIndividual", flow().getContactDetails()));
    verify(caseOutcomeService, times(1))
        .createIndividualLiableParty(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void saveCleanupDoesNotRemoveAReplacementDraft() throws Exception {
    draft().setContactDetailsComplete(true);
    final var replacement = new IndividualLiablePartyFlow("300000001", 123, "land", 8);
    doAnswer(
            invocation -> {
              session.setAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW, replacement);
              return null;
            })
        .when(caseOutcomeService)
        .createIndividualLiableParty(any(), any(), any(), any(), any(), any(), any());
    mvc.perform(
            post(BASE + "/other-details").session(session).param("journeyToken", journeyToken()))
        .andExpect(redirectedUrl("/case/outcome-and-awards/cost-award/7/liable-parties"));
    assertThat(flow()).isSameAs(replacement);
  }

  @ParameterizedTest
  @CsvSource({
    "add, employmentStatus",
    "add, assessedIncomeFrequency",
    "11/edit, employmentStatus",
    "11/edit, assessedIncomeFrequency"
  })
  void baselineRejectsMarkupInOtherDetailsForAddAndEdit(String journey, String field)
      throws Exception {
    final var draft = "add".equals(journey) ? draft() : editDraft();
    draft.setContactDetailsComplete(true);
    stubOtherDropdowns();
    mvc.perform(
            post(BASE.replace("/individual/add", "/individual/" + journey) + "/other-details")
                .session(session)
                .param("journeyToken", journeyToken())
                .param(field, "<markup>"))
        .andExpect(view().name(OTHER_VIEW))
        .andExpect(
            model()
                .attributeHasFieldErrorCode(
                    "liablePartyOtherDetails",
                    field,
                    BaselineTextValidator.INVALID_CHARACTER_CODE));
    assertThat(flow()).isSameAs(draft);
    verifyNotSaved();
  }

  @ParameterizedTest
  @ValueSource(strings = {"valid", "invalid", "incomplete"})
  void confirmStoresTheDraftOnlyOncePerRequest(String outcome) throws Exception {
    final var flow = draft();
    flow.setContactDetailsComplete(!"incomplete".equals(outcome));
    session = spy(session);
    if ("invalid".equals(outcome)) {
      stubOtherDropdowns();
    }

    final var response =
        mvc.perform(
            post(BASE + "/other-details")
                .session(session)
                .param("journeyToken", journeyToken())
                .param("employerName", "invalid".equals(outcome) ? "<invalid>" : "Employer"));
    switch (outcome) {
      case "valid" ->
          response.andExpect(redirectedUrl("/case/outcome-and-awards/cost-award/7/liable-parties"));
      case "invalid" -> response.andExpect(view().name(OTHER_VIEW));
      case "incomplete" ->
          response.andExpect(redirectedUrl(BASE + "?back=true&journeyToken=" + journeyToken()));
      default -> throw new IllegalArgumentException(outcome);
    }

    verify(session, times(1)).setAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW, flow);
  }

  @ParameterizedTest
  @ValueSource(strings = {"cost-award", "financial-award", "land", "asset"})
  void loadsExistingIndividualIntoBothSharedForms(String path) throws Exception {
    final var existing = new IndividualLiablePartyFlow("300000001", 123, path, 7, 11);
    existing.getContactDetails().setFirstName("Existing");
    existing.getOtherDetails().setEmployerName("Employer");
    when(caseOutcomeService.getIndividualLiableParty("300000001", 123, path, 7, 11))
        .thenReturn(existing);
    stubContactDropdowns();
    mvc.perform(
            get(EDIT.replace("cost-award", path))
                .session(session)
                .param("journeyToken", journeyToken()))
        .andExpect(view().name(CONTACT_VIEW))
        .andExpect(model().attribute("liablePartyIndividual", existing.getContactDetails()))
        .andExpect(model().attribute("individualFormUrl", EDIT.replace("cost-award", path)))
        .andExpect(
            model()
                .attribute(
                    "individualBackUrl", "/case/outcome-and-awards/" + path + "/7/liable-parties"));
    assertThat(flow()).isSameAs(existing);
    assertThat(flow().isEditing()).isTrue();
    assertThat(flow().getOtherDetails().getEmployerName()).isEqualTo("Employer");
    verifyNotSaved();
  }

  @Test
  void editNextPersistsValidatedContactsUsingTheSessionOpponentId() throws Exception {
    editDraft();
    stubRelationship(false);
    mvc.perform(validEditContact().param("id", "999").param("opponentId", "999"))
        .andExpect(redirectedUrl(EDIT + "/other-details?journeyToken=" + journeyToken()));
    verify(caseOutcomeService)
        .updateIndividualLiablePartyContactDetails(
            eq("300000001"),
            eq(123),
            eq("cost-award"),
            eq(7),
            eq(11),
            eq("testLoginId"),
            org.mockito.ArgumentMatchers.argThat(
                form -> "Jane".equals(form.getFirstName()) && form.getId() == null));
    assertThat(flow().isContactDetailsComplete()).isTrue();
    verify(caseOutcomeService, never())
        .createIndividualLiableParty(any(), any(), any(), any(), any(), any(), any());
    verify(caseOutcomeService, never())
        .updateIndividualLiablePartyOtherDetails(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void invalidEditContactsAreNotSaved() throws Exception {
    editDraft();
    stubContactDropdowns();
    mvc.perform(post(EDIT).session(session).param("journeyToken", journeyToken()))
        .andExpect(view().name(CONTACT_VIEW))
        .andExpect(model().attributeHasFieldErrors("liablePartyIndividual", "firstName"))
        .andExpect(model().attribute("individualFormUrl", EDIT));
    assertThat(flow().isContactDetailsComplete()).isFalse();
    verifyNotSaved();
  }

  @Test
  void editNextFailureRetainsBothFormsAndDoesNotAdvance() throws Exception {
    editDraft().getOtherDetails().setEmployerName("Existing employer");
    stubRelationship(false);
    stubContactDropdowns();
    doThrow(new IllegalStateException("Save failed"))
        .when(caseOutcomeService)
        .updateIndividualLiablePartyContactDetails(any(), any(), any(), any(), any(), any(), any());
    mvc.perform(validEditContact())
        .andExpect(view().name(CONTACT_VIEW))
        .andExpect(model().attributeHasErrors("liablePartyIndividual"));
    assertThat(flow().isContactDetailsComplete()).isFalse();
    assertThat(flow().getOtherDetails().getEmployerName()).isEqualTo("Existing employer");
  }

  @Test
  void editBackDiscardsUnvalidatedChangesAndRestoresSavedOtherDetails() throws Exception {
    final var draft = editDraft();
    draft.setContactDetailsComplete(true);
    draft.getContactDetails().setFirstName("Saved contact");
    draft.getOtherDetails().setOtherInformation("Earlier failed draft");
    final var saved = new IndividualLiablePartyFlow("300000001", 123, "cost-award", 7, 11);
    saved.getOtherDetails().setEmployerName("Saved employer");
    saved.getOtherDetails().setAssessedIncome("123.45");
    when(caseOutcomeService.getIndividualLiableParty("300000001", 123, "cost-award", 7, 11))
        .thenReturn(saved);
    mvc.perform(
            post(EDIT + "/other-details")
                .session(session)
                .param("journeyToken", journeyToken())
                .param("action", "back")
                .param("employerName", "Changed employer")
                .param("assessedIncome", "invalid"))
        .andExpect(redirectedUrl(EDIT + "?back=true&journeyToken=" + journeyToken()));
    assertThat(flow()).isSameAs(draft);
    assertThat(flow().getOtherDetails().getAssessedIncome()).isEqualTo("123.45");
    assertThat(flow().getOtherDetails().getEmployerName()).isEqualTo("Saved employer");
    assertThat(flow().getOtherDetails().getOtherInformation()).isNull();
    assertThat(flow().getContactDetails().getFirstName()).isEqualTo("Saved contact");
    assertThat(flow().isContactDetailsComplete()).isTrue();
    stubContactDropdowns();
    mvc.perform(
            get(EDIT).session(session).param("journeyToken", journeyToken()).param("back", "true"))
        .andExpect(model().attribute("liablePartyIndividual", flow().getContactDetails()))
        .andExpect(model().attribute("individualFormUrl", EDIT));
    mvc.perform(get(EDIT + "/other-details").session(session).param("journeyToken", journeyToken()))
        .andExpect(model().attribute("liablePartyOtherDetails", saved.getOtherDetails()));
    verifyNotSaved();
  }

  @Test
  void editBackShowsReloadFailuresWithoutSavingOrDiscardingContacts() throws Exception {
    editDraft().setContactDetailsComplete(true);
    flow().getContactDetails().setFirstName("Saved contact");
    stubOtherDropdowns();
    when(caseOutcomeService.getIndividualLiableParty("300000001", 123, "cost-award", 7, 11))
        .thenThrow(new IllegalStateException("API unavailable"));

    mvc.perform(
            post(EDIT + "/other-details")
                .session(session)
                .param("journeyToken", journeyToken())
                .param("action", "back")
                .param("employerName", "Unsaved change"))
        .andExpect(view().name(OTHER_VIEW))
        .andExpect(model().attributeHasErrors("liablePartyOtherDetails"));
    assertThat(flow().getContactDetails().getFirstName()).isEqualTo("Saved contact");
    verifyNotSaved();
  }

  @Test
  void editConfirmUpdatesOtherDetailsWithoutCreatingAnything() throws Exception {
    editDraft().setContactDetailsComplete(true);
    mvc.perform(
            post(EDIT + "/other-details")
                .session(session)
                .param("journeyToken", journeyToken())
                .param("employerName", "Changed employer")
                .param("assessmentDate", "1/2/2020"))
        .andExpect(redirectedUrl("/case/outcome-and-awards/cost-award/7/liable-parties"));
    verify(caseOutcomeService)
        .updateIndividualLiablePartyOtherDetails(
            eq("300000001"),
            eq(123),
            eq("cost-award"),
            eq(7),
            eq(11),
            eq("testLoginId"),
            org.mockito.ArgumentMatchers.argThat(
                form ->
                    "Changed employer".equals(form.getEmployerName())
                        && "01/02/2020".equals(form.getAssessmentDate())));
    assertThat(session.getAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW)).isNull();
    verify(caseOutcomeService, never())
        .createIndividualLiableParty(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void editConfirmRejectsInvalidOtherDetailsAndKeepsTheEditIdentity() throws Exception {
    editDraft().setContactDetailsComplete(true);
    stubOtherDropdowns();
    mvc.perform(
            post(EDIT + "/other-details")
                .session(session)
                .param("journeyToken", journeyToken())
                .param("assessmentDate", "01/01/1899")
                .param("assessedAssets", "-1"))
        .andExpect(view().name(OTHER_VIEW))
        .andExpect(
            model()
                .attributeHasFieldErrors(
                    "liablePartyOtherDetails", "assessmentDate", "assessedAssets"))
        .andExpect(model().attribute("individualFormUrl", EDIT));
    assertThat(flow().getOpponentId()).isEqualTo(11);
    verifyNotSaved();
  }

  @Test
  void editConfirmFailureKeepsTheDraftForRetry() throws Exception {
    editDraft().setContactDetailsComplete(true);
    stubOtherDropdowns();
    doThrow(new IllegalStateException("Save failed"))
        .when(caseOutcomeService)
        .updateIndividualLiablePartyOtherDetails(any(), any(), any(), any(), any(), any(), any());
    mvc.perform(
            post(EDIT + "/other-details")
                .session(session)
                .param("journeyToken", journeyToken())
                .param("employerName", "Changed"))
        .andExpect(view().name(OTHER_VIEW))
        .andExpect(model().attributeHasErrors("liablePartyOtherDetails"));
    assertThat(flow().getOtherDetails().getEmployerName()).isEqualTo("Changed");
    assertThat(flow().getOpponentId()).isEqualTo(11);
  }

  @Test
  void cancellingAfterEditNextDoesNotRevertSavedContactsOrSaveOtherDetails() throws Exception {
    editDraft();
    stubRelationship(false);
    mvc.perform(validEditContact())
        .andExpect(redirectedUrl(EDIT + "/other-details?journeyToken=" + journeyToken()));
    mvc.perform(get(EDIT + "/cancel").session(session).param("journeyToken", journeyToken()))
        .andExpect(redirectedUrl("/case/outcome-and-awards/cost-award/7/liable-parties"));
    verify(caseOutcomeService)
        .updateIndividualLiablePartyContactDetails(
            any(), any(), any(), any(), eq(11), any(), any());
    verify(caseOutcomeService, never())
        .updateIndividualLiablePartyOtherDetails(any(), any(), any(), any(), any(), any(), any());
    assertThat(session.getAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW)).isNull();
  }

  @Test
  void addAndEditRoutesCannotConsumeEachOthersDraftsOrAnotherOpponentId() {
    draft().setContactDetailsComplete(true);
    assertThatThrownBy(
            () ->
                mvc.perform(
                    post(EDIT + "/other-details")
                        .session(session)
                        .param("journeyToken", journeyToken())))
        .hasRootCauseInstanceOf(uk.gov.laa.ccms.caab.exception.CaabApplicationException.class);
    editDraft().setContactDetailsComplete(true);
    assertThatThrownBy(
            () ->
                mvc.perform(
                    post(BASE + "/other-details")
                        .session(session)
                        .param("journeyToken", journeyToken())))
        .hasRootCauseInstanceOf(uk.gov.laa.ccms.caab.exception.CaabApplicationException.class);
    assertThatThrownBy(
            () ->
                mvc.perform(
                    post(EDIT.replace("/11/", "/12/") + "/other-details")
                        .session(session)
                        .param("journeyToken", journeyToken())))
        .hasRootCauseInstanceOf(uk.gov.laa.ccms.caab.exception.CaabApplicationException.class);
    verifyNotSaved();
  }

  private IndividualLiablePartyFlow editDraft() {
    final var existing = new IndividualLiablePartyFlow("300000001", 123, "cost-award", 7, 11);
    session.setAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW, existing);
    return existing;
  }

  private MockHttpServletRequestBuilder validEditContact() {
    return post(EDIT)
        .session(session)
        .param("journeyToken", journeyToken())
        .param("title", "MS")
        .param("surname", "Smith")
        .param("firstName", "Jane")
        .param("relationshipToCase", "OPPONENT")
        .param("relationshipToClient", "OTHER");
  }

  @ParameterizedTest
  @ValueSource(strings = {"cost-award", "financial-award", "land", "asset"})
  void startsAnIsolatedDraftForEachAwardType(String path) throws Exception {
    stubContactDropdowns();
    final var applicationOpponent = new IndividualOpponentFormData();
    applicationOpponent.setFirstName("Application opponent");
    session.setAttribute(CURRENT_OPPONENT, applicationOpponent);

    mvc.perform(
            get(BASE.replace("cost-award", path))
                .session(session)
                .param("journeyToken", journeyToken()))
        .andExpect(handler().handlerType(LiablePartyController.class))
        .andExpect(view().name(CONTACT_VIEW))
        .andExpect(model().attribute("awardPath", path))
        .andExpect(model().attribute("awardId", 7))
        .andExpect(
            model()
                .attribute(
                    "individualBackUrl", "/case/outcome-and-awards/" + path + "/7/liable-parties"));

    assertThat(session.getAttribute(CURRENT_OPPONENT)).isSameAs(applicationOpponent);
    assertThat(flow().getContactDetails()).isNotSameAs(applicationOpponent);
    assertThat(flow().getAwardPath()).isEqualTo(path);
    verify(caseOutcomeService).validatePartyAward("300000001", 123, path, 7);
    verifyNotSaved();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void startsFreshWithoutBackOrWithBackFalse(boolean explicitBack) throws Exception {
    final var existing = draft();
    existing.getContactDetails().setFirstName("Jane");
    existing.getOtherDetails().setEmployerName("Acme");
    existing.setContactDetailsComplete(true);
    stubContactDropdowns();
    final var request = get(BASE).session(session).param("journeyToken", journeyToken());
    if (explicitBack) {
      request.param("back", "false");
    }

    mvc.perform(request).andExpect(view().name(CONTACT_VIEW));

    assertThat(flow()).isNotSameAs(existing);
    assertThat(flow().getContactDetails().getFirstName()).isNull();
    assertThat(flow().getOtherDetails().getEmployerName()).isNull();
    assertThat(flow().isContactDetailsComplete()).isFalse();
    verify(caseOutcomeService).validatePartyAward("300000001", 123, "cost-award", 7);
    verifyNotSaved();
  }

  @ParameterizedTest
  @ValueSource(strings = {"cost-award", "financial-award", "land", "asset"})
  void backTrueResumesContactsWithoutStartingANewFlow(String path) throws Exception {
    final var existing = new IndividualLiablePartyFlow("300000001", 123, path, 7);
    existing.getContactDetails().setFirstName("Jane");
    existing.setContactDetailsComplete(true);
    session.setAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW, existing);
    stubContactDropdowns();

    mvc.perform(
            get(BASE.replace("cost-award", path))
                .session(session)
                .param("journeyToken", journeyToken())
                .param("back", "true"))
        .andExpect(view().name(CONTACT_VIEW))
        .andExpect(model().attribute("liablePartyIndividual", existing.getContactDetails()));

    assertThat(flow()).isSameAs(existing);
    assertThat(flow().getContactDetails().getFirstName()).isEqualTo("Jane");
    assertThat(flow().isContactDetailsComplete()).isTrue();
    verify(caseOutcomeService, never()).validatePartyAward(any(), any(), any(), any());
    verifyNotSaved();
  }

  @Test
  void backTrueRequiresAnExistingDraft() {
    assertThatThrownBy(
            () ->
                mvc.perform(
                    get(BASE)
                        .session(session)
                        .param("journeyToken", journeyToken())
                        .param("back", "true")))
        .hasRootCauseMessage("No individual party draft exists for this case and award");
    assertThat(session.getAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW)).isNull();
    verifyNotSaved();
  }

  @Test
  void separateContactDetailsMappingIsRemoved() throws Exception {
    final var existing = draft();
    mvc.perform(
            get(BASE + "/contact-details").session(session).param("journeyToken", journeyToken()))
        .andExpect(status().isNotFound());
    assertThat(flow()).isSameAs(existing);
    verifyNotSaved();
  }

  @Test
  void organisationJourneyRemainsUnimplemented() throws Exception {
    mvc.perform(
            get(BASE.replace("/individual", "/organisation"))
                .session(session)
                .param("journeyToken", journeyToken()))
        .andExpect(status().isNotFound());
    verifyNotSaved();
  }

  @Test
  void nextValidatesAndNormalisesContactsWithoutPersisting() throws Exception {
    draft();
    stubRelationship(false);
    mvc.perform(validContact().param("dateOfBirth", "1/2/2000"))
        .andExpect(redirectedUrl(BASE + "/other-details?journeyToken=" + journeyToken()));
    assertThat(flow().getContactDetails().getDateOfBirth()).isEqualTo("01/02/2000");
    assertThat(flow().isContactDetailsComplete()).isTrue();
    verifyNotSaved();
  }

  @Test
  void nextShowsRequiredContactErrorsAndCannotSkipAhead() throws Exception {
    draft();
    stubContactDropdowns();
    mvc.perform(post(BASE).session(session).param("journeyToken", journeyToken()))
        .andExpect(view().name(CONTACT_VIEW))
        .andExpect(
            model()
                .attributeHasFieldErrors(
                    "liablePartyIndividual",
                    "title",
                    "firstName",
                    "surname",
                    "relationshipToCase",
                    "relationshipToClient"));
    mvc.perform(get(BASE + "/other-details").session(session).param("journeyToken", journeyToken()))
        .andExpect(redirectedUrl(BASE + "?back=true&journeyToken=" + journeyToken()));
    verifyNotSaved();
  }

  @Test
  void childRequiresDateOfBirthAndSubmittedFlagsCannotOverrideIt() throws Exception {
    draft();
    stubRelationship(true);
    stubContactDropdowns();
    mvc.perform(validContact().param("dateOfBirthMandatory", "false"))
        .andExpect(view().name(CONTACT_VIEW))
        .andExpect(model().attributeHasFieldErrors("liablePartyIndividual", "dateOfBirth"));
    assertThat(flow().isContactDetailsComplete()).isFalse();
    verifyNotSaved();
  }

  @Test
  void validatesLengthsContactFormatsAndLegacyCertificateSpacing() throws Exception {
    draft();
    stubRelationship(false);
    stubContactDropdowns();
    mvc.perform(
            validContact()
                .param("middleNames", "a".repeat(36))
                .param("dateOfBirth", "31/02/2000")
                .param("nationalInsuranceNumber", "invalid")
                .param("telephoneMobile", "short")
                .param("certificateNumber", "AB  123")
                .param("addressLine1", "High Street")
                .param("country", "GBR")
                .param("postcode", "invalid"))
        .andExpect(view().name(CONTACT_VIEW))
        .andExpect(
            model()
                .attributeHasFieldErrors(
                    "liablePartyIndividual",
                    "middleNames",
                    "dateOfBirth",
                    "nationalInsuranceNumber",
                    "telephoneMobile",
                    "certificateNumber",
                    "postcode"));
    verifyNotSaved();
  }

  @Test
  void ignoresOwnershipAndOtherPageFieldsPostedToContactPage() throws Exception {
    draft();
    stubRelationship(false);
    mvc.perform(
            validContact()
                .param("id", "999")
                .param("partyId", "unrelated")
                .param("type", "Organisation")
                .param("appMode", "true")
                .param("amendment", "true")
                .param("otherInformation", "unexpected"))
        .andExpect(redirectedUrl(BASE + "/other-details?journeyToken=" + journeyToken()));
    final var contact = flow().getContactDetails();
    assertThat(contact.getId()).isNull();
    assertThat(contact.getPartyId()).isNull();
    assertThat(contact.getType()).isEqualTo("Individual");
    assertThat(contact.getAppMode()).isNull();
    assertThat(contact.getAmendment()).isNull();
    assertThat(contact.getOtherInformation()).isNull();
  }

  @Test
  void otherPageBackDiscardsUnvalidatedValuesAndPreservesContacts() throws Exception {
    draft().setContactDetailsComplete(true);
    flow().getContactDetails().setFirstName("Jane");
    flow().getOtherDetails().setOtherInformation("Earlier failed draft");
    mvc.perform(
            post(BASE + "/other-details")
                .session(session)
                .param("journeyToken", journeyToken())
                .param("action", "back")
                .param("employerAddress", "x".repeat(201))
                .param("assessedIncome", "not a number")
                .param("assessmentDate", "not a date"))
        .andExpect(redirectedUrl(BASE + "?back=true&journeyToken=" + journeyToken()));
    assertThat(flow().getOtherDetails().getAssessedIncome()).isNull();
    assertThat(flow().getOtherDetails().getEmployerAddress()).isNull();
    assertThat(flow().getOtherDetails().getAssessmentDate()).isNull();
    assertThat(flow().getOtherDetails().getOtherInformation()).isNull();
    assertThat(flow().getContactDetails().getFirstName()).isEqualTo("Jane");
    stubContactDropdowns();
    mvc.perform(
            get(BASE).session(session).param("journeyToken", journeyToken()).param("back", "true"))
        .andExpect(model().attribute("liablePartyIndividual", flow().getContactDetails()));
    mvc.perform(get(BASE + "/other-details").session(session).param("journeyToken", journeyToken()))
        .andExpect(model().attribute("liablePartyOtherDetails", flow().getOtherDetails()));
    verifyNotSaved();
  }

  @Test
  void confirmValidationErrorsRetainDraftAndRepopulateDropdowns() throws Exception {
    draft().setContactDetailsComplete(true);
    stubOtherDropdowns();
    mvc.perform(
            post(BASE + "/other-details")
                .session(session)
                .param("journeyToken", journeyToken())
                .param("employerAddress", "a".repeat(201))
                .param("assessedIncome", "-1")
                .param("assessedAssets", "1.234")
                .param("assessmentDate", "31/02/2026")
                .param("otherInformation", "a".repeat(2001)))
        .andExpect(view().name(OTHER_VIEW))
        .andExpect(
            model()
                .attributeHasFieldErrors(
                    "liablePartyOtherDetails",
                    "employerAddress",
                    "assessedIncome",
                    "assessedAssets",
                    "assessmentDate",
                    "otherInformation"))
        .andExpect(
            model()
                .attributeExists(
                    "employmentStatuses", "incomeFrequencies", "meansAssessmentOptions"));
    assertThat(flow().getOtherDetails().getEmployerAddress()).hasSize(201);
    verifyNotSaved();
  }

  @Test
  void confirmationSavesBothPagesOnlyOnceAndClearsOnlyItsOwnDraft() throws Exception {
    draft();
    stubRelationship(false);
    final var applicationOpponent = new IndividualOpponentFormData();
    session.setAttribute(CURRENT_OPPONENT, applicationOpponent);
    mvc.perform(validContact())
        .andExpect(redirectedUrl(BASE + "/other-details?journeyToken=" + journeyToken()));
    final var contact = flow().getContactDetails();
    mvc.perform(
            post(BASE + "/other-details")
                .session(session)
                .param("journeyToken", journeyToken())
                .param("employmentStatus", "EMPLOYED")
                .param("employerName", "Acme")
                .param("employerAddress", "High Street")
                .param("courtOrderedMeansAssessment", "true")
                .param("assessedIncome", "123.45")
                .param("assessedIncomeFrequency", "MONTHLY")
                .param("assessedAssets", "2000")
                .param("assessmentDate", "1/2/2030")
                .param("otherInformation", "Liable party details")
                .param("firstName", "tampered"))
        .andExpect(redirectedUrl("/case/outcome-and-awards/cost-award/7/liable-parties"));
    verify(caseOutcomeService)
        .createIndividualLiableParty(
            eq("300000001"),
            eq(123),
            eq("cost-award"),
            eq(7),
            eq("testLoginId"),
            eq(contact),
            org.mockito.ArgumentMatchers.argThat(
                other ->
                    "01/02/2030".equals(other.getAssessmentDate())
                        && "123.45".equals(other.getAssessedIncome())
                        && Boolean.TRUE.equals(other.getCourtOrderedMeansAssessment())
                        && "Liable party details".equals(other.getOtherInformation())));
    assertThat(contact.getFirstName()).isEqualTo("Jane");
    assertThat(session.getAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW)).isNull();
    assertThat(session.getAttribute(CURRENT_OPPONENT)).isSameAs(applicationOpponent);
    assertThatThrownBy(
            () ->
                mvc.perform(
                    post(BASE + "/other-details")
                        .session(session)
                        .param("journeyToken", journeyToken())))
        .hasRootCauseMessage("No individual party draft exists for this case and award");
  }

  @Test
  void confirmAllowsEmptyOptionalOtherDetails() throws Exception {
    draft().setContactDetailsComplete(true);
    mvc.perform(
            post(BASE + "/other-details").session(session).param("journeyToken", journeyToken()))
        .andExpect(redirectedUrl("/case/outcome-and-awards/cost-award/7/liable-parties"));
    verify(caseOutcomeService)
        .createIndividualLiableParty(
            eq("300000001"), eq(123), eq("cost-award"), eq(7), eq("testLoginId"), any(), any());
  }

  @Test
  void saveFailureIsVisibleAndDoesNotDiscardEitherPage() throws Exception {
    draft().setContactDetailsComplete(true);
    stubOtherDropdowns();
    doThrow(new IllegalStateException("API unavailable"))
        .when(caseOutcomeService)
        .createIndividualLiableParty(any(), any(), any(), any(), any(), any(), any());
    mvc.perform(
            post(BASE + "/other-details")
                .session(session)
                .param("journeyToken", journeyToken())
                .param("employerName", "Acme"))
        .andExpect(view().name(OTHER_VIEW))
        .andExpect(model().attributeHasErrors("liablePartyOtherDetails"));
    assertThat(flow().getOtherDetails().getEmployerName()).isEqualTo("Acme");
    assertThat(flow().isContactDetailsComplete()).isTrue();
  }

  @Test
  void confirmationCannotBypassContactDetails() throws Exception {
    draft();
    mvc.perform(
            post(BASE + "/other-details").session(session).param("journeyToken", journeyToken()))
        .andExpect(redirectedUrl(BASE + "?back=true&journeyToken=" + journeyToken()));
    verifyNotSaved();
  }

  @ParameterizedTest
  @ValueSource(strings = {"case", "provider", "award", "type", "readonly"})
  void rejectsStaleOrUnauthorisedFlowContext(String changed) {
    draft().setContactDetailsComplete(true);
    switch (changed) {
      case "case" ->
          ((ApplicationDetail) session.getAttribute(CASE)).setCaseReferenceNumber("other");
      case "provider" ->
          ((uk.gov.laa.ccms.data.model.UserDetail) session.getAttribute(USER_DETAILS))
              .getProvider()
              .setId(456);
      case "readonly" ->
          ((ApplicationDetail) session.getAttribute(CASE)).setAvailableFunctions(List.of());
      default -> {}
    }
    final String path =
        switch (changed) {
          case "award" -> BASE.replace("/7/", "/8/");
          case "type" -> BASE.replace("cost-award", "land");
          default -> BASE;
        };
    assertThatThrownBy(
            () ->
                mvc.perform(
                    get(path)
                        .session(session)
                        .param("journeyToken", journeyToken())
                        .param("back", "true")))
        .hasRootCauseInstanceOf(uk.gov.laa.ccms.caab.exception.CaabApplicationException.class);
    assertThatThrownBy(
            () ->
                mvc.perform(
                    post(path + "/other-details")
                        .session(session)
                        .param("journeyToken", journeyToken())))
        .hasRootCauseInstanceOf(uk.gov.laa.ccms.caab.exception.CaabApplicationException.class);
    verifyNotSaved();
  }

  @ParameterizedTest
  @CsvSource({
    "cost-award,", "financial-award,", "land,", "asset,",
    "cost-award,11", "financial-award,11", "land,11", "asset,11"
  })
  void cancelDiscardsDraftAndReturnsToTheCurrentAwardParties(String path, Integer opponentId)
      throws Exception {
    session.setAttribute(
        INDIVIDUAL_LIABLE_PARTY_FLOW,
        new IndividualLiablePartyFlow("300000001", 123, path, 7, opponentId));
    final var applicationOpponent = new IndividualOpponentFormData();
    session.setAttribute(CURRENT_OPPONENT, applicationOpponent);
    final String partyList = "/case/outcome-and-awards/" + path + "/7/liable-parties";
    final String individual = opponentId == null ? "add" : opponentId + "/edit";
    mvc.perform(
            get(partyList + "/individual/" + individual + "/cancel")
                .session(session)
                .param("journeyToken", journeyToken()))
        .andExpect(redirectedUrl(partyList));
    assertThat(session.getAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW)).isNull();
    assertThat(session.getAttribute(CURRENT_OPPONENT)).isSameAs(applicationOpponent);
    verifyNotSaved();
  }

  private MockHttpServletRequestBuilder validContact() {
    return post(BASE)
        .session(session)
        .param("journeyToken", journeyToken())
        .param("title", "MS")
        .param("surname", "Smith")
        .param("firstName", "Jane")
        .param("relationshipToCase", "OPPONENT")
        .param("relationshipToClient", "OTHER");
  }

  private IndividualLiablePartyFlow draft() {
    final var flow = new IndividualLiablePartyFlow("300000001", 123, "cost-award", 7);
    session.setAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW, flow);
    return flow;
  }

  private IndividualLiablePartyFlow flow() {
    return (IndividualLiablePartyFlow) session.getAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW);
  }

  private String journeyToken() {
    return flow() == null ? "" : flow().getJourneyToken();
  }

  private void stubContactDropdowns() {
    when(lookupService.getPersonToCaseRelationships())
        .thenReturn(Mono.just(new RelationshipToCaseLookupDetail().content(List.of())));
    when(lookupService.getCommonValues(any()))
        .thenReturn(Mono.just(new CommonLookupDetail().content(List.of())));
    when(lookupService.getCountries())
        .thenReturn(Mono.just(new CommonLookupDetail().content(List.of())));
  }

  private void stubOtherDropdowns() {
    when(lookupService.getCommonValues(any()))
        .thenReturn(Mono.just(new CommonLookupDetail().content(List.of())));
  }

  private void stubRelationship(boolean dobMandatory) {
    when(lookupService.getPersonToCaseRelationship("OPPONENT"))
        .thenReturn(
            Mono.just(
                Optional.of(
                    new RelationshipToCaseLookupValueDetail().dateOfBirthMandatory(dobMandatory))));
  }

  private void verifyNotSaved() {
    verify(caseOutcomeService, never())
        .createIndividualLiableParty(any(), any(), any(), any(), any(), any(), any());
    verify(caseOutcomeService, never())
        .updateIndividualLiablePartyContactDetails(any(), any(), any(), any(), any(), any(), any());
    verify(caseOutcomeService, never())
        .updateIndividualLiablePartyOtherDetails(any(), any(), any(), any(), any(), any(), any());
  }
}
