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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
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
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;

/** Maps award associations and outcome opponents to the party summary. */
@Mapper(componentModel = "spring", imports = OpponentUtil.class)
public interface LiablePartiesDisplayMapper {

  /**
   * Builds the liable or interested party summary for an award.
   *
   * @param award the award whose liable party links are displayed
   * @param opponents the opponents linked to the award
   * @param context the lookup data used to describe each party
   * @return the party summary
   */
  default LiablePartiesDisplay toLiablePartiesDisplay(
      final BaseAwardDetail award,
      final List<OpponentDetail> opponents,
      @Context final LiablePartiesMappingContext context) {
    final Map<Integer, OpponentDetail> opponentIndex =
        Optional.ofNullable(opponents).orElse(List.of()).stream()
            .filter(
                opponent -> {
                  if (opponent == null || opponent.getId() == null) {
                    log().warn("Ignoring liable party opponent with no id");
                    return false;
                  }
                  return true;
                })
            .collect(
                Collectors.toMap(OpponentDetail::getId, Function.identity(), (first, _) -> first));
    final List<LiablePartyDisplay> parties =
        Optional.ofNullable(award.getLiableParties()).orElse(List.of()).stream()
            .filter(
                link -> {
                  if (link == null || link.getOpponentId() == null) {
                    log().warn("Ignoring liable party link with no opponent id");
                    return false;
                  }
                  return true;
                })
            .map(
                link ->
                    Optional.ofNullable(opponentIndex.get(link.getOpponentId()))
                        .map(opponent -> toLiablePartyDisplay(link, opponent, context))
                        // The link still exists, so show it rather than hide a real liability.
                        .orElseGet(
                            () -> {
                              log()
                                  .warn(
                                      "Liable party {} has unresolved opponent id {}",
                                      link.getId(),
                                      link.getOpponentId());
                              return unresolvedLiablePartyDisplay(link);
                            }))
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
  @Mapping(target = "editable", source = "opponent", qualifiedByName = "awardPartyEditable")
  @Mapping(target = "organisation", expression = "java(OpponentUtil.isOrganisation(opponent))")
  @Mapping(target = "alreadyAdded", constant = "false")
  LiablePartyDisplay toLiablePartyDisplay(
      LiablePartyDetail party,
      OpponentDetail opponent,
      @Context LiablePartiesMappingContext context);

  /**
   * Builds a non-editable placeholder row for a liable party link whose opponent could not be
   * loaded. The name is left {@code null} so the view shows the localised placeholder text.
   *
   * @param party the liable party link
   * @return the placeholder row
   */
  default LiablePartyDisplay unresolvedLiablePartyDisplay(final LiablePartyDetail party) {
    return new LiablePartyDisplay(
        party.getId(), party.getOpponentId(), null, null, null, "", false, false, false);
  }

  /**
   * Builds a party list row for a case opponent that may be added to an award.
   *
   * @param rowId the id submitted when the row is selected
   * @param opponent the case opponent
   * @param alreadyAdded whether the opponent is already linked to the award
   * @param context the lookup data used to describe the party
   * @return the party list row
   */
  default LiablePartyDisplay toPartyListDisplay(
      final Integer rowId,
      final OpponentDetail opponent,
      final boolean alreadyAdded,
      final LiablePartiesMappingContext context) {
    return new LiablePartyDisplay(
        rowId,
        opponent.getId(),
        partyName(opponent, context),
        opponent.getType(),
        relationshipToCase(opponent, context),
        compactAddress(opponent.getAddress()),
        awardPartyEditable(opponent),
        OpponentUtil.isOrganisation(opponent),
        alreadyAdded);
  }

  @Named("partyName")
  default String partyName(
      final OpponentDetail opponent, @Context final LiablePartiesMappingContext context) {
    if (OpponentUtil.isOrganisation(opponent)) {
      // Contact name is only a fallback; some imported records have neither name.
      return Stream.of(opponent.getOrganisationName(), opponent.getContactNameRole())
          .filter(StringUtils::hasText)
          .findFirst()
          .orElse("undefined");
    }
    if (!OpponentUtil.INDIVIDUAL_OPPONENT.equalsIgnoreCase(opponent.getType())) {
      return "undefined";
    }
    return OpponentUtil.getPartyName(
        opponent,
        Optional.ofNullable(context)
            .map(LiablePartiesMappingContext::getContactTitle)
            .map(CommonLookupDetail::getContent)
            .orElse(List.of()));
  }

  @Named("relationshipToCase")
  default String relationshipToCase(
      final OpponentDetail opponent, @Context final LiablePartiesMappingContext context) {
    final Optional<LiablePartiesMappingContext> lookups = Optional.ofNullable(context);
    return OpponentUtil.getRelationshipToCase(
            opponent,
            lookups
                .map(LiablePartiesMappingContext::getOrganisationRelationshipsToCase)
                .map(RelationshipToCaseLookupDetail::getContent)
                .orElse(List.of()),
            lookups
                .map(LiablePartiesMappingContext::getIndividualRelationshipsToCase)
                .map(RelationshipToCaseLookupDetail::getContent)
                .orElse(List.of()))
        .getDescription();
  }

  /**
   * Checks whether a party is award-created and, for individuals, not shared.
   *
   * @param opponent the party whose edit link is being determined
   * @return true for award-created organisations and non-shared award-created individuals
   */
  @Named("awardPartyEditable")
  default boolean awardPartyEditable(final OpponentDetail opponent) {
    final String ebsId = opponent.getEbsId();
    return ebsId != null
        && ebsId.toUpperCase(Locale.ROOT).startsWith("AWARD")
        && (OpponentUtil.isOrganisation(opponent) || !Boolean.TRUE.equals(opponent.getSharedInd()));
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

  private static Logger log() {
    return LoggerFactory.getLogger(LiablePartiesDisplayMapper.class);
  }
}
