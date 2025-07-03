package uk.gov.laa.ccms.caab.mapper.context.submission;

import lombok.Builder;
import lombok.Data;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;

/** Context for mapping proceeding submission summary details. */
@Data
@Builder
public class ProceedingSubmissionSummaryMappingContext {

  /** The type of order lookup detail. */
  private CommonLookupDetail typeOfOrder;
}
