package uk.gov.laa.ccms.caab.mapper.context;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupValueDetail;
import uk.gov.laa.ccms.data.model.ProceedingDetail;
import uk.gov.laa.ccms.data.model.StageEndLookupValueDetail;
import uk.gov.laa.ccms.soa.gateway.model.ScopeLimitation;

/**
 * Class to hold a SOA ProceedingDetail together with all additional data needed to
 * perform a mapping to a CAAB ProceedingDetail.
 */
@Builder
@Data
public class ProceedingMappingContext {
  uk.gov.laa.ccms.soa.gateway.model.ProceedingDetail soaProceeding;

  /*
   * Lookup of additional data for this proceeding.
   */
  ProceedingDetail proceedingLookup;

  /*
   * Lookup of display data for this proceeding's status.
   */
  CommonLookupValueDetail proceedingStatusLookup;

  /*
   * The overall cost limitation for this proceeding.
   */
  BigDecimal proceedingCostLimitation;

  /*
   * Display details for this proceeding outcome's related Court.
   */
  CommonLookupValueDetail courtLookup;

  /*
   * Lookup of Outcome Result info for this Proceeding.
   */
  OutcomeResultLookupValueDetail outcomeResultLookup;

  /*
   * Lookup of the stage end display value for this proceeding and outcome.
   */
  StageEndLookupValueDetail stageEndLookup;

  /*
   * The lookup for Matter Type
   */
  CommonLookupValueDetail matterType;

  /*
   * The lookup for Level Of Service
   */
  CommonLookupValueDetail levelOfService;

  /*
   * The lookup for Client Involvement
   */
  CommonLookupValueDetail clientInvolvement;

  /*
   * A List of pairs of ScopeLimitation with associated lookup for display info.
   */
  List<Pair<ScopeLimitation, CommonLookupValueDetail>> scopeLimitations;

}
