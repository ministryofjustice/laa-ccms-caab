package uk.gov.laa.ccms.caab.constants;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_PROVIDER_REQUEST_EVIDENCE_UPLOAD_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_PROVIDER_REQUEST_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.GENERAL_PROVIDER_REQUEST_EVIDENCE_UPLOAD_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.GENERAL_PROVIDER_REQUEST_FLOW_FORM_DATA;

import lombok.Getter;

/** Identifies the supported provider request flows and their flow-specific configuration. */
@Getter
public enum ProviderRequestFlowType {
  GENERAL(
      "/general-provider-requests",
      GENERAL_PROVIDER_REQUEST_FLOW_FORM_DATA,
      GENERAL_PROVIDER_REQUEST_EVIDENCE_UPLOAD_FORM_DATA,
      false),
  CASE(
      "/case-provider-requests",
      CASE_PROVIDER_REQUEST_FLOW_FORM_DATA,
      CASE_PROVIDER_REQUEST_EVIDENCE_UPLOAD_FORM_DATA,
      true);

  private final String basePath;
  private final String flowSessionAttribute;
  private final String evidenceUploadSessionAttribute;
  private final boolean caseScoped;

  ProviderRequestFlowType(
      final String basePath,
      final String flowSessionAttribute,
      final String evidenceUploadSessionAttribute,
      final boolean caseScoped) {
    this.basePath = basePath;
    this.flowSessionAttribute = flowSessionAttribute;
    this.evidenceUploadSessionAttribute = evidenceUploadSessionAttribute;
    this.caseScoped = caseScoped;
  }
}
