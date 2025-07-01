package uk.gov.laa.ccms.caab.mapper.context;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.PriorAuthority;
import uk.gov.laa.ccms.data.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;

/**
 * Class to hold a EBS PriorAuthority together with all additional data needed to perform a mapping
 * to a CAAB PriorAuthority.
 */
@Builder
@Data
public class EbsPriorAuthorityMappingContext {

  /** The EBS Prior Authority to be mapped. */
  PriorAuthority ebsPriorAuthority;

  /** Lookup of Prior Authority Type display data. */
  PriorAuthorityTypeDetail priorAuthorityTypeLookup;

  /**
   * A List of PriorAuthorityDetail for this Prior Authority, paired with a lookup of display data.
   */
  List<Pair<PriorAuthorityDetail, CommonLookupValueDetail>> items;
}
