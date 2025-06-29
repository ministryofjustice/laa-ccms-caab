package uk.gov.laa.ccms.caab.bean.validators.provider;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.provider.ProviderFirmFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validator component responsible for validating {@link ProviderFirmFormData} objects. */
@Component
public class ProviderFirmValidator extends AbstractValidator {

  @Override
  public boolean supports(final Class<?> clazz) {
    return ProviderFirmFormData.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the details in {@link uk.gov.laa.ccms.caab.bean.provider.ProviderFirmFormData}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(final Object target, final Errors errors) {
    final ProviderFirmFormData providerFirmFormData = (ProviderFirmFormData) target;

    validateRequiredField(
        "providerFirmId", providerFirmFormData.getProviderFirmId(), "Provider firm", errors);
  }
}
