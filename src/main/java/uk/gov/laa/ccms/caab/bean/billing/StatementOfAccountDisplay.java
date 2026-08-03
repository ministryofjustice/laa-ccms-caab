package uk.gov.laa.ccms.caab.bean.billing;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 * View model for the Case Statement of Account screen. Combines the aggregated figures (shown per
 * current provider / prior solicitor / counsel column, plus the case-wide totals) with the flat
 * list of bills and payment-on-account invoices.
 *
 * <p>This mirrors the legacy PUI {@code CB01View} + {@code billPoaSummary} data.
 */
@Data
public class StatementOfAccountDisplay {

  /** The display name of the current provider firm (the first figures column header). */
  private String providerFirmName;

  /** Whether the logged-in user belongs to the provider that owns the case. */
  private boolean userBelongsToCurrentProvider;

  /** Figures for the current provider. */
  private SoaFigureColumn provider;

  /** Aggregated figures across all prior solicitor firms. */
  private SoaFigureColumn priorSolicitor;

  /** Aggregated figures across all counsel firms. */
  private SoaFigureColumn counsel;

  /** The case-wide total figures, derived by summing the three columns. */
  private SoaFigureColumn total;

  /** The counsel cost ceiling. EBS holds this against the case, not against a firm. */
  private BigDecimal counselCostCeiling;

  /** The part of the counsel cost ceiling not yet billed. */
  private BigDecimal counselCostCeilingRemaining;

  /**
   * The bills and payment-on-account rows: the submitted / authorised / rejected invoices from EBS
   * plus the provider's own draft bill and draft payments on account.
   */
  private List<BillPoaRow> billsAndPoa;

  /** Whether the provider already has a draft bill, so a new one cannot be created. */
  private boolean draftBillExists;

  /**
   * Whether the provider already has a draft payment on account, so a new one cannot be created.
   */
  private boolean draftPoaExists;
}
