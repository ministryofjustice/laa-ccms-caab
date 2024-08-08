package uk.gov.laa.ccms.caab.mapper.context.submission;

import lombok.Builder;
import lombok.Data;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;

/**
 * Context for mapping opponent submission summary details.
 */
@Data
@Builder
public class OpponentSubmissionSummaryMappingContext {

  /**
   * The contact title lookup detail.
   */
  private CommonLookupDetail contactTitle;

  /**
   * The organisation relationships to case lookup detail.
   */
  private RelationshipToCaseLookupDetail organisationRelationshipsToCase;

  /**
   * The individual relationships to case lookup detail.
   */
  private RelationshipToCaseLookupDetail individualRelationshipsToCase;

  /**
   * The relationship to client lookup detail.
   */
  private CommonLookupDetail relationshipToClient;

  /**
   * The country lookup detail.
   */
  private CommonLookupDetail country;
}
