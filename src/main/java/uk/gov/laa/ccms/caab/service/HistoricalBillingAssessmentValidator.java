package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getAssessmentAttribute;
import static uk.gov.laa.ccms.caab.util.AssessmentUtil.getAssessmentEntitiesForEntityType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType;

/** Validates and normalises fresh retained-invoice projections without changing stored data. */
@Component
public class HistoricalBillingAssessmentValidator {
  private static final String BILL_LINE_ENTITY_SUFFIX = "_BILL_LINE";
  private static final String ATTR_COURT_ASSESS_BILL = "COURT_ASSESS_BILL";
  private static final String ATTR_SUBMITTED_TOTAL_AMOUNT = "BILL_SUBMITTED_TOTAL_AMT";
  private static final String ATTR_SUBMITTED_TOTAL_VAT = "BILL_SUBMITTED_TOTAL_VAT";
  private static final String SUFFIX_DATE = "_DATE";
  private static final String SUFFIX_TIME = "_TIME";
  private static final String SUFFIX_RATE = "_RATE";
  private static final String SUFFIX_UPLIFT = "_UPLIFT";
  private static final String SUFFIX_NET_CLAIM = "_NET_CLAIM_INC_UPLFT";
  private static final String SUFFIX_VAT = "_VAT";
  private static final String SUFFIX_TOTAL_CLAIM = "_TOTAL_CLAIM";
  private static final DateTimeFormatter STORED_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
  private static final String CURRENCY_REGEX = "\\d+(\\.\\d+)?";

  /**
   * Normalises only a fresh historical projection; draft assessments retain their stored values.
   *
   * @param assessment the in-memory selected-invoice projection
   * @return whether the complete submitted claim can be rendered faithfully
   */
  public boolean prepareHistoricalSummary(final AssessmentDetail assessment) {
    if (!validHistoricalStructure(assessment)) {
      return false;
    }
    final List<AssessmentEntityDetail> globals =
        getAssessmentEntitiesForEntityType(assessment, AssessmentEntityType.GLOBAL);
    if (globals.size() != 1 || !normaliseCourtFlag(globals.getFirst())) {
      return false;
    }
    for (final AssessmentEntityTypeDetail type : assessment.getEntityTypes()) {
      if (!type.getName().endsWith(BILL_LINE_ENTITY_SUFFIX)) {
        continue;
      }
      if (type.getName().equals("OVERALL_ADJ_BILL_LINE")) {
        type.getEntities().removeIf(HistoricalBillingAssessmentValidator::assessedOnlyAdjustment);
      }
      final String prefix =
          type.getName().substring(0, type.getName().length() - BILL_LINE_ENTITY_SUFFIX.length());
      for (final AssessmentEntityDetail entity : type.getEntities()) {
        final AssessmentAttributeDetail time = getAssessmentAttribute(entity, prefix + SUFFIX_TIME);
        if (time != null && time.getValue() != null && !time.getValue().isBlank()) {
          final Long minutes = historicalMinutes(time.getValue());
          if (minutes == null) {
            return false;
          }
          time.setValue(String.format("%02d:%02d", minutes / 60, minutes % 60));
        }
        if (getAssessmentAttribute(entity, prefix + SUFFIX_VAT) == null) {
          final String net = attributeValue(entity, prefix + SUFFIX_NET_CLAIM);
          final String gross = attributeValue(entity, prefix + SUFFIX_TOTAL_CLAIM);
          final String vatClaim = attributeValue(entity, prefix + "_VAT_CLAIM");
          if (isCurrency(net)
              && isCurrency(gross)
              && isCurrency(vatClaim)
              && currency(net).signum() > 0
              && currency(net).compareTo(currency(gross)) == 0
              && currency(vatClaim).signum() == 0) {
            entity.addAttributesItem(
                new AssessmentAttributeDetail()
                    .name(prefix + SUFFIX_VAT)
                    .type("number")
                    .value("0"));
          }
        }
      }
    }
    return hasUsableHistoricalSummary(assessment);
  }

  private static boolean assessedOnlyAdjustment(final AssessmentEntityDetail entity) {
    return entity.getAttributes().stream().anyMatch(a -> a.getName().startsWith("ASSESSED_"))
        && List.of(SUFFIX_DATE, SUFFIX_NET_CLAIM, SUFFIX_VAT, SUFFIX_TOTAL_CLAIM, "_VAT_CLAIM")
            .stream()
            .noneMatch(suffix -> getAssessmentAttribute(entity, "OVERALL_ADJ" + suffix) != null);
  }

  private static boolean normaliseCourtFlag(final AssessmentEntityDetail global) {
    Boolean courtAssessed = null;
    for (final String name :
        List.of(ATTR_COURT_ASSESS_BILL, "COURT_ASSESSED_BILL", "GB_CLAIM_ASSESSED_BY_COURT")) {
      final String value = attributeValue(global, name).trim();
      if (value.isEmpty()) {
        continue;
      }
      final boolean yes = value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true");
      final boolean no = value.equalsIgnoreCase("no") || value.equalsIgnoreCase("false");
      if ((!yes && !no) || (courtAssessed != null && courtAssessed != yes)) {
        return false;
      }
      courtAssessed = yes;
    }
    if (courtAssessed != null) {
      final AssessmentAttributeDetail canonical =
          getAssessmentAttribute(global, ATTR_COURT_ASSESS_BILL);
      final String value = courtAssessed ? "Yes" : "No";
      if (canonical == null) {
        global.addAttributesItem(
            new AssessmentAttributeDetail().name(ATTR_COURT_ASSESS_BILL).type("text").value(value));
      } else {
        canonical.setValue(value);
      }
    }
    return true;
  }

