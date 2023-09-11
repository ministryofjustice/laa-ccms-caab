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
public class ClientAddressDetailsValidator extends AbstractValidator {

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
   * Validates the client address details in the {@link uk.gov.laa.ccms.caab.bean.ClientDetails}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(Object target, Errors errors) {
    ClientDetails clientDetails = (ClientDetails) target;

    if (!clientDetails.getVulnerableClient() && !clientDetails.getNoFixedAbode()) {
      validateRequiredField("country", clientDetails.getCountry(),
          "Country", errors);
      validateRequiredField("houseNameNumber", clientDetails.getHouseNameNumber(),
          "House name / number", errors);
      validatePostcodeFormat(clientDetails.getCountry(), clientDetails.getPostcode(), errors);
      validateRequiredField("addressLine1", clientDetails.getAddressLine1(),
          "Address line 1", errors);
      validateRequiredField("cityTown", clientDetails.getCityTown(),
          "City / Town", errors);
    } else if (clientDetails.getNoFixedAbode()) {
      if ((clientDetails.getCountry() != null && !clientDetails.getCountry().isEmpty())
          || (clientDetails.getHouseNameNumber() != null && !clientDetails.getHouseNameNumber().isEmpty())
          || (clientDetails.getPostcode() != null && !clientDetails.getPostcode().isEmpty())
          || (clientDetails.getAddressLine1() != null && !clientDetails.getAddressLine1().isEmpty())
          || (clientDetails.getAddressLine2() != null && !clientDetails.getAddressLine2().isEmpty())
          || (clientDetails.getCityTown() != null && !clientDetails.getCityTown().isEmpty())
          || (clientDetails.getCounty() != null && !clientDetails.getCounty().isEmpty())) {
        //if any field populated
        errors.reject("invalid.noFixedAbode",
            "You have indicated 'No Fixed Abode'. Please remove main address details or "
                + "uncheck box to amend your entry.");
      }
    }
  }
}
