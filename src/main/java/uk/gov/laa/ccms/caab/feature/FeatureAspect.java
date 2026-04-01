package uk.gov.laa.ccms.caab.feature;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/** Aspect responsible for handling the {@link RequiresFeature} annotation. */
@Aspect
@Component
@RequiredArgsConstructor
public class FeatureAspect {

  private final FeatureService featureService;

  /**
   * Checks the required feature has been enabled for methods annotated with {@link
   * RequiresFeature}.
   *
   * @param joinPoint the join point.
   * @param requiresFeature details of the feature required
   */
  @Around("@annotation(requiresFeature)")
  public Object checkRequiredFeature(ProceedingJoinPoint joinPoint, RequiresFeature requiresFeature)
      throws Throwable {

    Feature requiredFeature = requiresFeature.value();

    if (!featureService.isEnabled(requiredFeature)) {
      throw new FeatureDisabledException(requiredFeature);
    }
    return joinPoint.proceed();
  }
}
