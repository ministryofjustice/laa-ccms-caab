package uk.gov.laa.ccms.caab.bean.validators.opponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ORGANISATION_SEARCH_CRITERIA;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.OrganisationSearchCriteria;

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

}