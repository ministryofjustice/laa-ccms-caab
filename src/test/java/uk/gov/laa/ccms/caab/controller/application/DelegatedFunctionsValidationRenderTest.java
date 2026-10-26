package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.util.ConversionServiceUtils.getConversionService;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.util.HtmlUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import uk.gov.laa.ccms.caab.advice.GlobalBinderAdvice;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.BaselineTextValidator;
import uk.gov.laa.ccms.caab.bean.validators.application.DelegatedFunctionsValidator;
import uk.gov.laa.ccms.caab.constants.CaseContext;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.DevolvedPowersDetail;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.util.DateUtils;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.springboot.dialect.GovUkDialect;
import uk.gov.laa.springboot.dialect.MojCustomDialect;

class DelegatedFunctionsValidationRenderTest {

  private static final String DATE_FIELD = "delegatedFunctionUsedDate";
  private static final String FORM_VIEW = "application/select-delegated-functions";

  private MockMvc mockMvc;
  private ApplicationService applicationService;
  private ApplicationFormData form;
  private MockHttpSession session;
  private ApplicationDetail application;
  private UserDetail user;

  @BeforeEach
  void setup() {
    applicationService = mock(ApplicationService.class);
    final DelegatedFunctionsController controller =
        new DelegatedFunctionsController(new DelegatedFunctionsValidator(), applicationService);

    final GenericWebApplicationContext context =
        new GenericWebApplicationContext(new MockServletContext());
    context.refresh();
    final SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
    resolver.setApplicationContext(context);
    resolver.setPrefix("classpath:/templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");

    final ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
    messages.setBasename("messages");
    messages.setDefaultEncoding("UTF-8");
    final SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setTemplateEngineMessageSource(messages);
    engine.addDialect(new GovUkDialect());
    engine.addDialect(new MojCustomDialect());

    final ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
    viewResolver.setTemplateEngine(engine);
    viewResolver.setCharacterEncoding("UTF-8");
    viewResolver.setStaticVariables(
        Map.of(
            "cspNonce", "test-nonce",
            "researchPanelLink", "/research",
            "user", new UserDetail().functions(List.of())));

    mockMvc =
        standaloneSetup(controller)
            .setControllerAdvice(new GlobalBinderAdvice(new BaselineTextValidator()))
            .setConversionService(getConversionService())
            .setViewResolvers(viewResolver)
            .build();

    form = new ApplicationFormData();
    session = new MockHttpSession();
    session.setAttribute(APPLICATION_FORM_DATA, form);
    final DevolvedPowersDetail powers = new DevolvedPowersDetail();
    powers.setUsed(true);
    powers.setDateUsed(DateUtils.convertToDate("04/03/2024"));
    final ApplicationType type = new ApplicationType();
    type.setDevolvedPowers(powers);
    application = new ApplicationDetail();
    application.setId(123);
    application.setApplicationType(type);
    user = new UserDetail();
    session.setAttribute(APPLICATION, application);
    session.setAttribute(USER_DETAILS, user);
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc", "abc123", "1a/02/2024", "01/b2/2024", "01/02/20c4", "01/@2/2024"})
  void invalidCharactersRenderFieldAndSummaryErrorsWithoutLosingInput(String date)
      throws Exception {
    final MvcResult result = invalidSubmission("/application/delegated-functions", date);

    assertRenderedErrorsAndValue(result, date);
    verifyNoInteractions(applicationService);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/application/delegated-functions",
        "/amendments/delegated-functions",
        "/amendments/edit-delegated-functions"
      })
  void errorSummaryLinkTargetsTheRenderedDateInput(String path) throws Exception {
    final MvcResult result = invalidSubmission(path, "abc123");
    final String html = result.getResponse().getContentAsString();
    final String summary = summary(html);
    final String target = capture("<a\\b[^>]*href=\"#([^\"]+)\"", summary);
    final String input = dateInput(html);

    assertThat(attribute("id", input))
        .as("summary link must target the date input")
        .isEqualTo(target);
  }

  @ParameterizedTest
  @CsvSource({
    "application, 4/3/2024, /application/client/search",
    "application, 04/03/2024, /application/client/search",
    "amendments, 4/3/2024, /amendments/create"
  })
  void validDateNormalisesAndContinues(String context, String date, String redirect)
      throws Exception {
    mockMvc
        .perform(
            post("/" + context + "/delegated-functions")
                .session(session)
                .param("delegatedFunctions", "true")
                .param(DATE_FIELD, date))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(redirect));

