package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;

import jakarta.validation.Validator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentFormData;
import uk.gov.laa.ccms.caab.bean.validators.opponent.IndividualOpponentOtherDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.IndividualOpponentValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.OrganisationOpponentValidator;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.OpponentMapper;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.sections.PartyListRow;
import uk.gov.laa.ccms.caab.util.OpponentUtil;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;

/** Builds the party list used to add existing case opponents to an award. */
@Service
@RequiredArgsConstructor
public class PartyListService {

  private final ApplicationService applicationService;
  private final CaseOutcomeService caseOutcomeService;
  private final LookupService lookupService;
  private final IndividualOpponentFormService individualOpponentFormService;
  private final OpponentMapper opponentMapper;
  private final IndividualOpponentValidator individualOpponentValidator;
  private final OrganisationOpponentValidator organisationOpponentValidator;
  private final IndividualOpponentOtherDetailsValidator individualOpponentOtherDetailsValidator;
  private final Validator beanValidator;

  /**
   * A case opponent that can be selected on the party list.
   *
   * @param key the stable key submitted when the opponent is selected
   * @param opponent the case opponent
   * @param alreadyAdded whether the opponent is already linked to the award
   */
  public record PartyListCandidate(String key, OpponentDetail opponent, boolean alreadyAdded) {

    /**
     * Checks whether the opponent has no CAAB id, so must be created before it can be linked.
     *
     * @return true if the opponent must be created
     */
    public boolean requiresCreation() {
      return opponent.getId() == null;
    }
  }

  /**
   * Lists every case opponent that has an identity it can be linked or created by.
   *
   * <p>If there is a persisted draft for this case, its opponents are reloaded from the backend,
   * because the opponent journeys save changes without refreshing the in-session application.
   *
   * @param ebsCase the case whose outcome owns the award
   * @param currentApplication the in-session application, if any
   * @param providerId the provider owning the case
   * @param awardPath the award type's URL path segment
   * @param awardId the award parties are being added to
   * @param createdEbsIds EBS ids of opponents already created and linked to this award; needed
   *     because the CAAB API replaces a created opponent's EBS id with a synthetic one
   * @return the candidates, in case opponent order
   * @throws CaabApplicationException if a persisted draft cannot be reloaded
   */
  public List<PartyListCandidate> getCandidates(
      final ApplicationDetail ebsCase,
      final ApplicationDetail currentApplication,
      final Integer providerId,
      final String awardPath,
      final Integer awardId,
      final Set<String> createdEbsIds) {
    final Set<Integer> linkedIds = new HashSet<>();
    final Set<String> linkedEbsIds = new HashSet<>(createdEbsIds);
    for (final OpponentDetail linked :
        caseOutcomeService.getAwardLiableParties(
            ebsCase.getCaseReferenceNumber(), providerId, awardPath, awardId)) {
      linkedIds.add(linked.getId());
      linkedEbsIds.add(linked.getEbsId());
    }
    return caseOpponents(ebsCase, currentApplication).stream()
        .filter(Objects::nonNull)
        .flatMap(
            opponent ->
                key(opponent).stream()
                    .map(
                        key ->
                            new PartyListCandidate(
                                key,
                                opponent,
                                linkedIds.contains(opponent.getId())
                                    || linkedEbsIds.contains(opponent.getEbsId()))))
        .toList();
  }

  /**
   * Builds the display rows for the party list.
   *
   * @param candidates the candidates to display
   * @return one row per candidate
   */
  public List<PartyListRow> toRows(final List<PartyListCandidate> candidates) {
    final var titles =
        lookupService
            .getCommonValues(COMMON_VALUE_CONTACT_TITLE)
            .blockOptional()
            .map(CommonLookupDetail::getContent)
            .orElse(List.of());
    final var organisationRelationships =
        lookupService
            .getOrganisationToCaseRelationships()
            .blockOptional()
            .map(RelationshipToCaseLookupDetail::getContent)
            .orElse(List.of());
    final var personRelationships =
        lookupService
            .getPersonToCaseRelationships()
            .blockOptional()
            .map(RelationshipToCaseLookupDetail::getContent)
            .orElse(List.of());
    return candidates.stream()
        .map(
            candidate -> {
              final OpponentDetail opponent = candidate.opponent();
              return new PartyListRow(
                  candidate.key(),
                  OpponentUtil.getPartyName(opponent, titles),
                  opponent.getType(),
                  OpponentUtil.getRelationshipToCase(
                          opponent, organisationRelationships, personRelationships)
                      .getDescription(),
                  compactAddress(opponent.getAddress()),
                  candidate.alreadyAdded());
            })
        .toList();
  }

