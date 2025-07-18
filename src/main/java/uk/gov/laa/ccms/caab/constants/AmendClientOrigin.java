package uk.gov.laa.ccms.caab.constants;

import lombok.Getter;

/**
 * Enum representing different origins from which the Amend Client feature can be accessed. Each
 * enum constant is associated with a specific URL path and a corresponding text code.
 *
 * @author Jamie Briggs
 */
@Getter
public enum AmendClientOrigin {
  CASE_OVERVIEW("/case/overview", "site.cancelAndReturnToCaseOverview"),
  VIEW_CASE_DETAILS("/case/details", "site.cancelAndReturnToCaseDetails"),
  AMEND_CASE("/amendments/summary", "site.cancelAndReturnToAmendmentsSummary");

  private final String href;
  private final String textCode;

  AmendClientOrigin(String href, String textCode) {
    this.href = href;
    this.textCode = textCode;
  }
}
