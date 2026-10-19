package uk.gov.laa.ccms.caab.bean.validators.client;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.gov.laa.ccms.caab.bean.AddressSearchFormData;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;

/** Validates the uprn details provided by client address search. */
@Component
public class AddressSearchValidator implements Validator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from {@link
   *     uk.gov.laa.ccms.caab.bean.AddressSearchFormData}, {@code false} otherwise.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return AddressSearchFormData.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the client address search details in the {@link
   * uk.gov.laa.ccms.caab.bean.AddressSearchFormData}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(Object target, Errors errors) {
    AddressSearchFormData addressSearch = (AddressSearchFormData) target;
    if (!StringUtils.hasText(addressSearch.getUprn())) {
      errors.reject("required.uprn", "Please select an address");
    }
  }

  /** Validates that the submitted address belongs to the current result set. */
  public void validateSelection(
      final AddressSearchFormData addressSearch,
      final ResultsDisplay<AddressResultRowDisplay> searchResults,
      final Errors errors) {
    validate(addressSearch, errors);
    if (errors.hasErrors()) {
      return;
    }

    final boolean selectionExists =
        searchResults != null
            && searchResults.getContent() != null
            && searchResults.getContent().stream()
                .anyMatch(
                    result -> result != null && addressSearch.getUprn().equals(result.getUprn()));
    if (!selectionExists) {
      errors.rejectValue("uprn", "invalid.uprn", "Please select an address from the results");
    }
  }
}
