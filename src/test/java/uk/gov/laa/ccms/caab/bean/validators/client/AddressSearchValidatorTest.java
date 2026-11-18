package uk.gov.laa.ccms.caab.bean.validators.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t\n"})
  void validate_rejectsMissingUprnAsFieldError(final String uprn) {
    addressSearch.setUprn(uprn);

    addressSearchValidator.validate(addressSearch, errors);

    assertRequiredUprnFieldError();
  }

  @Test
  public void validate() {
    addressSearch.setUprn("12345");
    addressSearchValidator.validate(addressSearch, errors);
    assertFalse(errors.hasErrors());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t\n"})
  void validateSelection_rejectsMissingUprnAsFieldError(final String uprn) {
    addressSearch.setUprn(uprn);

    addressSearchValidator.validateSelection(addressSearch, resultsWithUprn("12345"), errors);

    assertRequiredUprnFieldError();
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

  private void assertRequiredUprnFieldError() {
    assertEquals(1, errors.getErrorCount());
    assertFalse(errors.hasGlobalErrors());
    assertTrue(errors.hasFieldErrors("uprn"));
    assertEquals("required.uprn", errors.getFieldError("uprn").getCode());
    assertEquals("Please select an address", errors.getFieldError("uprn").getDefaultMessage());
  }

  private ResultsDisplay<AddressResultRowDisplay> resultsWithUprn(final String uprn) {
    final AddressResultRowDisplay result = new AddressResultRowDisplay();
    result.setUprn(uprn);
    final ResultsDisplay<AddressResultRowDisplay> results = new ResultsDisplay<>();
    results.setContent(List.of(result));
    return results;
  }
}
