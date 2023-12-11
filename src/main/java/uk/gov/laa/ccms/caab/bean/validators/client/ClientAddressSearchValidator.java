package uk.gov.laa.ccms.caab.bean.validators.client;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressSearch;

/**
 * Validates the uprn details provided by client address search.
 */
@Component
public class ClientAddressSearchValidator implements Validator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from
   *         {@link uk.gov.laa.ccms.caab.bean.ClientFormDataAddressSearch},
   *         {@code false} otherwise.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return ClientFormDataAddressSearch.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the client address search details in the
   * {@link uk.gov.laa.ccms.caab.bean.ClientFormDataAddressSearch}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(Object target, Errors errors) {
    ClientFormDataAddressSearch addressSearch = (ClientFormDataAddressSearch) target;
    if (!StringUtils.hasText(addressSearch.getUprn())) {
      errors.reject("required.uprn",
          "Please select an address.");
    }
  }
}
