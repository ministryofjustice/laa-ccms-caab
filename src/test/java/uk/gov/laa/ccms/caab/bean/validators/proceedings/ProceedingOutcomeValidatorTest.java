package uk.gov.laa.ccms.caab.bean.validators.proceedings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingOutcomeFormData;

@ExtendWith(MockitoExtension.class)
class ProceedingOutcomeValidatorTest {

  @InjectMocks private ProceedingOutcomeValidator validator;

  private ProceedingOutcomeFormData formData;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    formData = new ProceedingOutcomeFormData();
    errors = new BeanPropertyBindingResult(formData, "proceedingOutcomeFormData");
  }

  /** Populates the mandatory fields so a test can isolate one free-text field. */
  private void withMandatoryFields() {
    formData.setDateOfFinalWork("01/01/2026");
    formData.setStageEnd("STAGE");
    formData.setResolutionMethod("METHOD");
    formData.setResult("RESULT");
    formData.setAlternativeResolution("ADR");
    formData.setWiderBenefits("BENEFIT");
  }

  @Test
  public void supports_ReturnsTrueForProceedingOutcomeFormData() {
    assertTrue(validator.supports(ProceedingOutcomeFormData.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(validator.supports(Object.class));
  }

  @Test
  public void validate_missingMandatoryFields_rejects() {
    validator.validate(formData, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getFieldError("stageEnd"));
    assertNotNull(errors.getFieldError("result"));
  }

  @Test
  public void validate_mandatoryFieldsOnly_passes() {
    withMandatoryFields();

    validator.validate(formData, errors);

    assertFalse(errors.hasErrors());
  }

  @ParameterizedTest
  @ValueSource(strings = {"<script>alert(1)</script>", "a < b", "closing bracket >"})
  @DisplayName("markup in the result information is rejected")
  public void validate_resultInfoContainingMarkup_rejects(final String value) {
    withMandatoryFields();
    formData.setResultInfo(value);

    validator.validate(formData, errors);

    assertNotNull(errors.getFieldError("resultInfo"));
    assertEquals("invalid.format", errors.getFieldError("resultInfo").getCode());
  }

  @ParameterizedTest
  @ValueSource(strings = {"<script>alert(1)</script>", "a < b"})
  @DisplayName("markup in the ADR information is rejected")
  public void validate_adrInfoContainingMarkup_rejects(final String value) {
    withMandatoryFields();
    formData.setAdrInfo(value);

    validator.validate(formData, errors);

    assertNotNull(errors.getFieldError("adrInfo"));
    assertEquals("invalid.format", errors.getFieldError("adrInfo").getCode());
  }

  @ParameterizedTest
  @ValueSource(strings = {"<script>alert(1)</script>", "AB/123 <b>"})
  @DisplayName("markup in the court case number is rejected")
  public void validate_courtCaseNumberContainingMarkup_rejects(final String value) {
    withMandatoryFields();
    formData.setOutcomeCourtCaseNo(value);

    validator.validate(formData, errors);

    assertNotNull(errors.getFieldError("outcomeCourtCaseNo"));
    assertEquals("invalid.format", errors.getFieldError("outcomeCourtCaseNo").getCode());
  }

  /**
   * The court case number is mapped from EBS, so the check must stay permissive enough to accept
   * the punctuation real stored references carry.
   */
  @ParameterizedTest
  @ValueSource(strings = {"AB123456", "AB/123456", "AB 123456", "AB-123/456"})
  public void validate_realCourtCaseNumbers_pass(final String value) {
    withMandatoryFields();
    formData.setOutcomeCourtCaseNo(value);

    validator.validate(formData, errors);

    assertNull(errors.getFieldError("outcomeCourtCaseNo"), "rejected: " + value);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  public void validate_blankFreeText_isAccepted(final String value) {
    withMandatoryFields();
    formData.setResultInfo(value);
    formData.setAdrInfo(value);
    formData.setOutcomeCourtCaseNo(value);

    validator.validate(formData, errors);

    assertFalse(errors.hasErrors());
  }

  @Test
  public void validate_resultInfoAboveMaximumLength_rejects() {
    withMandatoryFields();
    formData.setResultInfo("a".repeat(951));

    validator.validate(formData, errors);

    assertNotNull(errors.getFieldError("resultInfo"));
    assertEquals("length.exceeds.max", errors.getFieldError("resultInfo").getCode());
  }

  @Test
  public void validate_ordinaryFreeText_passes() {
    withMandatoryFields();
    formData.setResultInfo("Settled at hearing on 01/01/2026 - costs agreed at £1,250.00.");
    formData.setAdrInfo("Mediation was offered; the opponent's solicitor declined.");

    validator.validate(formData, errors);

    assertFalse(errors.hasErrors());
  }
}
