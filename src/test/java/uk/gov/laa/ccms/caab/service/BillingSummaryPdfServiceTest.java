package uk.gov.laa.ccms.caab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.context.IContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xhtmlrenderer.resource.XMLResource;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.bean.billing.SoaFigureColumn;
import uk.gov.laa.ccms.caab.bean.billing.StatementOfAccountDisplay;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;

@DisplayName("Billing summary PDF service tests")
class BillingSummaryPdfServiceTest {

  private BillingSummaryPdfService service;
  private SpringTemplateEngine templateEngine;

  @BeforeEach
  void setUp() {
    final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    templateEngine = new SpringTemplateEngine();
    templateEngine.setTemplateResolver(resolver);
    service = new BillingSummaryPdfService(templateEngine);
  }

  private AssessmentAttributeDetail attr(final String name, final String value) {
    return new AssessmentAttributeDetail().name(name).value(value);
  }

  private ApplicationDetail ebsCase() {
    return new ApplicationDetail()
        .caseReferenceNumber("300000123")
        .status(new StringDisplayValue().displayValue("Live"));
  }

  private StatementOfAccountDisplay statement() {
    final SoaFigureColumn provider = new SoaFigureColumn();
    provider.setBillsAuthorised(new BigDecimal("500.00"));
    provider.setCostLimitationRemaining(new BigDecimal("1500.00"));
    final StatementOfAccountDisplay display = new StatementOfAccountDisplay();
    display.setProvider(provider);
    return display;
  }

  private AssessmentDetail completedAssessment() {
    return new AssessmentDetail()
        .addEntityTypesItem(
            new AssessmentEntityTypeDetail()
                .name("global")
                .addEntitiesItem(
                    new AssessmentEntityDetail()
                        .name("300000123")
                        .addAttributesItem(attr("BILL_TYPE", "Counsel POA"))
                        .addAttributesItem(attr("BILL_DESCRIPTION", "POA claim"))
                        .addAttributesItem(attr("COURT_ASSESS_BILL", "No"))
                        .addAttributesItem(attr("BILL_SUBMITTED_TOTAL_AMT", "100.00"))
                        .addAttributesItem(attr("BILL_SUBMITTED_TOTAL_VAT", "20.00"))))
        .addEntityTypesItem(
            new AssessmentEntityTypeDetail()
                .name("PROFIT_COST_BILL_LINE")
                .addEntitiesItem(
                    new AssessmentEntityDetail()
                        .name("line-1")
                        .addAttributesItem(attr("PROFIT_COST_DATE", "01-01-2026"))
                        .addAttributesItem(attr("PROFIT_COST_COST_TYPE", "Profit cost"))
                        .addAttributesItem(attr("PROFIT_COST_CATEGORY_OF_WORK", "Preparation"))
                        .addAttributesItem(attr("PROFIT_COST_ACTIVITY", "Drafting"))
                        .addAttributesItem(attr("PROFIT_COST_TIME", "01:30"))
                        .addAttributesItem(attr("PROFIT_COST_ITEM", "1"))
                        .addAttributesItem(attr("PROFIT_COST_RATE", "50.00"))
                        .addAttributesItem(attr("PROFIT_COST_UPLIFT", "0"))
                        .addAttributesItem(attr("PROFIT_COST_NET_CLAIM_INC_UPLFT", "100.00"))
                        .addAttributesItem(attr("PROFIT_COST_VAT", "20"))
                        .addAttributesItem(attr("PROFIT_COST_TOTAL_CLAIM", "120.00"))
                        .addAttributesItem(attr("PROFIT_COST_FEE_EARNER", "A Solicitor"))
                        .addAttributesItem(attr("PROFIT_COST_PRIOR_AUTH", "N/A"))));
  }

  @Test
  @DisplayName("Renders a valid POA PDF from a completed assessment with a bill line")
  void rendersPdf() {
    final byte[] pdf =
        service.generatePoaSummary(
            ebsCase(),
            "Test Firm",
            completedAssessment(),
            statement(),
            new BigDecimal("2000.00"),
            "Draft");

    assertThat(pdf).isNotEmpty();
    // A well-formed PDF starts with the %PDF magic bytes, proving the template rendered and Flying
    // Saucer produced a document (i.e. the rendered HTML was valid XML).
    assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
  }

