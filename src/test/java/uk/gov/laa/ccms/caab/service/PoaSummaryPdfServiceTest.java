package uk.gov.laa.ccms.caab.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.bean.billing.SoaFigureColumn;
import uk.gov.laa.ccms.caab.bean.billing.StatementOfAccountDisplay;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;

@DisplayName("POA summary PDF service tests")
class PoaSummaryPdfServiceTest {

  private PoaSummaryPdfService service;

  @BeforeEach
  void setUp() {
    final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    final SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    service = new PoaSummaryPdfService(engine);
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

  @Test
  @DisplayName("Renders a valid PDF from a completed assessment with a bill line")
  void rendersPdf() {
    final AssessmentDetail assessment =
        new AssessmentDetail()
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

    final byte[] pdf =
        service.generatePoaSummary(
            ebsCase(), "Test Firm", assessment, statement(), new BigDecimal("2000.00"));

    assertThat(pdf).isNotEmpty();
    // A well-formed PDF starts with the %PDF magic bytes, proving the template rendered and Flying
    // Saucer produced a document (i.e. the rendered HTML was valid XML).
    assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
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
            ebsCase(), "Test Firm", assessment, statement(), BigDecimal.ZERO);

    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
  }
}
