package uk.gov.laa.ccms.caab.constants;

import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;

/**
 * The two things a provider submits from the case statement of account: a bill, or a payment on
 * account.
 *
 * <p>The legacy PUI treats these as one mechanism from the declaration onwards - {@code
 * RetrieveDeclarationText} branches on the submission type only to choose which assessment supplies
 * the {@code BILL_TYPE}, and both then run the same declaration screen and {@code
 * PerformSubmission}. This enum carries that difference so the declaration, submission and
 * confirmation can be shared here too.
 *
 * <p>The path value doubles as the URL segment and the message key segment, so {@code bill} and
 * {@code poa} keep the URLs and reference data they already had.
 */
public enum BillingContext {
  BILL("bill", AssessmentRulebase.BILLING),
  POA("poa", AssessmentRulebase.POA);

  private final String pathValue;
  private final AssessmentRulebase rulebase;

  BillingContext(final String pathValue, final AssessmentRulebase rulebase) {
    this.pathValue = pathValue;
    this.rulebase = rulebase;
  }

  public String getPathValue() {
    return pathValue;
  }

  /** The rulebase whose assessment backs this submission, and supplies its bill type. */
  public AssessmentRulebase getRulebase() {
    return rulebase;
  }

  /** The prefix the screens' message keys share, for example {@code billing.bill}. */
  public String getMessagePrefix() {
    return "billing." + pathValue;
  }

  /** The details screen this submission is made from. */
  public String getDetailsUrl() {
    return "/case/billing/" + pathValue;
  }

  /** The declaration screen for this submission. */
  public String getDeclarationUrl() {
    return getDetailsUrl() + "/declaration";
  }

  /** The confirmation shown once the submission has been made. */
  public String getConfirmationRedirect() {
    return "redirect:/case/billing/" + pathValue + "/confirmation";
  }

  public boolean isBill() {
    return this == BILL;
  }

  /**
   * Retrieves the BillingContext that corresponds to the given path value.
   *
   * @param pathValue the string representation of the path value to match against.
   * @return the matching BillingContext.
   * @throws IllegalArgumentException if the path value matches no constant.
   */
  public static BillingContext fromPathValue(final String pathValue) {
    for (final BillingContext context : BillingContext.values()) {
      if (context.pathValue.equals(pathValue)) {
        return context;
      }
    }
    throw new IllegalArgumentException("Invalid path value: " + pathValue);
  }
}
