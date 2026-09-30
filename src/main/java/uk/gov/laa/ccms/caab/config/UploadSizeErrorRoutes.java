package uk.gov.laa.ccms.caab.config;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.EVIDENCE_UPLOAD_FORM_DATA;

import uk.gov.laa.ccms.caab.constants.CaseContext;
import uk.gov.laa.ccms.caab.constants.ProviderRequestFlowType;
import uk.gov.laa.ccms.caab.constants.SendBy;

/** Resolves upload endpoints to the form model used to display a file-size error. */
final class UploadSizeErrorRoutes {

  private UploadSizeErrorRoutes() {}

  static String formName(String path) {
    for (ProviderRequestFlowType flow : ProviderRequestFlowType.values()) {
      if (path.equals(flow.getBasePath() + "/documents")) {
        return flow.getEvidenceUploadSessionAttribute();
      }
      if (path.equals(flow.getBasePath() + "/details")) {
        return "providerRequestDetails";
      }
    }
    for (CaseContext context : CaseContext.values()) {
      if (path.equals("/" + context.getPathValue() + "/evidence/add")) {
        return EVIDENCE_UPLOAD_FORM_DATA;
      }
    }
    if (path.equals("/case/outcome-and-awards/document/upload")) {
      return "outcomeAndAwardsDocumentUploadForm";
    }
    if (isNotificationAttachmentPath(path)) {
      return "attachmentUploadFormData";
    }
    return null;
  }

  static String redirectQuery(String path, String query) {
    if (isNotificationAttachmentPath(path) && query == null) {
      return "sendBy=" + SendBy.ELECTRONIC;
    }
    return query;
  }

  private static boolean isNotificationAttachmentPath(String path) {
    return path.matches("/notifications/[^/]+/attachments/upload");
  }
}
