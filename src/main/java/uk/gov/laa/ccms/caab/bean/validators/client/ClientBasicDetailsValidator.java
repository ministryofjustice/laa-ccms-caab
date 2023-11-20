package uk.gov.laa.ccms.caab.bean.validators.client;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validator component responsible for validating
 * {@link uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails} objects.
 */
@Component
public class ClientBasicDetailsValidator extends AbstractValidator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from
   *         {@link uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails},
   *         {@code false} otherwise.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return ClientFormDataBasicDetails.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the client basic details in the {@link ClientFormDataBasicDetails}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(Object target, Errors errors) {
    ClientFormDataBasicDetails basicDetails = (ClientFormDataBasicDetails) target;

    validateRequiredField("title", basicDetails.getTitle(),
        "Title", errors);
    validateRequiredField("surname", basicDetails.getSurname(),
        "Surname", errors);
    validateRequiredField("countryOfOrigin", basicDetails.getCountryOfOrigin(),
        "Country of origin", errors);
    validateRequiredField("gender", basicDetails.getGender(),
        "Gender", errors);
    validateRequiredField("maritalStatus", basicDetails.getMaritalStatus(),
        "Marital status", errors);
  }

}
