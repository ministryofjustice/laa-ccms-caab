package uk.gov.laa.ccms.caab.bean.validators.client;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.ClientDetails;

/**
 * Validator component responsible for validating {@link uk.gov.laa.ccms.caab.bean.ClientDetails}
 * objects.
 */
@Component
public class ClientAddressDetailsFindAddressValidator extends AbstractClientAddressValidator {

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
   * Validates the client address search in the {@link uk.gov.laa.ccms.caab.bean.ClientDetails}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(Object target, Errors errors) {
    ClientDetails clientDetails = (ClientDetails) target;

    if (clientDetails.isNoAddressLookup()) {
      errors.reject(
          "address.none",
          "Your input for address details has not returned any results.");
    } else {
      if (clientDetails.getNoFixedAbode()) {
        validateNoFixedAbode(clientDetails, errors);
      }

      validateRequiredField("country", clientDetails.getCountry(),
          "Country", errors);
      if (StringUtils.hasText(clientDetails.getCountry())) {
        if (!clientDetails.getCountry().equals("GBR")) {
          errors.rejectValue("country", "required.GBR",
              "The address lookup system is not available for the country you have "
                  + "selected. Please enter the address manually.");
        }
      }
      validateRequiredField("houseNameNumber", clientDetails.getHouseNameNumber(),
          "House name / number", errors);
      validatePostcodeFormat(clientDetails.getCountry(), clientDetails.getPostcode(), errors);
    }
  }
}


