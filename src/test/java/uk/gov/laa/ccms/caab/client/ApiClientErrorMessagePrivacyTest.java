package uk.gov.laa.ccms.caab.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;

class ApiClientErrorMessagePrivacyTest {

  private final CaabApiClientErrorHandler handler = new CaabApiClientErrorHandler();
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
  private final Logger logger =
      (Logger) LoggerFactory.getLogger(AbstractApiClientErrorHandler.class);

  @BeforeEach
  void attach() {
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void detach() {
    logger.detachAppender(appender);
  }

  @Test
  void queryParameterValuesAreNeitherLoggedNorPutInTheException() {
    var parameters = new LinkedMultiValueMap<String, String>();
    parameters.add("surname", "Smith");
    parameters.add("dob", "1980-02-03");

    var result =
        handler.handleApiRetrieveError(new RuntimeException("boom"), "Clients", parameters);

    assertThatThrownBy(result::block)
        .hasMessageContaining("surname")
        .hasMessageContaining("dob")
        .hasMessageNotContaining("Smith")
        .hasMessageNotContaining("1980");
    assertThat(appender.list.getFirst().getFormattedMessage())
        .contains("surname", "dob")
        .doesNotContain("Smith", "1980");
  }

  @Test
  void aLoginIdIsNotLoggedOrPutInTheExceptionAsAnEmailAddress() {
    var result =
        handler.handleApiRetrieveError(
            new RuntimeException("boom"), "User", "login id", "jane.doe@example.com");

    assertThatThrownBy(result::block).hasMessageNotContaining("jane.doe");
    assertThat(appender.list.getFirst().getFormattedMessage()).doesNotContain("jane.doe");
  }

  @Test
  void anEntraEmailIsNotLoggedOrPutInTheExceptionEither() {
    var result =
        handler.handleApiRetrieveError(
            new RuntimeException("boom"),
            "Entra user mapping",
            "entra email",
            "jane.doe@test.gov.uk");

    assertThatThrownBy(result::block).hasMessageNotContaining("jane.doe");
    assertThat(appender.list.getFirst().getFormattedMessage()).doesNotContain("jane.doe");
  }

  @Test
  void anOpaqueIdentifierIsStillNamedSoSupportCanFindTheRecord() {
    var result =
        handler.handleApiRetrieveError(new RuntimeException("boom"), "Document", "id", "12345");

    assertThatThrownBy(result::block).hasMessageContaining("12345");
  }
}