  @Test
  @DisplayName("Renders a valid bill PDF from a completed assessment with a bill line")
  void rendersBillPdf() throws IOException {
    final byte[] pdf =
        service.generateBillSummary(
            ebsCase(),
            "Test Firm",
            completedAssessment(),
            statement(),
            new BigDecimal("2000.00"),
            "Draft");

    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    final PdfReader reader = new PdfReader(pdf);
    try {
      final String text = new PdfTextExtractor(reader).getTextFromPage(1).replaceAll("\\s+", "");
      assertThat(text)
          .contains(
              "01-JAN-2026ProfitcostPreparationDrafting01:30150.000.00100.0020.00120.00ASolicitorN/A");
    } finally {
      reader.close();
    }
  }

  @Test
  @DisplayName("Embeds the LAA logo in both reports")
  void embedsLogo() {
    // The renderer is handed the HTML as a string with no base URI, so only an inlined image
    // resolves. Assert the image really lands in the PDF rather than trusting the markup.
    for (final byte[] pdf :
        List.of(
            service.generateBillSummary(
                ebsCase(),
                "Test Firm",
                completedAssessment(),
                statement(),
                BigDecimal.ZERO,
                "Draft"),
            service.generatePoaSummary(
                ebsCase(),
                "Test Firm",
                completedAssessment(),
                statement(),
                BigDecimal.ZERO,
                "Draft"))) {
      assertThat(new String(pdf, StandardCharsets.ISO_8859_1))
          .as("the report should carry an embedded image XObject")
          .contains("/Image");
    }

    // Negative control: an image-free document through the same renderer must not contain the
    // marker, so the assertion above is really detecting the logo.
    final SpringTemplateEngine imagelessEngine = mock(SpringTemplateEngine.class);
    when(imagelessEngine.process(eq("pdf/billing-summary"), any(IContext.class)))
        .thenReturn("<html><body><p>No image here</p></body></html>");
    final byte[] imageless =
        new BillingSummaryPdfService(imagelessEngine)
            .generateBillSummary(
                ebsCase(),
                "Test Firm",
                completedAssessment(),
                statement(),
                BigDecimal.ZERO,
                "Draft");
    assertThat(new String(imageless, StandardCharsets.ISO_8859_1)).doesNotContain("/Image");
  }

  @Test
  @DisplayName("Titles the two reports as the legacy PUI handlers do")
  void reportTitles() {
    // The title is the only thing separating the legacy GetBillSummary and GetPoaSummary handlers,
    // so assert it on the context rather than trying to read it back out of the compressed PDF.
    final SpringTemplateEngine mockEngine = mock(SpringTemplateEngine.class);
    when(mockEngine.process(eq("pdf/billing-summary"), any(IContext.class)))
        .thenReturn("<html><body></body></html>");
    final BillingSummaryPdfService mockedService = new BillingSummaryPdfService(mockEngine);

    mockedService.generateBillSummary(
        ebsCase(), "Test Firm", completedAssessment(), statement(), BigDecimal.ZERO, "Draft");
    mockedService.generatePoaSummary(
        ebsCase(), "Test Firm", completedAssessment(), statement(), BigDecimal.ZERO, "Draft");

    final ArgumentCaptor<IContext> captor = ArgumentCaptor.forClass(IContext.class);
    verify(mockEngine, times(2)).process(eq("pdf/billing-summary"), captor.capture());
    assertThat(captor.getAllValues().get(0).getVariable("data")).isEqualTo("BILL REPORT");
    assertThat(captor.getAllValues().get(1).getVariable("data")).isEqualTo("POA REPORT");
  }

