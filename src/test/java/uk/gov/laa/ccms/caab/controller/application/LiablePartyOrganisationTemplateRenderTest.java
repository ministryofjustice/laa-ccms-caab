package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ORGANISATION_LIABLE_PARTY_FLOW;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.ActiveCaseModelAdvice;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationLiablePartyFlow;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationOpponentFormData;
import uk.gov.laa.ccms.caab.bean.validators.opponent.OrganisationOpponentValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.OrganisationSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ClientDetail;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.OrganisationResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.service.CaseOutcomeService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.OpponentService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.springboot.dialect.GovUkDialect;
import uk.gov.laa.springboot.dialect.MojCustomDialect;

class LiablePartyOrganisationTemplateRenderTest {
  private MockMvc mvc;
  private MockHttpSession session;
  private OpponentService opponentService;

  @BeforeEach
  void setUp() {
    final var context = new GenericWebApplicationContext(new MockServletContext());
    context.refresh();
    final var resolver = new SpringResourceTemplateResolver();
    resolver.setApplicationContext(context);
    resolver.setPrefix("classpath:/templates/");
    resolver.setSuffix(".html");
    final var messages = new ResourceBundleMessageSource();
    messages.setBasename("messages");
    final var engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setTemplateEngineMessageSource(messages);
    engine.addDialect(new GovUkDialect());
    engine.addDialect(new MojCustomDialect());
    final var views = new ThymeleafViewResolver();
    views.setTemplateEngine(engine);
    views.setCharacterEncoding("UTF-8");
    final var lookup = mock(LookupService.class);
    when(lookup.getCommonValues(any()))
        .thenReturn(
            Mono.just(
                new CommonLookupDetail()
                    .addContentItem(
                        new CommonLookupValueDetail().code("LA").description("Local Authority"))));
    when(lookup.getCountries()).thenReturn(Mono.just(new CommonLookupDetail().content(List.of())));
    when(lookup.getOrganisationToCaseRelationships())
        .thenReturn(
            Mono.just(
                new RelationshipToCaseLookupDetail()
                    .addContentItem(
                        new RelationshipToCaseLookupValueDetail()
                            .code("OPP")
                            .description("Opponent"))));
    opponentService = mock(OpponentService.class);
    mvc =
        standaloneSetup(
                new LiablePartyController(
                    mock(CaseOutcomeService.class),
                    null,
                    null,
                    null,
                    lookup,
                    opponentService,
                    new OrganisationSearchCriteriaValidator(),
                    new OrganisationOpponentValidator()))
            .setControllerAdvice(new ActiveCaseModelAdvice())
            .setConversionService(
                uk.gov.laa.ccms.caab.util.ConversionServiceUtils.getConversionService())
            .setViewResolvers(views)
            .build();
    session = new MockHttpSession();
    session.setAttribute(
        CASE,
        new ApplicationDetail()
            .caseReferenceNumber("300000001")
            .client(new ClientDetail().firstName("Jane").surname("Doe").reference("CLIENT-1"))
            .providerDetails(
                new ApplicationProviderDetails()
                    .providerCaseReference("PROV-123")
                    .provider(new IntDisplayValue().id(123)))
            .availableFunctions(List.of(FunctionConstants.OUTCOME_WITH_DISCHARGE)));
    session.setAttribute(USER_DETAILS, ApplicationTestUtils.buildUser());
  }

  @ParameterizedTest
  @ValueSource(strings = {"cost-award", "financial-award", "land", "asset"})
  void searchPagePostsToTheAwardJourneyWithItsToken(String path) throws Exception {
    final String html = render(get(org(path) + "/search"));
    final String token = token();

    assertThat(html)
        .contains(
            "action=\"" + org(path) + "/search\"",
            "name=\"journeyToken\" value=\"" + token + "\"",
            "name=\"name\"",
            "href=\"" + org(path) + "/create?journeyToken=" + token + "\"",
            "href=\"" + org(path) + "/cancel?journeyToken=" + token + "\"",
            "300000001")
        .doesNotContain("/application/opponents");
    final boolean interested = "land".equals(path) || "asset".equals(path);
    assertThat(html).contains(interested ? "interested parties" : "liable parties");
  }

  @Test
  void tooManyResultsShowsAnErrorOnTheSearchPage() throws Exception {
    draft("cost-award").getSearchCriteria().setName("Council");
    when(opponentService.getOrganisations(any(), any(), any(), anyInt(), anyInt()))
        .thenThrow(new TooManyResultsException("too many"));

    final String html = render(get(org("cost-award") + "/search/results"));

    assertThat(html).contains("govuk-error-summary", "too many results");
  }

