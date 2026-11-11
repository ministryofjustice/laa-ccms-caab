package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_AWARD_ASSESSMENT_STATUS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_VALUATION_BASIS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.AWARD_TYPE_FORM;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.AwardTypeForm;
import uk.gov.laa.ccms.caab.bean.award.CostAwardFormData;
import uk.gov.laa.ccms.caab.bean.award.FinancialAwardFormData;
import uk.gov.laa.ccms.caab.bean.award.LandAwardFormData;
import uk.gov.laa.ccms.caab.bean.award.OtherAssetAwardFormData;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.AwardTypeValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.CostAwardValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.FinancialAwardValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.LandAwardValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.OtherAssetAwardValidator;
import uk.gov.laa.ccms.caab.bean.validators.awards.TimeRecoveryValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.IndividualOpponentOtherDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.IndividualOpponentValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.OrganisationOpponentValidator;
import uk.gov.laa.ccms.caab.client.CaabApiClientException;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.mapper.CostAwardMapper;
import uk.gov.laa.ccms.caab.mapper.FinancialAwardMapper;
import uk.gov.laa.ccms.caab.mapper.LandAwardMapper;
import uk.gov.laa.ccms.caab.mapper.LiablePartiesDisplayMapper;
import uk.gov.laa.ccms.caab.mapper.LiablePartiesDisplayMapperImpl;
import uk.gov.laa.ccms.caab.mapper.OpponentMapper;
import uk.gov.laa.ccms.caab.mapper.OpponentMapperImpl;
import uk.gov.laa.ccms.caab.mapper.OtherAssetAwardMapper;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.CaseOutcomeDetail;
import uk.gov.laa.ccms.caab.model.CostAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardRequest;
import uk.gov.laa.ccms.caab.model.LandAwardDetail;
import uk.gov.laa.ccms.caab.model.LandAwardRequest;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardDetail;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardRequest;
import uk.gov.laa.ccms.caab.model.TimeRecoveryDetail;
import uk.gov.laa.ccms.caab.model.TimeRecoveryRequest;
import uk.gov.laa.ccms.caab.model.sections.LiablePartiesDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.CaseOutcomeService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
class AwardControllerTest {

  @Mock private LookupService lookupService;
  @Mock private CaseOutcomeService caseOutcomeService;
  @Mock private ApplicationService applicationService;
  @Mock private AwardTypeValidator awardTypeValidator;
  @Mock private CostAwardValidator costAwardValidator;
  @Mock private FinancialAwardValidator financialAwardValidator;
  @Mock private CostAwardMapper costAwardMapper;
  @Mock private FinancialAwardMapper financialAwardMapper;
  @Mock private OtherAssetAwardValidator otherAssetAwardValidator;
  @Mock private OtherAssetAwardMapper otherAssetAwardMapper;
  @Mock private LandAwardValidator landAwardValidator;
  @Mock private LandAwardMapper landAwardMapper;
  @Spy private TimeRecoveryValidator timeRecoveryValidator = new TimeRecoveryValidator();
  @Mock private IndividualOpponentValidator individualOpponentValidator;
  @Mock private OrganisationOpponentValidator organisationOpponentValidator;

  // A real (spied) other-details validator - not a mock - so the employer-name / assessed-income
  // length and limit checks it performs are genuinely exercised when a candidate is filtered.
  @Spy
  private IndividualOpponentOtherDetailsValidator individualOpponentOtherDetailsValidator =
      new IndividualOpponentOtherDetailsValidator();

  // A real (spied) mapper so toIndividualOpponentOtherDetailsFormData genuinely copies the
  // opponent's employment/assessment fields onto the form under test, rather than returning an
  // empty mock result that would hide genuine validation failures.
  @Spy private OpponentMapper opponentMapper = new OpponentMapperImpl();

  @Spy
  private LiablePartiesDisplayMapper liablePartiesDisplayMapper =
      new LiablePartiesDisplayMapperImpl();

  // A real (spied) Bean Validation validator - rather than a mock that would silently skip
  // validation - so that the candidate opponent filtering under test genuinely exercises the
  // form's Bean Validation constraints (for example, field length limits) alongside the custom
  // validators, matching what the dedicated add/edit screens enforce.
  @Spy private jakarta.validation.Validator beanValidator = newInitialisedBeanValidator();

  // Constructed explicitly in setUp() rather than via Mockito's @InjectMocks reflection: several
  // constructor parameters (the custom award/opponent validators and the generic Bean Validation
  // beanValidator) all implement the same org.springframework.validation.Validator interface, and
  // @InjectMocks cannot reliably match an ambiguous mock to the correct constructor slot once
  // there is more than one candidate of a compatible type.
  private AwardController controller;

  private MockMvcTester mockMvc;
  private ApplicationDetail ebsCase;
  private UserDetail user;
  private CaseOutcomeDetail caseOutcome;

  private static jakarta.validation.Validator newInitialisedBeanValidator() {
    final LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
    factory.afterPropertiesSet();
    return factory;
  }

  /**
   * Renders the party list page (GET) within the given session and returns the per-render {@code
   * rowIdentityToken} it produced, so a following POST can submit it back and have its selection
   * validated against this render's row identity snapshot.
   */
  private String renderPartyListAndCaptureToken(
      final org.springframework.mock.web.MockHttpSession httpSession,
      final ApplicationDetail activeApplication) {
    final var result =
        mockMvc.perform(
            get("/case/outcome-and-awards/land/7/liable-parties/add")
                .session(httpSession)
                .sessionAttr(CASE, ebsCase)
                .sessionAttr(APPLICATION, activeApplication)
                .sessionAttr(USER_DETAILS, user));
    return (String) result.getMvcResult().getModelAndView().getModel().get("rowIdentityToken");
  }

  @BeforeEach
  void setUp() {
    controller =
        new AwardController(
            lookupService,
            caseOutcomeService,
            applicationService,
            awardTypeValidator,
            costAwardValidator,
            financialAwardValidator,
            costAwardMapper,
            financialAwardMapper,
            otherAssetAwardValidator,
            otherAssetAwardMapper,
            landAwardValidator,
            landAwardMapper,
            timeRecoveryValidator,
            individualOpponentValidator,
            organisationOpponentValidator,
            individualOpponentOtherDetailsValidator,
            opponentMapper,
            liablePartiesDisplayMapper,
            beanValidator);
    mockMvc = MockMvcTester.create(MockMvcBuilders.standaloneSetup(controller).build());
    ebsCase = new ApplicationDetail().caseReferenceNumber("300000001");
    ebsCase.setAvailableFunctions(List.of(FunctionConstants.OUTCOME_WITH_DISCHARGE));
    caseOutcome = new CaseOutcomeDetail().id(55);
    ebsCase.setCaseOutcome(caseOutcome);
    user = ApplicationTestUtils.buildUser();
    lenient().when(costAwardMapper.toCostAward(any())).thenReturn(new CostAwardDetail());
    lenient()
        .when(financialAwardMapper.toFinancialAwardRequest(any()))
        .thenReturn(new FinancialAwardRequest());
    lenient()
        .when(otherAssetAwardMapper.toOtherAssetAwardRequest(any()))
        .thenReturn(new OtherAssetAwardRequest());
    lenient().when(landAwardMapper.toLandAwardRequest(any())).thenReturn(new LandAwardRequest());
    lenient()
        .when(lookupService.getCommonValues(any()))
        .thenReturn(Mono.just(new CommonLookupDetail().content(Collections.emptyList())));
    // Default: a present (if empty) relationships response, distinct from the lookup being
    // unavailable (a missing response body or null content), so existing fixtures that don't
    // themselves rely on a particular relationship continue to pass candidate validation.
    lenient()
        .when(lookupService.getPersonToCaseRelationships())
        .thenReturn(Mono.just(new RelationshipToCaseLookupDetail().content(List.of())));
    lenient()
        .when(lookupService.getOrganisationToCaseRelationships())
        .thenReturn(Mono.just(new RelationshipToCaseLookupDetail().content(List.of())));
    // Default: no parties already added to the award, so party-list candidates are unaffected
    // unless a test overrides this to verify the already-added exclusion.
    lenient()
        .when(caseOutcomeService.getLiablePartiesDisplay(any(), any(), any(), any()))
        .thenReturn(new LiablePartiesDisplay(null, null, null, false, false, List.of()));
    lenient()
        .when(caseOutcomeService.getAwardLiableParties(any(), any(), any(), any()))
        .thenReturn(List.of());
    // Default: the in-session application is treated as the persisted draft's own up-to-date
    // snapshot unless a test stubs a genuinely different reload result, so existing fixtures that
    // build their opponents directly onto the in-session application continue to work unchanged.
    lenient().when(applicationService.getApplication(any())).thenReturn(Mono.empty());
    // Default: simulate the real service's behaviour of invoking the per-opponent callbacks as
    // soon as each existing opponent is "linked" and each new opponent is "created", so tests
    // exercising the checkbox-hidden / already-added tracking behaviour don't each need to restub
    // this themselves.
    lenient()
        .doAnswer(
            invocation -> {
              final List<Integer> existingOpponentIds = invocation.getArgument(4);
              final List<OpponentDetail> newOpponents = invocation.getArgument(5);
              final Consumer<Integer> onExistingOpponentLinked = invocation.getArgument(7);
              final Consumer<OpponentDetail> onNewOpponentCreated = invocation.getArgument(8);
              if (existingOpponentIds != null) {
                existingOpponentIds.forEach(onExistingOpponentLinked);
              }
              if (newOpponents != null) {
                newOpponents.forEach(onNewOpponentCreated);
              }
              return null;
            })
        .when(caseOutcomeService)
        .addLiableParties(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Nested
  @DisplayName("GET: award liable and interested parties")
  class LiablePartiesTests {

    @ParameterizedTest
    @CsvSource({
      "cost-award, false, cost-award/7",
      "financial-award, false, financial-award/7",
      "land, false, land-property/7",
      "asset, false, asset/7",
      "land, true, land/7/time-related-recovery",
      "asset, true, asset/7/time-related-recovery"
    })
    void loadsSelectedAwardWithinCurrentCaseAndProvider(
        String awardPath, boolean timeRelatedRecovery, String backPath) {
      final var award =
          new LiablePartiesDisplay(
              "LAND", "Property", BigDecimal.TEN, false, timeRelatedRecovery, List.of());
      when(caseOutcomeService.getLiablePartiesDisplay("300000001", 123, awardPath, 7))
          .thenReturn(award);

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/" + awardPath + "/7/liable-parties")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasViewName("application/liable-parties")
          .model()
          .containsEntry("award", award)
          .containsEntry("awardPath", awardPath)
          .containsEntry("awardId", 7)
          .containsEntry("backUrl", "/case/outcome-and-awards/" + backPath);

      verify(caseOutcomeService).getLiablePartiesDisplay("300000001", 123, awardPath, 7);
      verifyNoMoreInteractions(caseOutcomeService);
    }

    @Test
    void selectingKnownOpponentsPersistsThemAndRedirectsToTheSummary() {
      final ApplicationDetail activeApplication = new ApplicationDetail();
      activeApplication.setCaseReferenceNumber(ebsCase.getCaseReferenceNumber());
      activeApplication.setId(999);
      lenient()
          .when(applicationService.getApplication("999"))
          .thenReturn(Mono.just(activeApplication));
      activeApplication.setOpponents(
          List.of(
              new OpponentDetail()
                  .id(42)
                  .type("INDIVIDUAL")
                  .title("MISS")
                  .firstName("Jane")
                  .surname("Smith")
                  .relationshipToCase("CHILD")
                  .relationshipToClient("CLIENT"),
              new OpponentDetail()
                  .id(43)
                  .type("ORGANISATION")
                  .organisationName("Acme & Co")
                  .organisationType("COMPANY")
                  .relationshipToCase("OWNER")
                  .relationshipToClient("CLIENT")));
      final org.springframework.mock.web.MockHttpSession httpSession =
          new org.springframework.mock.web.MockHttpSession();

      // The party list must be rendered (GET) before a selection from it is submitted (POST), so
      // that the rows' identities are recorded in-session and can be confirmed stable at POST time.
      final String rowIdentityToken =
          renderPartyListAndCaptureToken(httpSession, activeApplication);

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/land/7/liable-parties/add")
                      .session(httpSession)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, activeApplication)
                      .sessionAttr(USER_DETAILS, user)
                      .param("rowIdentityToken", rowIdentityToken)
                      .param("selectedPartyIds", "42", "43")))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/outcome-and-awards/land/7/liable-parties");

      verify(caseOutcomeService)
          .addLiableParties(
              eq("300000001"),
              eq(123),
              eq("land"),
              eq(7),
              eq(List.of(42, 43)),
              eq(List.of()),
              eq(user.getLoginId()),
              any(),
              any());
    }

