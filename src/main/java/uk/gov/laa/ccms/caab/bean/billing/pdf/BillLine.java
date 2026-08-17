package uk.gov.laa.ccms.caab.bean.billing.pdf;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

/**
 * One claim line on the POA summary report, populated from a single {@code *_BILL_LINE} entity of
 * the completed billing assessment. Ports the legacy PUI {@code BillLine}.
 */
@Data
@Builder
public class BillLine {

  private int lineNumber;

  /** The claim date, kept for ordering; the report renders {@link #dateDisplay}. */
  private LocalDate date;

  private String dateDisplay;
  private String costType;
  private String categoryOfWork;
  private String workConducted;
  private String hoursMinClaimed;
  private String itemsClaimed;
  private BigDecimal rateClaimed;
  private BigDecimal upliftClaimed;
  private BigDecimal netClaim;
  private BigDecimal vat;
  private BigDecimal totalClaim;
  private String feeEarner;
  private String priorAuthority;
}