  @ParameterizedTest
  @CsvSource({
    "bill, Draft", "poa, Draft", "bill, Under Assessment", "poa, Paid (Unrecouped)",
    "bill,", "poa, ''", "bill, Part-Recouped", "poa, '   '"
  })
  void reportHeaderHasStatusAndCourtAssessedInTheirExactCells(String report, String status)
      throws Exception {
    final SpringTemplateEngine capturingEngine = spy(templateEngine);
    final BillingSummaryPdfService capturingService = new BillingSummaryPdfService(capturingEngine);
    final byte[] pdf =
        report.equals("bill")
            ? capturingService.generateBillSummary(
                ebsCase(), "Test Firm", completedAssessment(), statement(), BigDecimal.ZERO, status)
            : capturingService.generatePoaSummary(
                ebsCase(),
                "Test Firm",
                completedAssessment(),
                statement(),
                BigDecimal.ZERO,
                status);
    final ArgumentCaptor<IContext> context = ArgumentCaptor.forClass(IContext.class);
    verify(capturingEngine).process(eq("pdf/billing-summary"), context.capture());
    final String html = templateEngine.process("pdf/billing-summary", context.getValue());
    final NodeList cells =
        (NodeList)
            XPathFactory.newInstance()
                .newXPath()
                .evaluate(
                    "//*[local-name()='table' and @id='top-section']"
                        + "/*[local-name()='tr'][3]/*[local-name()='td']",
                    XMLResource.load(new StringReader(html)).getDocument(),
                    XPathConstants.NODESET);
    final List<String> values = new ArrayList<>();
    final List<String> classes = new ArrayList<>();
    for (int index = 0; index < cells.getLength(); index++) {
      final Element cell = (Element) cells.item(index);
      values.add(cell.getTextContent().trim());
      classes.add(cell.getAttribute("class"));
    }
    assertThat(values)
        .containsExactly(
            "Bill Type",
            "Counsel POA",
            "Bill Status",
            status == null ? "" : status.trim(),
            "Court Assessed Bill",
            "No",
            "Total Value of Claim (\u00a3)",
            "120.00");
    assertThat(classes)
        .containsExactly(
            "tcell", "value", "tcell", "value", "tcell", "value", "tcell", "value currency");
    final PdfReader reader = new PdfReader(pdf);
    try {
      assertThat(new PdfTextExtractor(reader).getTextFromPage(1).replaceAll("\\s+", ""))
          .contains(
              "BillStatus" + (status == null ? "" : status.replaceAll("\\s+", "")),
              "CourtAssessedBillNo",
              "TotalValueofClaim(\u00a3)120.00");
    } finally {
      reader.close();
    }
  }

  @Test
  @DisplayName("Renders a valid PDF when the assessment has no bill lines")
  void rendersPdfWithNoBillLines() {
    final AssessmentDetail assessment =
        new AssessmentDetail()
            .addEntityTypesItem(
                new AssessmentEntityTypeDetail()
                    .name("global")
                    .addEntitiesItem(new AssessmentEntityDetail().name("300000123")));

    final byte[] pdf =
        service.generatePoaSummary(
            ebsCase(), "Test Firm", assessment, statement(), BigDecimal.ZERO, "Draft");

    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
  }

  @Test
  void completeHistoricalTotalsAndOptionalTimeAreUsable() {
    assertThat(BillingSummaryPdfService.hasUsableHistoricalSummary(completedAssessment())).isTrue();
  }

  @ParameterizedTest
  @CsvSource({"BILL_SUBMITTED_TOTAL_AMT,100.01", "BILL_SUBMITTED_TOTAL_VAT,20.01"})
  void historicalAvailabilityRequiresReconciledSubmittedTotals(String name, String value) {
    AssessmentDetail assessment = completedAssessment();
    assessment.getEntityTypes().getFirst().getEntities().getFirst().getAttributes().stream()
        .filter(attribute -> attribute.getName().equals(name))
        .findFirst()
        .orElseThrow()
        .setValue(value);

    assertThat(BillingSummaryPdfService.hasUsableHistoricalSummary(assessment)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"01:60", "01:00:01", "-1:00", "invalid"})
  void historicalAvailabilityRejectsInvalidOrLossyTime(String value) {
    AssessmentDetail assessment = completedAssessment();
    assessment.getEntityTypes().get(1).getEntities().getFirst().getAttributes().stream()
        .filter(attribute -> attribute.getName().equals("PROFIT_COST_TIME"))
        .findFirst()
        .orElseThrow()
        .setValue(value);

    assertThat(BillingSummaryPdfService.hasUsableHistoricalSummary(assessment)).isFalse();
  }
}
