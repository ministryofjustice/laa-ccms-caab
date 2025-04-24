package uk.gov.laa.ccms.caab.bean;

import java.beans.PropertyEditor;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

/**
 * NestedBindingResult is an implementation of the BindingResult interface that represents
 * a binding result scoped to a specific nested property of a parent binding result.
 * It delegates of all BindingResult methods to the parent, while qualifying
 * field names with the nested property prefix.
 *
 * @author Jamie Briggs
 * @see BindingResult
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class NestedBindingResult implements BindingResult {
  private final BindingResult parent;
  private final String nestedPropertyName;

  /**
   * Creates an instance of {@code NestedBindingResult} representing a nested scoped
   * binding result for a specific property of the parent {@code BindingResult}.
   *
   * @param parent the parent {@code BindingResult} from which the nested binding result derives and delegates operations
   * @param nestedPropertyName the name of the nested property to scope the binding result to
   * @return a {@code NestedBindingResult} scoped to the specified nested property
   */
  public static NestedBindingResult asNestedProperty(BindingResult parent,
      String nestedPropertyName) {
    return new NestedBindingResult(parent, nestedPropertyName);
  }

  @Override
  public Object getTarget() {
    return parent.getTarget();
  }

  @Override
  public Map<String, Object> getModel() {
    return parent.getModel();
  }

  @Override
  public Object getRawFieldValue(String field) {
    return parent.getRawFieldValue(nestedPropertyName + "." + field);
  }

  @Override
  public PropertyEditor findEditor(String field, Class<?> valueType) {
    return parent.findEditor(nestedPropertyName + "." + field, valueType);
  }

  @Override
  public PropertyEditorRegistry getPropertyEditorRegistry() {
    return parent.getPropertyEditorRegistry();
  }

  @Override
  public String[] resolveMessageCodes(String errorCode) {
    return parent.resolveMessageCodes(errorCode);
  }

  @Override
  public String[] resolveMessageCodes(String errorCode, String field) {
    return parent.resolveMessageCodes(errorCode, nestedPropertyName + "." + field);
  }

  @Override
  public void addError(ObjectError error) {
    parent.addError(error);
  }

  @Override
  public String getObjectName() {
    return parent.getObjectName();
  }

  @Override
  public void reject(String errorCode, Object[] errorArgs, String defaultMessage) {
    parent.reject(errorCode, errorArgs, defaultMessage);
  }

  @Override
  public void rejectValue(String field, String errorCode, Object[] errorArgs,
      String defaultMessage) {
    parent.rejectValue(nestedPropertyName + "." + field, errorCode, errorArgs, defaultMessage);
  }

  @Override
  public List<ObjectError> getGlobalErrors() {
    return parent.getGlobalErrors();
  }

  @Override
  public List<FieldError> getFieldErrors() {
    return parent.getFieldErrors();
  }

  @Override
  public Object getFieldValue(String field) {
    return parent.getFieldValue(nestedPropertyName + "." + field);
  }
}
