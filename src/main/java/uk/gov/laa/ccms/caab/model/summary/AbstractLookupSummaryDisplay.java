package uk.gov.laa.ccms.caab.model.summary;

import java.util.HashMap;
import lombok.Builder;
import lombok.Data;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

@Data
public abstract class AbstractLookupSummaryDisplay {
  private HashMap<String, CommonLookupValueDetail> lookups =
      new HashMap<>();
}
