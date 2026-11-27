package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ORGANISATION_LIABLE_PARTY_FLOW;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.controller.application.LiablePartyController.ORGANISATION_CREATE_VIEW;
import static uk.gov.laa.ccms.caab.controller.application.LiablePartyController.ORGANISATION_RESULTS_VIEW;
import static uk.gov.laa.ccms.caab.controller.application.LiablePartyController.ORGANISATION_SEARCH_VIEW;
import static uk.gov.laa.ccms.caab.controller.application.LiablePartyController.ORGANISATION_SHARED_VIEW;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.GlobalBinderAdvice;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationLiablePartyFlow;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationOpponentFormData;
import uk.gov.laa.ccms.caab.bean.validators.BaselineTextValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.OrganisationOpponentValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.OrganisationSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.OrganisationResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.service.CaseOutcomeService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.OpponentService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LiablePartyOrganisationControllerTest {
  private static final String BASE = "/case/outcome-and-awards/cost-award/7/liable-parties";
  private static final String ORG = BASE + "/organisation";

  @Mock private CaseOutcomeService caseOutcomeService;
  @Mock private OpponentService opponentService;
  @Mock private LookupService lookupService;
  private MockMvc mvc;
  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    mvc =
        standaloneSetup(
                new LiablePartyController(
                    caseOutcomeService,
                    null,
                    null,
                    null,
                    lookupService,
                    opponentService,
                    new OrganisationSearchCriteriaValidator(),
                    new OrganisationOpponentValidator()))
            .setControllerAdvice(new GlobalBinderAdvice(new BaselineTextValidator()))
            .build();
    session = new MockHttpSession();
    session.setAttribute(
        CASE,
        new ApplicationDetail()
            .caseReferenceNumber("300000001")
            .availableFunctions(List.of(FunctionConstants.OUTCOME_WITH_DISCHARGE)));
    session.setAttribute(USER_DETAILS, ApplicationTestUtils.buildUser());
    when(lookupService.getCommonValues(any())).thenReturn(Mono.just(new CommonLookupDetail()));
    when(lookupService.getCountries()).thenReturn(Mono.just(new CommonLookupDetail()));
    when(lookupService.getOrganisationToCaseRelationships())
        .thenReturn(
            Mono.just(
                new RelationshipToCaseLookupDetail()
                    .addContentItem(
                        new RelationshipToCaseLookupValueDetail().code("OPP").description("Opp"))));
  }

  @ParameterizedTest
  @ValueSource(strings = {"cost-award", "financial-award", "land", "asset"})
  void startingTheSearchCreatesAnAwardScopedDraft(String path) throws Exception {
    final var existing = draft();

    mvc.perform(get(ORG.replace("cost-award", path) + "/search").session(session))
        .andExpect(view().name(ORGANISATION_SEARCH_VIEW))
        .andExpect(model().attribute("awardPath", path))
        .andExpect(
            model()
                .attribute(
                    "organisationFormUrl",
                    "/case/outcome-and-awards/" + path + "/7/liable-parties/organisation"));

    assertThat(flow()).isNotSameAs(existing);
    assertThat(flow().getAwardPath()).isEqualTo(path);
    verify(caseOutcomeService).validatePartyAward("300000001", 123, path, 7);
  }

  @Test
  void readOnlyCasesCannotStartTheJourney() {
    session.setAttribute(
        CASE,
        new ApplicationDetail().caseReferenceNumber("300000001").availableFunctions(List.of()));

    assertThatThrownBy(() -> mvc.perform(get(ORG + "/search").session(session)))
        .hasRootCauseMessage("The case outcome is read-only");
    assertThat(session.getAttribute(ORGANISATION_LIABLE_PARTY_FLOW)).isNull();
  }

  @Test
  void backResumesTheExistingSearchCriteria() throws Exception {
    final var existing = draft();
    existing.getSearchCriteria().setName("Luton");

    mvc.perform(
            get(ORG + "/search")
                .session(session)
                .param("back", "true")
                .param("journeyToken", token()))
        .andExpect(view().name(ORGANISATION_SEARCH_VIEW))
        .andExpect(model().attribute("organisationSearchCriteria", existing.getSearchCriteria()));

    assertThat(flow()).isSameAs(existing);
    verify(caseOutcomeService, never()).validatePartyAward(any(), any(), any(), any());
  }

  @Test
  void staleOrMissingTokensCannotUseTheDraft() {
    final var existing = draft();
    final var requests =
        List.of(
            get(ORG + "/search").param("back", "true"),
            post(ORG + "/search").param("name", "Luton"),
            get(ORG + "/search/results"),
            get(ORG + "/ORG-1/select"),
            post(ORG + "/select").param("relationshipToCase", "OPP"),
            get(ORG + "/create"),
            post(ORG + "/create").param("organisationName", "Test"),
            get(ORG + "/cancel"));
    for (final var request : requests) {
      request.session(session).param("journeyToken", "stale");
      assertThatThrownBy(() -> mvc.perform(request))
          .hasRootCauseMessage("No organisation party draft exists for this case and award");
      assertThat(flow()).isSameAs(existing);
    }
    verifyNotSaved();
  }

  @Test
  void draftsCannotCrossAwards() {
    session.setAttribute(
        ORGANISATION_LIABLE_PARTY_FLOW,
        new OrganisationLiablePartyFlow("300000001", 123, "land", 7));

    assertThatThrownBy(
            () -> mvc.perform(get(ORG + "/create").session(session).param("journeyToken", token())))
        .hasRootCauseMessage("No organisation party draft exists for this case and award");
  }

  @Test
  void searchRequiresAnOrganisationName() throws Exception {
    draft();

    mvc.perform(post(ORG + "/search").session(session).param("journeyToken", token()))
        .andExpect(view().name(ORGANISATION_SEARCH_VIEW))
        .andExpect(model().attributeHasFieldErrors("organisationSearchCriteria", "name"));
  }

  @Test
  void validSearchStoresCriteriaAndRedirectsToResults() throws Exception {
    final var existing = draft();
    existing.setSelectedOrganisation(new OrganisationOpponentFormData());

    mvc.perform(
            post(ORG + "/search")
                .session(session)
                .param("journeyToken", token())
                .param("name", "Luton")
                .param("city", "Luton"))
        .andExpect(redirectedUrl(ORG + "/search/results?journeyToken=" + token()));

    assertThat(flow().getSearchCriteria().getName()).isEqualTo("Luton");
    assertThat(flow().getSelectedOrganisation()).isNull();
  }

  @Test
  void resultsAreStoredAndDisplayed() throws Exception {
    draft().getSearchCriteria().setName("Test");
    final var results = results("ORG-1");
    when(opponentService.getOrganisations(
            any(), eq("testLoginId"), eq("testUserType"), eq(1), eq(10)))
        .thenReturn(results);

    mvc.perform(
            get(ORG + "/search/results")
                .session(session)
                .param("journeyToken", token())
                .param("page", "1"))
        .andExpect(view().name(ORGANISATION_RESULTS_VIEW))
        .andExpect(model().attribute("organisationSearchResults", results))
        .andExpect(
            model().attribute("currentUrl", ORG + "/search/results?journeyToken=" + token()));

    assertThat(flow().getSearchResults()).isSameAs(results);
  }

  @Test
  void resultsWithoutCriteriaReturnToTheSearch() throws Exception {
    draft();

    mvc.perform(get(ORG + "/search/results").session(session).param("journeyToken", token()))
        .andExpect(redirectedUrl(ORG + "/search?back=true&journeyToken=" + token()));
    verify(opponentService, never()).getOrganisations(any(), any(), any(), anyInt(), anyInt());
  }

  @Test
  void tooManyResultsRedisplaysTheSearchWithAnError() throws Exception {
    draft().getSearchCriteria().setName("Council");
    when(opponentService.getOrganisations(any(), any(), any(), anyInt(), anyInt()))
        .thenThrow(new TooManyResultsException("too many"));

    mvc.perform(get(ORG + "/search/results").session(session).param("journeyToken", token()))
        .andExpect(view().name(ORGANISATION_SEARCH_VIEW))
        .andExpect(model().attributeHasErrors("organisationSearchCriteria"));
    assertThat(flow().getSearchResults()).isNull();
  }

  @Test
  void selectingAnOrganisationNotInTheResultsIsRejected() {
    draft().setSearchResults(results("ORG-1"));

    assertThatThrownBy(
            () ->
                mvc.perform(
                    get(ORG + "/ORG-2/select").session(session).param("journeyToken", token())))
        .hasRootCauseMessage("Invalid organisation partyId supplied");
    verify(opponentService, never()).getOrganisationOpponent(any(), any(), any());
  }

  @Test
  void selectingAnOfferedOrganisationShowsItsDetails() throws Exception {
    draft().setSearchResults(results("ORG-1"));
    final var organisation = sharedOrganisation();
    when(opponentService.getOrganisationOpponent("ORG-1", "testLoginId", "testUserType"))
        .thenReturn(organisation);

    mvc.perform(get(ORG + "/ORG-1/select").session(session).param("journeyToken", token()))
        .andExpect(view().name(ORGANISATION_SHARED_VIEW))
        .andExpect(model().attributeExists("liablePartySharedOrganisation", "relationshipsToCase"));

    assertThat(flow().getSelectedOrganisation()).isSameAs(organisation);
    assertThat(flow().getSelectedOrganisation().getShared()).isTrue();
  }

  @Test
  void confirmingASharedOrganisationRequiresRelationships() throws Exception {
    draft().setSelectedOrganisation(sharedOrganisation());

    mvc.perform(post(ORG + "/select").session(session).param("journeyToken", token()))
        .andExpect(view().name(ORGANISATION_SHARED_VIEW))
        .andExpect(
            model()
                .attributeHasFieldErrors(
                    "liablePartySharedOrganisation", "relationshipToCase", "relationshipToClient"))
        .andExpect(
            model()
                .attribute(
                    "liablePartySharedOrganisation",
                    org.hamcrest.Matchers.hasProperty(
                        "organisationName", org.hamcrest.Matchers.is("Luton Borough Council"))));
    verifyNotSaved();
  }

  @Test
  void confirmingASharedOrganisationOnlyAcceptsRelationshipFields() throws Exception {
    draft().setSelectedOrganisation(sharedOrganisation());

    mvc.perform(
            post(ORG + "/select")
                .session(session)
                .param("journeyToken", token())
                .param("relationshipToCase", "OPP")
                .param("relationshipToClient", "OTHER")
                .param("otherInformation", "Some info")
                .param("organisationName", "Tampered")
                .param("partyId", "ORG-999")
                .param("shared", "false"))
        .andExpect(redirectedUrl(BASE));

    final var captor = ArgumentCaptor.forClass(OrganisationOpponentFormData.class);
    verify(caseOutcomeService)
        .createOrganisationLiableParty(
            eq("300000001"), eq(123), eq("cost-award"), eq(7), eq("testLoginId"), captor.capture());
    final var saved = captor.getValue();
    assertThat(saved.getOrganisationName()).isEqualTo("Luton Borough Council");
    assertThat(saved.getPartyId()).isEqualTo("ORG-1");
    assertThat(saved.getShared()).isTrue();
    assertThat(saved.getRelationshipToCase()).isEqualTo("OPP");
    assertThat(saved.getRelationshipToClient()).isEqualTo("OTHER");
    assertThat(saved.getOtherInformation()).isEqualTo("Some info");
    assertThat(session.getAttribute(ORGANISATION_LIABLE_PARTY_FLOW)).isNull();
  }

  @Test
  void aSharedOrganisationAlreadyOnTheAwardIsNotAddedAgain() throws Exception {
    draft().setSelectedOrganisation(sharedOrganisation());
    when(caseOutcomeService.hasSharedOrganisationLiableParty(
            "300000001", 123, "cost-award", 7, "ORG-1"))
        .thenReturn(true);

    mvc.perform(validSharedConfirm())
        .andExpect(view().name(ORGANISATION_SHARED_VIEW))
        .andExpect(model().attributeHasErrors("liablePartySharedOrganisation"));
    verifyNotSaved();
    assertThat(flow()).isNotNull();
  }

  @Test
  void sharedSaveFailureKeepsTheDraftForRetry() throws Exception {
    draft().setSelectedOrganisation(sharedOrganisation());
    doThrow(new CaabApiClientException("down", org.springframework.http.HttpStatus.BAD_GATEWAY))
        .when(caseOutcomeService)
        .createOrganisationLiableParty(any(), any(), any(), any(), any(), any());

    mvc.perform(validSharedConfirm())
        .andExpect(view().name(ORGANISATION_SHARED_VIEW))
        .andExpect(model().attributeHasErrors("liablePartySharedOrganisation"));
    assertThat(flow()).isNotNull();
  }

  @Test
  void confirmingWithoutASelectionReturnsToTheSearch() throws Exception {
    draft();

    mvc.perform(validSharedConfirm())
        .andExpect(redirectedUrl(ORG + "/search?back=true&journeyToken=" + token()));
    verifyNotSaved();
  }

  @Test
  void createFormIsPrefilledFromTheSearch() throws Exception {
    final var existing = draft();
    existing.getSearchCriteria().setName("Test");
    existing.getSearchCriteria().setPostcode("SP10 3AJ");

    mvc.perform(get(ORG + "/create").session(session).param("journeyToken", token()))
        .andExpect(view().name(ORGANISATION_CREATE_VIEW))
        .andExpect(
            model()
                .attribute(
                    "liablePartyOrganisation",
                    org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.hasProperty(
                            "organisationName", org.hamcrest.Matchers.is("Test")),
                        org.hamcrest.Matchers.hasProperty(
                            "postcode", org.hamcrest.Matchers.is("SP10 3AJ")))))
        .andExpect(
            model()
                .attributeExists(
                    "organisationTypes",
                    "relationshipsToCase",
                    "relationshipsToClient",
                    "countries",
                    "currentlyTradingOptions"));
  }

  @Test
  void createRequiresMandatoryFields() throws Exception {
    draft();

    mvc.perform(post(ORG + "/create").session(session).param("journeyToken", token()))
        .andExpect(view().name(ORGANISATION_CREATE_VIEW))
        .andExpect(
            model()
                .attributeHasFieldErrors(
                    "liablePartyOrganisation",
                    "organisationName",
                    "organisationType",
                    "relationshipToCase",
                    "relationshipToClient"));
    verifyNotSaved();
  }

  @Test
  void createSavesANewUnsharedOrganisationAndIgnoresIdentityFields() throws Exception {
    draft();

    mvc.perform(
            post(ORG + "/create")
                .session(session)
                .param("journeyToken", token())
                .param("organisationName", "Test")
                .param("organisationType", "LA")
                .param("relationshipToCase", "OPP")
                .param("relationshipToClient", "OTHER")
                .param("type", "Individual")
                .param("shared", "true")
                .param("partyId", "ORG-999")
                .param("id", "55"))
        .andExpect(redirectedUrl(BASE));

    final var captor = ArgumentCaptor.forClass(OrganisationOpponentFormData.class);
    verify(caseOutcomeService)
        .createOrganisationLiableParty(
            eq("300000001"), eq(123), eq("cost-award"), eq(7), eq("testLoginId"), captor.capture());
    final var saved = captor.getValue();
    assertThat(saved.getOrganisationName()).isEqualTo("Test");
    assertThat(saved.getType()).isEqualTo("Organisation");
    assertThat(saved.getShared()).isFalse();
    assertThat(saved.getPartyId()).isNull();
    assertThat(saved.getId()).isNull();
    assertThat(session.getAttribute(ORGANISATION_LIABLE_PARTY_FLOW)).isNull();
  }

  @Test
  void cancelDiscardsTheDraft() throws Exception {
    draft();

    mvc.perform(get(ORG + "/cancel").session(session).param("journeyToken", token()))
        .andExpect(redirectedUrl(BASE));
    assertThat(session.getAttribute(ORGANISATION_LIABLE_PARTY_FLOW)).isNull();
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
      validSharedConfirm() {
    return post(ORG + "/select")
        .session(session)
        .param("journeyToken", token())
        .param("relationshipToCase", "OPP")
        .param("relationshipToClient", "OTHER");
  }

  private OrganisationLiablePartyFlow draft() {
    final var flow = new OrganisationLiablePartyFlow("300000001", 123, "cost-award", 7);
    session.setAttribute(ORGANISATION_LIABLE_PARTY_FLOW, flow);
    return flow;
  }

  private OrganisationLiablePartyFlow flow() {
    return (OrganisationLiablePartyFlow) session.getAttribute(ORGANISATION_LIABLE_PARTY_FLOW);
  }

  private String token() {
    return flow() == null ? null : flow().getJourneyToken();
  }

  private static ResultsDisplay<OrganisationResultRowDisplay> results(final String partyId) {
    final var row = new OrganisationResultRowDisplay();
    row.setPartyId(partyId);
    row.setName("Luton Borough Council");
    final var results = new ResultsDisplay<>(List.of(row));
    results.setTotalElements(1);
    results.setTotalPages(1);
    results.setNumber(0);
    results.setSize(10);
    return results;
  }

  private static OrganisationOpponentFormData sharedOrganisation() {
    final var organisation = new OrganisationOpponentFormData();
    organisation.setPartyId("ORG-1");
    organisation.setShared(true);
    organisation.setOrganisationName("Luton Borough Council");
    organisation.setOrganisationType("LA");
    organisation.setCity("Luton");
    return organisation;
  }

  private void verifyNotSaved() {
    verify(caseOutcomeService, never())
        .createOrganisationLiableParty(any(), any(), any(), any(), any(), any());
  }
}
