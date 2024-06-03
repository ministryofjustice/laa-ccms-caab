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

  private final OpponentDetail opponent;
  private final CommonLookupValueDetail titleCommonLookupValue;

}
