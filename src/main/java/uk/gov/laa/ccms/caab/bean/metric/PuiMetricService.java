package uk.gov.laa.ccms.caab.bean.metric;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.laa.ccms.caab.client.CaabApiClient;

/**
 * PuiMetricService is a component responsible for maintaining and updating
 * metrics related to the PUI application lifecycle.It uses Prometheus as the metrics backend.
 *
 * <p>Metrics:</p>
 * <ul>
 *   <li>Applications Started: Tracks the total number of applications that have been started.</li>
 *   <li>Applications Copied: Tracks the total number of applications which were copied.</li>
 *   <li>Applications Submitted: Tracks the total number of applications which were submitted.</li>
 *   <li>Total Applications in TDS: Returns the total applications which are
 *    currently stored in TDS.</li>
 * </ul>
 *
 * @author Jamie Briggs
 */
@Component
public class PuiMetricService {

  private final Counter applicationsCreatedCounter;
  private final Counter applicationsCopiedCounter;
  private final Counter applicationsSubmittedCounter;
  private final Counter applicationsAbandonedCounter;
  private final Gauge totalApplicationsInFlightGauge;
  private final CaabApiClient caabApiClient;

  public PuiMetricService(PrometheusRegistry meterRegistry, CaabApiClient caabApiClient) {
    this.applicationsCreatedCounter = Counter.builder()
        .name("pui_applications_started")
        .help("Total number of applications started")
        .register(meterRegistry);
    this.applicationsCopiedCounter = Counter.builder()
        .name("pui_applications_copied")
        .help("Total number of applications copied")
        .register(meterRegistry);
    this.applicationsSubmittedCounter = Counter.builder()
        .name("pui_applications_submitted")
        .help("Total number of applications started")
        .register(meterRegistry);
    this.applicationsAbandonedCounter = Counter.builder()
        .name("pui_applications_abandoned")
        .help("Total number of applications abandoned")
        .register(meterRegistry);
    this.totalApplicationsInFlightGauge = Gauge.builder()
        .name("pui_total_applications_in_tds")
        .help("Total number of applications in TDS")
        .register(meterRegistry);
    this.caabApiClient = caabApiClient;
  }

  public void incrementCreatedApplicationsCount() {
    applicationsCreatedCounter.inc();
  }

  public void incrementSubmitApplicationsCount() {
    applicationsSubmittedCounter.inc();
  }

  public void incrementCopyCount() {
    applicationsCopiedCounter.inc();
  }

  public void incrementAbandonedCount() {
    applicationsAbandonedCounter.inc();
  }

  @Scheduled(fixedRate = 10000)
  public void updateTotalApplicationsGauge() {
    totalApplicationsInFlightGauge.set(caabApiClient
        .getTotalApplications().blockOptional().orElse(0L));
  }

}
