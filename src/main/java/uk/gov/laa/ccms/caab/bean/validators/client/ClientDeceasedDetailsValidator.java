package uk.gov.laa.ccms.caab.bean.validators.client;

import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientFormDataDeceasedDetails;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validator component responsible for validating
 * {@link uk.gov.laa.ccms.caab.bean.ClientFormDataDeceasedDetails} objects.
 */
@Component
@Slf4j
public class ClientDeceasedDetailsValidator extends AbstractValidator {

  private static final String SOA_DATE_FORMAT = "dd/MM/yyyy";

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from
   *     {@link uk.gov.laa.ccms.caab.bean.ClientFormDataDeceasedDetails},
   *     {@code false} otherwise.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return ClientFormDataDeceasedDetails.class.isAssignableFrom(clazz);
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
    ClientFormDataDeceasedDetails deceasedDetails = (ClientFormDataDeceasedDetails) target;
    validateDateFieldFormats(deceasedDetails, errors);
  }

  private void validateDateFieldFormats(
      ClientFormDataDeceasedDetails deceasedDetails, Errors errors) {

    validateNumericField("dodDay", deceasedDetails.getDodDay(), "the day", errors);
    validateNumericField("dodMonth", deceasedDetails.getDodMonth(), "the month", errors);
    validateNumericField("dodYear", deceasedDetails.getDodYear(), "the year", errors);

    if (!errors.hasErrors()) {
      deceasedDetails.setDateOfDeath(
          buildDateString(
              deceasedDetails.getDodDay(),
              deceasedDetails.getDodMonth(),
              deceasedDetails.getDodYear()));

      Date date = validateValidDateField(
          deceasedDetails.getDateOfDeath(),
          "dateOfDeath",
          "date of death", SOA_DATE_FORMAT,  errors);
      validateDateInPast(date, "dateOfDeath", "date of death", errors);
    }
  }

  private String buildDateString(String day, String month,
      String year) {
    return String.format("%s/%s/%s", day, month, year);
  }
}
