package uk.gov.laa.ccms.caab.util;

import org.springframework.util.StringUtils;
import uk.gov.laa.ccms.caab.constants.assessment.InstanceMappingPrefix;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

/**
 * Utility class for handling proceeding-related operations.
 */
public class OpponentUtil {

  private static final String OPPONENT_TYPE_ORGANISATION = "ORGANISATION";
  private static final String OPPONENT_TYPE_PERSON = "PERSON";
  public static final String INDIVIDUAL_OPPONENT = "Individual";
  public static final String ORGANISATION_OPPONENT = "Organisation";


  /**
   * Retrieves the OPA instance mapping ID for the given opponent detail.
   *
   * @param opponent the opponent detail
   * @return the OPA instance mapping ID, prefixed if EBS ID is not available
   */
  public static String getOpaInstanceMappingId(final OpponentDetail opponent) {
    if (opponent.getEbsId() == null) {
      return InstanceMappingPrefix.OPPONENT.getPrefix() + opponent.getId();
    }
    return opponent.getEbsId();
  }

  /**
   * Determines the OPA opponent type based on the given type string.
   *
   * @param type the type string to check
   * @return the opponent type, either as an organisation or a person
   */
  public static String getOpaOpponentType(final String type) {
    if (OPPONENT_TYPE_ORGANISATION.equalsIgnoreCase(type)) {
      return OPPONENT_TYPE_ORGANISATION;
    } else {
      return OPPONENT_TYPE_PERSON;
    }
  }

  /**
   * Retrieves the display name of the given opponent based on its type.
   *
   * @param opponent the opponent detail
   * @param titleLookup the common lookup value detail for title descriptions
   * @return the display name of the opponent
   */
  public static String getDisplayName(
      final OpponentDetail opponent,
      final CommonLookupValueDetail titleLookup) {

    if (INDIVIDUAL_OPPONENT.equalsIgnoreCase(opponent.getType())) {
      return getFullName(opponent, titleLookup);
    } else if (ORGANISATION_OPPONENT.equalsIgnoreCase(opponent.getType())) {
      return opponent.getOrganisationName();
    } else {
      return getFullName(opponent, titleLookup);
    }
  }

  /**
   * Constructs the full name of the given opponent based on its details and title lookup.
   *
   * @param opponent the opponent detail
   * @param titleLookup the common lookup value detail for title descriptions
   * @return the full name of the opponent, or "undefined" if no name is available
   */
  protected static String getFullName(
      final OpponentDetail opponent,
      final CommonLookupValueDetail titleLookup) {
    final StringBuilder builder = new StringBuilder();

    if (INDIVIDUAL_OPPONENT.equalsIgnoreCase(opponent.getType())) {
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
    } else if (ORGANISATION_OPPONENT.equalsIgnoreCase(opponent.getType())) {
      if (StringUtils.hasText(opponent.getContactNameRole())) {
        builder.append(opponent.getContactNameRole());
      }
    }

    if (builder.isEmpty()) {
      return "undefined";
    } else {
      return builder.toString();
    }
  }

  /**
   * Retrieves an opponent detail by the specified EBS ID from the given application detail.
   *
   * @param application the application detail containing the opponents
   * @param id the EBS ID of the opponent to retrieve
   * @return the opponent detail with the specified EBS ID, or {@code null} if not found
   */
  public static OpponentDetail getOpponentByEbsId(
      final ApplicationDetail application,
      final String id) {
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
      final ApplicationDetail application,
      final Integer id) {
    if (application.getOpponents() != null && id != null) {
      for (final OpponentDetail opponent : application.getOpponents()) {
        if (opponent.getId().equals(id)) {
          return opponent;
        }
      }
    }
    return null;
  }
}
