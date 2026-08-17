package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getAssessmentAttribute;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getAssessmentEntitiesForEntityType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.bean.billing.SoaFigureColumn;
import uk.gov.laa.ccms.caab.bean.billing.StatementOfAccountDisplay;
import uk.gov.laa.ccms.caab.bean.billing.pdf.BillLine;
import uk.gov.laa.ccms.caab.bean.billing.pdf.BottomSectionData;
import uk.gov.laa.ccms.caab.bean.billing.pdf.TopSectionData;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;

/**
 * Builds the bill and POA summary report PDFs from the completed billing assessment, mirroring the
 * legacy PUI's {@code InvoiceGenerationService}. The claim lines and header figures are read from
 * the assessment (the OPA interview wrote them there) and rendered through a Thymeleaf template to
 * PDF.
 *
 * <p>One generator serves both reports, as it does in the legacy PUI: its {@code GetBillSummary}
 * and {@code GetPoaSummary} handlers are identical bar the report title and which assessment they
 * read.
 *
 * <p>Unlike the legacy PUI, the report is generated on demand and streamed rather than stored in
 * S3: it is a stateless view of the current assessment, so there is nothing to persist.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BillingSummaryPdfService {

  private static final String TEMPLATE = "pdf/billing-summary";
  private static final String POA_REPORT_TITLE = "POA REPORT";
  private static final String BILL_REPORT_TITLE = "BILL REPORT";
  private static final String BILL_LINE_ENTITY_SUFFIX = "_BILL_LINE";
  private static final String LOGO_RESOURCE = "static/assets/images/laa.png";

  private static final String ATTR_BILL_TYPE = "BILL_TYPE";
  private static final String ATTR_BILL_DESCRIPTION = "BILL_DESCRIPTION";
  private static final String ATTR_COURT_ASSESS_BILL = "COURT_ASSESS_BILL";
  private static final String ATTR_SUBMITTED_TOTAL_AMOUNT = "BILL_SUBMITTED_TOTAL_AMT";
  private static final String ATTR_SUBMITTED_TOTAL_VAT = "BILL_SUBMITTED_TOTAL_VAT";

  // Per-line attribute suffixes, prefixed by the bill-line entity type minus "_BILL_LINE".
  private static final String SUFFIX_DATE = "_DATE";
  private static final String SUFFIX_COST_TYPE = "_COST_TYPE";
  private static final String SUFFIX_CATEGORY_OF_WORK = "_CATEGORY_OF_WORK";
  private static final String SUFFIX_ACTIVITY = "_ACTIVITY";
  private static final String SUFFIX_TIME = "_TIME";
  private static final String SUFFIX_ITEM = "_ITEM";
  private static final String SUFFIX_RATE = "_RATE";
  private static final String SUFFIX_UPLIFT = "_UPLIFT";
  private static final String SUFFIX_NET_CLAIM = "_NET_CLAIM_INC_UPLFT";
  private static final String SUFFIX_VAT = "_VAT";
  private static final String SUFFIX_TOTAL_CLAIM = "_TOTAL_CLAIM";
  private static final String SUFFIX_FEE_EARNER = "_FEE_EARNER";
  private static final String SUFFIX_PRIOR_AUTH = "_PRIOR_AUTH";

  private static final DateTimeFormatter STORED_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
  private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
  private static final String CURRENCY_REGEX = "\\d+(\\.\\d+)?";

  private final SpringTemplateEngine templateEngine;

  /**
   * The LAA logo, inlined as a data URI. The renderer is given no base URI - the HTML is handed to
   * it as a string - so an embedded image is the one form it can resolve in every environment,
   * packaged jar included. Read once and held, since it never changes.
   */
  private String logoDataUri;

  /**
   * Renders the POA summary report for a completed POA assessment to a PDF.
   *
   * @param ebsCase the case the POA belongs to.
   * @param providerName the submitting provider's name.
   * @param assessment the completed POA assessment.
   * @param statement the case statement of account, for the header cost figures.
   * @param allocatedCostLimit the provider's allocated cost limit.
   * @return the rendered PDF bytes.
   */
  public byte[] generatePoaSummary(
      final ApplicationDetail ebsCase,
      final String providerName,
      final AssessmentDetail assessment,
      final StatementOfAccountDisplay statement,
      final BigDecimal allocatedCostLimit) {

    return generateSummary(
        POA_REPORT_TITLE, ebsCase, providerName, assessment, statement, allocatedCostLimit);
  }

  /**
   * Renders the bill summary report for a completed billing assessment to a PDF.
   *
   * @param ebsCase the case the bill belongs to.
   * @param providerName the submitting provider's name.
   * @param assessment the completed billing assessment.
   * @param statement the case statement of account, for the header cost figures.
   * @param allocatedCostLimit the provider's allocated cost limit.
   * @return the rendered PDF bytes.
   */
  public byte[] generateBillSummary(
      final ApplicationDetail ebsCase,
      final String providerName,
      final AssessmentDetail assessment,
      final StatementOfAccountDisplay statement,
      final BigDecimal allocatedCostLimit) {

    return generateSummary(
        BILL_REPORT_TITLE, ebsCase, providerName, assessment, statement, allocatedCostLimit);
  }

  private byte[] generateSummary(
      final String reportTitle,
      final ApplicationDetail ebsCase,
      final String providerName,
      final AssessmentDetail assessment,
      final StatementOfAccountDisplay statement,
      final BigDecimal allocatedCostLimit) {

    final List<BillLine> billLines = buildBillLines(assessment);
    final TopSectionData top =
        buildTopSection(ebsCase, providerName, assessment, statement, allocatedCostLimit);
    final BottomSectionData bottom = buildBottomSection(billLines);

    final Context context = new Context();
    context.setVariable("data", reportTitle);
    context.setVariable("logo", logoDataUri());
    context.setVariable("topSectionData", top);
    context.setVariable("billLines", billLines);
    context.setVariable("bottomSectionData", bottom);

    final String html = templateEngine.process(TEMPLATE, context);

    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      final ITextRenderer renderer = new ITextRenderer();
      renderer.setDocumentFromString(html);
      renderer.layout();
      renderer.createPDF(out);
      return out.toByteArray();
    } catch (final Exception e) {
      throw new CaabApplicationException("Failed to generate the " + reportTitle + " PDF", e);
    }
  }

  /**
   * The LAA logo as a data URI, or an empty string if it cannot be read - a missing logo should
   * cost the user the report's header image, not the report.
   */
  private synchronized String logoDataUri() {
    if (logoDataUri == null) {
      try (InputStream logo = new ClassPathResource(LOGO_RESOURCE).getInputStream()) {
        logoDataUri =
            "data:image/png;base64," + Base64.getEncoder().encodeToString(logo.readAllBytes());
      } catch (final IOException e) {
        log.warn("Could not read the report logo from {}; rendering without it", LOGO_RESOURCE, e);
        logoDataUri = "";
      }
    }
    return logoDataUri;
  }

  private TopSectionData buildTopSection(
      final ApplicationDetail ebsCase,
      final String providerName,
      final AssessmentDetail assessment,
      final StatementOfAccountDisplay statement,
      final BigDecimal allocatedCostLimit) {

    final AssessmentEntityDetail global = globalEntity(assessment);
    final SoaFigureColumn providerColumn =
        Optional.ofNullable(statement)
            .map(StatementOfAccountDisplay::getProvider)
            .orElseGet(SoaFigureColumn::new);

    return TopSectionData.builder()
        .caseReference(ebsCase.getCaseReferenceNumber())
        .providerName(providerName)
        .billType(attributeValue(global, ATTR_BILL_TYPE))
        .description(attributeValue(global, ATTR_BILL_DESCRIPTION))
        .caseStatus(caseStatus(ebsCase))
        // The legacy report leaves the total POA blank.
        .totalPoa("")
        .totalBills(orZero(providerColumn.getBillsAuthorised()))
        .courtAssessedBill(attributeValue(global, ATTR_COURT_ASSESS_BILL))
        .costLimit(orZero(allocatedCostLimit))
        .availableCostLimit(orZero(providerColumn.getCostLimitationRemaining()))
        .totalValueOfClaim(
            currency(attributeValue(global, ATTR_SUBMITTED_TOTAL_AMOUNT))
                .add(currency(attributeValue(global, ATTR_SUBMITTED_TOTAL_VAT))))
        .build();
  }

  private List<BillLine> buildBillLines(final AssessmentDetail assessment) {
    final List<BillLine> lines = new ArrayList<>();
    if (assessment == null || assessment.getEntityTypes() == null) {
      return lines;
    }

    for (final AssessmentEntityTypeDetail entityType : assessment.getEntityTypes()) {
      if (entityType.getName() == null
          || !entityType.getName().endsWith(BILL_LINE_ENTITY_SUFFIX)
          || entityType.getEntities() == null) {
        continue;
      }
      final String prefix =
          entityType
              .getName()
              .substring(0, entityType.getName().length() - BILL_LINE_ENTITY_SUFFIX.length());

      for (final AssessmentEntityDetail entity : entityType.getEntities()) {
        final LocalDate date = parseDate(attributeValue(entity, prefix + SUFFIX_DATE));
        // The legacy report drops any line without a parseable date.
        if (date == null) {
          continue;
        }
        lines.add(
            BillLine.builder()
                .date(date)
                .dateDisplay(DISPLAY_DATE.format(date).toUpperCase(Locale.UK))
                .costType(attributeValue(entity, prefix + SUFFIX_COST_TYPE))
                .categoryOfWork(attributeValue(entity, prefix + SUFFIX_CATEGORY_OF_WORK))
                .workConducted(attributeValue(entity, prefix + SUFFIX_ACTIVITY))
                .hoursMinClaimed(attributeValue(entity, prefix + SUFFIX_TIME))
                .itemsClaimed(attributeValue(entity, prefix + SUFFIX_ITEM))
                .rateClaimed(currency(attributeValue(entity, prefix + SUFFIX_RATE)))
                .upliftClaimed(currency(attributeValue(entity, prefix + SUFFIX_UPLIFT)))
                .netClaim(currency(attributeValue(entity, prefix + SUFFIX_NET_CLAIM)))
                .vat(currency(attributeValue(entity, prefix + SUFFIX_VAT)))
                .totalClaim(currency(attributeValue(entity, prefix + SUFFIX_TOTAL_CLAIM)))
                .feeEarner(attributeValue(entity, prefix + SUFFIX_FEE_EARNER))
                .priorAuthority(attributeValue(entity, prefix + SUFFIX_PRIOR_AUTH))
                .build());
      }
    }

    lines.sort(
        Comparator.comparing(BillLine::getDate)
            .thenComparing(line -> line.getCostType() == null ? "" : line.getCostType()));
    int lineNumber = 1;
    for (final BillLine line : lines) {
      line.setLineNumber(lineNumber++);
    }
    return lines;
  }

  private BottomSectionData buildBottomSection(final List<BillLine> billLines) {
    BigDecimal totalNet = BigDecimal.ZERO;
    BigDecimal totalClaim = BigDecimal.ZERO;
    long totalMinutes = 0;
    for (final BillLine line : billLines) {
      totalNet = totalNet.add(orZero(line.getNetClaim()));
      totalClaim = totalClaim.add(orZero(line.getTotalClaim()));
      totalMinutes += minutes(line.getHoursMinClaimed());
    }
    return BottomSectionData.builder()
        .totalHoursMinutesClaimed(String.format("%02d:%02d", totalMinutes / 60, totalMinutes % 60))
        .totalNetClaim(totalNet)
        .totalClaimIncVatAndUplift(totalClaim)
        .build();
  }

  private AssessmentEntityDetail globalEntity(final AssessmentDetail assessment) {
    return getAssessmentEntitiesForEntityType(assessment, AssessmentEntityType.GLOBAL).stream()
        .findFirst()
        .orElse(null);
  }

  private String attributeValue(final AssessmentEntityDetail entity, final String name) {
    return Optional.ofNullable(getAssessmentAttribute(entity, name))
        .map(AssessmentAttributeDetail::getValue)
        .orElse("");
  }

  private String caseStatus(final ApplicationDetail ebsCase) {
    return Optional.ofNullable(ebsCase.getStatus())
        .map(StringDisplayValue::getDisplayValue)
        .orElse("");
  }

  private LocalDate parseDate(final String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value.trim(), STORED_DATE);
    } catch (final DateTimeParseException e) {
      return null;
    }
  }

  /**
   * Parses an OPA currency value, mirroring the legacy PUI: commas stripped, non-numeric to zero.
   */
  private BigDecimal currency(final String value) {
    if (value == null) {
      return BigDecimal.ZERO;
    }
    final String cleaned = value.replace(",", "").trim();
    return cleaned.matches(CURRENCY_REGEX) ? new BigDecimal(cleaned) : BigDecimal.ZERO;
  }

  /**
   * Parses an {@code HH:MM} claimed-time value to minutes; anything unparseable contributes zero.
   */
  private long minutes(final String hoursMinutes) {
    if (hoursMinutes == null || !hoursMinutes.matches("\\d+:\\d{1,2}")) {
      return 0;
    }
    final String[] parts = hoursMinutes.split(":");
    return Long.parseLong(parts[0]) * 60 + Long.parseLong(parts[1]);
  }

  private BigDecimal orZero(final BigDecimal amount) {
    return amount == null ? BigDecimal.ZERO : amount;
  }
}
