package uk.gov.laa.ccms.caab.feature;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotation to control access to methods by feature. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresFeature {

  /**
   * The feature required.
   *
   * @return the feature required.
   */
  Feature value();

  /**
   * An expression describing the condition to apply the feature flag.
   *
   * @return the condition expression.
   */
  String conditionExpression() default "";
}
