package uk.gov.laa.ccms.caab.bean.request;

import lombok.Data;

/** Holds the flow data for a provider request, including the request type form data. */
@Data
public class ProviderRequestFlowFormData {

  private ProviderRequestTypeFormData requestTypeFormData;

  private ProviderRequestDetailsFormData requestDetailsFormData;

  private String caseReferenceNumber;

  /** The case reference and request type the current {@code requestDetailsFormData} belongs to. */
  private String requestDetailsContext;

  public ProviderRequestFlowFormData() {
    this.requestTypeFormData = new ProviderRequestTypeFormData();
    this.requestDetailsFormData = new ProviderRequestDetailsFormData();
  }

  public void resetRequestDetailsFormData() {
    this.requestDetailsFormData = new ProviderRequestDetailsFormData();
  }

  /**
   * Resets the entire flow, clearing the selected request type, request details and case reference
   * so a new request cannot inherit context from a previous one.
   */
  public void reset() {
    this.requestTypeFormData = new ProviderRequestTypeFormData();
    this.requestDetailsFormData = new ProviderRequestDetailsFormData();
    this.caseReferenceNumber = null;
    this.requestDetailsContext = null;
  }

  /**
   * Ties the in-progress request details to the enquiry they were entered for. Only one enquiry's
   * details are held in the session, so returning to an earlier wizard page (via the back button)
   * would otherwise render — or submit — a different enquiry's answers and document session. When
   * the details belong to another enquiry they are discarded in favour of an empty form.
   *
   * @param caseReferenceNumber the case reference of the page being rendered or submitted
   * @param requestType the request type of the page being rendered or submitted
   */
  public void alignRequestDetailsTo(final String caseReferenceNumber, final String requestType) {
    final String context = caseReferenceNumber + "|" + requestType;
    if (requestDetailsContext != null && !requestDetailsContext.equals(context)) {
      resetRequestDetailsFormData();
    }
    this.requestDetailsContext = context;
  }
}
