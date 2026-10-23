package uk.gov.laa.ccms.caab.logging;

import static org.junit.jupiter.api.Assertions.*;

import io.micrometer.context.ContextSnapshotFactory;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class SessionCorrelationContextTest {

  @Test
  void restoresWorkerMdcAfterSuccessfulAndFailingCallbacksWithoutChangingTracing()
      throws Exception {
    var propagation = new SessionCorrelationContext();
    propagation.afterPropertiesSet();
    try (var worker = Executors.newSingleThreadExecutor()) {
      MDC.put("sessionID", "request-correlation");
      var snapshot = ContextSnapshotFactory.builder().build().captureAll();
      MDC.remove("sessionID");
      worker
          .submit(
              () -> {
                MDC.put("sessionID", "worker-previous");
                MDC.put("traceId", "worker-trace");
                MDC.put("spanId", "worker-span");
                try {
                  for (boolean fail : new boolean[] {false, true}) {
                    try {
                      try (var scope = snapshot.setThreadLocals()) {
                        assertEquals("request-correlation", MDC.get("sessionID"));
                        assertEquals("worker-trace", MDC.get("traceId"));
                        assertEquals("worker-span", MDC.get("spanId"));
                        if (fail) {
                          throw new IllegalStateException("synthetic callback failure");
                        }
                      }
                    } catch (IllegalStateException expected) {
                      assertTrue(fail);
                    }
                    assertEquals("worker-previous", MDC.get("sessionID"));
                    assertEquals("worker-trace", MDC.get("traceId"));
                    assertEquals("worker-span", MDC.get("spanId"));
                  }
                } finally {
                  MDC.remove("sessionID");
                  MDC.remove("traceId");
                  MDC.remove("spanId");
                }
              })
          .get();
      assertNull(MDC.get("sessionID"));
    } finally {
      MDC.remove("sessionID");
      propagation.destroy();
    }
  }
}
