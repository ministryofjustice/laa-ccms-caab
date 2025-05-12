package uk.gov.laa.ccms.caab.feature;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class FeatureAspectTest {

  @Mock
  private FeatureService featureService;

  @Mock
  private SpelExpressionParser spelExpressionParser;

  @InjectMocks
  private FeatureAspect featureAspect;

  @Test
  @DisplayName("checkRequiredFeature join point proceeds when the given feature is enabled")
  void shouldProceedWhenFeatureEnabled() throws Throwable {
    RequiresFeature requiresFeature = mock(RequiresFeature.class);
    when(requiresFeature.value()).thenReturn(Feature.AMENDMENTS);

    when(featureService.isEnabled(Feature.AMENDMENTS)).thenReturn(true);

    ProceedingJoinPoint proceedingJoinPoint = mock(ProceedingJoinPoint.class);

    featureAspect.checkRequiredFeature(proceedingJoinPoint, requiresFeature);

    verify(proceedingJoinPoint).proceed();
  }

  @Test
  @DisplayName(
      "checkRequiredFeature join point throws an exception when the given feature is disabled")
  void shouldThrowExceptionWhenFeatureDisabled() {
    RequiresFeature requiresFeature = mock(RequiresFeature.class);
    when(requiresFeature.value()).thenReturn(Feature.AMENDMENTS);

    when(featureService.isEnabled(Feature.AMENDMENTS)).thenReturn(false);

    ProceedingJoinPoint proceedingJoinPoint = mock(ProceedingJoinPoint.class);

    assertThrows(
        FeatureDisabledException.class,
        () -> featureAspect.checkRequiredFeature(proceedingJoinPoint, requiresFeature),
        "Expected FeatureDisabledException to be thrown, but it wasn't.");
  }

  @Test
  @DisplayName(
      "checkRequiredFeature join point proceeds when "
          + "the provided condition is met and the feature is enabled")
  void shouldProceedWhenConditionMetAndFeatureEnabled() throws Throwable {
    RequiresFeature requiresFeature = mock(RequiresFeature.class);
    when(requiresFeature.value()).thenReturn(Feature.AMENDMENTS);
    when(requiresFeature.conditionExpression()).thenReturn("test expression");

    when(featureService.isEnabled(Feature.AMENDMENTS)).thenReturn(true);

    ProceedingJoinPoint proceedingJoinPoint = mock(ProceedingJoinPoint.class);
    when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{});
    MethodSignature signature = mock(MethodSignature.class);
    when(signature.getParameterNames()).thenReturn(new String[]{});
    when(proceedingJoinPoint.getSignature()).thenReturn(signature);

    Expression expression = mock(Expression.class);
    when(spelExpressionParser.parseExpression(any())).thenReturn(expression);
    when(expression.getValue(any(), any())).thenReturn(Boolean.TRUE);

    featureAspect.checkRequiredFeature(proceedingJoinPoint, requiresFeature);

    verify(proceedingJoinPoint).proceed();
  }

  @Test
  @DisplayName(
      "checkRequiredFeature join point proceeds when "
          + "the given feature is enabled but the condition has not been met")
  void shouldProceedWhenConditionNotMetAndFeatureEnabled() throws Throwable {
    RequiresFeature requiresFeature = mock(RequiresFeature.class);
    when(requiresFeature.value()).thenReturn(Feature.AMENDMENTS);
    when(requiresFeature.conditionExpression()).thenReturn("test expression");

    when(featureService.isEnabled(Feature.AMENDMENTS)).thenReturn(true);

    ProceedingJoinPoint proceedingJoinPoint = mock(ProceedingJoinPoint.class);
    when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{});
    MethodSignature signature = mock(MethodSignature.class);
    when(signature.getParameterNames()).thenReturn(new String[]{});
    when(proceedingJoinPoint.getSignature()).thenReturn(signature);

    Expression expression = mock(Expression.class);
    when(spelExpressionParser.parseExpression(any())).thenReturn(expression);
    when(expression.getValue(any(), any())).thenReturn(Boolean.FALSE);

    featureAspect.checkRequiredFeature(proceedingJoinPoint, requiresFeature);

    verify(proceedingJoinPoint).proceed();
  }

  @Test
  @DisplayName(
      "checkRequiredFeature join point throws an exception when "
          + "the provided condition is met but the given feature is disabled")
  void shouldThrowExceptionWhenConditionMetAndFeatureDisabled() {
    RequiresFeature requiresFeature = mock(RequiresFeature.class);
    when(requiresFeature.value()).thenReturn(Feature.AMENDMENTS);
    when(requiresFeature.conditionExpression()).thenReturn("test expression");

    when(featureService.isEnabled(Feature.AMENDMENTS)).thenReturn(false);

    ProceedingJoinPoint proceedingJoinPoint = mock(ProceedingJoinPoint.class);
    when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{});
    MethodSignature signature = mock(MethodSignature.class);
    when(signature.getParameterNames()).thenReturn(new String[]{});
    when(proceedingJoinPoint.getSignature()).thenReturn(signature);

    Expression expression = mock(Expression.class);
    when(spelExpressionParser.parseExpression(any())).thenReturn(expression);
    when(expression.getValue(any(), any())).thenReturn(Boolean.TRUE);

    assertThrows(
        FeatureDisabledException.class,
        () -> featureAspect.checkRequiredFeature(proceedingJoinPoint, requiresFeature),
        "Expected FeatureDisabledException to be thrown, but it wasn't.");
  }
}
