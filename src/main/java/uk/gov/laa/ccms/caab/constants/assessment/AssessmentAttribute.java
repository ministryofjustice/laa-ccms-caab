package uk.gov.laa.ccms.caab.constants.assessment;

import lombok.Getter;

/**
 * Defines the attributes associated with an assessment.
 *
 * <p>These attributes listed are the ones mapped from application data into the assessment, not the
 * ones mapped from the assessment to the application.
 */
@Getter
public enum AssessmentAttribute {

  // proceedings
  CLIENT_INVOLVEMENT_TYPE("text", true, true),
  LEAD_PROCEEDING("boolean", true, true),
  LEVEL_OF_SERVICE("text", true, true),
  MATTER_TYPE("text", true, true),
  NEW_OR_EXISTING("text", true, true),
  PROCEEDING_ID("text", true, true),
  PROCEEDING_NAME("text", true, true),
  PROCEEDING_ORDER_TYPE("text", true, true),
  REQUESTED_SCOPE("text", true, true),
  SCOPE_LIMIT_IS_DEFAULT("boolean", true, true),

  // opponents
  OPPONENT_DOB("date", true, true),
  OTHER_PARTY_ID("text", true, true),
  OTHER_PARTY_NAME("text", true, true),
  OTHER_PARTY_TYPE("text", true, true),
  RELATIONSHIP_TO_CASE("text", true, true),
  RELATIONSHIP_TO_CLIENT("text", true, true),

  // global
  APPLICATION_CASE_REF("text", true, true),
  APP_AMEND_TYPE("text", true, true),
  CATEGORY_OF_LAW("text", true, true),
  CLIENT_VULNERABLE("boolean", true, true),
  COST_LIMIT_CHANGED_FLAG("text", true, true),
  COUNTRY("text", true, true),
  COUNTY("text", true, true),
  DATE_ASSESSMENT_STARTED("date", true, true),
  DATE_OF_BIRTH("date", true, true),
  DEFAULT_COST_LIMITATION("currency", true, true),
  DELEGATED_FUNCTIONS_DATE("date", true, true),
  DEVOLVED_POWERS_CONTRACT_FLAG("text", true, true),
  ECF_FLAG("boolean", true, true),
  FIRST_NAME("text", true, true),
  HIGH_PROFILE("boolean", true, true),
  HOME_OFFICE_NO("text", true, true),
  LAR_SCOPE_FLAG("boolean", true, true),
  LEAD_PROCEEDING_CHANGED("boolean", true, true),
  MARITIAL_STATUS("text", true, true),
  MEANS_EVIDENCE_REQD("boolean", true, true),
  MERITS_EVIDENCE_REQD("boolean", true, true),
  NEW_APPL_OR_AMENDMENT("text", true, true),
  NI_NO("text", true, true),
  POA_OR_BILL_FLAG("text", true, true),
  POST_CODE("text", true, true),
  PROVIDER_CASE_REFERENCE("text", true, true),
  PROVIDER_HAS_CONTRACT("boolean", true, true),
  REQ_COST_LIMITATION("currency", true, true),
  SURNAME("text", true, true),
  SURNAME_AT_BIRTH("text", true, true),
  USER_PROVIDER_FIRM_ID("number", true, true),
  USER_TYPE("text", true, true);

  private final String type;
  private final boolean prepopulated;
  private final boolean asked;

  /**
   * Initializes the enum with the specified attribute values.
   *
   * @param type the type of the assessment attribute
   * @param prepopulated whether the attribute is prepopulated
   * @param asked whether the attribute is asked
   */
  AssessmentAttribute(final String type, final boolean prepopulated, final boolean asked) {
    this.type = type;
    this.prepopulated = prepopulated;
    this.asked = asked;
  }
}
