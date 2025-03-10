package uk.gov.laa.ccms.caab.feature;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Aspect responsible for handling the {@link RequiresFeature} annotation.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class FeatureAspect {

  private final SpelExpressionParser parser;
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
    String conditionExpression = requiresFeature.conditionExpression();

    boolean conditionMet = isConditionMet(conditionExpression, joinPoint);

    if (conditionMet && !featureService.isEnabled(requiredFeature)) {
      throw new FeatureDisabledException(requiredFeature);
    }
    return joinPoint.proceed();
  }

  /**
   * Checks whether the required condition has been met.
   *
   * @param conditionExpression SpEL expression describing the required state of the method
   *     parameters to pass the condition.
   * @param joinPoint the aspect join point.
   * @return true if the condition has been met, false otherwise.
   */
  private boolean isConditionMet(String conditionExpression, ProceedingJoinPoint joinPoint) {

    if (!StringUtils.hasText(conditionExpression)) {
      return true;
    }

    StandardEvaluationContext context = new StandardEvaluationContext();
    context.setVariables(getMethodParameters(joinPoint));

    Boolean result = parser.parseExpression(conditionExpression).getValue(context, Boolean.class);

    return Boolean.TRUE.equals(result);
  }

  /**
   * Retrieve the method parameters from the join point, in a map consisting of entries in the
   * format {parameter_name, parameter_value}.
   *
   * @param joinPoint the aspect join point.
   * @return the method parameters, including name and value.
   */
  private Map<String, Object> getMethodParameters(ProceedingJoinPoint joinPoint) {

    CodeSignature signature = (CodeSignature) joinPoint.getSignature();
    String[] parameterNames = signature.getParameterNames();
    Object[] args = joinPoint.getArgs();

    return IntStream.range(0, args.length)
        .boxed()
        .collect(Collectors.toMap(i -> parameterNames[i], i -> args[i]));
  }
}
