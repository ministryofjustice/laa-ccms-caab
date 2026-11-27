package uk.gov.laa.ccms.caab.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class SentApplicationsTest {

  @Test
  void recordsSentApplicationsAndForgetsOne() {
    MockHttpSession session = new MockHttpSession();

    SentApplications.record(session, 1, "t1");
    SentApplications.record(session, 2, "t2");
    SentApplications.forget(session, 1);

    assertThat(SentApplications.transactionFor(session, 1)).isEmpty();
    assertThat(SentApplications.transactionFor(session, 2)).isEqualTo(Optional.of("t2"));
    assertThat(SentApplications.transactionFor(session, null)).isEmpty();
  }

  @Test
  void concurrentRecordsAreAllKept() throws InterruptedException {
    MockHttpSession session = new MockHttpSession();
    int count = 200;
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(8);

    IntStream.range(0, count)
        .forEach(
            id ->
                pool.execute(
                    () -> {
                      try {
                        start.await();
                      } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                      }
                      SentApplications.record(session, id, "t" + id);
                    }));
    start.countDown();
    pool.shutdown();
    assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

    IntStream.range(0, count)
        .forEach(id -> assertThat(SentApplications.transactionFor(session, id)).contains("t" + id));
  }
}
