package uk.gov.laa.ccms.caab.feature;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.expression.spel.standard.SpelExpressionParser;

@ExtendWith(MockitoExtension.class)
public class FeatureAspectTest {

  @Mock private FeatureService featureService;

  @Mock private SpelExpressionParser spelExpressionParser;

  @InjectMocks private FeatureAspect featureAspect;

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
}
