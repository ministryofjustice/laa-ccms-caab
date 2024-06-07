package uk.gov.laa.ccms.caab.mapper.context;

import lombok.Builder;
import lombok.Getter;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;


/**
 * Context class for mapping an assessment opponent details.
 */
@Builder
@Getter
public class AssessmentOpponentMappingContext {

  /**
   * The opponent details.
   */
  private final OpponentDetail opponent;

  /**
   * The title common lookup value details, containing code and description.
   */
  private final CommonLookupValueDetail titleCommonLookupValue;


}
