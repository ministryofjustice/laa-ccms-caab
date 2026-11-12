package uk.gov.laa.ccms.caab.bean.opponent;

import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/** Award-specific draft, deliberately separate from the application opponent session attributes. */
@Getter
@RequiredArgsConstructor
public class IndividualLiablePartyFlow {
  private final String journeyToken = UUID.randomUUID().toString();
  private final String caseReference;
  private final Integer providerId;
  private final String awardPath;
  private final Integer awardId;
  private final Integer opponentId;

  /**
   * Creates an empty draft for adding an individual to an award.
   *
   * @param caseReference the case reference number
   * @param providerId the provider owning the case
   * @param awardPath the award type's URL path segment
   * @param awardId the award receiving the new party
   */
  public IndividualLiablePartyFlow(
      final String caseReference,
      final Integer providerId,
      final String awardPath,
      final Integer awardId) {
    this(caseReference, providerId, awardPath, awardId, null);
  }

  /**
   * Indicates whether this draft represents an existing opponent.
   *
   * @return true when the draft has an opponent ID
   */
  public boolean isEditing() {
    return opponentId != null;
  }

  @Setter private IndividualOpponentFormData contactDetails = new IndividualOpponentFormData();

  @Setter
  private IndividualOpponentOtherDetailsFormData otherDetails =
      new IndividualOpponentOtherDetailsFormData();

  @Setter private boolean contactDetailsComplete;
}
