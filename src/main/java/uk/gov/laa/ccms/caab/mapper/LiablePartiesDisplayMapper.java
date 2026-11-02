package uk.gov.laa.ccms.caab.mapper;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_COST;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.AWARD_TYPE_FINANCIAL;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.context.LiablePartiesMappingContext;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.BaseAwardDetail;
import uk.gov.laa.ccms.caab.model.CostAwardDetail;
import uk.gov.laa.ccms.caab.model.LandAwardDetail;
import uk.gov.laa.ccms.caab.model.LiablePartyDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardDetail;
import uk.gov.laa.ccms.caab.model.sections.LiablePartiesDisplay;
import uk.gov.laa.ccms.caab.model.sections.LiablePartyDisplay;
import uk.gov.laa.ccms.caab.util.OpponentUtil;

/** Maps award associations and outcome opponents to the party summary. */
@Mapper(componentModel = "spring", imports = OpponentUtil.class)
public interface LiablePartiesDisplayMapper {

  default LiablePartiesDisplay toLiablePartiesDisplay(
      final BaseAwardDetail award,
      final List<OpponentDetail> opponents,
      @Context final LiablePartiesMappingContext context) {
    final Map<Integer, OpponentDetail> opponentIndex =
        Optional.ofNullable(opponents).orElse(List.of()).stream()
            .filter(
                opponent -> {
                  if (opponent == null || opponent.getId() == null) {
                    LoggerFactory.getLogger(LiablePartiesDisplayMapper.class)
                        .warn("Ignoring liable party opponent with no id");
                    return false;
                  }
                  return true;
                })
            .collect(
                Collectors.toMap(OpponentDetail::getId, Function.identity(), (first, _) -> first));
    final List<LiablePartyDisplay> parties =
        Optional.ofNullable(award.getLiableParties()).orElse(List.of()).stream()
            .map(
                link -> {
                  final OpponentDetail opponent = opponentIndex.get(link.getOpponentId());
                  if (opponent == null) {
                    throw new CaabApplicationException(
                        "Could not resolve opponent for liable party: " + link.getId());
                  }
                  return toLiablePartyDisplay(link, opponent, context);
                })
            .toList();
    return new LiablePartiesDisplay(
        award.getAwardType(),
        award.getDescription(),
        awardAmount(award),
        AWARD_TYPE_COST.equals(award.getAwardType())
            || AWARD_TYPE_FINANCIAL.equals(award.getAwardType()),
        switch (award) {
          case LandAwardDetail land -> Boolean.TRUE.equals(land.getRecoveryOfAwardTimeRelated());
          case OtherAssetAwardDetail asset ->
              Boolean.TRUE.equals(asset.getRecoveryOfAwardTimeRelated());
          default -> false;
        },
        parties);
  }

  @Mapping(target = "id", source = "party.id")
  @Mapping(target = "opponentId", source = "opponent.id")
  @Mapping(target = "name", source = "opponent", qualifiedByName = "partyName")
  @Mapping(target = "partyType", source = "opponent.type")
  @Mapping(
      target = "relationshipToCase",
      source = "opponent",
      qualifiedByName = "relationshipToCase")
  @Mapping(target = "address", source = "opponent.address", qualifiedByName = "compactAddress")
  @Mapping(target = "editable", source = "opponent.ebsId", qualifiedByName = "awardPartyEditable")
  @Mapping(target = "organisation", expression = "java(OpponentUtil.isOrganisation(opponent))")
  LiablePartyDisplay toLiablePartyDisplay(
      LiablePartyDetail party,
      OpponentDetail opponent,
      @Context LiablePartiesMappingContext context);

  @Named("partyName")
  default String partyName(
      final OpponentDetail opponent, @Context final LiablePartiesMappingContext context) {
    if (OpponentUtil.isOrganisation(opponent)) {
      if (awardPartyEditable(opponent.getEbsId())) {
        return opponent.getOrganisationName();
      }
      return StringUtils.hasText(opponent.getContactNameRole())
          ? opponent.getContactNameRole()
          : opponent.getOrganisationName();
    }
    if (!OpponentUtil.INDIVIDUAL_OPPONENT.equalsIgnoreCase(opponent.getType())) {
      return "undefined";
    }
    return OpponentUtil.getPartyName(opponent, context.getContactTitle().getContent());
  }

  @Named("relationshipToCase")
  default String relationshipToCase(
      final OpponentDetail opponent, @Context final LiablePartiesMappingContext context) {
    return OpponentUtil.getRelationshipToCase(
            opponent,
            context.getOrganisationRelationshipsToCase().getContent(),
            context.getIndividualRelationshipsToCase().getContent())
        .getDescription();
  }

  @Named("awardPartyEditable")
  default boolean awardPartyEditable(final String ebsId) {
    return ebsId != null && ebsId.toUpperCase(Locale.ROOT).startsWith("AWARD");
  }

  @Named("compactAddress")
  default String compactAddress(final AddressDetail address) {
    return address == null
        ? ""
        : Stream.of(address.getAddressLine1(), address.getPostcode())
            .filter(StringUtils::hasText)
            .collect(Collectors.joining(" "));
  }

  default BigDecimal awardAmount(final BaseAwardDetail award) {
    if (award instanceof CostAwardDetail cost) {
      return Stream.of(
              cost.getPreCertificateLscCost(), cost.getPreCertificateOtherCost(),
              cost.getCertificateCostLsc(), cost.getCertificateCostMarket())
          .filter(Objects::nonNull)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    if (award instanceof LandAwardDetail land) {
      return land.getValuationAmount();
    }
    if (award instanceof OtherAssetAwardDetail asset) {
      return asset.getValuationAmount();
    }
    return award.getAwardAmount();
  }
}
