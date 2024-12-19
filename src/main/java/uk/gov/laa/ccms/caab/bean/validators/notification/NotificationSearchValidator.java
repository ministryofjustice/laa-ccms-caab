package uk.gov.laa.ccms.caab.bean.validators.notification;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.ALPHA_NUMERIC_SLASH_SPACE_STRING;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CHARACTER_SET_C;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CHARACTER_SET_F;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.DOUBLE_SPACE;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.FIRST_CHARACTER_MUST_BE_ALPHA;

import java.time.LocalDate;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

/**
 * Validator component responsible for validating
 * {@link uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria} objects.
 */
@Component
@Slf4j
public class NotificationSearchValidator extends AbstractValidator {

  private static final String ISO_DATE_FORMAT = "yyyy-MM-dd";
  private static final String DATE_FORMAT = "dd/MM/yyyy";

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from
   *     {@link uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria}, {@code false} otherwise.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return NotificationSearchCriteria.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the client address search details in the
   * {@link uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(Object target, Errors errors) {
    NotificationSearchCriteria searchCriteria = (NotificationSearchCriteria) target;
    // check that at least one field is populated
    validateAtLeastOneFieldValidated(target, errors);
    // validate the dates - only if they are populated

    if (!errors.hasErrors()) {
      validateDateFieldFormats(searchCriteria, errors);
      validateCaseRef(searchCriteria.getCaseReference(), errors);
      validateClientSurname(searchCriteria.getClientSurname(), errors);
      validateProviderCaseRef(searchCriteria.getProviderCaseReference(), errors);
    }
  }

  private void validateAtLeastOneFieldValidated(Object target, Errors errors) {
    if (!errors.hasErrors()) {
      NotificationSearchCriteria searchCriteria = (NotificationSearchCriteria) target;
      if (!StringUtils.hasText(searchCriteria.getAssignedToUserId())
          && !StringUtils.hasText(searchCriteria.getNotificationFromDate())
          && !StringUtils.hasText(searchCriteria.getNotificationToDate())
          && !StringUtils.hasText(searchCriteria.getProviderCaseReference())
          && !StringUtils.hasText(searchCriteria.getCaseReference())
          && !StringUtils.hasText(searchCriteria.getClientSurname())
          && searchCriteria.getFeeEarnerId() == null) {
        errors.reject("invalid.criteria",
            "You must provide at least one search criteria below. "
                + "Please amend your entry.");
      }
    }
  }

  private void validateDateFieldFormats(NotificationSearchCriteria criteria, Errors errors) {
    String dateFromFieldName = "notificationFromDate";
    String dateFromDisplayName = "date from";

    String dateToFieldName = "notificationToDate";
    String dateToDisplayName = "date to";

    // Validate from
    Date from = null;
    try {
      String dateFrom = criteria.getNotificationFromDate();
      from = validateValidDateField(dateFrom, dateFromFieldName, dateFromDisplayName,
          DATE_FORMAT, errors);
    } catch (CaabApplicationException e) {
      reportInvalidDate(dateFromFieldName, dateFromDisplayName, errors);
    }

    // Validate to
    Date to = null;
    try {
      String dateTo = criteria.getNotificationToDate();
      to = validateValidDateField(dateTo, dateToFieldName, dateToDisplayName,
          DATE_FORMAT, errors);
    } catch (CaabApplicationException e) {
      reportInvalidDate(dateToFieldName, dateToDisplayName, errors);
    }

    //  Validate from date is not in the future
    if (from != null) {
      validateDateInPast(from, dateFromFieldName, dateFromDisplayName, errors);
    }

    // Validate to date is not in the future
    if (to != null) {
      validateDateInPast(to, dateToFieldName, dateToDisplayName, errors);
    }

    // Validate that To is after From date
    if (from != null && to != null) {
      validateFromBeforeToDates(from, dateFromFieldName, to, errors);
      validateLessThanThreeYearsBetweenDates(from, dateToFieldName, to, errors);
    }

  }


  protected void validateLessThanThreeYearsBetweenDates(final Date fromDate, final String fieldName,
      final Date toDate, Errors errors) {

    LocalDate fromLocalDate = fromDate.toInstant().atZone(java.time.ZoneId.systemDefault())
        .toLocalDate();
    LocalDate toLocalDate = toDate.toInstant().atZone(java.time.ZoneId.systemDefault())
        .toLocalDate();
    LocalDate threeYearsAfterFrom = fromLocalDate.plusYears(3L);

    // If three years after From date is still before the To date, then the date range is more than
    //  three years
    if (threeYearsAfterFrom.isBefore(toLocalDate)) {
      errors.rejectValue(fieldName, "validation.date.range-exceeds-three-years.error-text");
    }
  }
/*

  @Deprecated
  private void validateDateFieldFormatsOld(NotificationSearchCriteria criteria, Errors errors) {
    String dateFromFieldName = "notificationFromDate";
    String dateFromDisplayName = "date from";

    String dateToFieldName = "notificationToDate";
    String dateToDisplayName = "date to";

    Triple<String, String, String> fromYear = Triple.of(criteria.getNotificationFromDateYear(),
        "year", "notificationFromDateYear");
    Triple<String, String, String> fromMonth = Triple.of(criteria.getNotificationFromDateMonth(),
        "month", "notificationFromDateMonth");
    Triple<String, String, String> fromDay = Triple.of(criteria.getNotificationFromDateDay(),
        "day", "notificationFromDateDay");

    Triple<String, String, String> toYear = Triple.of(criteria.getNotificationToDateYear(),
        "year", "notificationToDateYear");
    Triple<String, String, String> toMonth = Triple.of(criteria.getNotificationToDateMonth(),
        "month", "notificationToDateMonth");
    Triple<String, String, String> toDay = Triple.of(criteria.getNotificationToDateDay(),
        "day", "notificationToDateDay");

    boolean fromDateFullyPopulated = dateFullyPopulated(fromYear.getLeft(), fromMonth.getLeft(),
        fromDay.getLeft());
    boolean toDateFullyPopulated = dateFullyPopulated(toYear.getLeft(), toMonth.getLeft(),
        toDay.getLeft());

    boolean fromDateEmpty = dateEmpty(fromYear.getLeft(), fromMonth.getLeft(), fromDay.getLeft());
    boolean toDateEmpty = dateEmpty(toYear.getLeft(), toMonth.getLeft(), toDay.getLeft());

    if (!fromDateEmpty && !fromDateFullyPopulated) {
      reportMissingDateFields(dateFromFieldName, dateFromDisplayName, errors);
    }

    Date from = null;

    if (fromDateFullyPopulated) {
      validateNumericField(fromYear.getRight(), fromYear.getLeft(), fromYear.getMiddle(), errors);
      validateNumericField(fromMonth.getRight(), fromMonth.getLeft(),
          fromMonth.getMiddle(), errors);
      validateNumericField(fromDay.getRight(), fromDay.getLeft(), fromDay.getMiddle(), errors);

      try {
        String dateFrom = criteria.getNotificationFromDate();
        from = validateValidDateField(dateFrom, dateFromFieldName, dateFromDisplayName,
            ISO_DATE_FORMAT, errors);
      } catch (CaabApplicationException e) {
        reportInvalidDate(dateFromFieldName, dateFromDisplayName, errors);
      }

      //ensure date is in the past and not the future
      if (from != null) {
        validateDateInPast(from, dateFromFieldName, dateFromDisplayName, errors);
      }
    }

    if (!toDateEmpty && !toDateFullyPopulated) {
      reportMissingDateFields(dateToFieldName, dateToDisplayName, errors);
    }

    Date to = null;

    if (toDateFullyPopulated) {
      validateNumericField(toYear.getRight(), toYear.getLeft(), toYear.getMiddle(), errors);
      validateNumericField(toMonth.getRight(), toMonth.getLeft(), toMonth.getMiddle(), errors);
      validateNumericField(toDay.getRight(), toDay.getLeft(), toDay.getMiddle(), errors);

      try {
        String dateTo = criteria.getNotificationToDate();
        to = validateValidDateField(dateTo, dateToFieldName, dateFromDisplayName,
            ISO_DATE_FORMAT, errors);
      } catch (CaabApplicationException e) {
        reportInvalidDate(dateToFieldName, dateToDisplayName, errors);
      }

      if (to != null) {
        validateDateInPast(to, dateToFieldName, dateToDisplayName, errors);
      }
    }

    if (from != null && to != null) {
      validateFromAfterToDates(from, dateFromFieldName, to, errors);
    }
  }
*/
  
  private void validateCaseRef(final String caseRef, Errors errors) {
    if (StringUtils.hasText(caseRef)) {
      //check no double spaces
      if (!caseRef.matches(ALPHA_NUMERIC_SLASH_SPACE_STRING)) {
        errors.rejectValue("caseReference", "invalid.case-ref",
            "Your input for 'LAA application / case reference' contains an "
                + "invalid character. Please amend your entry using numbers, "
                + "letters and spaces only");
      } else if (caseRef.matches(DOUBLE_SPACE)) {
        errors.rejectValue("caseReference", "invalid.case-ref",
            "Your input for 'LAA application / case reference'"
                + " contains double spaces. Please amend your entry.");
      }
    }
  }

  private void validateClientSurname(String clientSurname, Errors errors) {
    if (StringUtils.hasText(clientSurname)) {
      if (!clientSurname.matches(FIRST_CHARACTER_MUST_BE_ALPHA)) {
        errors.rejectValue("clientSurname", "invalid.surname",
            "Your input for 'Client surname' is invalid. "
                + "The first character must be a letter. Please amend your entry.");
      } else if (!clientSurname.matches(CHARACTER_SET_C)) {
        errors.rejectValue("clientSurname", "invalid.surname-char",
            "Your input for 'Client surname' contains an invalid character. "
                + "Please amend your entry.");
      } else if (patternMatches(clientSurname, DOUBLE_SPACE)) {
        errors.rejectValue("clientSurname", "invalid.surname",
            "Your input for 'Client surname'"
                + " contains double spaces. Please amend your entry.");
      }
    }
  }

  private void validateProviderCaseRef(String providerCaseReference, Errors errors) {
    if (StringUtils.hasText(providerCaseReference)) {
      if (!providerCaseReference.matches(CHARACTER_SET_F)) {
        errors.rejectValue("providerCaseReference",
            "invalid.providerCaseReference-char",
            "Your input for 'Provider case reference' contains an invalid character. "
                + "Please amend your entry.");
      }
    }
  }
}
