package uk.gov.laa.ccms.caab.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogMaskerTest {

  @Test
  void aNullUrlBecomesEmpty() {
    assertThat(LogMasker.withoutQuery(null)).isEmpty();
  }

  @Test
  void withoutQueryDropsOnlyTheQueryString() {
    assertThat(LogMasker.withoutQuery("https://x.test/a/b?ref=1&n=Jane"))
        .isEqualTo("https://x.test/a/b");
  }

  @org.junit.jupiter.api.Test
  void errorSummaryKeepsStatusFieldsAndCodesButNeverMessagesOrValues() {
    String body =
        """
        {"status":400,"error":"Cannot update Jane Doe at 10 High Street",
         "message":"Jane Doe at 10 High Street",
         "errors":[{"field":"surname","code":"NotBlank","rejectedValue":"Doe",
         "defaultMessage":"Doe is not allowed"}]}
        """;

    String summary = LogMasker.errorSummary(body);

    org.assertj.core.api.Assertions.assertThat(summary)
        .contains("status=400", "field=surname", "code=NotBlank")
        .doesNotContain("Jane", "High Street", "Doe", "error=");
  }

  @org.junit.jupiter.api.Test
  void errorSummaryNeverReadsInsideARejectedValue() {
    String body =
        """
        {"status":422,"errors":[{"field":"party","code":"Invalid",
         "rejectedValue":{"field":"Jane","code":"Doe","errors":[{"field":"Smith"}]}}]}
        """;

    String summary = LogMasker.errorSummary(body);

    org.assertj.core.api.Assertions.assertThat(summary)
        .contains("field=party", "code=Invalid")
        .doesNotContain("Jane", "Doe", "Smith");
  }

  @org.junit.jupiter.api.Test
  void errorSummaryDropsAValueThatIsNotASingleIdentifier() {
    org.assertj.core.api.Assertions.assertThat(
            LogMasker.errorSummary("{\"errors\":[{\"field\":\"Jane Doe\",\"code\":\"a b\"}]}"))
        .isEqualTo("[body omitted]");
  }

  @org.junit.jupiter.api.Test
  void errorSummaryOmitsABodyThatIsNotJson() {
    org.assertj.core.api.Assertions.assertThat(
            LogMasker.errorSummary("<html>Jane Doe, 10 High Street</html>"))
        .isEqualTo("[body omitted]");
    org.assertj.core.api.Assertions.assertThat(LogMasker.errorSummary(" ")).isEmpty();
    org.assertj.core.api.Assertions.assertThat(LogMasker.errorSummary(null)).isEmpty();
  }
}