  /**
   * Finds the selected opponents that must be created but would fail the validation applied on the
   * dedicated add screens. Opponents with a CAAB id are linked as they are, so are not checked.
   *
   * @param selected the selected candidates
   * @return the keys of the candidates that cannot be created
   * @throws CaabApplicationException if the relationship lookup used for validation fails
   */
  public Set<String> findInvalid(final List<PartyListCandidate> selected) {
    return selected.stream()
        .filter(PartyListCandidate::requiresCreation)
        .filter(candidate -> !isValid(candidate.opponent()))
        .map(PartyListCandidate::key)
        .collect(Collectors.toSet());
  }

  private List<OpponentDetail> caseOpponents(
      final ApplicationDetail ebsCase, final ApplicationDetail currentApplication) {
    final boolean draftForThisCase =
        currentApplication != null
            && currentApplication.getId() != null
            && Objects.equals(
                currentApplication.getCaseReferenceNumber(), ebsCase.getCaseReferenceNumber());
    final ApplicationDetail source =
        draftForThisCase
            ? applicationService
                .getApplication(String.valueOf(currentApplication.getId()))
                .blockOptional()
                .orElseThrow(
                    () ->
                        new CaabApplicationException(
                            "Failed to reload application " + currentApplication.getId()))
            : ebsCase;
    return Optional.ofNullable(source.getOpponents()).orElse(List.of());
  }

  /** Keys an opponent by its CAAB id, or by its EBS id when it has not been added to CAAB yet. */
  private static Optional<String> key(final OpponentDetail opponent) {
    if (opponent.getId() != null) {
      return Optional.of("id-" + opponent.getId());
    }
    return Optional.ofNullable(opponent.getEbsId())
        .filter(StringUtils::hasText)
        .map(ebsId -> "ebs-" + ebsId);
  }

  private boolean isValid(final OpponentDetail opponent) {
    if (OpponentUtil.isOrganisation(opponent)) {
      return isValid(
          opponentMapper.toOrganisationOpponentFormData(
              opponent, null, null, null, null, null, false),
          "organisationOpponent",
          organisationOpponentValidator::validate);
    }
    if (!OpponentUtil.INDIVIDUAL_OPPONENT.equalsIgnoreCase(opponent.getType())) {
      return false;
    }
    final IndividualOpponentFormData contact =
        opponentMapper.toIndividualOpponentFormData(opponent, null, null, null, null, false);
    individualOpponentFormService.prepareForValidation(contact);
    return isValid(contact, "individualOpponent", individualOpponentValidator::validateLiableParty)
        && isValid(
            opponentMapper.toIndividualOpponentOtherDetailsFormData(opponent),
            "individualOpponentOtherDetails",
            individualOpponentOtherDetailsValidator::validate);
  }

  /** Applies Bean Validation followed by the given custom validation to a form. */
  private <T> boolean isValid(
      final T form, final String objectName, final BiConsumer<T, Errors> customValidation) {
    final Errors errors = new BeanPropertyBindingResult(form, objectName);
    new SpringValidatorAdapter(beanValidator).validate(form, errors);
    customValidation.accept(form, errors);
    return !errors.hasErrors();
  }

  private static String compactAddress(final AddressDetail address) {
    return address == null
        ? ""
        : Stream.of(address.getAddressLine1(), address.getPostcode())
            .filter(StringUtils::hasText)
            .collect(Collectors.joining(" "));
  }
}
