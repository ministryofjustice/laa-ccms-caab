package uk.gov.laa.ccms.caab.bean.billing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single row in the "Bills and payment on account" table on the Case Statement of Account screen.
 *
 * <p>Rows come from two sources: the submitted, authorised and rejected invoices returned by EBS,
 * and the provider's own draft bill and draft payments on account held in the CAAB store. Drafts
 * carry no submitted / authorised dates and are flagged so the view can offer the edit and delete
 * links, matching the legacy PUI {@code billPoaSummary}.
 *
 * @param type the invoice type, "Bill" or "POA".
 * @param status the invoice status, e.g. "Draft", "Submitted", "Authorised" or "Rejected".
 * @param dateSubmitted the date the invoice was submitted, if any.
 * @param dateAuthorised the date the invoice was authorised, if any.
 * @param amount the invoice amount.
 * @param draft whether the row is an unsubmitted draft held in the CAAB store.
 * @param copyable whether the row is a rejected bill the user may copy into a new draft.
 * @param billingIncidentId the EBS id the invoice is addressed by, needed to copy it. Drafts carry
 *     none, as they have never reached EBS.
 */
public record BillPoaRow(
    String type,
    String status,
    LocalDateTime dateSubmitted,
    LocalDateTime dateAuthorised,
    BigDecimal amount,
    boolean draft,
    boolean copyable,
    Long billingIncidentId) {

  /** Creates a row that carries no copy action, which is every row bar a rejected bill. */
  public BillPoaRow(
      final String type,
      final String status,
      final LocalDateTime dateSubmitted,
      final LocalDateTime dateAuthorised,
      final BigDecimal amount,
      final boolean draft) {
    this(type, status, dateSubmitted, dateAuthorised, amount, draft, false, null);
  }

  /** Creates a row for an invoice EBS holds, which can be addressed by its billing incident id. */
  public BillPoaRow(
      final String type,
      final String status,
      final LocalDateTime dateSubmitted,
      final LocalDateTime dateAuthorised,
      final BigDecimal amount,
      final boolean draft,
      final Long billingIncidentId) {
    this(type, status, dateSubmitted, dateAuthorised, amount, draft, false, billingIncidentId);
  }

  /** Returns a copy of this row with the copy action enabled. */
  public BillPoaRow withCopyable() {
    return new BillPoaRow(
        type, status, dateSubmitted, dateAuthorised, amount, draft, true, billingIncidentId);
  }
}
