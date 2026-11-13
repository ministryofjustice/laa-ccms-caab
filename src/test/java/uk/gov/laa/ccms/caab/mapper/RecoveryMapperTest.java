package uk.gov.laa.ccms.caab.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import uk.gov.laa.ccms.caab.bean.award.RecoveryFormData;
import uk.gov.laa.ccms.caab.util.DateUtils;

class RecoveryMapperTest {
  private final RecoveryMapper mapper = Mappers.getMapper(RecoveryMapper.class);

  @Test
  void normalisesBlankAmountsAndSerialisesOnlyEditableSnakeCaseFields() throws Exception {
    final var form = new RecoveryFormData();
    form.setSolicitorRecoveryAmount(".5");
    form.setCourtRecoveryAmount("");
    form.setClientRecoveryAmount(null);
    form.setLeaveOfCourtRequiredInd(false);
    form.setOfferDetails("Terms");
    final var request = mapper.toRequest(form);
    assertThat(request.getSolicitorRecoveryAmount()).isEqualTo(new BigDecimal("0.50"));
    assertThat(request.getCourtRecoveryAmount()).isEqualTo(new BigDecimal("0.00"));
    assertThat(request.getClientRecoveryAmount()).isEqualTo(new BigDecimal("0.00"));
    final var json = new ObjectMapper().valueToTree(request);
    assertThat(json.size()).isEqualTo(12);
    assertThat(json.get("solicitor_recovery_amount").decimalValue()).isEqualByComparingTo("0.50");
    assertThat(json.get("leave_of_court_required_ind").booleanValue()).isFalse();
    assertThat(json.get("offer_details").textValue()).isEqualTo("Terms");
    assertThat(json.has("award_amount")).isFalse();
    assertThat(json.has("recovered_amount")).isFalse();
    assertThat(json.has("id")).isFalse();
  }

  @Test
  void serialisesTheContractUsingTheActualDefaultWebClientCodecs() throws Exception {
    final var form = new RecoveryFormData();
    form.setSolicitorRecoveryAmount("10.50");
    form.setCourtRecoveryAmount("20");
    form.setClientRecoveryAmount("30");
    form.setSolicitorRecoveryDate("01/01/1900");
    form.setCourtRecoveryDate("01/01/2000");
    form.setClientRecoveryDate("29/02/2024");
    form.setSolicitorAmountPaidToLsc("1.10");
    form.setCourtAmountPaidToLsc("2.20");
    form.setClientAmountPaidToLsc("3.30");
    form.setOfferedAmount("4.40");
    form.setLeaveOfCourtRequiredInd(true);
    form.setOfferDetails("Terms");
    final var request = mapper.toRequest(form);
    final var uri = URI.create("http://localhost/recovery");
    final var output = new MockClientHttpRequest(HttpMethod.PUT, uri);
    ClientRequest.create(HttpMethod.PUT, uri)
        .body(BodyInserters.fromValue(request))
        .build()
        .writeTo(output, ExchangeStrategies.withDefaults())
        .block();
    final var json = new ObjectMapper().readTree(output.getBodyAsString().block());
    assertThat(json.get("solicitor_recovery_amount").decimalValue()).isEqualByComparingTo("10.50");
    assertThat(json.get("court_recovery_amount").decimalValue()).isEqualByComparingTo("20.00");
    assertThat(json.get("client_recovery_amount").decimalValue()).isEqualByComparingTo("30.00");
    assertThat(json.get("solicitor_amount_paid_to_lsc").decimalValue())
        .isEqualByComparingTo("1.10");
    assertThat(json.get("court_amount_paid_to_lsc").decimalValue()).isEqualByComparingTo("2.20");
    assertThat(json.get("client_amount_paid_to_lsc").decimalValue()).isEqualByComparingTo("3.30");
    assertThat(json.get("offered_amount").decimalValue()).isEqualByComparingTo("4.40");
    assertThat(Instant.parse(json.get("solicitor_recovery_date").textValue()))
        .isEqualTo(DateUtils.convertToDate("01/01/1900").toInstant());
    assertThat(Instant.parse(json.get("court_recovery_date").textValue()))
        .isEqualTo(DateUtils.convertToDate("01/01/2000").toInstant());
    assertThat(Instant.parse(json.get("client_recovery_date").textValue()))
        .isEqualTo(DateUtils.convertToDate("29/02/2024").toInstant());
    assertThat(json.get("leave_of_court_required_ind").booleanValue()).isTrue();
    assertThat(json.get("offer_details").textValue()).isEqualTo("Terms");
    assertThat(json.has("award_amount")).isFalse();
    assertThat(json.size()).isEqualTo(12);
  }
}
