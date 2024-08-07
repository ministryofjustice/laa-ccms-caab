package uk.gov.laa.ccms.caab.mapper.context.submission;

import lombok.Builder;
import lombok.Data;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;

@Data
@Builder
public class ProceedingSubmissionSummaryMappingContext {

  CommonLookupDetail typeOfOrder;

}
