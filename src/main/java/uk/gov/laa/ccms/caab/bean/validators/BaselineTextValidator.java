package uk.gov.laa.ccms.caab.bean.validators;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Service-wide baseline check applied to every bound form bean.
 *
 * <p>Individual validators decide what a given field may contain. This one decides what no field
 * may contain: angle brackets, which no legitimate CCMS value needs and which are the characters
 * that turn stored text into markup when it is rendered back.
 *
 * <p>It is a floor, not a replacement. A field with its own character set is still checked against
 * that set by its own validator; this only guarantees that a field nobody has written a rule for is
 * not thereby unrestricted.
 *
 * <p>Registered by {@link uk.gov.laa.ccms.caab.advice.GlobalBinderAdvice}. Note that Spring only
 * runs binder-registered validators for handlers whose model attribute carries {@code @Validated},
 * so this covers those handlers rather than literally every request. Control characters, which need
 * to be stripped everywhere, are handled during binding instead - see {@link
 * uk.gov.laa.ccms.caab.security.ControlCharacterStrippingEditor}. The {@code
 * PostHandlerValidationGuardTest} keeps the set of handlers that opt out from growing.
 */
@Component
public class BaselineTextValidator implements Validator {

  /** Error code reported when a value contains a disallowed character. */
  public static final String INVALID_CHARACTER_CODE = "invalid.character";

  private static final String INVALID_CHARACTER_MESSAGE =
      "Your input for '%s' contains an invalid character. Please amend your entry.";

  /** Characters no field may contain, regardless of its own character set. */
  private static final char[] DISALLOWED = {'<', '>'};

  /** Guards against unbounded walks of deeply nested session beans. */
  private static final int MAX_DEPTH = 3;

  /** Only the application's own types are walked; framework and JDK types are left alone. */
  private static final String APPLICATION_PACKAGE = "uk.gov.laa.ccms.caab";

  @Override
  public boolean supports(final Class<?> clazz) {
    return true;
  }

  @Override
  public void validate(final Object target, final Errors errors) {
    if (target == null) {
      return;
    }
    walk(target, "", errors, 0, Collections.newSetFromMap(new IdentityHashMap<>()));
  }

  private void walk(
      final Object target,
      final String path,
      final Errors errors,
      final int depth,
      final Set<Object> visited) {

    if (target == null || depth > MAX_DEPTH || !visited.add(target)) {
      return;
    }

    for (PropertyDescriptor descriptor : BeanUtils.getPropertyDescriptors(target.getClass())) {
      final Method reader = descriptor.getReadMethod();
      if (reader == null || "class".equals(descriptor.getName())) {
        continue;
      }

      final Object value = read(reader, target);
      if (value == null) {
        continue;
      }

      final String propertyPath =
          path.isEmpty() ? descriptor.getName() : path + "." + descriptor.getName();
      inspect(value, propertyPath, errors, depth, visited);
    }
  }

  private void inspect(
      final Object value,
      final String propertyPath,
      final Errors errors,
      final int depth,
      final Set<Object> visited) {

    switch (value) {
      case String text -> reject(text, propertyPath, errors);
      case Collection<?> collection -> {
        int index = 0;
        for (Object element : collection) {
          inspect(element, propertyPath + "[" + index++ + "]", errors, depth + 1, visited);
        }
      }
      case Map<?, ?> map ->
          map.forEach(
              (key, element) ->
                  inspect(element, propertyPath + "[" + key + "]", errors, depth + 1, visited));
      default -> {
        if (value.getClass().getName().startsWith(APPLICATION_PACKAGE)) {
          walk(value, propertyPath, errors, depth + 1, visited);
        }
      }
    }
  }

  private void reject(final String text, final String propertyPath, final Errors errors) {
    for (char disallowed : DISALLOWED) {
      if (text.indexOf(disallowed) >= 0) {
        final String message = INVALID_CHARACTER_MESSAGE.formatted(propertyPath);
        try {
          errors.rejectValue(propertyPath, INVALID_CHARACTER_CODE, message);
        } catch (final RuntimeException e) {
          // A derived path that the binder cannot resolve back to a field - for example an element
          // of an unordered collection. Still report it, just not against a specific field.
          errors.reject(INVALID_CHARACTER_CODE, message);
        }
        return;
      }
    }
  }

  private Object read(final Method reader, final Object target) {
    try {
      if (!reader.canAccess(target)) {
        return null;
      }
      return reader.invoke(target);
    } catch (final ReflectiveOperationException | RuntimeException e) {
      // A property that cannot be read cannot carry user input worth checking.
      return null;
    }
  }
}
