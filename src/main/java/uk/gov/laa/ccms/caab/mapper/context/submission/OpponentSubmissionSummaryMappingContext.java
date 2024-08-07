package uk.gov.laa.ccms.caab.mapper.context.submission;

import lombok.Builder;
import lombok.Data;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;

@Data
@Builder
public class OpponentSubmissionSummaryMappingContext {

  CommonLookupDetail contactTitle;
  RelationshipToCaseLookupDetail organisationRelationshipsToCase;
  RelationshipToCaseLookupDetail individualRelationshipsToCase;
  CommonLookupDetail relationshipToClient;

}
