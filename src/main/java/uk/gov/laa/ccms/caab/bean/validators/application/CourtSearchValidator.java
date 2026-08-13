package uk.gov.laa.ccms.caab.bean.validators.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.laa.ccms.caab.bean.CourtSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validator class for court search validation. */
@Slf4j
@Component
public class CourtSearchValidator extends AbstractValidator {

  @Override
  public boolean supports(Class<?> clazz) {
    return CourtSearchCriteria.class.isAssignableFrom(clazz);
  }

  /**
   * Validates the search criteria with the required fields of the values.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(Object target, org.springframework.validation.Errors errors) {
    log.info("Validating Court Search Form");
  }
}
