package uk.gov.laa.ccms.caab.bean;

import java.beans.PropertyEditor;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

@RequiredArgsConstructor
public class ChildBindingResult implements BindingResult {
  private final BindingResult parent;
  private final String childName;

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
    return parent.getRawFieldValue(childName + "." + field);
  }

  @Override
  public PropertyEditor findEditor(String field, Class<?> valueType) {
    return parent.findEditor(childName + "." + field, valueType);
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
    return parent.resolveMessageCodes(errorCode, childName + "." + field);
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
    parent.rejectValue(childName + "." + field, errorCode, errorArgs, defaultMessage);
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
    return parent.getFieldValue(childName + "." + field);
  }
}