  private static boolean validHistoricalStructure(final AssessmentDetail assessment) {
    return assessment != null
        && assessment.getEntityTypes() != null
        && assessment.getEntityTypes().stream()
            .allMatch(
                type ->
                    type != null
                        && type.getName() != null
                        && type.getEntities() != null
                        && type.getEntities().stream()
                            .allMatch(
                                entity ->
                                    entity != null
                                        && entity.getAttributes() != null
                                        && entity.getAttributes().stream()
                                            .allMatch(
                                                attribute ->
                                                    attribute != null
                                                        && attribute.getName() != null)));
  }

  private static Long historicalMinutes(final String value) {
    if (value == null || value.isBlank()) {
      return 0L;
    }
    if (!value.trim().matches("\\d+:[0-5]\\d(?::00)?")) {
      return null;
    }
    final String[] parts = value.trim().split(":");
    try {
      return Math.addExact(
          Math.multiplyExact(Long.parseLong(parts[0]), 60), Long.parseLong(parts[1]));
    } catch (final ArithmeticException | NumberFormatException error) {
      return null;
    }
  }

  /**
   * Historical reports must contain real totals and complete dated claim lines, rather than
   * silently rendering the zero defaults and omitted lines used by draft reports.
   */
  boolean hasUsableHistoricalSummary(final AssessmentDetail assessment) {
    if (!validHistoricalStructure(assessment)) {
      return false;
    }
    final List<AssessmentEntityDetail> globals =
        getAssessmentEntitiesForEntityType(assessment, AssessmentEntityType.GLOBAL);
    if (globals.size() != 1
        || !isCurrency(attributeValue(globals.getFirst(), ATTR_SUBMITTED_TOTAL_AMOUNT))
        || !isCurrency(attributeValue(globals.getFirst(), ATTR_SUBMITTED_TOTAL_VAT))) {
      return false;
    }
    boolean hasLine = false;
    BigDecimal net = BigDecimal.ZERO;
    BigDecimal gross = BigDecimal.ZERO;
    long minutes = 0;
    for (final AssessmentEntityTypeDetail type : assessment.getEntityTypes()) {
      if (type.getName() == null || !type.getName().endsWith(BILL_LINE_ENTITY_SUFFIX)) {
        continue;
      }
      final String prefix =
          type.getName().substring(0, type.getName().length() - BILL_LINE_ENTITY_SUFFIX.length());
      for (final AssessmentEntityDetail entity : type.getEntities()) {
        hasLine = true;
        final String storedDate = attributeValue(entity, prefix + SUFFIX_DATE);
        final LocalDate date = parseDate(storedDate);
        // SMART parsing normalizes dates such as 31 February; historical reports must not.
        if (date == null
            || !STORED_DATE.format(date).equals(storedDate.trim())
            || !isCurrency(attributeValue(entity, prefix + SUFFIX_NET_CLAIM))
            || !isCurrency(attributeValue(entity, prefix + SUFFIX_VAT))
            || !isCurrency(attributeValue(entity, prefix + SUFFIX_TOTAL_CLAIM))
            || !isOptionalCurrency(attributeValue(entity, prefix + SUFFIX_RATE))
            || !isOptionalCurrency(attributeValue(entity, prefix + SUFFIX_UPLIFT))) {
          return false;
        }
        final Long lineMinutes = historicalMinutes(attributeValue(entity, prefix + SUFFIX_TIME));
        if (lineMinutes == null || lineMinutes > Long.MAX_VALUE - minutes) {
          return false;
        }
        minutes += lineMinutes;
        net = net.add(currency(attributeValue(entity, prefix + SUFFIX_NET_CLAIM)));
        gross = gross.add(currency(attributeValue(entity, prefix + SUFFIX_TOTAL_CLAIM)));
      }
    }
    final AssessmentEntityDetail global = globals.getFirst();
    return hasLine
        && net.compareTo(currency(attributeValue(global, ATTR_SUBMITTED_TOTAL_AMOUNT))) == 0
        && gross.compareTo(
                currency(attributeValue(global, ATTR_SUBMITTED_TOTAL_AMOUNT))
                    .add(currency(attributeValue(global, ATTR_SUBMITTED_TOTAL_VAT))))
            == 0;
  }

  private static boolean isCurrency(final String value) {
    return value != null && value.replace(",", "").trim().matches(CURRENCY_REGEX);
  }

  private static boolean isOptionalCurrency(final String value) {
    return value == null || value.isBlank() || isCurrency(value);
  }

  private static String attributeValue(final AssessmentEntityDetail entity, final String name) {
    return Optional.ofNullable(getAssessmentAttribute(entity, name))
        .map(AssessmentAttributeDetail::getValue)
        .orElse("");
  }

  private static LocalDate parseDate(final String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value.trim(), STORED_DATE);
    } catch (final DateTimeParseException e) {
      return null;
    }
  }

  private static BigDecimal currency(final String value) {
    if (value == null) {
      return BigDecimal.ZERO;
    }
    final String cleaned = value.replace(",", "").trim();
    return cleaned.matches(CURRENCY_REGEX) ? new BigDecimal(cleaned) : BigDecimal.ZERO;
  }
}