    assertThat(form.getDelegatedFunctionUsedDate()).isEqualTo("04/03/2024");
    assertThat(session.getAttribute(APPLICATION_FORM_DATA)).isSameAs(form);
    verifyNoInteractions(applicationService);
  }

  @ParameterizedTest
  @CsvSource({"application, /application/client/search", "amendments, /amendments/create"})
  void noSelectionIgnoresAndClearsMalformedDate(String context, String redirect) throws Exception {
    mockMvc
        .perform(
            post("/" + context + "/delegated-functions")
                .session(session)
                .param("delegatedFunctions", "false")
                .param(DATE_FIELD, "abc123"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(redirect));

    assertThat(form.getDelegatedFunctionUsedDate()).isNull();
    verifyNoInteractions(applicationService);
  }

  @Test
  void amendmentCreationRejectsInvalidCharacters() throws Exception {
    final MvcResult result = invalidSubmission("/amendments/delegated-functions", "abc123");

    assertRenderedErrorsAndValue(result, "abc123");
    verifyNoInteractions(applicationService);
  }

  @Test
  void amendmentEditRejectsInvalidCharactersWithoutSavingOrChangingApplication() throws Exception {
    final Date originalDate = application.getApplicationType().getDevolvedPowers().getDateUsed();
    final MvcResult result = invalidSubmission("/amendments/edit-delegated-functions", "abc123");

    assertRenderedErrorsAndValue(result, "abc123");
    assertThat(result.getModelAndView().getModel())
        .containsEntry("caseContext", CaseContext.AMENDMENTS)
        .containsEntry("edit", true);
    assertThat(application.getApplicationType().getDevolvedPowers().getDateUsed())
        .isEqualTo(originalDate);
    assertThat(application.getApplicationType().getDevolvedPowers().getUsed()).isTrue();
    verifyNoInteractions(applicationService);
  }

  @ParameterizedTest
  @CsvSource({"true, 4/3/2024", "false, abc123"})
  void amendmentEditSavesValidSelectionAndClearsDateForNo(boolean used, String date)
      throws Exception {
    mockMvc
        .perform(
            post("/amendments/edit-delegated-functions")
                .session(session)
                .param("delegatedFunctions", Boolean.toString(used))
                .param(DATE_FIELD, date))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/amendments/sections/linked-cases"));

    assertThat(application.getApplicationType().getDevolvedPowers().getUsed()).isEqualTo(used);
    assertThat(application.getApplicationType().getDevolvedPowers().getDateUsed())
        .isEqualTo(used ? DateUtils.convertToDate("04/03/2024") : null);
    assertThat(form.getDelegatedFunctionUsedDate()).isEqualTo(used ? "04/03/2024" : null);
    verify(applicationService)
        .putApplicationTypeFormData(123, application.getApplicationType(), user);
  }

  private MvcResult invalidSubmission(String path, String date) throws Exception {
    return mockMvc
        .perform(
            post(path).session(session).param("delegatedFunctions", "true").param(DATE_FIELD, date))
        .andExpect(status().isOk())
        .andExpect(view().name(FORM_VIEW))
        .andExpect(
            model().attributeHasFieldErrorCode(APPLICATION_FORM_DATA, DATE_FIELD, "invalid.format"))
        .andReturn();
  }

  private void assertRenderedErrorsAndValue(MvcResult result, String date) throws Exception {
    final BindingResult binding =
        (BindingResult)
            result
                .getModelAndView()
                .getModel()
                .get(BindingResult.MODEL_KEY_PREFIX + APPLICATION_FORM_DATA);
    final FieldError error = binding.getFieldError(DATE_FIELD);
    assertThat(error).isNotNull();
    assertThat(error.getDefaultMessage()).isNotBlank();
    final String html = result.getResponse().getContentAsString();
    final String inline =
        capture("(<p\\b[^>]*class=\"[^\"]*\\bgovuk-error-message\\b[^\"]*\"[^>]*>.*?</p>)", html);

    assertThat(HtmlUtils.htmlUnescape(inline)).contains(error.getDefaultMessage());
    assertThat(HtmlUtils.htmlUnescape(summary(html))).contains(error.getDefaultMessage());
    final String input = dateInput(html);
    assertThat(attribute("value", input)).isEqualTo(date);
    assertThat(attribute("aria-describedby", input)).contains(attribute("id", inline));
    assertThat(form.getDelegatedFunctionUsedDate()).isEqualTo(date);
  }

  private static String summary(String html) {
    return capture("(<ul\\b[^>]*id=\"error-summary-list\"[^>]*>.*?</ul>)", html);
  }

  private static String dateInput(String html) {
    return capture("(<input\\b[^>]*name=\"" + DATE_FIELD + "\"[^>]*>)", html);
  }

  private static String attribute(String name, String tag) {
    return HtmlUtils.htmlUnescape(capture("\\b" + name + "=\"([^\"]*)\"", tag));
  }

  private static String capture(String regex, String html) {
    final Matcher matcher = Pattern.compile(regex, Pattern.DOTALL).matcher(html);
    assertThat(matcher.find()).as("rendered markup matching %s", regex).isTrue();
    return matcher.group(1);
  }
}
