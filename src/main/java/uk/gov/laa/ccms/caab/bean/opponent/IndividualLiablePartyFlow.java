package uk.gov.laa.ccms.caab.bean.opponent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/** Award-specific draft, deliberately separate from the application opponent session attributes. */
@Getter
@RequiredArgsConstructor
public class IndividualLiablePartyFlow {
  private final String caseReference;
  private final Integer providerId;
  private final String awardPath;
  private final Integer awardId;
  private final Integer opponentId;

  public IndividualLiablePartyFlow(
      final String caseReference,
      final Integer providerId,
      final String awardPath,
      final Integer awardId) {
    this(caseReference, providerId, awardPath, awardId, null);
  }

  public boolean isEditing() {
    return opponentId != null;
  }

  @Setter private IndividualOpponentFormData contactDetails = new IndividualOpponentFormData();

  @Setter
  private IndividualOpponentOtherDetailsFormData otherDetails =
      new IndividualOpponentOtherDetailsFormData();

  @Setter private boolean contactDetailsComplete;
}
