package uk.gov.laa.ccms.caab.mapper.context;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import uk.gov.laa.ccms.soa.gateway.model.Award;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetail;

/** Class to hold all data needed to perform a mapping to a CAAB CaseOutcome. */
@Builder
@Data
public class SoaCaseOutcomeMappingContext {

  /** The SOA Case to extract attributes as part of the CaseOutcome mapping. */
  CaseDetail soaCase;

  /** The Cost Awards for the Outome. */
  List<Award> costAwards;

  /** The Land Awards for the Outome. */
  List<Award> landAwards;

  /** The Financial Awards for the Outome. */
  List<Award> financialAwards;

  /** The Other Asset Awards for the Outome. */
  List<Award> otherAssetAwards;

  /**
   * A flat list of SoaProceedingMappingContext for all Proceedings in the Case. This will be used
   * to map to ProceedingOutcomes.
   */
  List<SoaProceedingMappingContext> proceedingOutcomes;
}
