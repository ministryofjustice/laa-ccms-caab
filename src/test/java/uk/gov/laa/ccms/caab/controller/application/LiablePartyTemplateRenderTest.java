package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.INDIVIDUAL_LIABLE_PARTY_FLOW;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import uk.gov.laa.ccms.caab.advice.ActiveCaseModelAdvice;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualLiablePartyFlow;
import uk.gov.laa.ccms.caab.bean.validators.opponent.IndividualOpponentOtherDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.IndividualOpponentValidator;
import uk.gov.laa.ccms.caab.constants.FunctionConstants;
import uk.gov.laa.ccms.caab.controller.application.section.OpponentsSectionController;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ClientDetail;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.service.CaseOutcomeService;
import uk.gov.laa.ccms.caab.service.IndividualOpponentFormService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.springboot.dialect.GovUkDialect;
import uk.gov.laa.springboot.dialect.MojCustomDialect;

class LiablePartyTemplateRenderTest {
  private MockMvc mvc;
  private MockHttpSession session;
  private CaseOutcomeService caseOutcomeService;

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
    when(lookup.getPersonToCaseRelationships())
        .thenReturn(
            reactor.core.publisher.Mono.just(
                new RelationshipToCaseLookupDetail().content(List.of())));
    when(lookup.getCommonValues(any()))
        .thenReturn(
            reactor.core.publisher.Mono.just(
                new CommonLookupDetail()
                    .addContentItem(
                        new CommonLookupValueDetail().code("EMPLOYED").description("Employed"))));
    when(lookup.getCountries())
        .thenReturn(reactor.core.publisher.Mono.just(new CommonLookupDetail().content(List.of())));
    caseOutcomeService = mock(CaseOutcomeService.class);
    mvc =
        standaloneSetup(
                new LiablePartyController(
                    caseOutcomeService,
                    new IndividualOpponentFormService(lookup),
                    new IndividualOpponentValidator(),
                    new IndividualOpponentOtherDetailsValidator(),
                    lookup),
                new OpponentsSectionController(
                    null,
                    null,
                    null,
                    lookup,
                    null,
                    null,
                    new IndividualOpponentValidator(),
                    new IndividualOpponentFormService(lookup)))
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
  void editContactFormUsesExistingIdentityAndRetainsBanner(String path) throws Exception {
    final var flow = new IndividualLiablePartyFlow("300000001", 123, path, 7, 11);
    flow.getContactDetails().setFirstName("Existing party");
    when(caseOutcomeService.getIndividualLiableParty("300000001", 123, path, 7, 11))
        .thenReturn(flow);
    final String edit = base(path).replace("/individual/add", "/individual/11/edit");
    final String html =
        mvc.perform(
                get(edit)
                    .session(session)
                    .param("journeyToken", journeyToken())
                    .flashAttr("user", new UserDetail().functions(List.of())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(html)
        .contains(
            "action=\"" + edit + "\"",
            "value=\"Existing party\"",
            "href=\"" + edit + "/cancel?journeyToken=" + journeyToken() + "\"",
            ">Next</button>",
            "href=\"/case/outcome-and-awards/" + path + "/7/liable-parties\"")
        .doesNotContain("action=\"" + base(path) + "\"");
    assertCaseBanner(html);
    assertCancelLink(html, path, edit);
  }

  @ParameterizedTest
  @ValueSource(strings = {"cost-award", "financial-award", "land", "asset"})
  void editOtherFormPrepopulatesOtherDetailsAndPostsToUpdateJourney(String path) throws Exception {
    final var flow = new IndividualLiablePartyFlow("300000001", 123, path, 7, 11);
    flow.setContactDetailsComplete(true);
    flow.getOtherDetails().setEmployerName("Existing employer");
    flow.getOtherDetails().setAssessedIncome("123.45");
    flow.getOtherDetails().setAssessmentDate("01/02/2020");
    session.setAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW, flow);
    final String edit = base(path).replace("/individual/add", "/individual/11/edit");
    final String html =
        mvc.perform(
                get(edit + "/other-details")
                    .session(session)
                    .param("journeyToken", journeyToken())
                    .flashAttr("user", new UserDetail().functions(List.of())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(html)
        .contains(
            "action=\"" + edit + "/other-details\"",
            "value=\"Existing employer\"",
            "value=\"123.45\"",
            "value=\"01/02/2020\"",
            "value=\"confirm\"",
            "value=\"back\"",
            "href=\"" + edit + "/cancel?journeyToken=" + journeyToken() + "\"")
        .doesNotContain("action=\"" + base(path) + "/other-details\"");
    assertCaseBanner(html);
    assertCancelLink(html, path, edit);
  }

  @Test
  void unansweredYesNoQuestionsDoNotDisplayNoAsTheSelectedAnswer() throws Exception {
    final var flow = new IndividualLiablePartyFlow("300000001", 123, "cost-award", 7, 11);
    flow.setContactDetailsComplete(true);
    session.setAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW, flow);
    final String edit = base("cost-award").replace("/individual/add", "/individual/11/edit");
    final String contactHtml =
        mvc.perform(
                get(edit)
                    .param("back", "true")
                    .session(session)
                    .param("journeyToken", journeyToken())
                    .flashAttr("user", new UserDetail().functions(List.of())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    final String otherHtml =
        mvc.perform(
                get(edit + "/other-details")
                    .session(session)
                    .param("journeyToken", journeyToken())
                    .flashAttr("user", new UserDetail().functions(List.of())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertUnansweredSelect(contactHtml, "legalAided");
    assertUnansweredSelect(otherHtml, "courtOrderedMeansAssessment");
  }

  private void assertUnansweredSelect(String html, String field) {
    final var select =
        Pattern.compile("(?s)<select\\b[^>]*name=\"" + field + "\"[^>]*>(.*?)</select>")
            .matcher(html);
    assertThat(select.find()).isTrue();
    assertThat(select.group(1))
        .doesNotContainPattern(
            "<option(?=[^>]*value=\"(?:true|false)\")(?=[^>]*\\bselected(?:\\s|=|>))[^>]*>");
  }

  @Test
  void existingApplicationOpponentStillRendersItsOriginalConfirmAndRadios() throws Exception {
    final String html =
        mvc.perform(
                get("/application/sections/opponents/individual/create")
                    .session(session)
                    .param("journeyToken", journeyToken())
                    .flashAttr("user", new UserDetail().functions(List.of())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(html)
        .contains(
            "action=\"/application/sections/opponents/individual/create\"",
            ">Confirm</button>",
            "type=\"radio\"",
            "name=\"legalAided\"")
        .doesNotContain("/liable-parties/", ">Next</button>");
    assertCaseBanner(html);
  }

  @ParameterizedTest
  @ValueSource(strings = {"cost-award", "financial-award", "land", "asset"})
  void rendersSharedContactFieldsWithAwardScopedActionAndNext(String path) throws Exception {
    final String base = base(path);
    final String html =
        mvc.perform(
                get(base)
                    .session(session)
                    .param("journeyToken", journeyToken())
                    .flashAttr("user", new UserDetail().functions(List.of())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(html)
        .contains(
            "Individual contact details",
            "action=\"" + base + "\"",
            ">Next</button>",
            "href=\"" + base + "/cancel?journeyToken=" + journeyToken() + "\"");
    for (String field :
        List.of(
            "title",
            "surname",
            "firstName",
            "middleNames",
            "dateOfBirth",
            "relationshipToCase",
            "relationshipToClient",
            "nationalInsuranceNumber",
            "houseNameOrNumber",
            "addressLine1",
            "addressLine2",
            "city",
            "county",
            "country",
            "postcode",
            "telephoneHome",
            "telephoneWork",
            "telephoneMobile",
            "faxNumber",
            "emailAddress",
            "legalAided",
            "certificateNumber")) {
      assertThat(html).contains("name=\"" + field + "\"");
    }
    assertThat(html)
        .containsPattern("<select[^>]*name=\"legalAided\"")
        .doesNotContain("/sections/opponents", ">Confirm</button>");
    assertCaseBanner(html);
    assertCancelLink(html, path, base);
  }

  @Test
  void backParameterRendersSavedContactsWithTheSameFormAction() throws Exception {
    final var flow = new IndividualLiablePartyFlow("300000001", 123, "cost-award", 7);
    flow.getContactDetails().setFirstName("Jane");
    session.setAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW, flow);

    final String html =
        mvc.perform(
                get(base("cost-award"))
                    .param("back", "true")
                    .session(session)
                    .param("journeyToken", journeyToken())
                    .flashAttr("user", new UserDetail().functions(List.of())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(html)
        .contains(
            "Individual contact details", "value=\"Jane\"", "action=\"" + base("cost-award") + "\"")
        .doesNotContain("/individual/contact-details", "action=\"" + base("cost-award") + "?");
    assertCaseBanner(html);
  }

  @ParameterizedTest
  @ValueSource(strings = {"cost-award", "financial-award", "land", "asset"})
  void rendersOptionalOtherDetailsWithLimitsDatepickerAndPostBack(String path) throws Exception {
    final var flow = new IndividualLiablePartyFlow("300000001", 123, path, 7);
    flow.setContactDetailsComplete(true);
    flow.getOtherDetails().setEmployerName("Acme & Co");
    session.setAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW, flow);
    final String html =
        mvc.perform(
                get(base(path) + "/other-details")
                    .session(session)
                    .param("journeyToken", journeyToken())
                    .flashAttr("user", new UserDetail().functions(List.of())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(html)
        .contains(
            "Individual other details",
            "Employer&#39;s",
            "name=\"employerAddress\"",
            "data-maxlength=\"200\"",
            "data-maxlength=\"2000\"",
            "name=\"assessmentDate\"",
            "name=\"courtOrderedMeansAssessment\"",
            "value=\"confirm\"",
            "value=\"back\"",
            "Acme &amp; Co")
        .doesNotContain("/sections/opponents");
    for (String field :
        List.of(
            "employmentStatus",
            "employerName",
            "assessedIncome",
            "assessedIncomeFrequency",
            "assessedAssets",
            "otherInformation")) {
      assertThat(html).contains("name=\"" + field + "\"");
    }
    assertCaseBanner(html);
    assertCancelLink(html, path, base(path));
  }

  @ParameterizedTest
  @ValueSource(strings = {"assessmentDate", "otherInformation"})
  void rendersFieldErrorsAndErrorSummaryAnchors(String field) throws Exception {
    final var flow = new IndividualLiablePartyFlow("300000001", 123, "cost-award", 7);
    flow.setContactDetailsComplete(true);
    session.setAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW, flow);
    final String value = "assessmentDate".equals(field) ? "31/02/2026" : "<markup>";
    final String html =
        mvc.perform(
                post(base("cost-award") + "/other-details")
                    .session(session)
                    .param("journeyToken", journeyToken())
                    .flashAttr("user", new UserDetail().functions(List.of()))
                    .param(field, value))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(html).contains("href=\"#" + field + "\"", "govuk-error-message");
    assertCaseBanner(html);
    if ("assessmentDate".equals(field)) {
      assertThat(html).contains("value=\"31/02/2026\"");
    } else {
      assertThat(html).contains("&lt;markup&gt;").doesNotContain("<markup>");
    }
  }

  @Test
  void contactValidationErrorsRetainTheCaseBanner() throws Exception {
    session.setAttribute(
        INDIVIDUAL_LIABLE_PARTY_FLOW,
        new IndividualLiablePartyFlow("300000001", 123, "cost-award", 7));
    final String html =
        mvc.perform(
                post(base("cost-award"))
                    .session(session)
                    .param("journeyToken", journeyToken())
                    .flashAttr("user", new UserDetail().functions(List.of())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(html).contains("href=\"#firstName\"", "govuk-error-message");
    assertCaseBanner(html);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "/other-details"})
  void reusesExistingActiveCaseForBothPages(String suffix) throws Exception {
    session.setAttribute(
        ACTIVE_CASE,
        ActiveCase.builder()
            .caseReferenceNumber("300000001")
            .client("Existing Client")
            .providerCaseReferenceNumber("EXISTING-REF")
            .build());
    final var flow = new IndividualLiablePartyFlow("300000001", 123, "cost-award", 7);
    flow.setContactDetailsComplete(true);
    session.setAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW, flow);
    final String html =
        mvc.perform(
                get(base("cost-award") + suffix)
                    .session(session)
                    .param("journeyToken", journeyToken())
                    .flashAttr("user", new UserDetail().functions(List.of())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(html)
        .containsOnlyOnce("ccms-phase-banner-active")
        .contains("Existing Client", "300000001", "EXISTING-REF");
  }

  private void assertCaseBanner(String html) {
    assertThat(html)
        .containsOnlyOnce("ccms-phase-banner-active")
        .containsOnlyOnce(
            "Please enter the details for any opponents and other parties involved in this "
                + "application/amendment.")
        .contains("Jane Doe", "300000001", "PROV-123");
  }

  private void assertCancelLink(String html, String path, String formUrl) {
    final String parties =
        "land".equals(path) || "asset".equals(path) ? "interested parties" : "liable parties";
    assertThat(html)
        .containsPattern(
            "(?s)<a\\b[^>]*href=\""
                + Pattern.quote(formUrl + "/cancel?journeyToken=" + journeyToken())
                + "\"[^>]*>\\s*"
                + "Cancel and return to "
                + parties
                + "\\s*</a>")
        .doesNotContain("Cancel and return to outcome and awards");
    assertThat(html)
        .contains("type=\"hidden\" name=\"journeyToken\" value=\"" + journeyToken() + "\"");
  }

  private String journeyToken() {
    final var flow = (IndividualLiablePartyFlow) session.getAttribute(INDIVIDUAL_LIABLE_PARTY_FLOW);
    return flow == null ? "" : flow.getJourneyToken();
  }

  private String base(String path) {
    return "/case/outcome-and-awards/" + path + "/7/liable-parties/individual/add";
  }
}
