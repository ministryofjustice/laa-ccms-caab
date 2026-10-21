package uk.gov.laa.ccms.caab.controller.billing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import uk.gov.laa.ccms.caab.bean.billing.BillPoaRow;
import uk.gov.laa.ccms.caab.bean.billing.StatementOfAccountDisplay;
import uk.gov.laa.ccms.caab.constants.BillingContext;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Smoke tests that render the billing pages through a real Thymeleaf engine, including the shared
 * layout and its partials.
 *
 * <p>The billing controller tests use a standalone MockMvc that only resolves view <em>names</em>,
 * so a page that renders through the layout incorrectly - for example omitting the {@code
 * breadcrumbs} fragment the layout requires - passes there but 500s at runtime. These render the
 * pages so that class of error is caught. The billing pages are {@code pageCategory='cases'}, for
 * which the phase banner's user/active-case blocks are skipped, so no session globals beyond an
 * (empty-function) user are needed.
 *
 * <p>The POA declaration page is not rendered here: its error-summary sits inside a {@code
 * th:object} form, so {@code #fields} needs the full Spring MVC request context that only a real
 * MVC render sets up. Its flow is covered by {@code BillingControllerTest}.
 */
@DisplayName("Billing template render smoke tests")
class BillingTemplateRenderTest {

  private static SpringTemplateEngine engine;

  @BeforeAll
  static void setUpEngine() {
    final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");

    final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("messages");
    messageSource.setDefaultEncoding("UTF-8");

    engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setTemplateEngineMessageSource(messageSource);
  }

  /**
   * Renders a page through the layout. Throws if the page or the layout cannot be processed, which
   * is what makes this a smoke test.
   */
  private String render(final String template, final Map<String, Object> model) {
    final MockServletContext servletContext = new MockServletContext();
    final JakartaServletWebApplication application =
        JakartaServletWebApplication.buildApplication(servletContext);
    final IWebExchange exchange =
        application.buildExchange(
            new MockHttpServletRequest(servletContext), new MockHttpServletResponse());

    final Map<String, Object> variables = new HashMap<>(model);
    // Referenced by the layout's script tags.
    variables.put("cspNonce", "test-nonce");
    // The shared header reads user.functions (via #lists.contains), so supply an empty one.
    variables.put("user", new UserDetail().functions(List.of()));

    return engine.process(template, new WebContext(exchange, Locale.UK, variables));
  }

  @Test
  @DisplayName("POA confirmation renders through the layout in the shared submission shape")
  void poaConfirmationRenders() {
    final String html =
        render(
            "application/billing/confirmation",
            Map.of("transactionId", "INV-1", "billingContext", BillingContext.POA));
    assertThat(html)
        .contains("poa-submission-reference")
        .contains("INV-1")
        // The key is built from the billing context, so a wrong one renders ??key??.
        .contains("Payment on account submitted")
        .doesNotContain("??");
    assertConfirmationShape(html);
  }

  /**
   * The billing confirmations follow the service's shared submission confirmation - a heading and
   * body text - rather than the GOV.UK confirmation panel they originally used.
   */
  private void assertConfirmationShape(final String html) {
    assertThat(html)
        .contains("govuk-heading-l")
        .contains("govuk-body-l")
        .doesNotContain("govuk-panel--confirmation");
  }

  @Test
  @DisplayName("POA details renders through the layout with the summary and submit actions")
  void poaDetailsRenders() {
    final Map<String, Object> model = new HashMap<>();
    model.put("assessmentStatus", "Complete");
    model.put("assessmentComplete", true);
    model.put("viewPoaSummary", true);

    final String html = render("application/billing/poa-details", model);
    assertThat(html).contains("poa-summary-link").contains("submit-poa-button");
  }

  @Test
  @DisplayName("Bill details renders through the layout with the summary and submit actions")
  void billDetailsRenders() {
    final Map<String, Object> model = new HashMap<>();
    model.put("assessmentStatus", "Complete");
    model.put("assessmentComplete", true);
    model.put("printDraftBill", true);

    final String html = render("application/billing/bill-details", model);
    assertThat(html)
        .contains("bill-details-link")
        .contains("bill-summary-link")
        // The summary opens the generated PDF, rather than being an inert placeholder.
        .contains("/case/billing/bill/summary")
        .contains("submit-bill-button")
        // The screen's two required headers, and the renamed return link.
        .contains("Billing")
        .contains("Status")
        .contains("Cancel and return to case statement of account");
  }

  @Test
  @DisplayName("Bill confirmation renders through the layout in the shared submission shape")
  void billConfirmationRenders() {
    final String html =
        render(
            "application/billing/confirmation",
            Map.of("transactionId", "INV-9", "billingContext", BillingContext.BILL));
    assertThat(html)
        .contains("bill-submission-reference")
        .contains("INV-9")
        .contains("Bill submitted")
        .doesNotContain("??");
    assertConfirmationShape(html);
  }

  @Test
  @DisplayName("Bill details renders a final validation failure")
  void billDetailsRendersSubmissionError() {
    final Map<String, Object> model = new HashMap<>();
    model.put("assessmentStatus", "In progress");
    model.put("assessmentComplete", false);
    model.put("printDraftBill", false);
    model.put("submissionError", "billing.bill.error.notComplete");

    final String html = render("application/billing/bill-details", model);
    assertThat(html)
        .contains("bill-submission-error")
        .contains("You must provide the Bill Details completely");
  }

  @Test
  @DisplayName("Bill details withholds the summary and enables no submit before the assessment")
  void billDetailsWithholdsSummaryBeforeAssessment() {
    final Map<String, Object> model = new HashMap<>();
    model.put("assessmentStatus", "Not started");
    model.put("assessmentComplete", false);
    model.put("printDraftBill", false);

    final String html = render("application/billing/bill-details", model);
    assertThat(html).contains("bill-details-link").doesNotContain("bill-summary-link");
    // Submit stays available before the assessment completes: the legacy PUI runs its final
    // validation on the click and explains the failure rather than disabling the button.
    assertThat(html).contains("submit-bill-button").doesNotContain("bill-submission-error");
  }

  /**
   * Builds the statement of account model around a single copyable rejected bill, so the copy
   * action's conditions can be exercised through a real render.
   *
   * <p>Whether a row is copyable at all - which includes it carrying a billing incident id - is
   * decided in {@code BillingService} and covered by its tests; the view only gates on the
   * resulting flag and the user's function.
   */
  private BillPoaRow copyableRejectedBill() {
    return new BillPoaRow(
            "Counsel Bill", "Rejected", null, null, new BigDecimal("100.00"), false, 222L)
        .withCopyable();
  }

  private BillPoaRow uncopyableRejectedBill() {
    return new BillPoaRow(
        "Counsel Bill", "Rejected", null, null, new BigDecimal("100.00"), false, 222L);
  }

  private BillPoaRow draftBill() {
    return new BillPoaRow("Counsel Bill", "Draft", null, null, new BigDecimal("50.00"), true, 333L);
  }

  private Map<String, Object> statementModel(final boolean canMaintainBill) {
    final BillPoaRow rejected =
        new BillPoaRow(
                "Counsel Bill", "Rejected", null, null, new BigDecimal("100.00"), false, 222L)
            .withCopyable();

    final Map<String, Object> model = new HashMap<>();
    model.put("caseReferenceNumber", "300000123");
    model.put("statementOfAccount", new StatementOfAccountDisplay());
    model.put("billsAndPoaPage", new PageImpl<>(List.of(rejected)));
    model.put("currentUrl", "http://localhost/case/billing");
    model.put("paginationAnchor", "bills-and-poa");
    model.put("showEnterUndertaking", false);
    model.put("showCreateBill", true);
    model.put("showCreatePoa", true);
    model.put("canMaintainPoa", true);
    model.put("canMaintainBill", canMaintainBill);
    model.put("draftInProgress", false);
    return model;
  }

  @Test
  @DisplayName("Statement of account offers the copy action on a copyable rejected bill")
  void statementOffersCopy() {
    final String html =
        render("application/billing/case-statement-of-account", statementModel(true));
    assertThat(html)
        .contains("/case/billing/bill/copy")
        .contains("name=\"billing-id\" value=\"222\"")
        .contains("ccms-copy-bill-link");
  }

  @Test
  @DisplayName("Statement of account withholds the copy action while a draft is in progress")
  void statementWithholdsCopyWhileDraftInProgress() {
    final Map<String, Object> model = statementModel(true);
    model.put("draftInProgress", true);
    model.put("showCreateBill", false);
    model.put("billsAndPoaPage", new PageImpl<>(List.of(draftBill(), uncopyableRejectedBill())));

    final String html = render("application/billing/case-statement-of-account", model);

    assertThat(html).doesNotContain("/case/billing/bill/copy");
    assertThat(html).contains("draft-in-progress-message");
  }

  @Test
  @DisplayName("Statement of account withholds the copy action without the bill function")
  void statementWithholdsCopyWithoutFunction() {
    // The controller refuses the copy without the function, so the link must not be offered.
    final String html =
        render("application/billing/case-statement-of-account", statementModel(false));
    assertThat(html).doesNotContain("/case/billing/bill/copy");
  }

  private Map<String, Object> submittedDetailsModel(final String status) {
    final Map<String, Object> model = new HashMap<>();
    model.put("readOnly", true);
    model.put("billingId", "555");
    model.put("detailsUnavailable", false);
    model.put("assessmentStatus", status);
    model.put("assessmentComplete", true);
    model.put("printDraftBill", true);
    model.put("viewPoaSummary", true);
    return model;
  }

  private String visibleText(final String html) {
    return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ");
  }

  @ParameterizedTest
  @CsvSource({
    "bill, Bill, Submitted",
    "bill, Counsel Bill, Authorised",
    "bill, Bill, Rejected",
    "poa, POA, Submitted",
    "poa, Counsel POA, Authorised",
    "poa, POA, Rejected"
  })
  @DisplayName("Submitted rows link to their own invoice without changing draft or copy actions")
  void statementLinksSelectedInvoice(final String context, final String type, final String status) {
    final Map<String, Object> model = statementModel(true);
    final BillPoaRow selected =
        new BillPoaRow(type, status, null, null, new BigDecimal("123.00"), false, 555L);
    model.put(
        "billsAndPoaPage",
        new PageImpl<>(
            List.of(
                new BillPoaRow("Bill", "Draft", null, null, BigDecimal.TEN, true),
                new BillPoaRow("POA", "Draft", null, null, BigDecimal.TEN, true),
                selected,
                copyableRejectedBill())));

    final String html = render("application/billing/case-statement-of-account", model);
    final String table = html.substring(html.indexOf("id=\"bills-and-poa-table\""));
    final Matcher rows = Pattern.compile("(?s)<tr\\b[^>]*>(.*?)</tr>").matcher(table);
    String selectedRow = null;
    while (rows.find()) {
      if (rows.group(1).contains("123.00")) {
        selectedRow = rows.group(1);
        break;
      }
    }
    assertThat(selectedRow)
        .as("the selected invoice's own row")
        .isNotNull()
        .contains("/case/billing/" + context + "?billing-id=555")
        .contains(">" + type + "</a>")
        .contains(">" + status + "</td>")
        .doesNotContain("/remove", "/copy");
    assertThat(html)
        .contains("href=\"/case/billing/bill\"", "href=\"/case/billing/poa\"")
        .contains("/case/billing/bill/remove", "/case/billing/poa/remove")
        .contains("/case/billing/bill/copy", "name=\"billing-id\" value=\"222\"");
  }

  @ParameterizedTest
  @CsvSource({"bill, Bill", "poa, POA"})
  @DisplayName("Submitted links require the matching maintain permission")
  void submittedLinkRequiresMatchingPermission(final String context, final String type) {
    final Map<String, Object> model = statementModel(true);
    model.put(context.equals("bill") ? "canMaintainBill" : "canMaintainPoa", false);
    model.put(
        "billsAndPoaPage",
        new PageImpl<>(
            List.of(new BillPoaRow(type, "Submitted", null, null, BigDecimal.TEN, false, 555L))));

    final String html = render("application/billing/case-statement-of-account", model);
    assertThat(html).doesNotContain("/case/billing/" + context + "?billing-id=555");
    assertThat(visibleText(html)).contains(type, "Submitted");
  }

  @ParameterizedTest
  @ValueSource(strings = {"Bill", "POA"})
  @DisplayName("An invoice without an ID remains visible with an unavailable indication")
  void submittedRowWithoutIdExplainsUnavailability(final String type) {
    final Map<String, Object> model = statementModel(true);
    model.put(
        "billsAndPoaPage",
        new PageImpl<>(
            List.of(
                new BillPoaRow(type, "Submitted", null, null, new BigDecimal("123.00"), false))));
    final String html = render("application/billing/case-statement-of-account", model);
    final String table =
        html.substring(html.indexOf("id=\"bills-and-poa-table\""), html.lastIndexOf("</table>"));
    assertThat(visibleText(table))
        .contains(type, "Submitted", "123.00")
        .containsPattern("(?i)(unavailable|not available)");
    assertThat(table)
        .doesNotContain("billing-id=", "href=\"/case/billing/bill\"", "href=\"/case/billing/poa\"");
  }

  @ParameterizedTest
  @CsvSource({
    "bill, Submitted",
    "bill, Authorised",
    "bill, Rejected",
    "poa, Submitted",
    "poa, Authorised",
    "poa, Rejected"
  })
  @DisplayName("Submitted details show invoice status and an invoice-specific inline summary")
  void submittedDetailsShowSelectedSummary(final String context, final String status) {
    final String html =
        render("application/billing/" + context + "-details", submittedDetailsModel(status));
    assertThat(html)
        .contains("id=\"" + context + "-status\"")
        .contains(">" + status + "</dd>")
        .contains("/case/billing/" + context + "/summary?billing-id=555")
        .contains("target=\"_blank\"")
        .contains("href=\"/case/billing\"");
    assertThat(visibleText(html)).contains("Cancel and return to case statement of account");
  }

  @ParameterizedTest
  @ValueSource(strings = {"bill", "poa"})
  @DisplayName("Submitted details contain no OPA or Submit action")
  void submittedDetailsAreReadOnly(final String context) {
    final String html =
        render("application/billing/" + context + "-details", submittedDetailsModel("Submitted"));
    assertThat(html)
        .doesNotContain("id=\"" + context + "-details-link\"")
        .doesNotContain("/application/assessments")
        .doesNotContain("id=\"submit-" + context + "-button\"")
        .doesNotContain("action=\"/case/billing/" + context + "/submit\"");
  }

  @ParameterizedTest
  @ValueSource(strings = {"bill", "poa"})
  @DisplayName("Submitted pages do not instruct users to create, enter or submit another claim")
  void submittedDetailsWithholdDraftInstructions(final String context) {
    final String html =
        render("application/billing/" + context + "-details", submittedDetailsModel("Submitted"));
    assertThat(visibleText(html))
        .doesNotContain("Create a bill", "Create a POA")
        .doesNotContain("to enter the line details", "select 'Submit'", "you have just entered");
  }

  @ParameterizedTest
  @ValueSource(strings = {"bill", "poa"})
  @DisplayName("Unavailable historical details explain the problem without offering unsafe actions")
  void unavailableSubmittedDetailsRender(final String context) {
    final Map<String, Object> model = submittedDetailsModel("Authorised");
    model.put("detailsUnavailable", true);
    model.put("printDraftBill", false);
    model.put("viewPoaSummary", false);
    final String html = render("application/billing/" + context + "-details", model);
    assertThat(visibleText(html))
        .contains("Authorised", "Cancel and return to case statement of account")
        .containsPattern("(?i)(unavailable|not available)");
    assertThat(html)
        .contains("href=\"/case/billing\"")
        .doesNotContain("/application/assessments")
        .doesNotContain("id=\"" + context + "-summary-link\"")
        .doesNotContain("id=\"submit-" + context + "-button\"");
  }

  @Test
  @DisplayName("Bill remove confirmation renders through the layout")
  void billRemoveRenders() {
    final String html = render("application/billing/bill-remove", Map.of());
    assertThat(html).contains("confirm-remove-bill-button");
  }

  @Test
  @DisplayName("POA remove confirmation renders through the layout")
  void poaRemoveRenders() {
    final String html = render("application/billing/poa-remove", Map.of());
    assertThat(html).contains("confirm-remove-poa-button");
  }
}
