package uk.gov.laa.ccms.caab.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.opa.session.OpaSessionJson;

/**
 * Client for the OPA connector's assess service.
 *
 * <p>The billing and POA rulebases derive their entity data (bill and POA history, provider firms,
 * prior authorities) during an assessment rather than receiving it as prepopulated input, so the
 * interview cannot resolve its completion goal unless the session has been assessed first. The
 * legacy PUI runs this call on every financial assessment start ({@code StartOpaAssessment} ->
 * {@code callOpa}) and writes the response back over both the working and pre-population sessions.
 *
 * <p>The endpoint is the connector's existing {@code puiAssessService}, already used by the legacy
 * PUI - nothing in the connector changes. It is unusual in taking {@code text/plain}: the body is a
 * JSON document, but the connector declares the request as plain text and parses it itself.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConnectorApiClient {

  private final @Qualifier("connectorApiWebClient") WebClient connectorApiWebClient;

  // The application registers no ObjectMapper bean, so one is held here rather than injected (as
  // CspReportController does). It also keeps the connector payload independent of any application
  // wide Jackson configuration, which must not alter the field names the connector parses.
  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final String ASSESS_URI = "/service-tds/puiAssessService";

  /**
   * Assesses an OPA session, returning the session enriched with everything the rulebase derived.
   *
   * @param session the OPA session to assess.
   * @return a Mono of the assessed session.
   */
  public Mono<OpaSessionJson> assess(final OpaSessionJson session) {
    final String body;
    try {
      body = objectMapper.writeValueAsString(session);
    } catch (final JsonProcessingException e) {
      return Mono.error(new CaabApplicationException("Failed to serialise the OPA session", e));
    }

    return connectorApiWebClient
        .post()
        .uri(ASSESS_URI)
        .contentType(MediaType.TEXT_PLAIN)
        .bodyValue(body)
        .retrieve()
        .bodyToMono(OpaSessionJson.class)
        .doOnError(
            e ->
                log.error(
                    "Failed to assess OPA session [{}] for case [{}]",
                    session.getAssessment(),
                    session.getTargetId(),
                    e));
  }
}
