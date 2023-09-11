package uk.gov.laa.ccms.caab.bean.validators.client;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validator component responsible for validating {@link uk.gov.laa.ccms.caab.bean.ClientDetails}
 * objects.
 */
@Component
public class ClientBasicDetailsValidator extends AbstractValidator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from
   *         {@link uk.gov.laa.ccms.caab.bean.ClientDetails}, {@code false} otherwise.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return ClientDetails.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the client basic details in the {@link ClientDetails}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(Object target, Errors errors) {
    ClientDetails clientDetails = (ClientDetails) target;

    validateRequiredField("title", clientDetails.getTitle(),
        "Title", errors);
    validateRequiredField("surname", clientDetails.getSurname(),
        "Surname", errors);
    validateRequiredField("countryOfOrigin", clientDetails.getCountryOfOrigin(),
        "Country of origin", errors);
    validateRequiredField("gender", clientDetails.getGender(),
        "Gender", errors);
    validateRequiredField("maritalStatus", clientDetails.getMaritalStatus(),
        "Marital status", errors);
  }

}