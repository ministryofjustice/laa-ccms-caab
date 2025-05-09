package uk.gov.laa.ccms.caab.config;

import org.springframework.stereotype.Component;


/**
 * Provides access to user role codes.
 * This class is used in thymeleaf templates to access role codes.
 */
@Component("roles")
public class UserRoleProvider {

  public String getAmendCase() {
    return UserRole.AMEND_CASE.getCode();
  }

  public String getAddProceeding() {
    return UserRole.ADD_PROCEEDING.getCode();
  }

  public String getViewNotificationAttachment() {
    return UserRole.VIEW_NOTIFICATION_ATTACHMENT.getCode();
  }

  public String getCreateBill() {
    return UserRole.CREATE_BILL.getCode();
  }

  public String getCreateApplication() {
    return UserRole.CREATE_APPLICATION.getCode();
  }

  public String getViewCaseBill() {
    return UserRole.VIEW_CASE_BILL.getCode();
  }

  public String getViewClientDetails() {
    return UserRole.VIEW_CLIENT_DETAILS.getCode();
  }

  public String getClearOutcome() {
    return UserRole.CLEAR_OUTCOME.getCode();
  }

  public String getDeleteBill() {
    return UserRole.DELETE_BILL.getCode();
  }

  public String getDeleteProceeding() {
    return UserRole.DELETE_PROCEEDING.getCode();
  }

  public String getUploadEvidence() {
    return UserRole.UPLOAD_EVIDENCE.getCode();
  }

  public String getViewNotifications() {
    return UserRole.VIEW_NOTIFICATIONS.getCode();
  }

  public String getCreatePaymentOnAccount() {
    return UserRole.CREATE_PAYMENT_ON_ACCOUNT.getCode();
  }

  public String getRequestCaseDischarge() {
    return UserRole.REQUEST_CASE_DISCHARGE.getCode();
  }

  public String getCreateCaseRequest() {
    return UserRole.CREATE_CASE_REQUEST.getCode();
  }

  public String getCreateProviderRequest() {
    return UserRole.CREATE_PROVIDER_REQUEST.getCode();
  }

  public String getSubmitAmendment() {
    return UserRole.SUBMIT_AMENDMENT.getCode();
  }

  public String getSubmitApplication() {
    return UserRole.SUBMIT_APPLICATION.getCode();
  }

  public String getSubmitBill() {
    return UserRole.SUBMIT_BILL.getCode();
  }

  public String getSubmitDocumentUpload() {
    return UserRole.SUBMIT_DOCUMENT_UPLOAD.getCode();
  }

  public String getSubmitNotification() {
    return UserRole.SUBMIT_NOTIFICATION.getCode();
  }

  public String getSubmitRegisterClient() {
    return UserRole.SUBMIT_REGISTER_CLIENT.getCode();
  }

  public String getSubmitCaseOutcome() {
    return UserRole.SUBMIT_CASE_OUTCOME.getCode();
  }

  public String getSubmitPaymentOnAccount() {
    return UserRole.SUBMIT_PAYMENT_ON_ACCOUNT.getCode();
  }

  public String getSubmitUpdateClient() {
    return UserRole.SUBMIT_UPDATE_CLIENT.getCode();
  }

  public String getEnterUndertaking() {
    return UserRole.ENTER_UNDERTAKING.getCode();
  }

  public String getUpdateProceedingOutcome() {
    return UserRole.UPDATE_PROCEEDING_OUTCOME.getCode();
  }

  public String getUpdateProceeding() {
    return UserRole.UPDATE_PROCEEDING.getCode();
  }

  public String getViewCaseDetails() {
    return UserRole.VIEW_CASE_DETAILS.getCode();
  }

  public String getViewProceedingOutcome() {
    return UserRole.VIEW_PROCEEDING_OUTCOME.getCode();
  }

  public String getViewProceeding() {
    return UserRole.VIEW_PROCEEDING.getCode();
  }

  public String getViewOutcome() {
    return UserRole.VIEW_OUTCOME.getCode();
  }

  public String getViewCasesAndApplications() {
    return UserRole.VIEW_CASES_AND_APPLICATIONS.getCode();
  }

  public String getAllowFastForward() {
    return UserRole.ALLOW_FAST_FORWARD.getCode();
  }
}
