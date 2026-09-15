package uk.gov.laa.ccms.caab.bean.validators.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COUNSEL_SEARCH_CRITERIA;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.CounselSearchCriteria;

@ExtendWith(MockitoExtension.class)
class CounselSearchValidatorTest {

  @InjectMocks private CounselSearchValidator counselSearchValidator;

  private CounselSearchCriteria counselSearchCriteria;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    counselSearchCriteria = new CounselSearchCriteria();
    errors = new BeanPropertyBindingResult(counselSearchCriteria, COUNSEL_SEARCH_CRITERIA);
  }

  @Test
  public void supports_ReturnsTrueForCounselSearchCriteriaClass() {
    assertTrue(counselSearchValidator.supports(CounselSearchCriteria.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(counselSearchValidator.supports(Object.class));
  }

  @Test
  public void validate_noCriteriaSupplied_rejects() {
    counselSearchValidator.validate(counselSearchCriteria, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getGlobalError());
    assertEquals("required.atLeastOneSearchCriteria", errors.getGlobalError().getCode());
  }

  @Test
  public void validate_validCriteria_passes() {
    counselSearchCriteria.setName("O'Neill-Smith (Jr)");
    counselSearchCriteria.setCompany("Smith & Co. Solicitors");
    counselSearchCriteria.setLaaCounselReference("ABC/123");

    counselSearchValidator.validate(counselSearchCriteria, errors);

    assertFalse(errors.hasErrors());
  }

  /**
   * The character check on the name and company fields was previously routed through a helper whose
   * guard made the check unreachable, so every value was accepted. These assert that it now runs.
   */
  @ParameterizedTest
  @ValueSource(strings = {"a$b", "a@b", "a_b", "a[b"})
  public void validate_nameOutsideCharacterSet_rejects(final String name) {
    counselSearchCriteria.setName(name);

    counselSearchValidator.validate(counselSearchCriteria, errors);

    assertNotNull(errors.getFieldError("name"));
    assertEquals("invalid.format", errors.getFieldError("name").getCode());
  }

  @ParameterizedTest
  @ValueSource(strings = {"a$b", "a@b", "a_b"})
  public void validate_companyOutsideCharacterSet_rejects(final String company) {
    counselSearchCriteria.setCompany(company);

    counselSearchValidator.validate(counselSearchCriteria, errors);

    assertNotNull(errors.getFieldError("company"));
    assertEquals("invalid.format", errors.getFieldError("company").getCode());
  }

  @Test
  public void validate_counselReferenceContainingMarkup_rejects() {
    counselSearchCriteria.setLaaCounselReference("<script>");

    counselSearchValidator.validate(counselSearchCriteria, errors);

    assertNotNull(errors.getFieldError("laaCounselReference"));
    assertEquals("invalid.format", errors.getFieldError("laaCounselReference").getCode());
  }

  @Test
  public void validate_nameBelowMinimumLength_rejects() {
    counselSearchCriteria.setName("ab");

    counselSearchValidator.validate(counselSearchCriteria, errors);

    assertNotNull(errors.getFieldError("name"));
    assertEquals("length.below.min", errors.getFieldError("name").getCode());
  }

  @Test
  public void validate_nameAboveMaximumLength_rejects() {
    counselSearchCriteria.setName("a".repeat(36));

    counselSearchValidator.validate(counselSearchCriteria, errors);

    assertNotNull(errors.getFieldError("name"));
    assertEquals("length.exceeds.max", errors.getFieldError("name").getCode());
  }

  @Test
  public void validate_categoryOnly_isSufficientCriteria() {
    counselSearchCriteria.setCategory("MAT");

    counselSearchValidator.validate(counselSearchCriteria, errors);

    assertFalse(errors.hasErrors());
    assertNull(errors.getFieldError("name"));
  }
}
