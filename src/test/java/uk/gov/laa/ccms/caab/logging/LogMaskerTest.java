package uk.gov.laa.ccms.caab.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogMaskerTest {

  @Test
  void masksPersonalDataPatterns() {
    String masked =
        LogMasker.mask(
            "name jane.doe@example.com nino AB 12 34 56 C dob 1980-02-03 and 03/02/1980 "
                + "phone 07700 900123 ref 300001");

    assertThat(masked)
        .doesNotContain("jane.doe", "AB 12 34 56 C", "1980", "07700 900123")
        .contains("ref 300001");
  }

  @Test
  void truncatesLongText() {
    assertThat(LogMasker.mask("x".repeat(LogMasker.MAX_LENGTH + 50)))
        .hasSize(LogMasker.MAX_LENGTH + "...[truncated]".length());
  }

  @Test
  void nullBecomesEmpty() {
    assertThat(LogMasker.mask(null)).isEmpty();
    assertThat(LogMasker.withoutQuery(null)).isEmpty();
  }

  @Test
  void withoutQueryDropsOnlyTheQueryString() {
    assertThat(LogMasker.withoutQuery("https://x.test/a/b?ref=1&n=Jane"))
        .isEqualTo("https://x.test/a/b");
  }
}
