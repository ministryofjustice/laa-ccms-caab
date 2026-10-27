package uk.gov.laa.ccms.caab.mapper.context;

import lombok.Builder;
import lombok.Data;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;

/** Reference data used to display liable and interested parties. */
@Data
@Builder
public class LiablePartiesMappingContext {
  private CommonLookupDetail contactTitle;
  private RelationshipToCaseLookupDetail organisationRelationshipsToCase;
  private RelationshipToCaseLookupDetail individualRelationshipsToCase;
}
