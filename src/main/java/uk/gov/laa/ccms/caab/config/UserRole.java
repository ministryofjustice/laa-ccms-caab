package uk.gov.laa.ccms.caab.config;

import java.util.Arrays;
import lombok.Getter;

/** Enumerates the allowed user roles. */
@Getter
public enum UserRole {
  AMEND_CASE("AC", "amend cases"),
  ADD_PROCEEDING("ADDPROC", "add proceedings"),
  VIEW_NOTIFICATION_ATTACHMENT("ATT", "retrieve/download notification attachments"),
  CREATE_BILL("BILL", "create bills"),
  CREATE_APPLICATION("CA", "create applications"),
  VIEW_CASE_BILL("CB", "view case bills"),
  VIEW_CLIENT_DETAILS("CD", "view client details"),
  CLEAR_OUTCOME("CLROUT", "clear outcomes"),
  DELETE_BILL("DB", "delete bills"),
  DELETE_PROCEEDING("DELPROC", "delete proceedings"),
  UPLOAD_EVIDENCE("EVID", "provide documents or evidence"),
  VIEW_NOTIFICATIONS("NOT", "view notifications"),
  CREATE_PAYMENT_ON_ACCOUNT("POA", "create payments on account"),
  REQUEST_CASE_DISCHARGE("POD", "request case discharges"),
  CREATE_CASE_REQUEST("PRC", "create general requests"),
  CREATE_PROVIDER_REQUEST("PRNC", "create provider requests"),
  SUBMIT_AMENDMENT("SUBAMD", "submit amendments"),
  SUBMIT_APPLICATION("SUBAPP", "submit applications"),
  SUBMIT_BILL("SUBBILL", "submit bills"),
  SUBMIT_DOCUMENT_UPLOAD("SUBDOC", "submit documents"),
  SUBMIT_NOTIFICATION("SUBNOT", "submit notifications"),
  SUBMIT_REGISTER_CLIENT("SUBRC", "register clients"),
  SUBMIT_CASE_OUTCOME("SUBOUT", "submit case outcomes"),
  SUBMIT_PAYMENT_ON_ACCOUNT("SUBPOA", "submit payments on account"),
  SUBMIT_UPDATE_CLIENT("SUBUC", "submit client details"),
  ENTER_UNDERTAKING("UND", "enter undertakings"),
  UPDATE_PROCEEDING_OUTCOME("UPDOUT", "update proceeding outcomes"),
  UPDATE_PROCEEDING("UPDPROC", "update proceedings"),
  VIEW_CASE_DETAILS("VC", "view case details"),
  VIEW_PROCEEDING_OUTCOME("VIEWOUT", "view proceeding outcomes"),
  VIEW_PROCEEDING("VIEWPROC", "view proceedings"),
  VIEW_OUTCOME("VO", "view outcomes"),
  VIEW_CASES_AND_APPLICATIONS("YCA", "view cases and applications"),
  ALLOW_FAST_FORWARD("FFBUT", "fast forward");

  private final String code;
  private final String description;

  /**
   * Initializes the enum with the specified code.
   *
   * @param code the PUI code for this role.
   */
  UserRole(final String code, final String description) {
    this.code = code;
    this.description = description;
  }

  /**
   * Find a {@code UserRole} based on the provided code.
   *
   * @param code the code to search with.
   * @return the {@code UserRole}, or throw an exception if not found.
   */
  public static UserRole findByCode(String code) {
    return Arrays.stream(UserRole.values())
        .filter(role -> role.getCode().equals(code))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Provided role code does not exist."));
  }
}
