package uk.gov.laa.ccms.caab.bean.validators.notification;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.ALPHA_NUMERIC_SLASH_SPACE_STRING;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CHARACTER_SET_C;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CHARACTER_SET_F;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.DOUBLE_SPACE;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.FIRST_CHARACTER_MUST_BE_ALPHA;

import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validator component responsible for validating
 * {@link uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria} objects.
 */
@Component
@Slf4j
public class NotificationSearchValidator extends AbstractValidator {

  private static final String SOA_DATE_FORMAT = "dd/MM/yyyy";

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
    // validate the dates - only if they are populated
    validateDateFieldFormats(searchCriteria, errors);
    validateCaseRef(searchCriteria.getCaseReference(), errors);
    validateClientSurname(searchCriteria.getClientSurname(), errors);
    validateProviderCaseRef(searchCriteria.getProviderCaseReference(), errors);
  }


  private void validateDateFieldFormats(NotificationSearchCriteria criteria, Errors errors) {
    Date from = null;
    Date to = null;
    boolean fromDateHasValue = false;
    boolean toDateHasValue = false;
    String fromDate;
    String toDate;
    if (StringUtils.isNotEmpty(criteria.getNotificationFromDateDay())) {
      fromDateHasValue = true;

      validateNumericField("notificationFromDateDay",
          criteria.getNotificationFromDateDay(), "the day", errors);
    }
    if (StringUtils.isNotEmpty(criteria.getNotificationFromDateMonth())) {
      fromDateHasValue = true;
      validateNumericField("notificationFromDateMonth",
          criteria.getNotificationFromDateMonth(), "the month", errors);
    }
    if (StringUtils.isNotEmpty(criteria.getNotificationFromDateYear())) {
      fromDateHasValue = true;
      validateNumericField("notificationFromDateYear",
          criteria.getNotificationFromDateYear(), "the year", errors);
    }

    if (StringUtils.isNotEmpty(criteria.getNotificationToDateDay())) {
      toDateHasValue = true;
      validateNumericField("notificationToDateDay",
          criteria.getNotificationToDateDay(), "the day", errors);
    }
    if (StringUtils.isNotEmpty(criteria.getNotificationToDateMonth())) {
      toDateHasValue = true;
      validateNumericField("notificationToDateMonth",
          criteria.getNotificationToDateMonth(), "the month", errors);
    }
    if (StringUtils.isNotEmpty(criteria.getNotificationToDateYear())) {
      toDateHasValue = true;
      validateNumericField("notificationToDateYear",
          criteria.getNotificationToDateYear(), "the year", errors);
    }
    if (fromDateHasValue && !errors.hasErrors()) {
      fromDate = buildDateString(criteria.getNotificationFromDateDay(),
          criteria.getNotificationFromDateMonth(),
          criteria.getNotificationFromDateYear());
      criteria.setDateFrom(fromDate);
    }
    if (toDateHasValue && !errors.hasErrors()) {
      toDate = buildDateString(criteria.getNotificationToDateDay(),
          criteria.getNotificationToDateMonth(),
          criteria.getNotificationToDateYear());
      criteria.setDateTo(toDate);
    }
    if (StringUtils.isNotEmpty(criteria.getDateFrom())) {
      from = validateValidDateField(criteria.getDateFrom(), "dateFrom", SOA_DATE_FORMAT,
          errors);
      //ensure date is in the past and not the future
      if (from != null) {
        validateDateInPast(from, "dateFrom", "from date", errors);
      }
    }
    if (StringUtils.isNotEmpty(criteria.getDateTo())) {
      to = validateValidDateField(criteria.getDateTo(), "dateTo", SOA_DATE_FORMAT, errors);
      if (to != null) {
        validateDateInPast(to, "dateTo", "to date", errors);
      }
    }
    if (from != null && to != null) {
      validateFromAfterToDates(from, "dateFrom", to, errors);
    }
  }

  private String buildDateString(String notificationDateDay, String notificationDateMonth,
      String notificationDateYear) {
    return notificationDateDay + "/"
        + notificationDateMonth + "/"
        + notificationDateYear;
  }

  private void validateCaseRef(final String caseRef, Errors errors) {
    if (StringUtils.isNotEmpty(caseRef)) {
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
    if (StringUtils.isNotEmpty(clientSurname)) {
      if (!clientSurname.matches(FIRST_CHARACTER_MUST_BE_ALPHA)) {
        errors.rejectValue("clientSurname", "invalid.surname",
            "Your input for 'Client surname' is invalid. "
                + "The first character must be a letter. Please amend your entry.");
      } else if (!clientSurname.matches(CHARACTER_SET_C)) {
        errors.rejectValue("clientSurname", "invalid.surname-char",
            "Your input for 'Client surname' contains an invalid character. "
                + "Please amend your entry.");
      } else if (clientSurname.matches(DOUBLE_SPACE)) {
        errors.rejectValue("clientSurname", "invalid.surname",
            "Your input for 'Client surname'"
                + " contains double spaces. Please amend your entry.");
      }
    }

  }

  private void validateProviderCaseRef(String providerCaseReference, Errors errors) {
    if (StringUtils.isNotEmpty(providerCaseReference)) {
      if (!providerCaseReference.matches(CHARACTER_SET_F)) {
        errors.rejectValue("providerCaseReference",
            "invalid.providerCaseReference-char",
            "Your input for 'Provider case reference' contains an invalid character. "
                + "Please amend your entry.");
      }
    }
  }
}
