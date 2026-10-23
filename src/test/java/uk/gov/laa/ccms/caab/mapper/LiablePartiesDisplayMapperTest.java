package uk.gov.laa.ccms.caab.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.context.LiablePartiesMappingContext;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.BaseAwardDetail;
import uk.gov.laa.ccms.caab.model.CostAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.LandAwardDetail;
import uk.gov.laa.ccms.caab.model.LiablePartyDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;

class LiablePartiesDisplayMapperTest {
  private final LiablePartiesDisplayMapper mapper =
      Mappers.getMapper(LiablePartiesDisplayMapper.class);

  @Test
  void mapsSelectedAwardLinksInOrderWithResolvedNamesAndCompactAddresses() {
    final var award =
        new FinancialAwardDetail()
            .awardType("DAMAGE")
            .description("Award")
            .awardAmount(new BigDecimal("123.45"))
            .liableParties(
                List.of(
                    new LiablePartyDetail().id(101).opponentId(11),
                    new LiablePartyDetail().id(102).opponentId(12)));
    final var person =
        person()
            .ebsId("award123")
            .confirmed(false)
            .address(
                new AddressDetail()
                    .addressLine1("1 High Street")
                    .addressLine2("Not shown")
                    .city("Not shown")
                    .postcode("AB1 2CD"));
    final var organisation =
        new OpponentDetail()
            .id(12)
            .type("Organisation")
            .organisationName("Acme Ltd")
            .contactNameRole("Do not display")
            .relationshipToCase("OWNER");
    final var result =
        mapper.toLiablePartiesDisplay(
            award, List.of(organisation, new OpponentDetail().id(99), person), context());

    assertThat(result.awardType()).isEqualTo("DAMAGE");
    assertThat(result.description()).isEqualTo("Award");
    assertThat(result.awardAmount()).isEqualByComparingTo("123.45");
    assertThat(result.costOrFinancial()).isTrue();
    assertThat(result.parties()).hasSize(2);
    final var individual = result.parties().getFirst();
    assertThat(individual.id()).isEqualTo(101);
    assertThat(individual.opponentId()).isEqualTo(11);
    assertThat(individual.name()).isEqualTo("Ms Jane Smith");
    assertThat(individual.relationshipToCase()).isEqualTo("Individual owner");
    assertThat(individual.address()).isEqualTo("1 High Street AB1 2CD");
    assertThat(individual.editable()).isTrue();
    final var org = result.parties().getLast();
    assertThat(org.name()).isEqualTo("Acme Ltd");
    assertThat(org.relationshipToCase()).isEqualTo("Organisation owner");
    assertThat(org.organisation()).isTrue();
    assertThat(org.editable()).isFalse();
    assertThat(org.address()).isEmpty();
    assertThat(person.getConfirmed()).isFalse();
  }

  @Test
  void usesCorrectAmountsForEachAwardTypeWithoutMutatingTheAward() {
    final CostAwardDetail cost =
        new CostAwardDetail()
            .awardType("COST")
            .preCertificateLscCost(BigDecimal.TEN)
            .preCertificateOtherCost(BigDecimal.TEN)
            .certificateCostLsc(BigDecimal.TEN)
            .certificateCostMarket(BigDecimal.TEN);
    final LandAwardDetail land =
        new LandAwardDetail()
            .awardType("LAND")
            .valuationAmount(new BigDecimal("300.00"))
            .awardAmount(BigDecimal.ONE);
    final OtherAssetAwardDetail asset =
        new OtherAssetAwardDetail()
            .awardType("ASSET")
            .valuationAmount(new BigDecimal("400.00"))
            .awardAmount(BigDecimal.ONE);
    assertThat(mapper.toLiablePartiesDisplay(cost, List.of(), null).awardAmount())
        .isEqualByComparingTo("40");
    assertThat(mapper.toLiablePartiesDisplay(cost, List.of(), null).costOrFinancial()).isTrue();
    assertThat(mapper.toLiablePartiesDisplay(land, List.of(), null).awardAmount())
        .isEqualByComparingTo("300");
    assertThat(mapper.toLiablePartiesDisplay(asset, List.of(), null).awardAmount())
        .isEqualByComparingTo("400");
    assertThat(mapper.toLiablePartiesDisplay(land, List.of(), null).costOrFinancial()).isFalse();
    assertThat(mapper.toLiablePartiesDisplay(asset, List.of(), null).costOrFinancial()).isFalse();
    assertThat(land.getAwardAmount()).isEqualTo(BigDecimal.ONE);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"123", "AWAR", "directory123"})
  void importedPartiesAreNotEditableAndUnknownCodesAreRetained(String ebsId) {
    final var person =
        person()
            .ebsId(ebsId)
            .title("UNKNOWN")
            .relationshipToCase("UNKNOWN")
            .address(new AddressDetail().postcode("AB1 2CD"));
    final var party =
        mapper
            .toLiablePartiesDisplay(linkedAward(), List.of(person), context())
            .parties()
            .getFirst();
    assertThat(party.editable()).isFalse();
    assertThat(party.name()).isEqualTo("UNKNOWN Jane Smith");
    assertThat(party.relationshipToCase()).isEqualTo("UNKNOWN");
    assertThat(party.address()).isEqualTo("AB1 2CD");
  }

  @Test
  void missingLinkedOpponentIsNotSilentlyHidden() {
    assertThatThrownBy(() -> mapper.toLiablePartiesDisplay(linkedAward(), List.of(), context()))
        .isInstanceOf(CaabApplicationException.class)
        .hasMessageContaining("liable party: 101");
    assertThatThrownBy(() -> mapper.toLiablePartiesDisplay(linkedAward(), null, context()))
        .isInstanceOf(CaabApplicationException.class)
        .hasMessageContaining("liable party: 101");
  }

  @Test
  void nullPartyCollectionProducesAnEmptySummaryWithoutLookups() {
    final var award = new FinancialAwardDetail().awardType("DAMAGE").liableParties(null);
    assertThat(mapper.toLiablePartiesDisplay(award, null, null).parties()).isEmpty();
  }

  private BaseAwardDetail linkedAward() {
    return new FinancialAwardDetail()
        .awardType("DAMAGE")
        .liableParties(List.of(new LiablePartyDetail().id(101).opponentId(11)));
  }

  private OpponentDetail person() {
    return new OpponentDetail()
        .id(11)
        .type("Individual")
        .title("MS")
        .firstName("Jane")
        .surname("Smith")
        .relationshipToCase("OWNER");
  }

  private LiablePartiesMappingContext context() {
    return LiablePartiesMappingContext.builder()
        .contactTitle(
            new CommonLookupDetail()
                .content(List.of(new CommonLookupValueDetail().code("MS").description("Ms"))))
        .organisationRelationshipsToCase(
            new RelationshipToCaseLookupDetail()
                .content(
                    List.of(
                        new RelationshipToCaseLookupValueDetail()
                            .code("OWNER")
                            .description("Organisation owner"))))
        .individualRelationshipsToCase(
            new RelationshipToCaseLookupDetail()
                .content(
                    List.of(
                        new RelationshipToCaseLookupValueDetail()
                            .code("OWNER")
                            .description("Individual owner"))))
        .build();
  }
}
