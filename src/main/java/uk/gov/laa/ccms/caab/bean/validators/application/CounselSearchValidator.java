package uk.gov.laa.ccms.caab.bean.validators.application;

import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.CHARACTER_SET_A;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.STANDARD_CHARACTER_SET;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.CounselSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;

/** Validator class for counsel search validation. */
@Slf4j
@Component
public class CounselSearchValidator extends AbstractValidator {
  @Override
  public boolean supports(Class<?> clazz) {
    return false;
  }

  /**
   * Validates the search criteria with the required fields of the values.
   *
   * @param target The object to be validated.
   * @param errors The Errors object to store validation errors.
   */
  @Override
  public void validate(Object target, Errors errors) {
    log.info("Validating Counsel Search Form");

    CounselSearchCriteria counselSearchCriteria = (CounselSearchCriteria) target;

    String name = counselSearchCriteria.getName();
    String company = counselSearchCriteria.getCompany();
    String laaCounselRef = counselSearchCriteria.getLaaCounselReference();
    String category = counselSearchCriteria.getCategory();

    validateAllFieldsAreEmpty("Name", null, errors, name, company, laaCounselRef, category);

    if (!errors.hasErrors()) {

      // Is category is null, empty, or not selected?
      // Is input field name null, empty?
      if (StringUtils.hasText(company)) {
        validateFieldPattern("Name", name, CHARACTER_SET_A, null, errors);
        validateFieldMaxLength("Name", name, 35, "name", errors);
      }

      // Is input field company null, empty?
      if (StringUtils.hasText(company)) {
        validateFieldPattern("Company", company, CHARACTER_SET_A, null, errors);
        validateFieldMaxLength("company", company, 35, "company", errors);
      }

      // Is input LAA council ref field null, empty?
      if (StringUtils.hasText(laaCounselRef)) {
        validateFieldPattern(
            "LAA Counsel Reference",
            laaCounselRef,
            STANDARD_CHARACTER_SET,
            "LAA Counsel Reference",
            errors);
        validateFieldMaxLength(
            "LAA Counsel Reference", laaCounselRef, 15, "LAA Counsel Reference", errors);
      }
    }
  }
}
