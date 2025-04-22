package uk.gov.laa.ccms.caab.bean.validators.opponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ORGANISATION_SEARCH_CRITERIA;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationSearchCriteria;

@ExtendWith(SpringExtension.class)
class OrganisationSearchCriteriaValidatorTest {

  @InjectMocks
  private OrganisationSearchCriteriaValidator organisationSearchCriteriaValidator;

  private OrganisationSearchCriteria searchCriteria;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    searchCriteria =
        new OrganisationSearchCriteria();
    errors = new BeanPropertyBindingResult(searchCriteria, ORGANISATION_SEARCH_CRITERIA);
  }

  @Test
  public void supports_ReturnsTrueForOrganisationSearchCriteriaClass() {
    assertTrue(organisationSearchCriteriaValidator.supports(OrganisationSearchCriteria.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(organisationSearchCriteriaValidator.supports(Object.class));
  }

  @Test
  public void validate_noErrors() {
    searchCriteria.setName("aname");

    organisationSearchCriteriaValidator.validate(searchCriteria, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  public void validate_generatesErrors() {
    organisationSearchCriteriaValidator.validate(searchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("name"));
    assertEquals("required.name", errors.getFieldError("name").getCode());
  }

  @ParameterizedTest
  @ValueSource(strings = {"South  Cardiff", " North:@Swansea;"})
  void validate_InvalidCityFormat(String city) {
    searchCriteria.setName("Good Company");
    searchCriteria.setCity(city);
    organisationSearchCriteriaValidator.validate(searchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validate_ValidCityFormat() {
    searchCriteria.setName("Good Company");
    searchCriteria.setCity("Bristol");
    organisationSearchCriteriaValidator.validate(searchCriteria, errors);
    assertFalse(errors.hasErrors());
    assertEquals(0, errors.getErrorCount());
  }

  @ParameterizedTest
  @ValueSource(strings = {"My  Org", "The:@Company;"})
  void validate_InvalidNameFormat(String name) {
    searchCriteria.setName(name);
    organisationSearchCriteriaValidator.validate(searchCriteria, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
  }

  @Test
  void validate_ValidNameFormat() {
    searchCriteria.setName("Good Company");
    organisationSearchCriteriaValidator.validate(searchCriteria, errors);
    assertFalse(errors.hasErrors());
    assertEquals(0, errors.getErrorCount());
  }
}