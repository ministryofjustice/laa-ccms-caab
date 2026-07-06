package uk.gov.laa.ccms.caab.bean.billing;

import java.util.List;
import lombok.Data;
import uk.gov.laa.ccms.soa.gateway.model.SoaAmountSummary;
import uk.gov.laa.ccms.soa.gateway.model.SoaInvoice;

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

  /** The case-wide total figures, as returned by EBS (includes the counsel cost ceiling). */
  private SoaAmountSummary total;

  /** The bills and payment-on-account invoices across all firms. */
  private List<SoaInvoice> invoices;
}
