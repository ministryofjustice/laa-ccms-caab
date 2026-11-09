package uk.gov.laa.ccms.caab.advice;

import org.springframework.beans.PropertyAccessException;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DefaultBindingErrorProcessor;
import org.springframework.validation.FieldError;

/** Keeps technical conversion and property-access diagnostics out of form error messages. */
public class SafeBindingErrorProcessor extends DefaultBindingErrorProcessor {

  @Override
  public void processPropertyAccessException(
      final PropertyAccessException exception, final BindingResult bindingResult) {
    final String field = exception.getPropertyName();
    Assert.state(field != null, "No field in binding exception");
    Object rejectedValue = exception.getValue();
    if (ObjectUtils.isArray(rejectedValue)) {
      rejectedValue =
          StringUtils.arrayToCommaDelimitedString(ObjectUtils.toObjectArray(rejectedValue));
    }

    bindingResult.addError(
        new FieldError(
            bindingResult.getObjectName(),
            field,
            rejectedValue,
            true,
            bindingResult.resolveMessageCodes(exception.getErrorCode(), field),
            getArgumentsForBindError(bindingResult.getObjectName(), field),
            "Enter a valid value."));
  }
}