  @Test
  void resultsPageLinksEachRowAndPaginatesWithinTheJourney() throws Exception {
    draft("land").getSearchCriteria().setName("Test");
    final var row = new OrganisationResultRowDisplay();
    row.setPartyId("ORG-1");
    row.setName("Test Valley Borough Council");
    row.setTypeDisplayValue("Local Authority");
    row.setCity("Andover");
    row.setPostcode("SP10 3AJ");
    final var results = new ResultsDisplay<>(List.of(row));
    results.setTotalElements(11);
    results.setTotalPages(2);
    results.setNumber(0);
    results.setSize(10);
    when(opponentService.getOrganisations(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(results);

    final String html = render(get(org("land") + "/search/results"));
    final String token = token();

    assertThat(html)
        .contains(
            "Test Valley Borough Council",
            "Andover",
            "SP10 3AJ",
            "href=\"" + org("land") + "/ORG-1/select?journeyToken=" + token + "\"",
            org("land") + "/search/results?journeyToken=" + token + "&amp;page=1",
            "href=\"" + org("land") + "/search?back=true&amp;journeyToken=" + token + "\"",
            "href=\"" + org("land") + "/create?journeyToken=" + token + "\"");
  }

  @Test
  void emptyResultsStillOfferCreatingANewOrganisation() throws Exception {
    draft("cost-award").getSearchCriteria().setName("Nothing");
    final var results = new ResultsDisplay<OrganisationResultRowDisplay>(List.of());
    results.setTotalElements(0);
    results.setTotalPages(0);
    results.setNumber(0);
    results.setSize(10);
    when(opponentService.getOrganisations(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(results);

    final String html = render(get(org("cost-award") + "/search/results"));

    assertThat(html)
        .contains("href=\"" + org("cost-award") + "/create?journeyToken=" + token() + "\"")
        .doesNotContain("/select?journeyToken=");
  }

  @Test
  void sharedOrganisationPageShowsReadOnlySummaryAndRelationshipFields() throws Exception {
    final var flow = draft("financial-award");
    final var row = new OrganisationResultRowDisplay();
    row.setPartyId("ORG-1");
    flow.setSearchResults(new ResultsDisplay<>(List.of(row)));
    final var organisation = new OrganisationOpponentFormData();
    organisation.setOrganisationName("Test Valley Borough Council");
    organisation.setOrganisationTypeDisplayValue("Local Authority");
    organisation.setCity("Andover");
    when(opponentService.getOrganisationOpponent(any(), any(), any())).thenReturn(organisation);

    final String html = render(get(org("financial-award") + "/ORG-1/select"));

    assertThat(html)
        .contains(
            "action=\"" + org("financial-award") + "/select\"",
            "name=\"journeyToken\" value=\"" + token() + "\"",
            "Test Valley Borough Council",
            "name=\"relationshipToCase\"",
            "name=\"relationshipToClient\"",
            "name=\"otherInformation\"",
            "href=\"" + org("financial-award") + "/cancel?journeyToken=" + token() + "\"")
        .doesNotContain("name=\"organisationName\"");
  }

  @Test
  void sharedOrganisationValidationErrorsAreDisplayed() throws Exception {
    final var flow = draft("cost-award");
    final var organisation = new OrganisationOpponentFormData();
    organisation.setShared(true);
    organisation.setPartyId("ORG-1");
    organisation.setOrganisationName("Test Valley Borough Council");
    flow.setSelectedOrganisation(organisation);

    final String html = render(post(org("cost-award") + "/select"));

    assertThat(html).contains("govuk-error-summary", "href=\"#relationshipToCase\"", "Test Valley");
  }

  @Test
  void createPagePostsANewOrganisationWithinTheJourney() throws Exception {
    draft("asset").getSearchCriteria().setName("Prefilled Ltd");

    final String html = render(get(org("asset") + "/create"));

    assertThat(html)
        .contains(
            "action=\"" + org("asset") + "/create\"",
            "name=\"journeyToken\" value=\"" + token() + "\"",
            "value=\"Prefilled Ltd\"",
            "name=\"organisationType\"",
            "name=\"addressLine1\"",
            "href=\"" + org("asset") + "/cancel?journeyToken=" + token() + "\"");
  }

  @Test
  void createValidationErrorsAreDisplayed() throws Exception {
    draft("cost-award");

    final String html = render(post(org("cost-award") + "/create"));

    assertThat(html).contains("govuk-error-summary", "href=\"#organisationName\"");
  }

  private String render(final MockHttpServletRequestBuilder request) throws Exception {
    if (token() != null) {
      request.param("journeyToken", token());
    }
    return mvc.perform(
            request.session(session).flashAttr("user", new UserDetail().functions(List.of())))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private OrganisationLiablePartyFlow draft(final String path) {
    final var flow = new OrganisationLiablePartyFlow("300000001", 123, path, 7);
    session.setAttribute(ORGANISATION_LIABLE_PARTY_FLOW, flow);
    return flow;
  }

  private String token() {
    final var flow =
        (OrganisationLiablePartyFlow) session.getAttribute(ORGANISATION_LIABLE_PARTY_FLOW);
    return flow == null ? null : flow.getJourneyToken();
  }

  private static String org(final String path) {
    return "/case/outcome-and-awards/" + path + "/7/liable-parties/organisation";
  }
}
