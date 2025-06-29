package uk.gov.laa.ccms.caab.mapper.context.submission;

import lombok.Builder;
import lombok.Data;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;

/** Context for mapping general details submission summary. */
@Data
@Builder
public class GeneralDetailsSubmissionSummaryMappingContext {

  /** The preferred address lookup detail. */
  CommonLookupDetail preferredAddress;

  /** The country lookup detail. */
  CommonLookupDetail country;
}
