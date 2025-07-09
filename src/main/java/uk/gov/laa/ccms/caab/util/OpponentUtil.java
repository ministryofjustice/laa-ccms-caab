package uk.gov.laa.ccms.caab.util;

import java.util.List;
import org.springframework.util.StringUtils;
import uk.gov.laa.ccms.caab.constants.assessment.InstanceMappingPrefix;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;

/** Utility class for handling proceeding-related operations. */
public final class OpponentUtil {

  private static final String OPPONENT_TYPE_ORGANISATION = "ORGANISATION";
  private static final String OPPONENT_TYPE_PERSON = "PERSON";
  public static final String INDIVIDUAL_OPPONENT = "Individual";

  /**
   * Retrieves the assessment mapping ID for the given opponent detail.
   *
   * @param opponent the opponent detail
   * @return the assessment mapping ID, prefixed if EBS ID is not available
   */
  public static String getAssessmentMappingId(final OpponentDetail opponent) {
    return opponent.getEbsId() == null
        ? InstanceMappingPrefix.OPPONENT.getPrefix() + opponent.getId()
        : opponent.getEbsId();
  }

  /**
   * Determines the assessment opponent type based on the given type string.
   *
   * @param type the type string to check
   * @return the opponent type, either as an organisation or a person
   */
  public static String getAssessmentOpponentType(final String type) {
    return INDIVIDUAL_OPPONENT.equalsIgnoreCase(type)
        ? OPPONENT_TYPE_PERSON
        : OPPONENT_TYPE_ORGANISATION;
  }

  /**
   * Determines if the given opponent detail represents an organisation.
   *
   * @param opponent the opponent detail
   * @return {@code true} if the opponent is an organisation, {@code false} otherwise
   */
  public static boolean isOrganisation(final OpponentDetail opponent) {
    return OPPONENT_TYPE_ORGANISATION.equalsIgnoreCase(opponent.getType());
  }

  /**
   * Retrieves the display name of the given opponent based on its type.
   *
   * @param opponent the opponent detail
   * @param titleLookup the common lookup value detail for title descriptions
   * @return the display name of the opponent
   */
  public static String getPartyName(
      final OpponentDetail opponent, final CommonLookupValueDetail titleLookup) {

    return isOrganisation(opponent)
        ? opponent.getOrganisationName()
        : getFullName(opponent, titleLookup);
  }

  /**
   * Retrieves the display name of the given opponent based on its type.
   *
   * @param opponent the opponent detail
   * @param titleLookups lookup of title values
   * @return the display name of the opponent
   */
  public static String getPartyName(
      final OpponentDetail opponent, final List<CommonLookupValueDetail> titleLookups) {

    // Find the correct title lookup for this opponent.
    CommonLookupValueDetail titleLookup =
        titleLookups.stream()
            .filter(title -> title.getCode().equals(opponent.getTitle()))
            .findFirst()
            .orElse(
                new CommonLookupValueDetail()
                    .code(opponent.getTitle())
                    .description(opponent.getTitle()));

    return getPartyName(opponent, titleLookup);
  }

  /**
   * Constructs the full name of the given opponent based on its details and title lookup.
   *
   * @param opponent the opponent detail
   * @param titleLookup the common lookup value detail for title descriptions
   * @return the full name of the opponent, or "undefined" if no name is available
   */
  protected static String getFullName(
      final OpponentDetail opponent, final CommonLookupValueDetail titleLookup) {
    final StringBuilder builder = new StringBuilder();

    if (StringUtils.hasText(opponent.getTitle())) {
      final String displayTitle = titleLookup.getDescription();
      if (displayTitle != null) {
        builder.append(displayTitle);
      } else {
        builder.append(opponent.getTitle());
      }
    }
    if (StringUtils.hasText(opponent.getFirstName())) {
      if (!builder.isEmpty()) {
        builder.append(" ");
      }
      builder.append(opponent.getFirstName());
    }

    if (StringUtils.hasText(opponent.getSurname())) {
      if (!builder.isEmpty()) {
        builder.append(" ");
      }
      builder.append(opponent.getSurname());
    }
    return builder.isEmpty() ? "undefined" : builder.toString();
  }

  /**
   * Retrieves an opponent detail by the specified EBS ID from the given application detail.
   *
   * @param application the application detail containing the opponents
   * @param id the EBS ID of the opponent to retrieve
   * @return the opponent detail with the specified EBS ID, or {@code null} if not found
   */
  public static OpponentDetail getOpponentByEbsId(
      final ApplicationDetail application, final String id) {
    if (application.getOpponents() != null && id != null) {
      for (final OpponentDetail opponent : application.getOpponents()) {
        if (id.equals(opponent.getEbsId())) {
          return opponent;
        }
      }
    }
    return null;
  }

  /**
   * Retrieves an opponent detail by the specified ID from the given application detail.
   *
   * @param application the application detail containing the opponents
   * @param id the ID of the opponent to retrieve
   * @return the opponent detail with the specified ID, or {@code null} if not found
   */
  public static OpponentDetail getOpponentById(
      final ApplicationDetail application, final Integer id) {
    if (application.getOpponents() != null && id != null) {
      for (final OpponentDetail opponent : application.getOpponents()) {
        if (opponent.getId().equals(id)) {
          return opponent;
        }
      }
    }
    return null;
  }

  /**
   * Find the relevant relationship to case for the supplied opponent, depending on whether it is an
   * organisation of individual.
   *
   * @param opponent - the opponent.
   * @param organisationRelationships - the list of organisation relationship lookups.
   * @param personRelationships - the list of individual relationship lookups
   * @return the relationship lookup.
   */
  public static RelationshipToCaseLookupValueDetail getRelationshipToCase(
      final OpponentDetail opponent,
      final List<RelationshipToCaseLookupValueDetail> organisationRelationships,
      final List<RelationshipToCaseLookupValueDetail> personRelationships) {
    List<RelationshipToCaseLookupValueDetail> relationships =
        isOrganisation(opponent) ? organisationRelationships : personRelationships;

    return relationships.stream()
        .filter(rel -> rel.getCode().equals(opponent.getRelationshipToCase()))
        .findFirst()
        .orElse(
            new RelationshipToCaseLookupValueDetail()
                .code(opponent.getRelationshipToCase())
                .description(opponent.getRelationshipToCase()));
  }

  /**
   * Find the relevant relationship to client lookup for the supplied opponent.
   *
   * @param opponent - the opponent.
   * @param relationships - the list of relationship lookups.
   * @return the relationship lookup.
   */
  public static CommonLookupValueDetail getRelationshipToClient(
      final OpponentDetail opponent, final List<CommonLookupValueDetail> relationships) {
    return relationships.stream()
        .filter(rel -> rel.getCode().equals(opponent.getRelationshipToClient()))
        .findFirst()
        .orElse(
            new CommonLookupValueDetail()
                .code(opponent.getRelationshipToClient())
                .description(opponent.getRelationshipToClient()));
  }

  /**
   * Find the relevant relationship to client lookup for the supplied opponent.
   *
   * @param opponent - the opponent.
   * @param relationships - the list of relationship lookups.
   * @return the relationship lookup.
   */
  public static CommonLookupValueDetail getOrganisationType(
      final OpponentDetail opponent, final List<CommonLookupValueDetail> relationships) {
    return relationships.stream()
        .filter(rel -> rel.getCode().equals(opponent.getOrganisationType()))
        .findFirst()
        .orElse(
            new CommonLookupValueDetail()
                .code(opponent.getOrganisationType())
                .description(opponent.getOrganisationType()));
  }

  private OpponentUtil() {}
}
