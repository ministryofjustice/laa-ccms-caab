package uk.gov.laa.ccms.caab.bean.opponent;

import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import uk.gov.laa.ccms.caab.model.OrganisationResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;

/**
 * Award-specific organisation search and selection draft, deliberately separate from the
 * application opponent session attributes so that it does not depend on an in-progress application.
 */
@Getter
@RequiredArgsConstructor
public class OrganisationLiablePartyFlow {
  private final String journeyToken = UUID.randomUUID().toString();
  private final String caseReference;
  private final Integer providerId;
  private final String awardPath;
  private final Integer awardId;

  @Setter private OrganisationSearchCriteria searchCriteria = new OrganisationSearchCriteria();

  /** The latest page of results, used to check that a selected organisation was offered. */
  @Setter private ResultsDisplay<OrganisationResultRowDisplay> searchResults;

  /** The shared organisation chosen from the results, awaiting its relationship details. */
  @Setter private OrganisationOpponentFormData selectedOrganisation;
}
