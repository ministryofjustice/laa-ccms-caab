package uk.gov.laa.ccms.caab.advice;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import uk.gov.laa.ccms.caab.security.ControlCharacterStrippingEditor;

/**
 * Applies the service-wide input baseline to every controller.
 *
 * <p>Two layers, because Spring applies them differently:
 *
 * <ul>
 *   <li>the property editor runs during binding, so control characters are stripped from every
 *       bound String on every request, whether or not the handler asks to be validated;
 *   <li>the baseline validator runs only where the model attribute carries {@code @Validated},
 *       which is how Spring gates binder-registered validators. Its coverage therefore grows as
 *       handlers are annotated, and {@code PostHandlerValidationGuardTest} stops the set of
 *       unannotated handlers from growing in the meantime.
 * </ul>
 *
 * <p>Registering the validator here rather than per controller means a new form bean is covered
 * without anyone remembering to wire it up, which is the point: the previous arrangement gave a
 * field no restrictions at all until somebody wrote a validator for it.
 */
@ControllerAdvice
public class GlobalBinderAdvice {

  /**
   * Applies the input baseline to every data binder.
   *
   * @param binder the binder for the current request.
   */
  @InitBinder
  public void initBinder(final WebDataBinder binder) {
    binder.registerCustomEditor(String.class, new ControlCharacterStrippingEditor());
  }
}
