package uk.gov.laa.ccms.caab.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.context.ContextSnapshotFactory;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.util.context.Context;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.UserDetail;

class ProviderContextTest {

  @AfterEach
  void tearDown() {
    ProviderContext.clear();
    ProviderContext.registerAccessor();
  }

  @Test
  void setProviderIdTrimsValidValue() {
    ProviderContext.setProviderId("  12345  ");
    assertThat(ProviderContext.getProviderId()).isEqualTo("12345");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "null", "NULL", " null "})
  void setProviderIdClearsBlankOrLiteralNullValues(final String value) {
    ProviderContext.setProviderId("12345");
    ProviderContext.setProviderId(value);
    assertThat(ProviderContext.getProviderId()).isNull();
  }

  @Test
  void setProviderIdIntegerHandlesNonNullAndNull() {
    ProviderContext.setProviderId(67890);
    assertThat(ProviderContext.getProviderId()).isEqualTo("67890");

    ProviderContext.setProviderId((Integer) null);
    assertThat(ProviderContext.getProviderId()).isNull();
  }

  @Test
  void setFromUserPopulatesWhenProviderIdPresentAndClearsOtherwise() {
    final UserDetail userWithProvider =
        new UserDetail().provider(new BaseProvider().id(54321).name("Firm"));
    ProviderContext.setFromUser(userWithProvider);
    assertThat(ProviderContext.getProviderId()).isEqualTo("54321");

    ProviderContext.setFromUser(new UserDetail().provider(new BaseProvider()));
    assertThat(ProviderContext.getProviderId()).isNull();

    ProviderContext.setProviderId("54321");
    ProviderContext.setFromUser(new UserDetail());
    assertThat(ProviderContext.getProviderId()).isNull();

    ProviderContext.setProviderId("54321");
    ProviderContext.setFromUser(null);
    assertThat(ProviderContext.getProviderId()).isNull();
  }

  @Test
  void resolveProviderIdPrefersThreadLocalAndFallsBackToReactorContext() {
    final Context context = Context.of(ProviderContext.CONTEXT_KEY, " 99999 ");

    assertThat(ProviderContext.resolveProviderId(context)).isEqualTo("99999");

    ProviderContext.setProviderId("11111");
    assertThat(ProviderContext.resolveProviderId(context)).isEqualTo("11111");

    ProviderContext.clear();
    assertThat(ProviderContext.resolveProviderId(Context.of(ProviderContext.CONTEXT_KEY, "null")))
        .isNull();
    assertThat(ProviderContext.resolveProviderId(Context.of(ProviderContext.CONTEXT_KEY, 123)))
        .isNull();
    assertThat(ProviderContext.resolveProviderId(Context.empty())).isNull();
    assertThat(ProviderContext.resolveProviderId(null)).isNull();
  }

  @Test
  void propagatesAcrossWorkerThreadsAndRestoresPreviousWorkerState() throws Exception {
    final ProviderContext lifecycleBean = new ProviderContext();
    lifecycleBean.afterPropertiesSet();
    try (var worker = Executors.newSingleThreadExecutor()) {
      ProviderContext.setProviderId("12345");
      final var snapshot = ContextSnapshotFactory.builder().build().captureAll();
      ProviderContext.clear();

      worker
          .submit(
              () -> {
                ProviderContext.setProviderId("98765");
                try {
                  for (final boolean fail : new boolean[] {false, true}) {
                    try {
                      try (var ignored = snapshot.setThreadLocals()) {
                        assertEquals("12345", ProviderContext.getProviderId());
                        if (fail) {
                          throw new IllegalStateException("synthetic failure");
                        }
                      }
                    } catch (final IllegalStateException expected) {
                      assertTrue(fail);
                    }
                    assertEquals("98765", ProviderContext.getProviderId());
                  }
                } finally {
                  ProviderContext.clear();
                }
              })
          .get();

      assertNull(ProviderContext.getProviderId());
    } finally {
      lifecycleBean.destroy();
    }
  }
}
