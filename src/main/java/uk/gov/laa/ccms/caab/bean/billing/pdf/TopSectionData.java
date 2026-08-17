package uk.gov.laa.ccms.caab.bean.billing.pdf;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * The header block of the POA summary report: the case and provider identifiers alongside the
 * headline financial figures. Ports the legacy PUI {@code TopSectionData}.
 */
@Data
@Builder
public class TopSectionData {

  private String caseReference;
  private String providerName;
  private String billType;
  private String description;
  private String caseStatus;

  /** The legacy report leaves the total POA blank, so it is carried as a (usually empty) string. */
  private String totalPoa;

  private BigDecimal totalBills;
  private String courtAssessedBill;
  private BigDecimal costLimit;
  private BigDecimal availableCostLimit;
  private BigDecimal totalValueOfClaim;
}