    @Test
    void partyListReloadsPersistedOpponentsRatherThanTrustingTheStaleInSessionSnapshot() {
      // The opponent add/edit/remove journeys (see OpponentsSectionController) persist their
      // changes straight to the backend without refreshing the in-session application, so a
      // same-case session object can still be stale - for example, omitting an opponent that was
      // just added via the opponents section. The persisted draft's opponents must always be
      // reloaded fresh from the backend rather than trusted from the session.
      final ApplicationDetail staleSessionApplication = new ApplicationDetail();
      staleSessionApplication.setCaseReferenceNumber(ebsCase.getCaseReferenceNumber());
      staleSessionApplication.setId(999);
      staleSessionApplication.setOpponents(List.of());

      final ApplicationDetail persistedApplication = new ApplicationDetail();
      persistedApplication.setCaseReferenceNumber(ebsCase.getCaseReferenceNumber());
      persistedApplication.setId(999);
      persistedApplication.setOpponents(
          List.of(
              new OpponentDetail()
                  .id(77)
                  .type("INDIVIDUAL")
                  .title("MR")
                  .firstName("Newly")
                  .surname("Added")
                  .relationshipToCase("CHILD")
                  .relationshipToClient("CLIENT")));
      when(applicationService.getApplication("999")).thenReturn(Mono.just(persistedApplication));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/land/7/liable-parties/add")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, staleSessionApplication)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying(
              "parties",
              parties ->
                  assertThat((List<?>) parties)
                      .extracting("name")
                      .containsExactly("MR Newly Added"));
    }

    @Test
    void displayingThePartyListRejectsAStaleSessionSnapshotWhenTheFreshDraftCannotBeReloaded() {
      // If the reload fails (or the backend returns no body) for a persisted draft, the stale
      // in-session snapshot must not be used as a silent fallback: the opponent journeys never
      // refresh it, so it could still list a since-removed opponent, or omit a since-added one, as
      // if the reload had succeeded. The user must see a load/retry error with no selectable
      // candidates instead.
      final ApplicationDetail staleSessionApplication = new ApplicationDetail();
      staleSessionApplication.setCaseReferenceNumber(ebsCase.getCaseReferenceNumber());
      staleSessionApplication.setId(999);
      staleSessionApplication.setOpponents(
          List.of(
              new OpponentDetail()
                  .id(77)
                  .type("INDIVIDUAL")
                  .title("MR")
                  .firstName("Stale")
                  .surname("Snapshot")
                  .relationshipToCase("CHILD")
                  .relationshipToClient("CLIENT")));
      when(applicationService.getApplication("999"))
          .thenReturn(Mono.error(new CaabApiClientException("backend unavailable")));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/land/7/liable-parties/add")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, staleSessionApplication)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasViewName("application/party-list-screen")
          .model()
          .containsEntry("partyListError", true)
          .containsEntry("partyListLoadError", true)
          .hasEntrySatisfying("parties", parties -> assertThat((List<?>) parties).isEmpty());
    }

    @Test
    void submittingSelectedPartiesRejectsAStaleSessionSnapshotWhenTheFreshDraftCannotBeReloaded() {
      // The same reload-failure protection must apply on submission: validating and saving against
      // a stale snapshot could link, or recreate, an opponent that has since been removed from the
      // persisted draft.
      final ApplicationDetail staleSessionApplication = new ApplicationDetail();
      staleSessionApplication.setCaseReferenceNumber(ebsCase.getCaseReferenceNumber());
      staleSessionApplication.setId(999);
      staleSessionApplication.setOpponents(
          List.of(
              new OpponentDetail()
                  .id(77)
                  .type("INDIVIDUAL")
                  .title("MR")
                  .firstName("Stale")
                  .surname("Snapshot")
                  .relationshipToCase("CHILD")
                  .relationshipToClient("CLIENT")));
      when(applicationService.getApplication("999")).thenReturn(Mono.empty());

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/land/7/liable-parties/add")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, staleSessionApplication)
                      .sessionAttr(USER_DETAILS, user)
                      .param("selectedPartyIds", "77")))
          .hasStatusOk()
          .hasViewName("application/party-list-screen")
          .model()
          .containsEntry("partyListError", true)
          .containsEntry("partyListLoadError", true);
      verifyNoInteractions(caseOutcomeService);
    }

    @Test
    void
        knownOpponentStaysMarkedAsAlreadyAddedEvenWhenTheBackendsLiablePartiesReadHasNotYetCaughtUp() {
      // The backend's own read of the award's liable parties (getAwardLiableParties) is re-fetched
      // fresh on every page load, but it is not guaranteed to immediately reflect a link that was
      // only just written (for example, due to eventual consistency). Without in-session tracking
      // of the opponent id as soon as it is linked, the checkbox would reappear on an immediate
      // subsequent page load and risk the same opponent being linked again.
      final ApplicationDetail activeApplication = new ApplicationDetail();
      activeApplication.setCaseReferenceNumber(ebsCase.getCaseReferenceNumber());
      activeApplication.setId(999);
      lenient()
          .when(applicationService.getApplication("999"))
          .thenReturn(Mono.just(activeApplication));
      activeApplication.setOpponents(
          List.of(
              new OpponentDetail()
                  .id(42)
                  .type("INDIVIDUAL")
                  .title("MISS")
                  .firstName("Jane")
                  .surname("Smith")
                  .relationshipToCase("CHILD")
                  .relationshipToClient("CLIENT")));
      final org.springframework.mock.web.MockHttpSession httpSession =
          new org.springframework.mock.web.MockHttpSession();

      final String rowIdentityToken =
          renderPartyListAndCaptureToken(httpSession, activeApplication);

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/land/7/liable-parties/add")
                      .session(httpSession)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, activeApplication)
                      .sessionAttr(USER_DETAILS, user)
                      .param("rowIdentityToken", rowIdentityToken)
                      .param("selectedPartyIds", "42")))
          .hasStatus3xxRedirection();

      // getAwardLiableParties is left stubbed to its default empty response (see setUp), simulating
      // the backend not yet reflecting the just-written link on this immediate subsequent read.
      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/land/7/liable-parties/add")
                      .session(httpSession)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, activeApplication)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying(
              "parties",
              parties ->
                  assertThat((List<?>) parties)
                      .extracting("name", "alreadyAdded")
                      .containsExactly(tuple("MISS Jane Smith", true)));
    }

    @Test
    void selectingRowsWithoutAResolvableOpponentIdShowsAnError() {
      final ApplicationDetail activeApplication = new ApplicationDetail();
      activeApplication.setCaseReferenceNumber(ebsCase.getCaseReferenceNumber());
      activeApplication.setId(999);
      lenient()
          .when(applicationService.getApplication("999"))
          .thenReturn(Mono.just(activeApplication));
      activeApplication.setOpponents(
          List.of(
              new OpponentDetail()
                  .type("INDIVIDUAL")
                  .title("MISS")
                  .firstName("Jane")
                  .surname("Smith")
                  .relationshipToCase("CHILD")
                  .relationshipToClient("CLIENT")));
      when(lookupService.getCommonValues(COMMON_VALUE_CONTACT_TITLE))
          .thenReturn(Mono.just(new CommonLookupDetail().content(Collections.emptyList())));
      when(lookupService.getPersonToCaseRelationships())
          .thenReturn(
              Mono.just(new RelationshipToCaseLookupDetail().content(Collections.emptyList())));
      when(lookupService.getOrganisationToCaseRelationships())
          .thenReturn(
              Mono.just(new RelationshipToCaseLookupDetail().content(Collections.emptyList())));

      // The opponent above has no real CAAB id and no ebsId either, so it has no reliable
      // identity at all and is omitted from the candidate list entirely; selecting row id "-1"
      // (which was never offered to the user) still cannot resolve to any row and surfaces the
      // same unresolved error as a defensive fallback.
      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/land/7/liable-parties/add")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, activeApplication)
                      .sessionAttr(USER_DETAILS, user)
                      .param("selectedPartyIds", "-1")))
          .hasStatusOk()
          .hasViewName("application/party-list-screen")
          .model()
          .containsEntry("partyListError", true)
          .containsEntry("partyListUnresolvedError", true);

      verify(caseOutcomeService, never())
          .addLiableParties(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void selectingAnImportedOpponentWithNoExistingIdCreatesAndLinksANewOpponent() {
      final ApplicationDetail activeApplication = new ApplicationDetail();
      activeApplication.setCaseReferenceNumber(ebsCase.getCaseReferenceNumber());
      activeApplication.setId(999);
      lenient()
          .when(applicationService.getApplication("999"))
          .thenReturn(Mono.just(activeApplication));
      final OpponentDetail importedOpponent =
          new OpponentDetail()
              .ebsId("61700985")
              .type("INDIVIDUAL")
              .title("MISS")
              .firstName("Jane")
              .surname("Smith")
              .relationshipToCase("CHILD")
              .relationshipToClient("CLIENT");
      activeApplication.setOpponents(List.of(importedOpponent));
      final org.springframework.mock.web.MockHttpSession httpSession =
          new org.springframework.mock.web.MockHttpSession();

      // The party list must be rendered (GET) before a selection from it is submitted (POST), so
      // that the rows' identities are recorded in-session and can be confirmed stable at POST time.
      final String rowIdentityToken =
          renderPartyListAndCaptureToken(httpSession, activeApplication);

      // The opponent above has a real ebsId but no CAAB id, so it cannot be linked by id; it is
      // the first (and only) entry in the opponents list, so it is assigned the synthetic row id
      // "-1", and selecting it creates and links a brand new opponent from its full details
      // rather than guessing at (and potentially reusing) another opponent's real id.
      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/land/7/liable-parties/add")
                      .session(httpSession)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, activeApplication)
                      .sessionAttr(USER_DETAILS, user)
                      .param("rowIdentityToken", rowIdentityToken)
                      .param("selectedPartyIds", "-1")))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/outcome-and-awards/land/7/liable-parties");

      verify(caseOutcomeService)
          .addLiableParties(
              eq("300000001"),
              eq(123),
              eq("land"),
              eq(7),
              eq(List.of()),
              eq(List.of(importedOpponent)),
              eq(user.getLoginId()),
              any(),
              any());
    }

    @Test
    void createdOpponentStaysMarkedAsAlreadyAddedEvenThoughTheBackendAssignsItASyntheticEbsId() {
      // The backend's create-and-link endpoint assigns non-shared opponents a synthetic ebsId
      // rather than preserving their real one (see AwardsService#createAwardLiableParty), so the
      // award's own liable parties can no longer be matched back to this opponent's real ebsId on
      // a later page load. The checkbox must still be hidden for the rest of this session.
      final ApplicationDetail activeApplication = new ApplicationDetail();
      activeApplication.setCaseReferenceNumber(ebsCase.getCaseReferenceNumber());
      activeApplication.setId(999);
      lenient()
          .when(applicationService.getApplication("999"))
          .thenReturn(Mono.just(activeApplication));
      final OpponentDetail importedOpponent =
          new OpponentDetail()
              .ebsId("61700985")
              .type("INDIVIDUAL")
              .title("MISS")
              .firstName("Jane")
              .surname("Smith")
              .relationshipToCase("CHILD")
              .relationshipToClient("CLIENT");
      activeApplication.setOpponents(List.of(importedOpponent));
      final org.springframework.mock.web.MockHttpSession httpSession =
          new org.springframework.mock.web.MockHttpSession();

      // The party list must be rendered (GET) before a selection from it is submitted (POST), so
      // that the rows' identities are recorded in-session and can be confirmed stable at POST time.
      final String rowIdentityToken =
          renderPartyListAndCaptureToken(httpSession, activeApplication);

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/land/7/liable-parties/add")
                      .session(httpSession)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, activeApplication)
                      .sessionAttr(USER_DETAILS, user)
                      .param("rowIdentityToken", rowIdentityToken)
                      .param("selectedPartyIds", "-1")))
          .hasStatus3xxRedirection();

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/land/7/liable-parties/add")
                      .session(httpSession)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, activeApplication)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying(
              "parties",
              parties ->
                  assertThat((List<?>) parties)
                      .extracting("name", "alreadyAdded")
                      .containsExactly(tuple("MISS Jane Smith", true)));
    }

    @Test
    void selectingARowWhoseIdentityChangedSinceItWasRenderedIsRejected() {
      // The party is rendered with no real CAAB id, so its row id is a synthetic position ("-1")
      // in the opponents list rather than a stable identity. Before the selection is submitted,
      // the same position in the (shared, session-held) opponents list is reassigned to a
      // different opponent - simulating a concurrent edit in another tab - so the submission must
      // be rejected rather than silently linking the new occupant of that position.
      final ApplicationDetail activeApplication = new ApplicationDetail();
      activeApplication.setCaseReferenceNumber(ebsCase.getCaseReferenceNumber());
      activeApplication.setId(999);
      lenient()
          .when(applicationService.getApplication("999"))
          .thenReturn(Mono.just(activeApplication));
      final OpponentDetail renderedOpponent =
          new OpponentDetail()
              .ebsId("61700985")
              .type("INDIVIDUAL")
              .title("MISS")
              .firstName("Jane")
              .surname("Smith")
              .relationshipToCase("CHILD")
              .relationshipToClient("CLIENT");
      activeApplication.setOpponents(List.of(renderedOpponent));
      final org.springframework.mock.web.MockHttpSession httpSession =
          new org.springframework.mock.web.MockHttpSession();

      final String rowIdentityToken =
          renderPartyListAndCaptureToken(httpSession, activeApplication);

      final OpponentDetail replacementOpponent =
          new OpponentDetail()
              .ebsId("99999999")
              .type("INDIVIDUAL")
              .title("MR")
              .firstName("John")
              .surname("Doe")
              .relationshipToCase("OPP")
              .relationshipToClient("EX_PARTNER");
      activeApplication.setOpponents(List.of(replacementOpponent));

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/land/7/liable-parties/add")
                      .session(httpSession)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, activeApplication)
                      .sessionAttr(USER_DETAILS, user)
                      .param("rowIdentityToken", rowIdentityToken)
                      .param("selectedPartyIds", "-1")))
          .hasStatusOk()
          .hasViewName("application/party-list-screen")
          .model()
          .containsEntry("partyListError", true)
          .containsEntry("partyListUnresolvedError", true);

      verify(caseOutcomeService, never())
          .addLiableParties(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void alreadyAddedIsDetectedByEitherIdOrEbsIdRegardlessOfWhichIsPresent() {
      // An amendment can give an opponent a real CAAB id even though its EBS id was already
      // recorded as added to this award - both identities must be checked, not just whichever one
      // happens to be set on the candidate row, or it would be offered for selection again.
      final ApplicationDetail activeApplication = new ApplicationDetail();
      activeApplication.setCaseReferenceNumber(ebsCase.getCaseReferenceNumber());
      activeApplication.setId(999);
      lenient()
          .when(applicationService.getApplication("999"))
          .thenReturn(Mono.just(activeApplication));
      activeApplication.setOpponents(
          List.of(
              new OpponentDetail()
                  .id(42)
                  .ebsId("61700985")
                  .type("INDIVIDUAL")
                  .title("MISS")
                  .firstName("Jane")
                  .surname("Smith")
                  .relationshipToCase("CHILD")
                  .relationshipToClient("CLIENT")));
      when(caseOutcomeService.getAwardLiableParties("300000001", 123, "land", 7))
          .thenReturn(List.of(new OpponentDetail().ebsId("61700985")));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/land/7/liable-parties/add")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, activeApplication)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying(
              "parties",
              parties ->
                  assertThat((List<?>) parties)
                      .extracting("name", "alreadyAdded")
                      .containsExactly(tuple("MISS Jane Smith", true)));
    }

    @Test
    void aLegacyOpponentLinkedInAnEarlierSessionIsNotRecognisedAsAlreadyAddedWithoutAStableId() {
      // A legacy case opponent with no real CAAB id was linked to this award in an earlier,
      // separate session (so there is no in-session record of it), and the backend's
      // create-and-link endpoint assigned it a brand new, synthetic EBS id rather than
      // preserving the original one - so neither id nor EBS id can match it back to the
      // already-linked opponent returned by getAwardLiableParties on this fresh page load.
      // Personal details (name/DOB/relationship) are deliberately NOT used as a fallback here,
      // since two distinct opponents can share the same personal details, which would wrongly
      // hide one of them. This is a known, accepted gap for legacy opponents with no stable
      // backend-persisted identity, tracked separately; it does not affect opponents added
      // within a single session, which are always tracked by their real id/EBS id.
      final ApplicationDetail activeApplication = new ApplicationDetail();
      activeApplication.setCaseReferenceNumber(ebsCase.getCaseReferenceNumber());
      activeApplication.setId(999);
      lenient()
          .when(applicationService.getApplication("999"))
          .thenReturn(Mono.just(activeApplication));
      activeApplication.setOpponents(
          List.of(
              new OpponentDetail()
                  .ebsId("61700985")
                  .type("INDIVIDUAL")
                  .title("MISS")
                  .firstName("Jane")
                  .surname("Smith")
                  .relationshipToCase("CHILD")
                  .relationshipToClient("CLIENT")));
      when(caseOutcomeService.getAwardLiableParties("300000001", 123, "land", 7))
          .thenReturn(
              List.of(
                  new OpponentDetail()
                      .id(99)
                      .ebsId("SYNTHETIC-1")
                      .type("INDIVIDUAL")
                      .title("MISS")
                      .firstName("Jane")
                      .surname("Smith")
                      .relationshipToCase("CHILD")
                      .relationshipToClient("CLIENT")));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/land/7/liable-parties/add")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, activeApplication)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying(
              "parties",
              parties ->
                  assertThat((List<?>) parties)
                      .extracting("name", "alreadyAdded")
                      .containsExactly(tuple("MISS Jane Smith", false)));
    }

    @Test
    void anOpponentCreatedBeforeALaterOneInTheSameBatchFailsIsStillRecordedAsAlreadyAdded() {
      // If a batch of newly-selected opponents is only partially created before a later one
      // fails, the earlier successes must still be remembered as already-added for the rest of
      // this session, since the backend has genuinely linked them - a retry must not re-create
      // them a second time.
      final ApplicationDetail activeApplication = new ApplicationDetail();
      activeApplication.setCaseReferenceNumber(ebsCase.getCaseReferenceNumber());
      activeApplication.setId(999);
      lenient()
          .when(applicationService.getApplication("999"))
          .thenReturn(Mono.just(activeApplication));
      final OpponentDetail firstOpponent =
          new OpponentDetail()
              .ebsId("61700985")
              .type("INDIVIDUAL")
              .title("MISS")
              .firstName("Jane")
              .surname("Smith")
              .relationshipToCase("CHILD")
              .relationshipToClient("CLIENT");
      final OpponentDetail secondOpponent =
          new OpponentDetail()
              .ebsId("61700986")
              .type("INDIVIDUAL")
              .title("MR")
              .firstName("John")
              .surname("Doe")
              .relationshipToCase("CHILD")
              .relationshipToClient("CLIENT");
      activeApplication.setOpponents(List.of(firstOpponent, secondOpponent));
      final org.springframework.mock.web.MockHttpSession httpSession =
          new org.springframework.mock.web.MockHttpSession();

      // The party list must be rendered (GET) before a selection from it is submitted (POST), so
      // that the rows' identities are recorded in-session and can be confirmed stable at POST
      // time.
      final String rowIdentityToken =
          renderPartyListAndCaptureToken(httpSession, activeApplication);

      // Simulate the backend creating the first opponent successfully (invoking the callback for
      // it), then failing on the second.
      doAnswer(
              invocation -> {
                final Consumer<OpponentDetail> onNewOpponentCreated = invocation.getArgument(8);
                onNewOpponentCreated.accept(firstOpponent);
                throw new RuntimeException("backend failure creating second opponent");
              })
          .when(caseOutcomeService)
          .addLiableParties(any(), any(), any(), any(), any(), any(), any(), any(), any());

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/land/7/liable-parties/add")
                      .session(httpSession)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, activeApplication)
                      .sessionAttr(USER_DETAILS, user)
                      .param("rowIdentityToken", rowIdentityToken)
                      .param("selectedPartyIds", "-1", "-2")))
          .failure()
          .isNotNull();

      // Reset the mock's addLiableParties stub back to the default no-op-success behaviour for
      // the follow-up GET, which does not itself attempt to create anything.
      reset(caseOutcomeService);
      lenient()
          .when(caseOutcomeService.getLiablePartiesDisplay(any(), any(), any(), any()))
          .thenReturn(new LiablePartiesDisplay(null, null, null, false, false, List.of()));
      lenient()
          .when(caseOutcomeService.getAwardLiableParties(any(), any(), any(), any()))
          .thenReturn(List.of());

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/land/7/liable-parties/add")
                      .session(httpSession)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, activeApplication)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying(
              "parties",
              parties ->
                  assertThat((List<?>) parties)
                      .extracting("name", "alreadyAdded")
                      .containsExactly(
                          tuple("MISS Jane Smith", true), tuple("MR John Doe", false)));
    }

    @Test
    void aBackendFailureAddingSelectedPartiesRedisplaysThePartyListInsteadOfAGenericErrorPage() {
      // Every other award-mutation endpoint in this controller catches CaabApiClientException and
      // redisplays its own form with a specific, actionable message rather than letting the
      // exception propagate to the generic catch-all error page; addSelectedParties must behave
      // the same way.
      final ApplicationDetail activeApplication = new ApplicationDetail();
      activeApplication.setCaseReferenceNumber(ebsCase.getCaseReferenceNumber());
      activeApplication.setId(999);
      lenient()
          .when(applicationService.getApplication("999"))
          .thenReturn(Mono.just(activeApplication));
      activeApplication.setOpponents(
          List.of(
              new OpponentDetail()
                  .id(42)
                  .type("INDIVIDUAL")
                  .title("MISS")
                  .firstName("Jane")
                  .surname("Smith")
                  .relationshipToCase("CHILD")
                  .relationshipToClient("CLIENT")));
      final org.springframework.mock.web.MockHttpSession httpSession =
          new org.springframework.mock.web.MockHttpSession();
      final String rowIdentityToken =
          renderPartyListAndCaptureToken(httpSession, activeApplication);

      doThrow(new CaabApiClientException("backend rejected the request"))
          .when(caseOutcomeService)
          .addLiableParties(any(), any(), any(), any(), any(), any(), any(), any(), any());

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/land/7/liable-parties/add")
                      .session(httpSession)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, activeApplication)
                      .sessionAttr(USER_DETAILS, user)
                      .param("rowIdentityToken", rowIdentityToken)
                      .param("selectedPartyIds", "42")))
          .hasStatusOk()
          .hasViewName("application/party-list-screen")
          .model()
          .containsEntry("partyListError", true)
          .containsEntry("partyListUnresolvedError", true);
    }

    @Test
    void unsupportedAwardHasNoRoute() {
      assertThat(mockMvc.perform(get("/case/outcome-and-awards/unknown/7/liable-parties")))
          .hasStatus(404);
      verifyNoInteractions(caseOutcomeService);
    }

    @Test
    void partyListIncludesOnlyValidOpponentsIncludingDeletableOnes() {
      final ApplicationDetail amendmentApplication = new ApplicationDetail();
      amendmentApplication.setCaseReferenceNumber(ebsCase.getCaseReferenceNumber());
      amendmentApplication.setId(999);
      lenient()
          .when(applicationService.getApplication("999"))
          .thenReturn(Mono.just(amendmentApplication));
      amendmentApplication.setOpponents(
          List.of(
              new OpponentDetail()
                  .ebsId("61700001")
                  .type("INDIVIDUAL")
                  .title("Ms")
                  .firstName("Jane")
                  .surname("Smith")
                  .relationshipToCase("OWNER")
                  .relationshipToClient("CLIENT")
                  .address(new AddressDetail().addressLine1("1 High Street").postcode("AB1 2CD")),
              new OpponentDetail()
                  .ebsId("61700002")
                  .type("ORGANISATION")
                  .organisationName("Acme & Co")
                  .organisationType("COMPANY")
                  .relationshipToCase("OWNER")
                  .relationshipToClient("CLIENT"),
              // deleteInd indicates the opponent is deletable, not already deleted, so a newly
              // added, otherwise-valid opponent with deleteInd(true) must still be selectable.
              new OpponentDetail()
                  .id(99)
                  .type("INDIVIDUAL")
                  .deleteInd(true)
                  .title("Mr")
                  .firstName("New")
                  .surname("Opponent")
                  .relationshipToCase("OWNER")
                  .relationshipToClient("CLIENT"),
              new OpponentDetail().type("INDIVIDUAL").deleteInd(true).title("Mr"),
              new OpponentDetail().type("ORGANISATION").organisationName("Ghost Ltd")));

      ebsCase.setOpponents(
          List.of(
              new OpponentDetail()
                  .type("INDIVIDUAL")
                  .title("Old")
                  .firstName("Legacy")
                  .surname("Case")
                  .relationshipToCase("OWNER")
                  .relationshipToClient("CLIENT")));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/land/7/liable-parties/add")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, amendmentApplication)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying(
              "parties",
              parties ->
                  assertThat((List<?>) parties)
                      .extracting("name")
                      .containsExactlyInAnyOrder("Ms Jane Smith", "Acme & Co", "Mr New Opponent"));
    }

    @Test
    void candidateValidationReusesTheAlreadyFetchedRelationshipsInsteadOfFetchingPerOpponent() {
      // IndividualOpponentFormService#prepareForValidation ordinarily makes its own per-opponent,
      // blocking remote call to look up a single relationship by code. GET already fetches every
      // person relationship once up front to build the display, so validating candidates here
      // must reuse that same list instead of repeating the per-code lookup for every individual
      // candidate - otherwise N candidates would mean N sequential remote calls, all made while
      // holding the session mutex.
      final ApplicationDetail activeApplication = new ApplicationDetail();
      activeApplication.setCaseReferenceNumber(ebsCase.getCaseReferenceNumber());
      activeApplication.setId(999);
      lenient()
          .when(applicationService.getApplication("999"))
          .thenReturn(Mono.just(activeApplication));
      activeApplication.setOpponents(
          List.of(
              new OpponentDetail()
                  .id(201)
                  .type("INDIVIDUAL")
                  .title("MISS")
                  .firstName("Jane")
                  .surname("Smith")
                  .relationshipToCase("OWNER")
                  .relationshipToClient("CLIENT"),
              // A child's relationship requires a date of birth, which this opponent does not
              // have, so shared validation must reject it - but using the pre-fetched
              // relationship list rather than its own per-opponent remote call.
              new OpponentDetail()
                  .id(202)
                  .type("INDIVIDUAL")
                  .title("MASTER")
                  .firstName("No")
                  .surname("Dob")
                  .relationshipToCase("CHILD")
                  .relationshipToClient("CLIENT")));
      when(lookupService.getPersonToCaseRelationships())
          .thenReturn(
              Mono.just(
                  new RelationshipToCaseLookupDetail()
                      .content(
                          List.of(
                              new RelationshipToCaseLookupValueDetail()
                                  .code("CHILD")
                                  .description("Child")
                                  .dateOfBirthMandatory(true)))));
      doAnswer(
              invocation -> {
                final IndividualOpponentFormData form = invocation.getArgument(0);
                final Errors errors = invocation.getArgument(1);
                if (form.isDateOfBirthMandatory() && !StringUtils.hasText(form.getDateOfBirth())) {
                  errors.reject("dateOfBirth.required");
                }
                return null;
              })
          .when(individualOpponentValidator)
          .validateLiableParty(any(), any());

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/land/7/liable-parties/add")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, activeApplication)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying(
              "parties",
              parties ->
                  assertThat((List<?>) parties)
                      .extracting("name")
                      .containsExactly("MISS Jane Smith"));

      // The per-code relationship lookup used by IndividualOpponentFormService#prepareForValidation
      // must never be called - the pre-fetched list is reused for every candidate instead.
      verify(lookupService, never()).getPersonToCaseRelationship(any());
      verify(lookupService, times(1)).getPersonToCaseRelationships();
    }

    @Test
    void displayingThePartyListRejectsCandidatesWhenRelationshipMetadataIsUnavailableOnGet() {
      // safeRelationshipValues-style handling would turn a missing response/null content into an
      // empty list, making every candidate's date of birth appear optional - a child could then
      // pass validation and be offered for creation without one. The lookup being unavailable
      // must instead surface the load/retry error, exactly like a failed application reload.
      final ApplicationDetail activeApplication = new ApplicationDetail();
      activeApplication.setCaseReferenceNumber(ebsCase.getCaseReferenceNumber());
      activeApplication.setId(999);
      lenient()
          .when(applicationService.getApplication("999"))
          .thenReturn(Mono.just(activeApplication));
      activeApplication.setOpponents(
          List.of(
              new OpponentDetail()
                  .id(401)
                  .type("INDIVIDUAL")
                  .title("MASTER")
                  .firstName("No")
                  .surname("Dob")
                  .relationshipToCase("CHILD")
                  .relationshipToClient("CLIENT")));
      when(lookupService.getPersonToCaseRelationships()).thenReturn(Mono.empty());

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/land/7/liable-parties/add")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, activeApplication)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasViewName("application/party-list-screen")
          .model()
          .containsEntry("partyListError", true)
          .containsEntry("partyListLoadError", true)
          .hasEntrySatisfying("parties", parties -> assertThat((List<?>) parties).isEmpty());
    }

    @Test
    void submittingSelectedPartiesRejectsCandidatesWhenRelationshipMetadataIsUnavailableOnPost() {
      final ApplicationDetail activeApplication = new ApplicationDetail();
      activeApplication.setCaseReferenceNumber(ebsCase.getCaseReferenceNumber());
      activeApplication.setId(999);
      lenient()
          .when(applicationService.getApplication("999"))
          .thenReturn(Mono.just(activeApplication));
      activeApplication.setOpponents(
          List.of(
              new OpponentDetail()
                  .id(401)
                  .type("INDIVIDUAL")
                  .title("MASTER")
                  .firstName("No")
                  .surname("Dob")
                  .relationshipToCase("CHILD")
                  .relationshipToClient("CLIENT")));
      when(lookupService.getPersonToCaseRelationships())
          .thenReturn(Mono.just(new RelationshipToCaseLookupDetail().content(null)));

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/land/7/liable-parties/add")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, activeApplication)
                      .sessionAttr(USER_DETAILS, user)
                      .param("selectedPartyIds", "401")))
          .hasStatusOk()
          .hasViewName("application/party-list-screen")
          .model()
          .containsEntry("partyListError", true)
          .containsEntry("partyListLoadError", true);
      verifyNoInteractions(caseOutcomeService);
    }

    @Test
    void candidateFilteringAppliesBeanValidationAlongsideTheCustomValidators() {
      // Only the dedicated custom validators run against a form built here - calling only
      // IndividualOpponentValidator/OrganisationOpponentValidator skips the Bean Validation
      // constraints declared on the form itself (for example, @Size field length limits), so an
      // imported party with an overlength name could remain selectable and be submitted despite
      // failing the dedicated add screens, which do enforce Bean Validation via @Validated.
      final String overlengthName = "N".repeat(36); // DEFAULT_CHARACTER_SIZE is 35
      final String overlengthOrganisationName = "O".repeat(361); // ORGANISATION_NAME_CHARACTER_SIZE
      final ApplicationDetail activeApplication = new ApplicationDetail();
      activeApplication.setCaseReferenceNumber(ebsCase.getCaseReferenceNumber());
      activeApplication.setId(999);
      lenient()
          .when(applicationService.getApplication("999"))
          .thenReturn(Mono.just(activeApplication));
      activeApplication.setOpponents(
          List.of(
              new OpponentDetail()
                  .id(301)
                  .type("INDIVIDUAL")
                  .title("MR")
                  .firstName("Valid")
                  .surname("Name")
                  .relationshipToCase("OWNER")
                  .relationshipToClient("CLIENT"),
              new OpponentDetail()
                  .id(302)
                  .type("INDIVIDUAL")
                  .title("MR")
                  .firstName("Valid")
                  .surname(overlengthName)
                  .relationshipToCase("OWNER")
                  .relationshipToClient("CLIENT"),
              new OpponentDetail()
                  .id(303)
                  .type("ORGANISATION")
                  .organisationName("Valid Org")
                  .organisationType("COMPANY")
                  .relationshipToCase("OWNER")
                  .relationshipToClient("CLIENT"),
              new OpponentDetail()
                  .id(304)
                  .type("ORGANISATION")
                  .organisationName(overlengthOrganisationName)
                  .organisationType("COMPANY")
                  .relationshipToCase("OWNER")
                  .relationshipToClient("CLIENT")));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/land/7/liable-parties/add")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, activeApplication)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying(
              "parties",
              parties ->
                  assertThat((List<?>) parties)
                      .extracting("name")
                      .containsExactlyInAnyOrder("MR Valid Name", "Valid Org"));
    }

    @Test
    void candidateFilteringValidatesEmploymentAndAssessmentOtherDetailsBeforeAcceptingACandidate() {
      // A selected opponent without a real CAAB id is sent to the backend's create-and-link
      // endpoint as the entire original OpponentDetail, including employment and
      // means-assessment fields that candidate validation previously never looked at. Those
      // fields must satisfy IndividualOpponentOtherDetailsValidator too, just as they would on
      // the dedicated other-details screen, or an opponent that could never actually be created
      // there would still be offered and submitted here regardless.
      final ApplicationDetail activeApplication = new ApplicationDetail();
      activeApplication.setCaseReferenceNumber(ebsCase.getCaseReferenceNumber());
      activeApplication.setId(999);
      lenient()
          .when(applicationService.getApplication("999"))
          .thenReturn(Mono.just(activeApplication));
      activeApplication.setOpponents(
          List.of(
              new OpponentDetail()
                  .id(501)
                  .type("INDIVIDUAL")
                  .title("MR")
                  .firstName("Valid")
                  .surname("Employee")
                  .relationshipToCase("OWNER")
                  .relationshipToClient("CLIENT")
                  .employerName("Acme Ltd"),
              // An employer name over the 35 character limit fails
              // IndividualOpponentOtherDetailsValidator even though the contact details alone are
              // otherwise valid.
              new OpponentDetail()
                  .id(502)
                  .type("INDIVIDUAL")
                  .title("MR")
                  .firstName("Overlength")
                  .surname("Employer")
                  .relationshipToCase("OWNER")
                  .relationshipToClient("CLIENT")
                  .employerName("E".repeat(36)),
              // An assessed income above the permitted 100000000 limit likewise fails the
              // other-details validator.
              new OpponentDetail()
                  .id(503)
                  .type("INDIVIDUAL")
                  .title("MR")
                  .firstName("Excessive")
                  .surname("Income")
                  .relationshipToCase("OWNER")
                  .relationshipToClient("CLIENT")
                  .assessedIncome(new java.math.BigDecimal("100000001"))));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/land/7/liable-parties/add")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, activeApplication)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying(
              "parties",
              parties ->
                  assertThat((List<?>) parties)
                      .extracting("name")
                      .containsExactly("MR Valid Employee"));
    }

    @Test
    void partyListIncludesSharedOrganisationsWithoutAnOrganisationType() {
      ebsCase.setOpponents(
          List.of(
              // Shared organisations are valid without an organisation type - see
              // OrganisationOpponentValidator, which only requires it for non-shared
              // organisations - so this opponent must still be shown and selectable.
              new OpponentDetail()
                  .ebsId("61700003")
                  .type("ORGANISATION")
                  .organisationName("Shared Org Ltd")
                  .sharedInd(true)
                  .relationshipToCase("OWNER")
                  .relationshipToClient("CLIENT")));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/land/7/liable-parties/add")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying(
              "parties",
              parties ->
                  assertThat((List<?>) parties)
                      .extracting("name")
                      .containsExactly("Shared Org Ltd"));
    }

    @Test
    void partyListFallsBackToTheCaseWhenTheSessionApplicationBelongsToAnotherCase() {
      // A stale "current application" left over from a previous, unrelated case must not be used
      // to build the party list - doing so would show (or let selections resolve against) the
      // wrong person's opponents.
      final ApplicationDetail staleApplication = new ApplicationDetail();
      staleApplication.setCaseReferenceNumber("999999999");
      staleApplication.setOpponents(
          List.of(
              new OpponentDetail().id(1).type("INDIVIDUAL").firstName("Other").surname("Person")));

      ebsCase.setOpponents(
          List.of(
              new OpponentDetail()
                  .ebsId("61700004")
                  .type("INDIVIDUAL")
                  .title("Ms")
                  .firstName("Jane")
                  .surname("Smith")
                  .relationshipToCase("CHILD")
                  .relationshipToClient("CLIENT")));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/land/7/liable-parties/add")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, staleApplication)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying(
              "parties",
              parties ->
                  assertThat((List<?>) parties)
                      .extracting("name")
                      .containsExactly("Ms Jane Smith"));
    }

    @Test
    void partyListUsesLookupDescriptionsForNamesAndRelationships() {
      ebsCase.setOpponents(
          List.of(
              new OpponentDetail()
                  .ebsId("61700005")
                  .type("INDIVIDUAL")
                  .title("MISS")
                  .firstName("Jane")
                  .surname("Smith")
                  .relationshipToCase("CHILD")
                  .relationshipToClient("CLIENT"),
              new OpponentDetail()
                  .ebsId("61700006")
                  .type("ORGANISATION")
                  .organisationName("Acme & Co")
                  .organisationType("COMPANY")
                  .relationshipToCase("OPP")
                  .relationshipToClient("CLIENT")));

      when(lookupService.getCommonValues(COMMON_VALUE_CONTACT_TITLE))
          .thenReturn(
              Mono.just(
                  new CommonLookupDetail()
                      .content(
                          List.of(
                              new CommonLookupValueDetail().code("MISS").description("Miss")))));
      when(lookupService.getPersonToCaseRelationships())
          .thenReturn(
              Mono.just(
                  new RelationshipToCaseLookupDetail()
                      .content(
                          List.of(
                              new RelationshipToCaseLookupValueDetail()
                                  .code("CHILD")
                                  .description("Child")))));
      when(lookupService.getOrganisationToCaseRelationships())
          .thenReturn(
              Mono.just(
                  new RelationshipToCaseLookupDetail()
                      .content(
                          List.of(
                              new RelationshipToCaseLookupValueDetail()
                                  .code("OPP")
                                  .description("Opponent")))));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/land/7/liable-parties/add")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying(
              "parties",
              parties ->
                  assertThat((List<?>) parties)
                      .extracting("name", "relationshipToCase")
                      .containsExactlyInAnyOrder(
                          tuple("Miss Jane Smith", "Child"), tuple("Acme & Co", "Opponent")));
    }

    @Test
    void partyListOnlyMarksAwardOpponentsAsEditable() {
      ebsCase.setOpponents(
          List.of(
              new OpponentDetail()
                  .id(10)
                  .ebsId("AWARD-1")
                  .type("INDIVIDUAL")
                  .title("MISS")
                  .firstName("Jane")
                  .surname("Smith")
                  .relationshipToCase("CHILD")
                  .relationshipToClient("CLIENT"),
              new OpponentDetail()
                  .id(11)
                  .ebsId("CASE-2")
                  .type("INDIVIDUAL")
                  .title("MISS")
                  .firstName("Sam")
                  .surname("Jones")
                  .relationshipToCase("CHILD")
                  .relationshipToClient("CLIENT")));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/land/7/liable-parties/add")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying(
              "parties",
              parties ->
                  assertThat((List<?>) parties)
                      .extracting("name", "editable")
                      .containsExactlyInAnyOrder(
                          tuple("MISS Jane Smith", true), tuple("MISS Sam Jones", false)));
    }

    @Test
    void partyListMarksOpponentsAlreadyAddedToTheAwardWithoutHidingTheRow() {
      ebsCase.setOpponents(
          List.of(
              new OpponentDetail()
                  .id(10)
                  .type("INDIVIDUAL")
                  .title("MISS")
                  .firstName("Jane")
                  .surname("Smith")
                  .relationshipToCase("CHILD")
                  .relationshipToClient("CLIENT"),
              new OpponentDetail()
                  .id(11)
                  .type("INDIVIDUAL")
                  .title("MISS")
                  .firstName("Sam")
                  .surname("Jones")
                  .relationshipToCase("CHILD")
                  .relationshipToClient("CLIENT")));
      when(caseOutcomeService.getAwardLiableParties("300000001", 123, "land", 7))
          .thenReturn(List.of(new OpponentDetail().id(10)));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/land/7/liable-parties/add")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying(
              "parties",
              parties ->
                  assertThat((List<?>) parties)
                      .extracting("name", "alreadyAdded")
                      .containsExactlyInAnyOrder(
                          tuple("MISS Jane Smith", true), tuple("MISS Sam Jones", false)))
          // The checkbox for row 10 (Jane Smith) is hidden since she is already added, so the
          // error-summary link must target the next selectable row (Sam Jones, row 11) rather
          // than a row whose checkbox no longer exists.
          .containsEntry("firstSelectablePartyId", 11);
    }

    @Test
    void reselectingAnAlreadyAddedOpponentIsRejected() {
      final ApplicationDetail activeApplication = new ApplicationDetail();
      activeApplication.setCaseReferenceNumber(ebsCase.getCaseReferenceNumber());
      activeApplication.setId(999);
      lenient()
          .when(applicationService.getApplication("999"))
          .thenReturn(Mono.just(activeApplication));
      activeApplication.setOpponents(
          List.of(
              new OpponentDetail()
                  .id(42)
                  .type("INDIVIDUAL")
                  .title("MISS")
                  .firstName("Jane")
                  .surname("Smith")
                  .relationshipToCase("CHILD")
                  .relationshipToClient("CLIENT")));
      when(caseOutcomeService.getAwardLiableParties("300000001", 123, "land", 7))
          .thenReturn(List.of(new OpponentDetail().id(42)));

      final MockHttpSession mockHttpSession = new MockHttpSession();
      final String rowIdentityToken =
          renderPartyListAndCaptureToken(mockHttpSession, activeApplication);

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/land/7/liable-parties/add")
                      .session(mockHttpSession)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(APPLICATION, activeApplication)
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr("rowIdentityToken", rowIdentityToken)
                      .param("selectedPartyIds", "42")))
          .hasStatusOk()
          .hasViewName("application/party-list-screen")
          .model()
          .containsEntry("partyListError", true);

      verify(caseOutcomeService, never())
          .addLiableParties(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void malformedSelectedPartyIdIsRejectedAsUnresolved() {
      ebsCase.setOpponents(
          List.of(
              new OpponentDetail()
                  .id(42)
                  .type("INDIVIDUAL")
                  .firstName("Jane")
                  .surname("Smith")
                  .relationshipToCase("CHILD")
                  .relationshipToClient("CLIENT")));

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/land/7/liable-parties/add")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)
                      .param("selectedPartyIds", "not-a-number")))
          .hasStatusOk()
          .hasViewName("application/party-list-screen")
          .model()
          .containsEntry("partyListUnresolvedError", true);

      verify(caseOutcomeService, never())
          .addLiableParties(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void readOnlyCaseCannotDisplayThePartyList() {
      ebsCase.setAvailableFunctions(List.of());

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/land/7/liable-parties/add")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasRedirectedUrl("/case/outcome-and-awards");

      verifyNoInteractions(applicationService);
      verify(caseOutcomeService, never()).getAwardLiableParties(any(), any(), any(), any());
    }
  }

  @Nested
  @DisplayName("Award deletion")
  class DeleteAwardTests {

    @ParameterizedTest
    @ValueSource(strings = {"COST", "DAMAGE", "LAND", "ASSET"})
    void confirmationPassesOnlyAwardTypeAndIdWithoutDeleting(String type) {
      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/awards/{type}/7/delete", type)
                      .sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/delete-award")
          .model()
          .containsEntry("awardType", type)
          .containsEntry("awardId", 7)
          .doesNotContainKey("award");

      verifyNoInteractions(caseOutcomeService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"COST", "DAMAGE", "LAND", "ASSET"})
    void confirmDeletesSelectedAwardAndRedirects(String type) {
      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/awards/{type}/7/delete", type)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/outcome-and-awards");

      verify(caseOutcomeService)
          .deleteAward(
              ebsCase.getCaseReferenceNumber(),
              user.getProvider().getId().intValue(),
              type,
              7,
              user.getLoginId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST"})
    void readOnlyCaseCannotLoadOrConfirmDeletion(String method) {
      ebsCase.setAvailableFunctions(List.of(FunctionConstants.VIEW_CASE_OUTCOME));
      MockHttpServletRequestBuilder request =
          "GET".equals(method)
              ? get("/case/outcome-and-awards/awards/COST/7/delete")
              : post("/case/outcome-and-awards/awards/COST/7/delete");

      assertThat(
              mockMvc.perform(request.sessionAttr(CASE, ebsCase).sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/outcome-and-awards");
      verifyNoInteractions(caseOutcomeService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST"})
    void missingCaseCannotLoadOrConfirmDeletion(String method) {
      MockHttpServletRequestBuilder request =
          "GET".equals(method)
              ? get("/case/outcome-and-awards/awards/COST/7/delete")
              : post("/case/outcome-and-awards/awards/COST/7/delete");

      assertThat(mockMvc.perform(request.sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/outcome-and-awards");
      verifyNoInteractions(caseOutcomeService);
    }

    @Test
    void deletionFailureDoesNotRedirectAsSuccess() {
      doThrow(new IllegalStateException("Deletion failed"))
          .when(caseOutcomeService)
          .deleteAward(any(), any(), any(), any(), any());

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/awards/COST/7/delete")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasFailed()
          .failure()
          .hasCauseInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  @DisplayName("GET: /case/outcome-and-awards/award-type")
  class SelectAwardTypeTests {

    @Test
    @DisplayName("Should display award types returned by the lookup service to the model")
    void shouldDisplayAwardTypesReturnedByLookup() {
      final AwardTypeLookupValueDetail costAward =
          new AwardTypeLookupValueDetail()
              .code("COST_AWARD")
              .description("Cost award")
              .awardType("COST")
              .enabled(true);

      final AwardTypeLookupDetail awardTypes =
          new AwardTypeLookupDetail().addContentItem(costAward);

      when(lookupService.getAwardTypes()).thenReturn(Mono.just(awardTypes));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/award-type").sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/select-award-type")
          .model()
          .containsEntry("awardTypes", awardTypes.getContent())
          .hasEntrySatisfying(
              AWARD_TYPE_FORM, value -> assertThat(value).isInstanceOf(AwardTypeForm.class));

      verify(lookupService).getAwardTypes();
    }

    @Test
    @DisplayName("Should display an empty award type list when the lookup returns no data")
    void shouldDisplayEmptyAwardTypesWhenLookupReturnsNoData() {
      when(lookupService.getAwardTypes()).thenReturn(Mono.empty());

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/award-type").sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/select-award-type")
          .model()
          .containsEntry("awardTypes", Collections.emptyList())
          .hasEntrySatisfying(
              AWARD_TYPE_FORM, value -> assertThat(value).isInstanceOf(AwardTypeForm.class));

      verify(lookupService).getAwardTypes();
    }

    @Test
    @DisplayName("Should redirect read-only cases away from award type management")
    void shouldRedirectReadOnlyCaseFromAwardTypeManagement() {
      ebsCase.setAvailableFunctions(List.of(FunctionConstants.VIEW_CASE_OUTCOME));

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/award-type")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/outcome-and-awards");
    }

    @Test
    @DisplayName("Should redirect award type management when no case is in session")
    void shouldRedirectAwardTypeManagementWithoutCase() {
      assertThat(mockMvc.perform(get("/case/outcome-and-awards/award-type")))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/outcome-and-awards");
      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/award-type")
                      .sessionAttr(AWARD_TYPE_FORM, new AwardTypeForm())
                      .param("awardTypeCode", "COST")))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/outcome-and-awards");

      verifyNoInteractions(lookupService, awardTypeValidator);
    }
  }

  @Nested
  @DisplayName("POST: /case/outcome-and-awards/award-type")
  class SubmitAwardTypeTests {

    @ParameterizedTest(name = "{0} resolves to {2} and redirects to {3}")
    @CsvSource({
      "FIN_ASSET, Financial Asset, ASSET, /case/outcome-and-awards/asset",
      "OTH_ASSET, Other Asset, ASSET, /case/outcome-and-awards/asset",
      "ENFORCEMENT, Enforcement Cost Award, COST, /case/outcome-and-awards/cost-award",
      "COST, Cost Award, COST, /case/outcome-and-awards/cost-award",
      "COST_AGR, Cost Settlement, COST, /case/outcome-and-awards/cost-award",
      "DAMAGE, Financial or Punitive Damages, DAMAGE, /case/outcome-and-awards/financial-award",
      "DAMAGE_AGR, Financial Settlement, DAMAGE, /case/outcome-and-awards/financial-award",
      "LAND, Land/Property, LAND, /case/outcome-and-awards/land-property"
    })
    void shouldRedirectAccordingToAwardType(
        final String code,
        final String description,
        final String awardType,
        final String expectedRedirect) {

      final AwardTypeLookupValueDetail lookupValue =
          new AwardTypeLookupValueDetail()
              .code(code)
              .description(description)
              .awardType(awardType)
              .enabled(true);

      final AwardTypeLookupDetail lookupDetail =
          new AwardTypeLookupDetail().addContentItem(lookupValue);

      when(lookupService.getAwardTypes()).thenReturn(Mono.just(lookupDetail));

      final AwardTypeForm awardTypeForm = new AwardTypeForm();

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/award-type")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(AWARD_TYPE_FORM, awardTypeForm)
                      .param("awardTypeCode", code)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl(expectedRedirect);

      assertThat(awardTypeForm.getAwardTypeCode()).isEqualTo(code);
      assertThat(awardTypeForm.getDescription())
          .isEqualTo(StringUtils.capitalize(awardType.toLowerCase()));
      assertThat(awardTypeForm.getAwardType()).isEqualTo(awardType);

      verify(awardTypeValidator).validate(any(AwardTypeForm.class), any());
      verify(lookupService).getAwardTypes();
    }

    @Test
    @DisplayName("Should return select award type view when validation fails")
    void shouldReturnSelectAwardTypeViewWhenValidationFails() {
      final AwardTypeLookupValueDetail lookupValue =
          new AwardTypeLookupValueDetail()
              .code("FIN_ASSET")
              .description("Financial Asset")
              .awardType("ASSET")
              .enabled(true);

      final AwardTypeLookupDetail lookupDetail =
          new AwardTypeLookupDetail().addContentItem(lookupValue);

      when(lookupService.getAwardTypes()).thenReturn(Mono.just(lookupDetail));

      doAnswer(
              invocation -> {
                final Errors errors = invocation.getArgument(1);
                errors.rejectValue(
                    "awardTypeCode", "required.awardTypeCode", "Please complete 'Award type'.");
                return null;
              })
          .when(awardTypeValidator)
          .validate(any(AwardTypeForm.class), any());

      final AwardTypeForm awardTypeForm = new AwardTypeForm();

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/award-type")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(AWARD_TYPE_FORM, awardTypeForm)
                      .param("awardTypeCode", "")))
          .hasStatusOk()
          .hasViewName("application/select-award-type")
          .model()
          .containsEntry("awardTypes", lookupDetail.getContent())
          .hasEntrySatisfying(
              BindingResult.MODEL_KEY_PREFIX + AWARD_TYPE_FORM,
              value -> {
                final BindingResult result = (BindingResult) value;

                assertThat(result.hasFieldErrors("awardTypeCode")).isTrue();
                assertThat(result.getFieldError("awardTypeCode")).isNotNull();
                assertThat(result.getFieldError("awardTypeCode").getCode())
                    .isEqualTo("required.awardTypeCode");
              });

      verify(awardTypeValidator).validate(any(AwardTypeForm.class), any());
      verify(lookupService).getAwardTypes();
    }

    @Test
    @DisplayName("Should reject an award type code that is not present in the lookup")
    void shouldRejectUnknownAwardTypeCode() {
      final AwardTypeLookupValueDetail lookupValue =
          new AwardTypeLookupValueDetail()
              .code("FIN_ASSET")
              .description("Financial Asset")
              .awardType("ASSET")
              .enabled(true);

      final AwardTypeLookupDetail lookupDetail =
          new AwardTypeLookupDetail().addContentItem(lookupValue);

      when(lookupService.getAwardTypes()).thenReturn(Mono.just(lookupDetail));

      final AwardTypeForm awardTypeForm = new AwardTypeForm();

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/award-type")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(AWARD_TYPE_FORM, awardTypeForm)
                      .param("awardTypeCode", "UNKNOWN")))
          .hasStatusOk()
          .hasViewName("application/select-award-type")
          .model()
          .containsEntry("awardTypes", lookupDetail.getContent())
          .hasEntrySatisfying(
              BindingResult.MODEL_KEY_PREFIX + AWARD_TYPE_FORM,
              value -> {
                final BindingResult result = (BindingResult) value;

                assertThat(result.hasFieldErrors("awardTypeCode")).isTrue();
                assertThat(result.getFieldError("awardTypeCode")).isNotNull();
                assertThat(result.getFieldError("awardTypeCode").getCode())
                    .isEqualTo("awardType.invalid");
                assertThat(result.getFieldError("awardTypeCode").getDefaultMessage())
                    .isEqualTo("Please select a valid award type.");
              });

      assertThat(awardTypeForm.getAwardTypeCode()).isEqualTo("UNKNOWN");

      verify(awardTypeValidator).validate(any(AwardTypeForm.class), any());
      verify(lookupService).getAwardTypes();
    }

    @Test
    @DisplayName("Should reject an unsupported award type category")
    void shouldRejectUnsupportedAwardTypeCategory() {
      final AwardTypeLookupValueDetail lookupValue =
          new AwardTypeLookupValueDetail()
              .code("NEW_AWARD")
              .description("New award")
              .awardType("UNKNOWN")
              .enabled(true);

      final AwardTypeLookupDetail lookupDetail =
          new AwardTypeLookupDetail().addContentItem(lookupValue);

      when(lookupService.getAwardTypes()).thenReturn(Mono.just(lookupDetail));

      final AwardTypeForm awardTypeForm = new AwardTypeForm();

      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/award-type")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(AWARD_TYPE_FORM, awardTypeForm)
                      .param("awardTypeCode", "NEW_AWARD")))
          .hasStatusOk()
          .hasViewName("application/select-award-type")
          .model()
          .containsEntry("awardTypes", lookupDetail.getContent())
          .hasEntrySatisfying(
              BindingResult.MODEL_KEY_PREFIX + AWARD_TYPE_FORM,
              value -> {
                final BindingResult result = (BindingResult) value;

                assertThat(result.hasFieldErrors("awardTypeCode")).isTrue();
                assertThat(result.getFieldError("awardTypeCode")).isNotNull();
                assertThat(result.getFieldError("awardTypeCode").getCode())
                    .isEqualTo("awardType.unsupported");
                assertThat(result.getFieldError("awardTypeCode").getDefaultMessage())
                    .isEqualTo("The selected award type is not supported.");
              });

      assertThat(awardTypeForm.getAwardTypeCode()).isEqualTo("NEW_AWARD");
      assertThat(awardTypeForm.getDescription()).isEqualTo("Unknown");
      assertThat(awardTypeForm.getAwardType()).isEqualTo("UNKNOWN");

      verify(awardTypeValidator).validate(any(AwardTypeForm.class), any());
      verify(lookupService).getAwardTypes();
    }
  }

  @Nested
  @DisplayName("GET: /case/outcome-and-awards/cost-award")
  class CostAwardGetTests {

    @Test
    void getNewCostAwardPreservesSelectedAwardMetadataAndInitialisesTotals() {
      final AwardTypeForm awardTypeForm = new AwardTypeForm();
      awardTypeForm.setAwardTypeCode("COST_AGR");
      awardTypeForm.setAwardType("COST");
      awardTypeForm.setDescription("Cost");

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/cost-award")
                      .sessionAttr(AWARD_TYPE_FORM, awardTypeForm)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasViewName("application/cost-award")
          .model()
          .containsEntry("totalPreCertificateCosts", "0.00")
          .containsEntry("totalCertificateCostsAwarded", "0.00")
          .containsEntry("totalCostsAwarded", "0.00")
          .hasEntrySatisfying(
              "costAward",
              value ->
                  assertThat(value)
                      .extracting(
                          "awardCode",
                          "awardType",
                          "description",
                          "laaFundedLegalCosts",
                          "otherPreCertificateCosts",
                          "laaRate",
                          "marketRate",
                          "interestRate")
                      .containsExactly(
                          "COST_AGR", "COST", "Cost", "0.00", "0.00", "0.00", "0.00", "0.00"));

      verify(lookupService).getCommonValues(COMMON_VALUE_AWARD_ASSESSMENT_STATUS);
    }

    @Test
    void getNewCostAwardWithNonCostSessionAwardTypeReturnsToOverview() {
      final AwardTypeForm awardTypeForm = new AwardTypeForm();
      awardTypeForm.setAwardTypeCode("DAMAGE_AGR");
      awardTypeForm.setAwardType("DAMAGE");
      awardTypeForm.setDescription("Financial Settlement");

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/cost-award")
                      .sessionAttr(AWARD_TYPE_FORM, awardTypeForm)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasRedirectedUrl("/case/outcome-and-awards");
    }

    @Test
    void getNewCostAwardWithoutSelectedAwardMetadataReturnsToOverview() {
      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/cost-award")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasRedirectedUrl("/case/outcome-and-awards");
    }

    @Test
    void getExistingCostAwardLoadsPersistedValues() {
      final CostAwardDetail award =
          new CostAwardDetail()
              .id(7)
              .awardCode("COST")
              .awardType("COST")
              .description("Cost")
              .dateOfOrder(new Date())
              .awardAmount(new java.math.BigDecimal("123.45"))
              .awardedBy("COURT");
      when(caseOutcomeService.getCostAward("300000001", 123, 7)).thenReturn(Optional.of(award));
      final CostAwardFormData formData = new CostAwardFormData();
      formData.setId(7);
      formData.setLaaFundedLegalCosts("123.45");
      when(costAwardMapper.toCostAwardFormData(award)).thenReturn(formData);

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/cost-award/7")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasViewName("application/cost-award")
          .model()
          .hasEntrySatisfying(
              "costAward",
              value ->
                  assertThat(value)
                      .extracting("id", "laaFundedLegalCosts", "otherPreCertificateCosts")
                      .containsExactly(7, "123.45", "0.00"));
    }
  }

  @Nested
  @DisplayName("POST: /case/outcome-and-awards/cost-award")
  class CostAwardPostTests {

    @Test
    void postWithoutIdCreatesCostAward() {
      when(caseOutcomeService.createCostAward(any(), any(), any(), any(), any())).thenReturn(19);
      assertThat(mockMvc.perform(validCostAwardPost()))
          .hasRedirectedUrl("/case/outcome-and-awards/cost-award/19/liable-parties");

      verify(caseOutcomeService)
          .createCostAward(
              eq("300000001"),
              eq(user.getProvider().getId().intValue()),
              any(CostAwardDetail.class),
              eq(user.getLoginId()),
              eq(caseOutcome));
    }

    @Test
    void postWithoutPermissionRedirectsForReadOnlyCase() {
      ebsCase.setAvailableFunctions(List.of(FunctionConstants.VIEW_CASE_OUTCOME));

      assertThat(mockMvc.perform(validCostAwardPost()))
          .hasRedirectedUrl("/case/outcome-and-awards");

      verify(caseOutcomeService, never()).createCostAward(any(), any(), any(), any(), any());
    }

    @Test
    void postWithIdUpdatesCostAward() {
      assertThat(mockMvc.perform(validCostAwardPost().param("id", "7")))
          .hasRedirectedUrl("/case/outcome-and-awards/cost-award/7/liable-parties");

      verify(caseOutcomeService)
          .updateCostAward(
              eq("300000001"),
              eq(user.getProvider().getId().intValue()),
              eq(7),
              any(CostAwardFormData.class),
              eq(user.getLoginId()));
    }

    @Test
    void postWithCalculateRedisplaysCostAwardPageWithoutSaving() {
      assertThat(mockMvc.perform(validCostAwardPost().param("action", "calculate")))
          .hasViewName("application/cost-award")
          .model()
          .containsEntry("totalPreCertificateCosts", "3.00")
          .containsEntry("totalCertificateCostsAwarded", "7.00")
          .containsEntry("totalCostsAwarded", "10.00");

      verify(caseOutcomeService, never()).createCostAward(any(), any(), any(), any(), any());
      verify(caseOutcomeService, never()).updateCostAward(any(), any(), any(), any(), any());
    }

    @Test
    void postWithCalculateOnlyValidatesCostAmounts() {
      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/cost-award")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)
                      .param("action", "calculate")
                      .param("laaFundedLegalCosts", "1.50")
                      .param("marketRate", "2.25")))
          .hasViewName("application/cost-award")
          .model()
          .doesNotHaveErrors()
          .containsEntry("totalPreCertificateCosts", "1.50")
          .containsEntry("totalCertificateCostsAwarded", "2.25")
          .containsEntry("totalCostsAwarded", "3.75");

      verify(costAwardValidator).validateCostAmounts(any(CostAwardFormData.class), any());
      verify(costAwardValidator, never()).validate(any(), any());
    }

    @Test
    void postWithCalculateTreatsInvalidAmountsAsZero() {
      assertThat(
              mockMvc.perform(
                  post("/case/outcome-and-awards/cost-award")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)
                      .param("action", "calculate")
                      .param("laaFundedLegalCosts", "1.234")
                      .param("otherPreCertificateCosts", "2.00")
                      .param("laaRate", "abc")
                      .param("marketRate", "3.50")))
          .hasViewName("application/cost-award")
          .model()
          .containsEntry("totalPreCertificateCosts", "2.00")
          .containsEntry("totalCertificateCostsAwarded", "3.50")
          .containsEntry("totalCostsAwarded", "5.50");
    }

    @Test
    void invalidPostRedisplaysCostAwardPage() {
      doAnswer(
              invocation -> {
                invocation
                    .getArgument(1, Errors.class)
                    .rejectValue("dateOfOrder", "invalid.format", "Invalid date");
                return null;
              })
          .when(costAwardValidator)
          .validate(any(), any());

      assertThat(mockMvc.perform(validCostAwardPost()))
          .hasViewName("application/cost-award")
          .model()
          .hasErrors();
    }

    @Test
    void apiFailureRedisplaysCostAwardPageWithError() {
      doThrow(new CaabApiClientException("API failure"))
          .when(caseOutcomeService)
          .createCostAward(any(), any(), any(), any(), any());

      assertThat(mockMvc.perform(validCostAwardPost()))
          .hasViewName("application/cost-award")
          .model()
          .hasErrors();
    }

    @Test
    void postWithoutIdRejectsAwardTypeDetailsThatDoNotMatchTheSession() {
      final AwardTypeForm selectedAwardType = new AwardTypeForm();
      selectedAwardType.setAwardTypeCode("COST_AGR");
      selectedAwardType.setAwardType("COST");
      selectedAwardType.setDescription("Different");

      assertThat(
              mockMvc.perform(validCostAwardPost().sessionAttr(AWARD_TYPE_FORM, selectedAwardType)))
          .hasViewName("application/cost-award")
          .model()
          .hasErrors();
    }

    @Test
    void postWithoutIdRejectsNonCostSessionAwardType() {
      final AwardTypeForm selectedAwardType = new AwardTypeForm();
      selectedAwardType.setAwardTypeCode("DAMAGE_AGR");
      selectedAwardType.setAwardType("DAMAGE");
      selectedAwardType.setDescription("Financial Settlement");

      assertThat(
              mockMvc.perform(validCostAwardPost().sessionAttr(AWARD_TYPE_FORM, selectedAwardType)))
          .hasViewName("application/cost-award")
          .model()
          .hasErrors();
    }
  }

  @Nested
  @DisplayName("GET: /case/outcome-and-awards/financial-award")
  class FinancialAwardGetTests {

    @Test
    void getNewFinancialAwardPreservesSelectedAwardMetadata() {
      final AwardTypeForm awardTypeForm = new AwardTypeForm();
      awardTypeForm.setAwardTypeCode("DAMAGE_AGR");
      awardTypeForm.setAwardType("DAMAGE");
      awardTypeForm.setDescription("Financial Settlement");

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/financial-award")
                      .sessionAttr(AWARD_TYPE_FORM, awardTypeForm)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasViewName("application/financial-award")
          .model()
          .hasEntrySatisfying(
              "financialAward",
              value ->
                  assertThat(value)
                      .extracting("awardCode", "awardType", "description")
                      .containsExactly("DAMAGE_AGR", "DAMAGE", "Financial Settlement"));
    }

    @Test
    void getNewFinancialAwardWithoutSelectedAwardMetadataReturnsToOverview() {
      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/financial-award")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasRedirectedUrl("/case/outcome-and-awards");
    }

    @Test
    void getExistingFinancialAwardLoadsPersistedValues() {
      final FinancialAwardDetail award =
          new FinancialAwardDetail()
              .id(7)
              .awardCode("DAMAGE")
              .awardType("DAMAGE")
              .description("Damage")
              .dateOfOrder(new Date())
              .awardAmount(new java.math.BigDecimal("123.45"))
              .interimAward("0")
              .awardedBy("COURT");
      when(caseOutcomeService.getFinancialAward("300000001", 123, 7))
          .thenReturn(Optional.of(award));
      final FinancialAwardFormData formData = new FinancialAwardFormData();
      formData.setId(7);
      formData.setAwardAmount("123.45");
      when(financialAwardMapper.toFinancialAwardFormData(award)).thenReturn(formData);

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/financial-award/7")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasViewName("application/financial-award")
          .model()
          .hasEntrySatisfying(
              "financialAward",
              value ->
                  assertThat(value).extracting("id", "awardAmount").containsExactly(7, "123.45"));
    }
  }

  @Nested
  @DisplayName("POST: /case/outcome-and-awards/financial-award")
  class FinancialAwardPostTests {

    @Test
    void postWithoutIdCreatesFinancialAward() {
      when(caseOutcomeService.createFinancialAward(any(), any(), any(), any(), any()))
          .thenReturn(7);
      assertThat(mockMvc.perform(validPost()))
          .hasRedirectedUrl("/case/outcome-and-awards/financial-award/7/liable-parties");

      verify(caseOutcomeService)
          .createFinancialAward(
              eq("300000001"),
              eq(user.getProvider().getId().intValue()),
              any(FinancialAwardRequest.class),
              eq(user.getLoginId()),
              eq(caseOutcome));
    }

    @Nested
    @DisplayName("GET: /case/outcome-and-awards/land-property")
    class LandAwardGetTests {

      @Test
      void getNewLandAwardPreservesSelectedAwardMetadata() {
        final AwardTypeForm awardTypeForm = landAwardType();

        assertThat(
                mockMvc.perform(
                    get("/case/outcome-and-awards/land-property")
                        .sessionAttr(AWARD_TYPE_FORM, awardTypeForm)
                        .sessionAttr(CASE, ebsCase)
                        .sessionAttr(USER_DETAILS, user)))
            .hasViewName("application/land-property")
            .model()
            .hasEntrySatisfying(
                "landAward",
                value ->
                    assertThat(value)
                        .extracting("awardCode", "awardType", "description")
                        .containsExactly("LAND", "LAND", null));
      }

      @Test
      void getNewLandAwardWithoutSelectedAwardMetadataReturnsToOverview() {
        assertThat(
                mockMvc.perform(
                    get("/case/outcome-and-awards/land-property")
                        .sessionAttr(CASE, ebsCase)
                        .sessionAttr(USER_DETAILS, user)))
            .hasRedirectedUrl("/case/outcome-and-awards");
      }

      @Test
      void getExistingLandAwardLoadsPersistedValues() {
        final LandAwardDetail award = new LandAwardDetail().id(7);
        final LandAwardFormData form = new LandAwardFormData();
        form.setId(7);
        form.setEquity("150000.00");
        when(caseOutcomeService.getLandAward("300000001", 123, 7)).thenReturn(Optional.of(award));
        when(landAwardMapper.toLandAwardFormData(award)).thenReturn(form);

        assertThat(
                mockMvc.perform(
                    get("/case/outcome-and-awards/land-property/7")
                        .sessionAttr(CASE, ebsCase)
                        .sessionAttr(USER_DETAILS, user)))
            .hasViewName("application/land-property")
            .model()
            .hasEntrySatisfying(
                "landAward",
                value ->
                    assertThat(value).extracting("id", "equity").containsExactly(7, "150000.00"));
      }
    }

    @Nested
    @DisplayName("POST: /case/outcome-and-awards/land-property")
    class LandAwardPostTests {

      @Test
      void postWithoutIdCalculatesEquityAndCreatesLandAwardThenOpensTimeRecovery() {
        when(caseOutcomeService.createLandAward(any(), any(), any(), any())).thenReturn(7);
        assertThat(mockMvc.perform(validLandPost()))
            .hasRedirectedUrl("/case/outcome-and-awards/land/7/time-related-recovery");

        verify(landAwardMapper)
            .toLandAwardRequest(
                org.mockito.ArgumentMatchers.argThat(form -> "150000.00".equals(form.getEquity())));
        verify(caseOutcomeService)
            .createLandAward(
                eq("300000001"),
                eq(user.getProvider().getId().intValue()),
                any(LandAwardRequest.class),
                eq(user.getLoginId()));
      }

      @Test
      void calculateRedisplaysCalculatedEquityWithoutSaving() {
        assertThat(mockMvc.perform(validLandPost().param("action", "calculate")))
            .hasViewName("application/land-property")
            .model()
            .hasEntrySatisfying(
                "landAward",
                value -> assertThat(value).extracting("equity").isEqualTo("150000.00"));

        verify(caseOutcomeService, org.mockito.Mockito.never())
            .createLandAward(any(), any(), any(), any());
      }

      @Test
      void postWithIdUpdatesLandAward() {
        assertThat(mockMvc.perform(validLandPost().param("id", "7")))
            .hasRedirectedUrl("/case/outcome-and-awards/land/7/time-related-recovery");

        verify(caseOutcomeService)
            .updateLandAward(
                eq("300000001"),
                eq(user.getProvider().getId().intValue()),
                eq(7),
                any(LandAwardRequest.class),
                eq(user.getLoginId()));
      }

      @Test
      void postWithoutTimeRecoveryOpensInterestedParties() {
        when(caseOutcomeService.createLandAward(any(), any(), any(), any())).thenReturn(7);
        assertThat(mockMvc.perform(validLandPost().param("recoveryOfAwardTimeRelated", "N")))
            .hasRedirectedUrl("/case/outcome-and-awards/land/7/liable-parties");
      }

      @Test
      void switchingExistingLandAwardToNoSkipsRecoveryPage() {
        assertThat(
                mockMvc.perform(
                    validLandPost().param("id", "7").param("recoveryOfAwardTimeRelated", "N")))
            .hasRedirectedUrl("/case/outcome-and-awards/land/7/liable-parties");
        verify(caseOutcomeService)
            .updateLandAward(
                eq("300000001"),
                eq(user.getProvider().getId().intValue()),
                eq(7),
                any(LandAwardRequest.class),
                eq(user.getLoginId()));
      }

      @Test
      void invalidPostRedisplaysLandAwardPage() {
        doAnswer(
                invocation -> {
                  invocation
                      .getArgument(1, Errors.class)
                      .rejectValue("valuationAmount", "invalid.currency", "Invalid amount");
                  return null;
                })
            .when(landAwardValidator)
            .validate(any(), any());

        assertThat(mockMvc.perform(validLandPost()))
            .hasViewName("application/land-property")
            .model()
            .hasErrors();
      }

      @Test
      void postWithoutIdRejectsAwardTypeDetailsThatDoNotMatchTheSession() {
        final AwardTypeForm selectedAwardType = landAwardType();
        selectedAwardType.setAwardTypeCode("OTHER");

        assertThat(mockMvc.perform(validLandPost().sessionAttr(AWARD_TYPE_FORM, selectedAwardType)))
            .hasViewName("application/land-property")
            .model()
            .hasErrors();
      }
    }

    @Test
    void postWithIdUpdatesFinancialAward() {
      assertThat(mockMvc.perform(validPost().param("id", "7")))
          .hasRedirectedUrl("/case/outcome-and-awards/financial-award/7/liable-parties");

      verify(caseOutcomeService)
          .updateFinancialAward(
              eq("300000001"),
              eq(user.getProvider().getId().intValue()),
              eq(7),
              any(FinancialAwardRequest.class),
              eq(user.getLoginId()));
    }

    @Test
    void invalidPostRedisplaysFinancialAwardPage() {
      doAnswer(
              invocation -> {
                invocation
                    .getArgument(1, Errors.class)
                    .rejectValue("awardAmount", "invalid.currency", "Invalid amount");
                return null;
              })
          .when(financialAwardValidator)
          .validate(any(), any());

      assertThat(mockMvc.perform(validPost()))
          .hasViewName("application/financial-award")
          .model()
          .hasErrors();
    }

    @Test
    void missingCaseOutcomeRedisplaysFinancialAwardPageWithError() {
      doThrow(new IllegalStateException("No case outcome"))
          .when(caseOutcomeService)
          .createFinancialAward(any(), any(), any(), any(), any());

      assertThat(mockMvc.perform(validPost()))
          .hasViewName("application/financial-award")
          .model()
          .hasErrors();
    }

    @Test
    void apiFailureRedisplaysFinancialAwardPageWithError() {
      doThrow(new CaabApiClientException("API failure"))
          .when(caseOutcomeService)
          .createFinancialAward(any(), any(), any(), any(), any());

      assertThat(mockMvc.perform(validPost()))
          .hasViewName("application/financial-award")
          .model()
          .hasErrors();
    }

    @Test
    void postWithoutIdRejectsAwardTypeDetailsThatDoNotMatchTheSession() {
      final AwardTypeForm selectedAwardType = new AwardTypeForm();
      selectedAwardType.setAwardTypeCode("DAMAGE");
      selectedAwardType.setAwardType("DAMAGE");
      selectedAwardType.setDescription("Financial or Punitive Damages");

      assertThat(mockMvc.perform(validPost().sessionAttr(AWARD_TYPE_FORM, selectedAwardType)))
          .hasViewName("application/financial-award")
          .model()
          .hasErrors();
    }
  }

  @Nested
  @DisplayName("GET: /case/outcome-and-awards/asset")
  class OtherAssetAwardGetTests {

    @Test
    void getNewOtherAssetAwardPreservesSelectedAwardMetadata() {
      final AwardTypeForm awardTypeForm = otherAssetAwardType();

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/asset")
                      .sessionAttr(AWARD_TYPE_FORM, awardTypeForm)
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasViewName("application/other-asset-award")
          .model()
          .containsKey("valuationBasisOptions")
          .hasEntrySatisfying(
              "otherAssetAward",
              value ->
                  assertThat(value)
                      .extracting("awardCode", "awardType", "description")
                      .containsExactly("OTH_ASSET", "ASSET", null));

      verify(lookupService).getCommonValues(COMMON_VALUE_VALUATION_BASIS);
    }

    @Test
    void getNewOtherAssetAwardWithoutSelectedAwardMetadataReturnsToOverview() {
      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/asset")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasRedirectedUrl("/case/outcome-and-awards");
    }

    @Test
    void getExistingOtherAssetAwardLoadsPersistedValues() {
      final OtherAssetAwardDetail award =
          new OtherAssetAwardDetail()
              .id(8)
              .awardCode("OTH_ASSET")
              .awardType("ASSET")
              .description("Asset")
              .valuationAmount(new java.math.BigDecimal("123.45"));
      when(caseOutcomeService.getOtherAssetAward("300000001", 123, 8))
          .thenReturn(Optional.of(award));
      final OtherAssetAwardFormData formData = new OtherAssetAwardFormData();
      formData.setId(8);
      formData.setValuationAmount("123.45");
      when(otherAssetAwardMapper.toOtherAssetAwardFormData(award)).thenReturn(formData);

      assertThat(
              mockMvc.perform(
                  get("/case/outcome-and-awards/asset/8")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasViewName("application/other-asset-award")
          .model()
          .containsKey("valuationBasisOptions")
          .hasEntrySatisfying(
              "otherAssetAward",
              value ->
                  assertThat(value)
                      .extracting("id", "valuationAmount")
                      .containsExactly(8, "123.45"));
    }
  }

  @Nested
  @DisplayName("POST: /case/outcome-and-awards/asset")
  class OtherAssetAwardPostTests {

    @Test
    void newTimeRelatedAssetOpensRecoveryUsingCreatedId() {
      when(caseOutcomeService.createOtherAssetAward(any(), any(), any(), any())).thenReturn(8);

      assertThat(mockMvc.perform(validOtherAssetPost(true)))
          .hasRedirectedUrl("/case/outcome-and-awards/asset/8/time-related-recovery");
    }

    @Test
    void existingTimeRelatedAssetOpensRecoveryUsingExistingId() {
      assertThat(mockMvc.perform(validOtherAssetPost(true).param("id", "8")))
          .hasRedirectedUrl("/case/outcome-and-awards/asset/8/time-related-recovery");
      verify(caseOutcomeService)
          .updateOtherAssetAward(
              eq("300000001"),
              eq(123),
              eq(8),
              any(OtherAssetAwardRequest.class),
              eq(user.getLoginId()));
    }

    @Test
    void postWithoutIdCreatesOtherAssetAward() {
      when(caseOutcomeService.createOtherAssetAward(any(), any(), any(), any())).thenReturn(8);
      assertThat(mockMvc.perform(validOtherAssetPost()))
          .hasRedirectedUrl("/case/outcome-and-awards/asset/8/liable-parties");

      verify(caseOutcomeService)
          .createOtherAssetAward(
              eq("300000001"),
              eq(user.getProvider().getId().intValue()),
              any(OtherAssetAwardRequest.class),
              eq(user.getLoginId()));
    }

    @Test
    void postWithIdUpdatesOtherAssetAward() {
      assertThat(mockMvc.perform(validOtherAssetPost().param("id", "8")))
          .hasRedirectedUrl("/case/outcome-and-awards/asset/8/liable-parties");

      verify(caseOutcomeService)
          .updateOtherAssetAward(
              eq("300000001"),
              eq(user.getProvider().getId().intValue()),
              eq(8),
              any(OtherAssetAwardRequest.class),
              eq(user.getLoginId()));
    }

    @Test
    void invalidPostRedisplaysOtherAssetAwardPage() {
      doAnswer(
              invocation -> {
                invocation
                    .getArgument(1, Errors.class)
                    .rejectValue("valuationAmount", "invalid.currency", "Invalid amount");
                return null;
              })
          .when(otherAssetAwardValidator)
          .validate(any(), any());

      assertThat(mockMvc.perform(validOtherAssetPost()))
          .hasViewName("application/other-asset-award")
          .model()
          .hasErrors();
    }

    @Test
    void missingRequiredFieldsRedisplayPageAndDoNotCallApi() {
      doAnswer(
              invocation -> {
                new OtherAssetAwardValidator()
                    .validate(invocation.getArgument(0), invocation.getArgument(1));
                return null;
              })
          .when(otherAssetAwardValidator)
          .validate(any(), any());

      final var result =
          mockMvc.perform(
              post("/case/outcome-and-awards/asset")
                  .param("awardCode", "OTH_ASSET")
                  .param("awardType", "ASSET")
                  .param("description", "Entered description is retained")
                  .sessionAttr(CASE, ebsCase)
                  .sessionAttr(USER_DETAILS, user)
                  .sessionAttr(AWARD_TYPE_FORM, otherAssetAwardType()));

      assertThat(result)
          .hasViewName("application/other-asset-award")
          .model()
          .hasErrors()
          .containsKeys(
              "otherAssetAward", "valuationBasisOptions", "awardedByOptions", "recoveryOptions")
          .hasEntrySatisfying(
              BindingResult.MODEL_KEY_PREFIX + "otherAssetAward",
              value ->
                  assertThat(((BindingResult) value).getFieldErrors())
                      .extracting("field")
                      .contains(
                          "dateOfOrder",
                          "awardedBy",
                          "valuationAmount",
                          "valuationCriteria",
                          "valuationDate",
                          "recoveryOfAwardTimeRelated"))
          .hasEntrySatisfying(
              "otherAssetAward",
              value ->
                  assertThat(value)
                      .extracting("description")
                      .isEqualTo("Entered description is retained"));
      verifyNoInteractions(otherAssetAwardMapper, caseOutcomeService);
    }

    @Test
    void apiFailureRedisplaysOtherAssetAwardPageWithError() {
      doThrow(new CaabApiClientException("API failure"))
          .when(caseOutcomeService)
          .createOtherAssetAward(any(), any(), any(), any());

      assertThat(mockMvc.perform(validOtherAssetPost()))
          .hasViewName("application/other-asset-award")
          .model()
          .hasErrors();
    }

    @Test
    void postWithoutIdRejectsAwardTypeDetailsThatDoNotMatchTheSession() {
      final AwardTypeForm selectedAwardType = otherAssetAwardType();
      selectedAwardType.setAwardTypeCode("FIN_ASSET");

      assertThat(
              mockMvc.perform(
                  validOtherAssetPost().sessionAttr(AWARD_TYPE_FORM, selectedAwardType)))
          .hasViewName("application/other-asset-award")
          .model()
          .hasErrors();
    }
  }

  @Nested
  @DisplayName("Time-related recovery for land and asset awards")
  class TimeRecoveryTests {
    @ParameterizedTest
    @ValueSource(strings = {"land", "asset"})
    void readOnlyCaseCannotDisplayOrSaveRecovery(String awardPath) {
      ebsCase.setAvailableFunctions(List.of(FunctionConstants.VIEW_CASE_OUTCOME));

      assertThat(mockMvc.perform(timeRecoveryGet(awardPath)))
          .hasRedirectedUrl("/case/outcome-and-awards");
      assertThat(mockMvc.perform(validTimeRecoveryPost(awardPath)))
          .hasRedirectedUrl("/case/outcome-and-awards");

      verifyNoInteractions(caseOutcomeService);
    }

    @ParameterizedTest
    @CsvSource({"land, LAND", "asset, ASSET"})
    void displaysNewRecoveryWithParentSummary(String awardPath, String awardType) {
      stubTimeRecoveryAward(awardPath, true, null);

      assertThat(mockMvc.perform(timeRecoveryGet(awardPath)))
          .hasViewName("application/time-related-recovery")
          .model()
          .hasEntrySatisfying(
              "award",
              value ->
                  assertThat(value)
                      .extracting("awardType", "description", "awardAmount")
                      .containsExactly(
                          awardType, "Property or asset", new BigDecimal("150000.00")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"land", "asset"})
    void prefillsExistingRecovery(String awardPath) {
      stubTimeRecoveryAward(
          awardPath,
          true,
          new TimeRecoveryDetail()
              .triggeringEvent("Previous event")
              .effectiveDate(
                  Date.from(
                      LocalDate.of(2027, 6, 15).atStartOfDay(ZoneId.systemDefault()).toInstant()))
              .timeRelatedRecoveryDetails("Previous details"));

      assertThat(mockMvc.perform(timeRecoveryGet(awardPath)))
          .hasViewName("application/time-related-recovery")
          .model()
          .hasEntrySatisfying(
              "timeRecovery",
              value ->
                  assertThat(value)
                      .extracting("triggeringEvent", "effectiveDate", "timeRelatedRecoveryDetails")
                      .containsExactly("Previous event", "15/06/2027", "Previous details"));
    }

    @ParameterizedTest
    @CsvSource({"land, land-property", "asset, asset"})
    void redirectsWhenParentFlagIsNotSet(String awardPath, String parentPath) {
      stubTimeRecoveryAward(awardPath, false, null);

      assertThat(mockMvc.perform(timeRecoveryGet(awardPath)))
          .hasRedirectedUrl("/case/outcome-and-awards/" + parentPath + "/7");
      assertThat(mockMvc.perform(validTimeRecoveryPost(awardPath)))
          .hasRedirectedUrl("/case/outcome-and-awards/" + parentPath + "/7");
      verify(caseOutcomeService, org.mockito.Mockito.never())
          .upsertTimeRecovery(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @CsvSource({
      "land, LAND, 01/01/2030",
      "asset, ASSET, 01/01/2030",
      "land, LAND, 1/1/2030",
      "asset, ASSET, 1/1/2030",
      "land, LAND, ''",
      "asset, ASSET, ''"
    })
    void validPostUpsertsAndRedirects(String awardPath, String awardType, String effectiveDate) {
      stubTimeRecoveryAward(awardPath, true, null);

      assertThat(mockMvc.perform(timeRecoveryPost(awardPath, "Triggered", effectiveDate)))
          .hasRedirectedUrl("/case/outcome-and-awards/" + awardPath + "/7/liable-parties");
      verify(caseOutcomeService)
          .upsertTimeRecovery(
              eq("300000001"),
              eq(user.getProvider().getId().intValue()),
              eq(awardType),
              eq(7),
              eq(
                  new TimeRecoveryRequest("Triggered", "Details")
                      .effectiveDate(
                          effectiveDate.isEmpty()
                              ? null
                              : Date.from(
                                  LocalDate.of(2030, 1, 1)
                                      .atStartOfDay(ZoneId.systemDefault())
                                      .toInstant()))),
              eq(user.getLoginId()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"land", "asset"})
    void invalidPostKeepsUserInputAndShowsErrors(String awardPath) {
      stubTimeRecoveryAward(awardPath, true, null);

      assertThat(mockMvc.perform(timeRecoveryPost(awardPath, "", "01/01/1899")))
          .hasViewName("application/time-related-recovery")
          .model()
          .hasErrors()
          .hasEntrySatisfying(
              "timeRecovery",
              value ->
                  assertThat(value)
                      .extracting("effectiveDate", "timeRelatedRecoveryDetails")
                      .containsExactly("01/01/1899", "Details"));
      verify(caseOutcomeService, org.mockito.Mockito.never())
          .upsertTimeRecovery(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @CsvSource({"land, false", "asset, false", "land, true", "asset, true"})
    void saveFailureRedisplaysRecoveryWithError(String awardPath, boolean missingCaseOutcome) {
      stubTimeRecoveryAward(awardPath, true, null);
      doThrow(
              missingCaseOutcome
                  ? new IllegalStateException("Case outcome unavailable")
                  : new CaabApiClientException("API failure"))
          .when(caseOutcomeService)
          .upsertTimeRecovery(any(), any(), any(), any(), any(), any());

      assertThat(mockMvc.perform(validTimeRecoveryPost(awardPath)))
          .hasViewName("application/time-related-recovery")
          .model()
          .hasErrors()
          .containsKeys("award", "awardPath", "awardId")
          .hasEntrySatisfying(
              "timeRecovery",
              value ->
                  assertThat(value)
                      .extracting("triggeringEvent", "effectiveDate", "timeRelatedRecoveryDetails")
                      .containsExactly("Triggered", "01/01/2030", "Details"));
    }

    @Test
    void unsupportedAwardTypeHasNoRecoveryRoute() {
      assertThat(mockMvc.perform(timeRecoveryGet("cost-award"))).hasStatus(404);
      assertThat(mockMvc.perform(validTimeRecoveryPost("cost-award"))).hasStatus(404);
      verifyNoInteractions(caseOutcomeService);
    }
  }

  private void stubTimeRecoveryAward(
      String awardPath, boolean timeRelated, TimeRecoveryDetail recovery) {
    if ("land".equals(awardPath)) {
      when(caseOutcomeService.getLandAward("300000001", 123, 7))
          .thenReturn(
              Optional.of(
                  new LandAwardDetail()
                      .id(7)
                      .description("Property or asset")
                      .valuationAmount(new BigDecimal("150000.00"))
                      .recoveryOfAwardTimeRelated(timeRelated)
                      .timeRecovery(recovery)));
    } else {
      when(caseOutcomeService.getOtherAssetAward("300000001", 123, 7))
          .thenReturn(
              Optional.of(
                  new OtherAssetAwardDetail()
                      .id(7)
                      .description("Property or asset")
                      .valuationAmount(new BigDecimal("150000.00"))
                      .awardedAmount(new BigDecimal("20000.00"))
                      .recoveryOfAwardTimeRelated(timeRelated)
                      .timeRecovery(recovery)));
    }
  }

  private MockHttpServletRequestBuilder timeRecoveryGet(String awardPath) {
    return get("/case/outcome-and-awards/" + awardPath + "/7/time-related-recovery")
        .sessionAttr(CASE, ebsCase)
        .sessionAttr(USER_DETAILS, user);
  }

  private MockHttpServletRequestBuilder validTimeRecoveryPost(String awardPath) {
    return timeRecoveryPost(awardPath, "Triggered", "01/01/2030");
  }

  private MockHttpServletRequestBuilder timeRecoveryPost(
      String awardPath, String triggeringEvent, String effectiveDate) {
    return post("/case/outcome-and-awards/" + awardPath + "/7/time-related-recovery")
        .sessionAttr(CASE, ebsCase)
        .sessionAttr(USER_DETAILS, user)
        .param("triggeringEvent", triggeringEvent)
        .param("effectiveDate", effectiveDate)
        .param("timeRelatedRecoveryDetails", "Details");
  }

  private MockHttpServletRequestBuilder validPost() {
    final AwardTypeForm selectedAwardType = new AwardTypeForm();
    selectedAwardType.setAwardTypeCode("DAMAGE_AGR");
    selectedAwardType.setAwardType("DAMAGE");
    selectedAwardType.setDescription("Financial Settlement");

    return post("/case/outcome-and-awards/financial-award")
        .param("awardCode", "DAMAGE_AGR")
        .param("awardType", "DAMAGE")
        .param("description", "Financial Settlement")
        .param("dateOfOrder", "01/01/2025")
        .param("awardAmount", "123.45")
        .param("interimAward", "0")
        .param("awardedBy", "COURT")
        .sessionAttr(CASE, ebsCase)
        .sessionAttr(USER_DETAILS, user)
        .sessionAttr(AWARD_TYPE_FORM, selectedAwardType);
  }

  private MockHttpServletRequestBuilder validCostAwardPost() {
    final AwardTypeForm selectedAwardType = new AwardTypeForm();
    selectedAwardType.setAwardTypeCode("COST_AGR");
    selectedAwardType.setAwardType("COST");
    selectedAwardType.setDescription("Cost");

    return post("/case/outcome-and-awards/cost-award")
        .param("awardCode", "COST_AGR")
        .param("awardType", "COST")
        .param("description", "Cost")
        .param("dateOfOrder", "01/01/2025")
        .param("courtAssessmentStatus", "A")
        .param("laaFundedLegalCosts", "1.00")
        .param("otherPreCertificateCosts", "2.00")
        .param("laaRate", "3.00")
        .param("marketRate", "4.00")
        .param("awardedBy", "COURT")
        .param("interestRate", "8.5")
        .sessionAttr(CASE, ebsCase)
        .sessionAttr(USER_DETAILS, user)
        .sessionAttr(AWARD_TYPE_FORM, selectedAwardType);
  }

  private MockHttpServletRequestBuilder validLandPost() {
    return post("/case/outcome-and-awards/land-property")
        .param("awardCode", "LAND")
        .param("awardType", "LAND")
        .param("description", "Land")
        .param("dateOfOrder", "01/01/2025")
        .param("addressLine1", "1 High Street")
        .param("valuationAmount", "250000.00")
        .param("valuationCriteria", "MARKET")
        .param("valuationDate", "02/01/2025")
        .param("disputedPercentage", "50.00")
        .param("awardedPercentage", "25.00")
        .param("mortgageAmountDue", "100000.00")
        .param("awardedBy", "COURT")
        .param("recovery", "RECOVERED")
        .param("recoveryOfAwardTimeRelated", "Y")
        .sessionAttr(CASE, ebsCase)
        .sessionAttr(USER_DETAILS, user)
        .sessionAttr(AWARD_TYPE_FORM, landAwardType());
  }

  private AwardTypeForm landAwardType() {
    final AwardTypeForm awardType = new AwardTypeForm();
    awardType.setAwardTypeCode("LAND");
    awardType.setAwardType("LAND");
    awardType.setDescription("Land");
    return awardType;
  }

  private MockHttpServletRequestBuilder validOtherAssetPost() {
    return validOtherAssetPost(false);
  }

  private MockHttpServletRequestBuilder validOtherAssetPost(boolean timeRelated) {
    return post("/case/outcome-and-awards/asset")
        .param("awardCode", "OTH_ASSET")
        .param("awardType", "ASSET")
        .param("description", "Antique jewellery")
        .param("dateOfOrder", "01/01/2025")
        .param("awardedBy", "COURT")
        .param("valuationAmount", "123.45")
        .param("valuationCriteria", "AGREED")
        .param("valuationDate", "02/01/2025")
        .param("awardedPercentage", "75")
        .param("recovery", "UNKNOWN")
        .param("recoveryOfAwardTimeRelated", Boolean.toString(timeRelated))
        .sessionAttr(CASE, ebsCase)
        .sessionAttr(USER_DETAILS, user)
        .sessionAttr(AWARD_TYPE_FORM, otherAssetAwardType());
  }

  private AwardTypeForm otherAssetAwardType() {
    final AwardTypeForm selectedAwardType = new AwardTypeForm();
    selectedAwardType.setAwardTypeCode("OTH_ASSET");
    selectedAwardType.setAwardType("ASSET");
    selectedAwardType.setDescription("Asset");
    return selectedAwardType;
  }
}
