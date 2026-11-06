package uk.gov.laa.ccms.caab.bean.validators.client;

import static uk.gov.laa.ccms.caab.util.DateUtils.COMPONENT_DATE_PATTERN;
import static uk.gov.laa.ccms.caab.util.DateUtils.convertToLocalDate;

import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataDeceasedDetails;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validator component responsible for validating {@link
 * uk.gov.laa.ccms.caab.bean.ClientFormDataDeceasedDetails} objects.
 */
@Component
@Slf4j
public class ClientDeceasedDetailsValidator extends AbstractValidator {

  private static final String DATE_OF_DEATH_BEFORE_BIRTH =
      "The date you have entered for date of death is before the client's date of birth. "
          + "Please amend your entry.";

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from {@link
   *     uk.gov.laa.ccms.caab.bean.ClientFormDataDeceasedDetails}, {@code false} otherwise.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return ClientFormDataDeceasedDetails.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the client address search details in the {@link
   * uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(Object target, Errors errors) {
    ClientFormDataDeceasedDetails deceasedDetails = (ClientFormDataDeceasedDetails) target;
    validateDateFieldFormats(deceasedDetails, errors);
  }

  /**
   * Rejects a date of death before the client's date of birth. A death on the date of birth is
   * allowed.
   *
   * @param deceasedDetails The already validated deceased details.
   * @param basicDetails The client's basic details holding the date of birth.
   * @param errors The Errors object to store validation errors.
   */
  public void validateNotBeforeDateOfBirth(
      ClientFormDataDeceasedDetails deceasedDetails,
      ClientFormDataBasicDetails basicDetails,
      Errors errors) {
    if (errors.hasErrors()
        || basicDetails == null
        || !StringUtils.hasText(basicDetails.getDateOfBirth())) {
      return;
    }

    if (convertToLocalDate(deceasedDetails.getDateOfDeath())
        .isBefore(convertToLocalDate(basicDetails.getDateOfBirth()))) {
      errors.rejectValue("dateOfDeath", "invalid.input", DATE_OF_DEATH_BEFORE_BIRTH);
    }
  }

  private void validateDateFieldFormats(
      ClientFormDataDeceasedDetails deceasedDetails, Errors errors) {

    String fieldName = "dateOfDeath";
    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors, fieldName, "required.dod", "Please complete 'date of death'");

    if (StringUtils.hasText(deceasedDetails.getDateOfDeath())) {
      Date date =
          validateValidDateField(
              deceasedDetails.getDateOfDeath(),
              fieldName,
              "date of death",
              COMPONENT_DATE_PATTERN,
              errors);
      if (!errors.hasErrors()) {
        validateDateInPast(date, fieldName, "date of death", errors);
      }
    }
  }
}
