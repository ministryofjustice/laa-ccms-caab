package uk.gov.laa.ccms.caab.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentOtherDetailsFormData;
import uk.gov.laa.ccms.caab.model.OpponentDetail;

class NullableMapperTest {
  private final OpponentMapper mapper = Mappers.getMapper(OpponentMapper.class);

  @ParameterizedTest
  @MethodSource("nullableValues")
  void unwrapsNullableValuesForMappersAndOtherCallers(JsonNullable<?> value, Object expected) {
    assertThat(NullableMapper.unwrap(value)).isEqualTo(expected);
  }

  private static Stream<Arguments> nullableValues() {
    return Stream.of(
        Arguments.of(null, null),
        Arguments.of(JsonNullable.undefined(), null),
        Arguments.of(JsonNullable.of(null), null),
        Arguments.of(JsonNullable.of(false), false),
        Arguments.of(JsonNullable.of(true), true),
        Arguments.of(JsonNullable.of(""), ""),
        Arguments.of(JsonNullable.of("reference"), "reference"),
        Arguments.of(JsonNullable.of(BigDecimal.ZERO), BigDecimal.ZERO),
        Arguments.of(JsonNullable.of(new BigDecimal("12.50")), new BigDecimal("12.50")));
  }

  @ParameterizedTest
  @MethodSource("values")
  void wrapsValuesIncludingNullAsExplicitlyPresent(Object value) {
    final var wrapped = NullableMapper.wrap(value);
    assertThat(wrapped.isPresent()).isTrue();
    assertThat(wrapped.get()).isEqualTo(value);
  }

  private static Stream<Object> values() {
    return Stream.of(null, false, true, "", "reference", BigDecimal.ZERO, new BigDecimal("12.50"));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(booleans = {true, false})
  void preservesAnswersAndExplicitClearingWhenMappingForms(Boolean answer) {
    final var contact = new IndividualOpponentFormData();
    contact.setLegalAided(answer);
    final var opponent = mapper.toIndividualOpponent(contact);
    assertThat(opponent.getLegalAided()).isEqualTo(JsonNullable.of(answer));

    opponent.courtOrderedMeansAssessment(true);
    final var other = new IndividualOpponentOtherDetailsFormData();
    other.setCourtOrderedMeansAssessment(answer);
    mapper.updateIndividualOtherDetails(other, opponent);
    assertThat(opponent.getCourtOrderedMeansAssessment()).isEqualTo(JsonNullable.of(answer));
  }

  @ParameterizedTest
  @MethodSource("nullableAnswers")
  void unwrapsNullableApiAnswersAcrossFormsAndDisplays(JsonNullable<Boolean> answer) {
    final Boolean expected = answer == null ? null : answer.orElse(null);
    final var opponent = new OpponentDetail();
    opponent.setLegalAided(answer);
    opponent.setCourtOrderedMeansAssessment(answer);

    assertThat(
            mapper
                .toIndividualOpponentFormData(opponent, null, null, null, null, true)
                .getLegalAided())
        .isEqualTo(expected);
    assertThat(
            mapper
                .toIndividualOpponentOtherDetailsFormData(opponent)
                .getCourtOrderedMeansAssessment())
        .isEqualTo(expected);

    final var display =
        Mappers.getMapper(IndividualDetailsSectionDisplayMapper.class)
            .toIndividualEmploymentDetailsSectionDisplay(opponent);
    assertThat(display.getPartyIsLegalAided()).isEqualTo(expected);
    assertThat(display.getHadCourtOrderedMeansAssessment()).isEqualTo(expected);

    final var ebs = Mappers.getMapper(EbsApplicationMapper.class).toEbsPerson(opponent);
    assertThat(ebs.getPartyLegalAidedInd()).isEqualTo(expected);
    assertThat(ebs.getCourtOrderedMeansAssessment()).isEqualTo(expected);

    final var soa = Mappers.getMapper(SoaApplicationMapper.class).toSoaPerson(opponent);
    assertThat(soa.isPartyLegalAidedInd()).isEqualTo(expected);
    assertThat(soa.isCourtOrderedMeansAssessment()).isEqualTo(expected);
  }

  private static Stream<JsonNullable<Boolean>> nullableAnswers() {
    return Stream.of(
        null,
        JsonNullable.undefined(),
        JsonNullable.of(null),
        JsonNullable.of(false),
        JsonNullable.of(true));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(booleans = {true, false})
  void preservesNullableAnswersThroughTheActualWebClientCodecs(Boolean answer) throws Exception {
    final var opponent =
        new OpponentDetail().legalAided(answer).courtOrderedMeansAssessment(answer);
    final var uri = URI.create("http://localhost/opponents");
    final var output = new MockClientHttpRequest(HttpMethod.PUT, uri);
    ClientRequest.create(HttpMethod.PUT, uri)
        .body(BodyInserters.fromValue(opponent))
        .build()
        .writeTo(output, ExchangeStrategies.withDefaults())
        .block();
    final var body = output.getBodyAsString().block();
    final var json = new ObjectMapper().readTree(body);
    for (String field : new String[] {"legal_aided", "court_ordered_means_assessment"}) {
      assertThat(json.has(field)).as(field).isTrue();
      if (answer == null) {
        assertThat(json.get(field).isNull()).as(field).isTrue();
      } else {
        assertThat(json.get(field).isBoolean()).as(field).isTrue();
        assertThat(json.get(field).booleanValue()).as(field).isEqualTo(answer);
      }
    }
    final var restored =
        ClientResponse.create(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(body)
            .build()
            .bodyToMono(OpponentDetail.class)
            .block();
    assertThat(restored.getLegalAided()).isEqualTo(JsonNullable.of(answer));
    assertThat(restored.getCourtOrderedMeansAssessment()).isEqualTo(JsonNullable.of(answer));
  }
}
