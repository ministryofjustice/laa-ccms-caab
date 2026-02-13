package uk.gov.laa.ccms.caab.bean.validators.declaration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.SummarySubmissionFormData;
import uk.gov.laa.ccms.caab.bean.declaration.DynamicCheckbox;

@ExtendWith(MockitoExtension.class)
class DeclarationSubmissionValidatorTest {

  @InjectMocks private DeclarationSubmissionValidator declarationSubmissionValidator;

  private SummarySubmissionFormData summarySubmissionFormData;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    summarySubmissionFormData = new SummarySubmissionFormData();
    errors = new BeanPropertyBindingResult(summarySubmissionFormData, "summarySubmissionFormData");
  }

  @Test
  public void supports_ReturnsTrueForSummarySubmissionFormDataClass() {
    assertTrue(declarationSubmissionValidator.supports(SummarySubmissionFormData.class));
  }

  @Test
  public void supports_ReturnsFalseForOtherClasses() {
    assertFalse(declarationSubmissionValidator.supports(Object.class));
  }

  @Test
  public void validate_WithNullDeclarationOptions_HasErrors() {
    summarySubmissionFormData.setDeclarationOptions(null);
    declarationSubmissionValidator.validate(summarySubmissionFormData, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getGlobalError());
    assertEquals("declaration.required", errors.getGlobalError().getCode());
  }

  @Test
  public void validate_WithUncheckedDeclarationOptions_HasErrors() {
    final DynamicCheckbox uncheckedCheckbox = new DynamicCheckbox();
    uncheckedCheckbox.setChecked(false);
    summarySubmissionFormData.setDeclarationOptions(List.of(uncheckedCheckbox));

    declarationSubmissionValidator.validate(summarySubmissionFormData, errors);

    assertTrue(errors.hasErrors());
    assertNotNull(errors.getGlobalError());
    assertEquals("declaration.required", errors.getGlobalError().getCode());
  }

  @Test
  public void validate_WithCheckedDeclarationOptions_NoErrors() {
    final DynamicCheckbox checkedCheckbox = new DynamicCheckbox();
    checkedCheckbox.setChecked(true);
    summarySubmissionFormData.setDeclarationOptions(List.of(checkedCheckbox));

    declarationSubmissionValidator.validate(summarySubmissionFormData, errors);

    assertFalse(errors.hasErrors());
  }
}
