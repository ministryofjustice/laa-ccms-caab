package uk.gov.laa.ccms.caab.bean.metric;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PuiMetricService tests")
class PuiMetricServiceTest {

  PuiMetricService puiMetricService;
  @Mock PrometheusRegistry prometheusRegistry;

  @BeforeEach
  void beforeEach() {
    puiMetricService = new PuiMetricService(prometheusRegistry);
  }

  @Test
  @DisplayName("Verify counters initialized")
  void verifyCounterInitialized() {
    // Then
    verify(prometheusRegistry, times(1)).register(puiMetricService.getApplicationsCreatedCounter());
    verify(prometheusRegistry, times(1)).register(puiMetricService.getApplicationsCopiedCounter());
    verify(prometheusRegistry, times(1))
        .register(puiMetricService.getApplicationsSubmittedCounter());
    verify(prometheusRegistry, times(1))
        .register(puiMetricService.getApplicationsAbandonedCounter());
  }

  @Test
  @DisplayName("Should increment created counter")
  void shouldIncrementCreatedCounter() {
    // When
    puiMetricService.incrementCreatedApplicationsCount("123");
    // Then
    assertThat(puiMetricService.getApplicationsCreatedCounter().get()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should increment created counter twice")
  void shouldIncrementCreatedCounterTwice() {
    // When
    puiMetricService.incrementCreatedApplicationsCount("123");
    puiMetricService.incrementCreatedApplicationsCount("123");
    // Then
    assertThat(puiMetricService.getApplicationsCreatedCounter().get()).isEqualTo(2);
  }

  @Test
  @DisplayName("Should increment copied counter")
  void shouldIncrementCopiedCounter() {
    // When
    puiMetricService.incrementCopyAndCreatedCount("456", "123");
    // Then
    assertThat(puiMetricService.getApplicationsCreatedCounter().get()).isEqualTo(1);
    assertThat(puiMetricService.getApplicationsCopiedCounter().get()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should increment copied counter twice")
  void shouldIncrementCopiedCounterTwice() {
    // When
    puiMetricService.incrementCopyAndCreatedCount("456", "123");
    puiMetricService.incrementCopyAndCreatedCount("456", "123");
    // Then
    assertThat(puiMetricService.getApplicationsCreatedCounter().get()).isEqualTo(2);
    assertThat(puiMetricService.getApplicationsCopiedCounter().get()).isEqualTo(2);
  }

  @Test
  @DisplayName("Should increment submitted counter")
  void shouldIncrementSubmittedCounter() {
    // When
    puiMetricService.incrementSubmitApplicationsCount("123");
    // Then
    assertThat(puiMetricService.getApplicationsSubmittedCounter().get()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should increment submitted counter twice")
  void shouldIncrementSubmittedCounterTwice() {
    // When
    puiMetricService.incrementSubmitApplicationsCount("123");
    puiMetricService.incrementSubmitApplicationsCount("123");
    // Then
    assertThat(puiMetricService.getApplicationsSubmittedCounter().get()).isEqualTo(2);
  }

  @Test
  @DisplayName("Should increment abandoned counter")
  void shouldIncrementAbandonedCounter() {
    // When
    puiMetricService.incrementAbandonedCount("123");
    // Then
    assertThat(puiMetricService.getApplicationsAbandonedCounter().get()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should increment abandoned counter twice")
  void shouldIncrementAbandonedCounterTwice() {
    // When
    puiMetricService.incrementAbandonedCount("123");
    puiMetricService.incrementAbandonedCount("123");
    // Then
    assertThat(puiMetricService.getApplicationsAbandonedCounter().get()).isEqualTo(2);
  }
}
