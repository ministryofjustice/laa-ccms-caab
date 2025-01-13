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

    boolean fromDateEmpty = criteria.getNotificationFromDate() == null
        || !StringUtils.hasText(criteria.getNotificationFromDate());
    boolean toDateEmpty = criteria.getNotificationToDate() == null
        || !StringUtils.hasText(criteria.getNotificationToDate());

    // Validate from
    Date from = null;
    if (!fromDateEmpty) {
      try {
        String dateFrom = criteria.getNotificationFromDate();
        from = validateValidDateField(dateFrom, dateFromFieldName, dateFromDisplayName,
            DATE_FORMAT, errors);
      } catch (CaabApplicationException e) {
        reportInvalidDate(dateFromFieldName, dateFromDisplayName, errors);
      }
    }

    // Validate to
    Date to = null;
    if (!toDateEmpty) {
      try {
        String dateTo = criteria.getNotificationToDate();
        to = validateValidDateField(dateTo, dateToFieldName, dateToDisplayName,
            DATE_FORMAT, errors);
      } catch (CaabApplicationException e) {
        reportInvalidDate(dateToFieldName, dateToDisplayName, errors);
      }
    }

    boolean fromInvalid = fromDateEmpty || from == null;
    //  Validate from date is not in the future
    if (!fromInvalid) {
      validateDateInPast(from, dateFromFieldName, dateFromDisplayName, errors);
    }

    boolean toInvalid = toDateEmpty || to == null;
    // Validate to date is not in the future
    if (!toInvalid) {
      validateDateInPast(to, dateToFieldName, dateToDisplayName, errors);
    }

    // Validate that To is after From date
    if (!fromInvalid && !toInvalid) {
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
