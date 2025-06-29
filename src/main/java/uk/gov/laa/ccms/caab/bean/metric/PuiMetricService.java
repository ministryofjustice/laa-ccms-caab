package uk.gov.laa.ccms.caab.bean.metric;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * PuiMetricService is a component responsible for maintaining and updating metrics related to the
 * PUI application lifecycle. It uses Prometheus as the metrics backend.
 *
 * <p>Metrics:
 *
 * <ul>
 *   <li>Applications Started: Tracks the total number of applications that have been started.
 *   <li>Applications Copied: Tracks the total number of applications which were copied.
 *   <li>Applications Submitted: Tracks the total number of applications which were submitted.
 *   <li>Applications Abondoned: Tracks the total number of applications which were abandoned.
 * </ul>
 *
 * @author Jamie Briggs
 */
@Slf4j
@Getter
@Component
public class PuiMetricService {

  private final Counter applicationsCreatedCounter;
  private final Counter applicationsCopiedCounter;
  private final Counter applicationsSubmittedCounter;
  private final Counter applicationsAbandonedCounter;

  /**
   * Constructs an instance of PuiMetricService, initializing various metrics that track the
   * lifecycle of PUI applications.
   *
   * @param meterRegistry the PrometheusRegistry instance used to register counters and gauges.
   */
  public PuiMetricService(PrometheusRegistry meterRegistry) {
    this.applicationsCreatedCounter =
        Counter.builder()
            .name("pui_applications_started")
            .help("Total number of applications started")
            .register(meterRegistry);
    this.applicationsCopiedCounter =
        Counter.builder()
            .name("pui_applications_copied")
            .help("Total number of applications copied")
            .register(meterRegistry);
    this.applicationsSubmittedCounter =
        Counter.builder()
            .name("pui_applications_submitted")
            .help("Total number of applications started")
            .register(meterRegistry);
    this.applicationsAbandonedCounter =
        Counter.builder()
            .name("pui_applications_abandoned")
            .help("Total number of applications abandoned")
            .register(meterRegistry);
  }

  /** Increments the created applications counter by 1. */
  public void incrementCreatedApplicationsCount(String caseReference) {
    log.info("Case reference {} created", caseReference);
    applicationsCreatedCounter.inc();
  }

  /** Increments the submitted applications counter by 1. */
  public void incrementSubmitApplicationsCount(String caseReference) {
    log.info("Case reference {} submitted", caseReference);
    applicationsSubmittedCounter.inc();
  }

  /** Increments the copied applications counter by 1. */
  public void incrementCopyAndCreatedCount(String sourceCaseReference, String caseReference) {
    log.info("Case reference {} created via copy from {}", caseReference, sourceCaseReference);
    applicationsCopiedCounter.inc();
    applicationsCreatedCounter.inc();
  }

  /** Increments the abandoned applications counter by 1. */
  public void incrementAbandonedCount(String caseReference) {
    log.info("Case reference {} abandoned", caseReference);
    applicationsAbandonedCounter.inc();
  }
}
