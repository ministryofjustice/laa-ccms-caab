package uk.gov.laa.ccms.caab.bean.billing.pdf;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * The totals footer of the POA summary report, computed from the claim lines. Ports the legacy PUI
 * {@code BottomSectionData}.
 */
@Data
@Builder
public class BottomSectionData {

  private String totalHoursMinutesClaimed;
  private BigDecimal totalNetClaim;
  private BigDecimal totalClaimIncVatAndUplift;
}
