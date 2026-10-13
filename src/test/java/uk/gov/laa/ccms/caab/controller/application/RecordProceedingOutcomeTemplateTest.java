package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Record proceeding outcome template tests")
class RecordProceedingOutcomeTemplateTest {

  @Test
  @DisplayName("Court case number stays bound as a hidden field when no court is selected")
  void courtCaseNumberStaysBoundAsHiddenFieldWhenNoCourtIsSelected() throws IOException {
    final String template =
        Files.readString(
            Path.of("src/main/resources/templates/application/record-proceeding-outcome.html"));

    assertThat(template)
        .contains(
            "th:unless=\"${proceedingOutcome.courtCode != null and !#strings.isEmpty(proceedingOutcome.courtCode)}\"")
        .contains("type=\"hidden\"")
        .contains("th:field=\"*{outcomeCourtCaseNo}\"");
  }
}
