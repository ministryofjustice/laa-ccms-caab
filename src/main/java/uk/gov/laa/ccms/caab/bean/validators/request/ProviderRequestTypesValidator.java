package uk.gov.laa.ccms.caab.bean.validators.request;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.request.ProviderRequestTypeFormData;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/**
 * Validator component responsible for validating
 * {@link uk.gov.laa.ccms.caab.bean.request.ProviderRequestTypeFormData} objects.
 */
@Component
public class ProviderRequestTypesValidator extends AbstractValidator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from
   *         {@link uk.gov.laa.ccms.caab.bean.request.ProviderRequestTypeFormData},
   *         {@code false} otherwise.
   */
  @Override
  public boolean supports(final Class<?> clazz) {
    return ProviderRequestTypeFormData.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the provider request type details in the
   * {@link uk.gov.laa.ccms.caab.bean.request.ProviderRequestTypeFormData}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(final Object target, final Errors errors) {
    final ProviderRequestTypeFormData providerRequestTypeFormData =
        (ProviderRequestTypeFormData) target;

    validateRequiredField("providerRequestType",
        providerRequestTypeFormData.getProviderRequestType(), "Request type", errors);
  }

}


