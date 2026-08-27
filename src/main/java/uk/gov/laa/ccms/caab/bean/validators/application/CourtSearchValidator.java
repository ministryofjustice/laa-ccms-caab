package uk.gov.laa.ccms.caab.bean.validators.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
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
  public void validate(Object target, Errors errors) {
    log.info("Validating Court Search Form");

    CourtSearchCriteria courtSearchCriteria = (CourtSearchCriteria) target;

    String courtCode = courtSearchCriteria.getCourtCode();
    String courtName = courtSearchCriteria.getCourtName();

    validateAtLeastOneSearchCriteria(target, errors);

    if (!errors.hasErrors()) {
      if (org.springframework.util.StringUtils.hasText(courtCode)) {
        validateFieldMaxLength("courtCode", courtCode, 35, "Court Code", errors);
      }
      if (org.springframework.util.StringUtils.hasText(courtName)) {
        validateFieldMaxLength("courtName", courtName, 35, "Court Name", errors);
      }
    }
  }

  /**
   * Validates that at least one search criteria is provided in the {@link CourtSearchCriteria}.
   *
   * @param target The target object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  public void validateAtLeastOneSearchCriteria(Object target, Errors errors) {
    CourtSearchCriteria searchCriteria = (CourtSearchCriteria) target;

    if (!StringUtils.hasText(searchCriteria.getCourtCode())
        && !StringUtils.hasText(searchCriteria.getCourtName())) {
      errors.rejectValue(
          null,
          "required.atLeastOneSearchCriteria",
          "You must provide at least one search criteria below. Please amend your entry.");
    }
  }
}
