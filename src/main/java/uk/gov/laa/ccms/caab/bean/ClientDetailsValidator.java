package uk.gov.laa.ccms.caab.bean;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * Validator component responsible for validating {@link ClientDetails} objects.
 */
@Component
public class ClientDetailsValidator implements Validator {

  /**
   * Determines if the Validator supports the provided class.
   *
   * @param clazz The class to check for support.
   * @return {@code true} if the class is assignable from {@link ClientDetails}, {@code false}
   *         otherwise.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return ClientDetails.class.isAssignableFrom(clazz);
  }

  /**
   * Validate the selected title in the {@link ClientDetails}.
   *
   * @param target The object to validate
   * @param errors The Errors object to store validation errors.
   */
  public void validateTitle(Object target, Errors errors) {
    ValidationUtils.rejectIfEmpty(errors, "title",
        "required.title", "Please complete 'Title'.");
  }

  /**
   * Validates the surname in the {@link ClientDetails}.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  public void validateSurname(Object target, Errors errors) {
    ValidationUtils.rejectIfEmpty(errors, "surname",
        "required.surname", "Please complete 'Surname'.");
  }

  /**
   * Validate the selected Country of Origin in the {@link ClientDetails}.
   *
   * @param target The object to validate
   * @param errors The Errors object to store validation errors.
   */
  public void validateCountryOfOrigin(Object target, Errors errors) {
    ValidationUtils.rejectIfEmpty(errors, "countryOfOrigin",
        "required.countryOfOrigin", "Please complete 'Country of origin'.");
  }


  /**
   * Validate the selected Gender in the {@link ClientDetails}.
   *
   * @param target The object to validate
   * @param errors The Errors object to store validation errors.
   */
  public void validateGender(Object target, Errors errors) {
    ValidationUtils.rejectIfEmpty(errors, "gender",
        "required.gender", "Please complete 'Gender'.");
  }


  /**
   * Validate the selected Marital status in the {@link ClientDetails}.
   *
   * @param target The object to validate
   * @param errors The Errors object to store validation errors.
   */
  public void validateMaritalStatus(Object target, Errors errors) {
    ValidationUtils.rejectIfEmpty(errors, "maritalStatus",
        "required.maritalStatus", "Please complete 'Marital status'.");
  }

  /**
   * Validates the provided target object.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(Object target, Errors errors) {
    validateTitle(target, errors);
    validateSurname(target, errors);
    validateCountryOfOrigin(target, errors);
    validateGender(target, errors);
    validateMaritalStatus(target, errors);
  }
}
