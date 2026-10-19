package uk.gov.laa.ccms.caab.bean.validators.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.AddressSearchFormData;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;

@ExtendWith(MockitoExtension.class)
class AddressSearchValidatorTest {

  @InjectMocks private AddressSearchValidator addressSearchValidator;

  private AddressSearchFormData addressSearch;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    addressSearch =
        new AddressSearchFormData(); // Assuming that the default constructor sets all fields to
    // null.
    errors = new BeanPropertyBindingResult(addressSearch, "clientDetails");
  }

  @Test
  public void supports_ReturnsTrueForApplicationDetailsClass() {
    assertTrue(addressSearchValidator.supports(AddressSearchFormData.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(addressSearchValidator.supports(Object.class));
  }

  @Test
  public void validate_errors() {
    addressSearchValidator.validate(addressSearch, errors);
    assertTrue(errors.hasErrors());
    assertEquals(1, errors.getErrorCount());
    assertEquals("required.uprn", errors.getAllErrors().getFirst().getCode());
  }

  @Test
  public void validate() {
    addressSearch.setUprn("12345");
    addressSearchValidator.validate(addressSearch, errors);
    assertFalse(errors.hasErrors());
  }

  @Test
  void validateSelection_acceptsUprnFromCurrentResults() {
    addressSearch.setUprn("12345");
    ResultsDisplay<AddressResultRowDisplay> results = resultsWithUprn("12345");

    addressSearchValidator.validateSelection(addressSearch, results, errors);

    assertFalse(errors.hasErrors());
  }

  @Test
  void validateSelection_rejectsUprnOutsideCurrentResults() {
    addressSearch.setUprn("stale-uprn");
    ResultsDisplay<AddressResultRowDisplay> results = resultsWithUprn("12345");

    addressSearchValidator.validateSelection(addressSearch, results, errors);

    assertTrue(errors.hasFieldErrors("uprn"));
    assertEquals("invalid.uprn", errors.getFieldError("uprn").getCode());
  }

  private ResultsDisplay<AddressResultRowDisplay> resultsWithUprn(final String uprn) {
    final AddressResultRowDisplay result = new AddressResultRowDisplay();
    result.setUprn(uprn);
    final ResultsDisplay<AddressResultRowDisplay> results = new ResultsDisplay<>();
    results.setContent(List.of(result));
    return results;
  }
}
