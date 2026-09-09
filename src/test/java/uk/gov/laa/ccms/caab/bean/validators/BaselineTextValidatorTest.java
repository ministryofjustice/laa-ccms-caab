package uk.gov.laa.ccms.caab.bean.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

class BaselineTextValidatorTest {

  private BaselineTextValidator validator;

  @BeforeEach
  void setUp() {
    validator = new BaselineTextValidator();
  }

  @Test
  void supports_everyType() {
    assertTrue(validator.supports(Object.class));
    assertTrue(validator.supports(SimpleForm.class));
  }

  @Test
  void validate_nullTarget_doesNotThrow() {
    final SimpleForm form = new SimpleForm();
    final Errors errors = new BeanPropertyBindingResult(form, "form");

    validator.validate(null, errors);

    assertFalse(errors.hasErrors());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "<script>alert(1)</script>",
        "'><img src=x onerror=alert(1)>",
        "a < b",
        "closing bracket >"
      })
  void validate_valueContainingAngleBrackets_rejects(final String value) {
    final SimpleForm form = new SimpleForm();
    form.setName(value);
    final Errors errors = new BeanPropertyBindingResult(form, "form");

    validator.validate(form, errors);

    assertNotNull(errors.getFieldError("name"));
    assertEquals(
        BaselineTextValidator.INVALID_CHARACTER_CODE, errors.getFieldError("name").getCode());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"O'Brien & Co.", "21-27, St Pauls Street", "ABC/123", "a \"quoted\" value"})
  void validate_ordinaryValues_pass(final String value) {
    final SimpleForm form = new SimpleForm();
    form.setName(value);
    final Errors errors = new BeanPropertyBindingResult(form, "form");

    validator.validate(form, errors);

    assertFalse(errors.hasErrors(), "rejected a legitimate value: " + value);
  }

  @Test
  void validate_reportsEveryOffendingField() {
    final SimpleForm form = new SimpleForm();
    form.setName("<bad>");
    form.setDescription("<also bad>");
    form.setReference("fine");
    final Errors errors = new BeanPropertyBindingResult(form, "form");

    validator.validate(form, errors);

    assertEquals(2, errors.getErrorCount());
    assertNotNull(errors.getFieldError("name"));
    assertNotNull(errors.getFieldError("description"));
  }

  @Test
  void validate_nestedApplicationBean_isWalked() {
    final ParentForm parent = new ParentForm();
    final SimpleForm child = new SimpleForm();
    child.setName("<script>");
    parent.setChild(child);
    final Errors errors = new BeanPropertyBindingResult(parent, "form");

    validator.validate(parent, errors);

    assertNotNull(errors.getFieldError("child.name"));
  }

  @Test
  void validate_stringsInsideCollections_areWalked() {
    final ParentForm parent = new ParentForm();
    parent.setTags(List.of("clean", "<dirty>"));
    final Errors errors = new BeanPropertyBindingResult(parent, "form");

    validator.validate(parent, errors);

    assertTrue(errors.hasErrors());
  }

  @Test
  void validate_stringsInsideMaps_areWalked() {
    final ParentForm parent = new ParentForm();
    parent.setDynamicOptions(Map.of("optionA", "<dirty>"));
    final Errors errors = new BeanPropertyBindingResult(parent, "form");

    validator.validate(parent, errors);

    assertTrue(errors.hasErrors());
  }

  @Test
  void validate_selfReferencingBean_terminates() {
    final ParentForm parent = new ParentForm();
    parent.setSelf(parent);
    parent.setTags(List.of("<bad>"));
    final Errors errors = new BeanPropertyBindingResult(parent, "form");

    validator.validate(parent, errors);

    assertTrue(errors.hasErrors());
  }

  /** Plain accessors rather than Lombok: the annotation processor is main-source only. */
  public static class SimpleForm {
    private String name;
    private String description;
    private String reference;

    public String getName() {
      return name;
    }

    public void setName(final String name) {
      this.name = name;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(final String description) {
      this.description = description;
    }

    public String getReference() {
      return reference;
    }

    public void setReference(final String reference) {
      this.reference = reference;
    }
  }

  /** Exercises nested beans, collections, maps and a self-reference. */
  public static class ParentForm {
    private SimpleForm child;
    private List<String> tags;
    private Map<String, String> dynamicOptions;
    private ParentForm self;

    public SimpleForm getChild() {
      return child;
    }

    public void setChild(final SimpleForm child) {
      this.child = child;
    }

    public List<String> getTags() {
      return tags;
    }

    public void setTags(final List<String> tags) {
      this.tags = tags;
    }

    public Map<String, String> getDynamicOptions() {
      return dynamicOptions;
    }

    public void setDynamicOptions(final Map<String, String> dynamicOptions) {
      this.dynamicOptions = dynamicOptions;
    }

    public ParentForm getSelf() {
      return self;
    }

    public void setSelf(final ParentForm self) {
      this.self = self;
    }
  }
}
