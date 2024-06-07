package uk.gov.laa.ccms.caab.opa.context;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.opa.context.exception.ConnectorSecurityContextException;

/**
 * Represents a context token with various parameters required for validation and processing
 * within the PUI and OWD (connector) systems.
 */
@Setter
@Getter
@Slf4j
public class ContextToken {


  // query String associated to this context
  // i.e. {url}?val=xxxxxxx  used in checking the context within the PUI Connector Load() operation
  // - e.g. TTL procesing 5-10 seconds.
  public static final String QUERY_KEY = "val";
  // i.e. {url}?valret=xxxxxxx  may be used in PUI during contextToken validation for the return
  // journey OWD->PUI. set during the save operation TTL ?
  public static final String QUERY_KEY_RET = "valret";

  // common mandatory context param
  // PUI business case ref.
  public static final String CASE_ID = "id";
  // rule base enumeration id
  public static final String RULE_BASE_ID = "rb";
  // pui Provider Id AKA OwnerId
  public static final String PROVIDER_ID = "pid";
  // timestamp An attempt to determine if thi context is valid within a time window -
  // to guard against bookmarking of reinjection of an old link.
  public static final String TIME_TO_LIVE = "ttl";
  // invoked form
  public static final String INVOKED_FORM = "ifo";
  // Pui returned URL
  public static final String RETURN_URL = "url";
  //Required for return URL
  protected static final String EZGOV_ID = "ezg";

  private String caseId;
  private Long rulebaseId;
  private String providerId;
  private Long ttl;    // ie. system.currentMillis

  private String invokedForm;
  private String returnUrl;
  private String ezgovId;

  // optional correlation ids to assist with diag. problems between connector and PUI invocations
  // users PUI web session id - used for logging faults
  public static final String SESSION_ID = "sid";

  // the user id that invoked the OWD (connector) link - who's link was attempt to be re-injected.
  public static final String USER_ID = "uid";
  private String userSessionId = "";

  // default value if not set. Used byconnector save (i.e. modifed by)
  private String userId = "connector";

  /**
   * Forms a JSON structure with values necessary for OWD to invoke its data connector with a
   * valid context.
   *
   * @return String value of JSON context structure
   * @throws UnsupportedOperationException if any mandatory field is null
   */
  public String createJsonContextToken() {
    final JSONObject jsonObj = new JSONObject();

    // check manadatory items.
    if (this.caseId == null) {
      throw new UnsupportedOperationException("caseId is null cannot create ContextToken");
    }
    if (this.rulebaseId == null) {
      throw new UnsupportedOperationException("rulebaseId is null cannot create ContextToken");
    }
    if (this.providerId == null) {
      throw new UnsupportedOperationException("provider Id is null cannot create ContextToken");
    }
    if (this.ttl == null) {
      throw new UnsupportedOperationException("ttl is null cannot create ContextToken");
    }
    if (this.invokedForm == null) {
      throw new UnsupportedOperationException("invokedForm is null cannot create ContextToken");
    }
    if (this.returnUrl == null) {
      throw new UnsupportedOperationException("returnURL is null cannot create ContextToken");
    }
    if (this.ezgovId == null) {
      throw new UnsupportedOperationException("ezgovId is null cannot create ContextToken");
    }
    jsonObj.put(ContextToken.CASE_ID, this.getCaseId());
    jsonObj.put(ContextToken.RULE_BASE_ID, this.getRulebaseId());
    jsonObj.put(ContextToken.PROVIDER_ID, this.getProviderId());
    jsonObj.put(ContextToken.TIME_TO_LIVE, this.getTtl());
    jsonObj.put(ContextToken.INVOKED_FORM, this.getInvokedForm());
    jsonObj.put(ContextToken.RETURN_URL, this.getReturnUrl());
    // optional items
    jsonObj.put(ContextToken.USER_ID, this.getUserId());
    jsonObj.put(ContextToken.SESSION_ID, this.getUserSessionId());
    jsonObj.put(ContextToken.EZGOV_ID, this.getEzgovId());

    return jsonObj.toString();
  }


  /**
   * Parses values from a JSON string to set the fields of this Context Token.
   *
   * @param jsonStr JSON string to parse
   * @return this ContextToken instance with updated fields
   * @throws ConnectorSecurityContextException if the JSON structure is invalid
   */
  public ContextToken parseJsonContextToken(final String jsonStr)
      throws ConnectorSecurityContextException {

    final JSONObject jsonObj = new JSONObject(jsonStr);

    if (jsonObj == null) {
      throw new ConnectorSecurityContextException(
          "RequestContext Json Structure should not be null");
    }

    // process Mandatory properties
    if (!jsonObj.has(ContextToken.CASE_ID)) {
      throw new ConnectorSecurityContextException(
          "Unable to locate expected top level Jsonstructure '" + ContextToken.CASE_ID
              + "' from parsed structure");
    }
    if (!jsonObj.has(ContextToken.RULE_BASE_ID)) {
      throw new ConnectorSecurityContextException(
          "Unable to locate expected top level Jsonstructure '" + ContextToken.RULE_BASE_ID
              + "' from parsed structure");
    }
    if (!jsonObj.has(ContextToken.PROVIDER_ID)) {
      throw new ConnectorSecurityContextException(
          "Unable to locate expected top level Jsonstructure '" + ContextToken.PROVIDER_ID
              + "' from parsed structure");
    }
    if (!jsonObj.has(ContextToken.TIME_TO_LIVE)) {
      throw new ConnectorSecurityContextException(
          "Unable to locate expected top level Jsonstructure '" + ContextToken.TIME_TO_LIVE
              + "' from parsed structure");
    }
    if (!jsonObj.has(ContextToken.INVOKED_FORM)) {
      throw new ConnectorSecurityContextException(
          "Unable to locate expected top level Jsonstructure '" + ContextToken.INVOKED_FORM
              + "' from parsed structure");
    }
    if (!jsonObj.has(ContextToken.RETURN_URL)) {
      throw new ConnectorSecurityContextException(
          "Unable to locate expected top level Jsonstructure '" + ContextToken.RETURN_URL
              + "' from parsed structure");
    }
    if (!jsonObj.has(ContextToken.EZGOV_ID)) {
      throw new ConnectorSecurityContextException(
          "Unable to locate expected top level Jsonstructure '" + ContextToken.EZGOV_ID
              + "' from parsed structure");
    }
    // process optional properties
    if (!jsonObj.has(ContextToken.USER_ID)) {
      throw new ConnectorSecurityContextException(
          "Unable to locate expected top level Jsonstructure '" + ContextToken.USER_ID
              + "' from parsed structure");
    }
    if (!jsonObj.has(ContextToken.SESSION_ID)) {
      throw new ConnectorSecurityContextException(
          "Unable to locate expected top level Jsonstructure '" + ContextToken.SESSION_ID
              + "' from parsed structure");
    }
    this.setCaseId((String) jsonObj.get(ContextToken.CASE_ID));
    this.setRulebaseId(jsonObj.getLong(ContextToken.RULE_BASE_ID));
    this.setProviderId((String) jsonObj.get(ContextToken.PROVIDER_ID));
    this.setTtl(jsonObj.getLong(ContextToken.TIME_TO_LIVE));
    this.setInvokedForm((String) jsonObj.get(ContextToken.INVOKED_FORM));
    this.setReturnUrl((String) jsonObj.get(ContextToken.RETURN_URL));
    this.setEzgovId((String) jsonObj.get(ContextToken.EZGOV_ID));
    this.setUserSessionId((String) jsonObj.get(ContextToken.SESSION_ID));
    this.setUserId((String) jsonObj.get(ContextToken.USER_ID));

    return this;
  }

  /**
   * Checks if the given query parameter name is handled by the connector load.
   *
   * @param queryParamName the query parameter name
   * @return true if handled, false otherwise
   */
  public boolean handlesConnectorLoad(final String queryParamName) {
    return ContextToken.QUERY_KEY.equals(queryParamName);
  }

  /**
   * Checks if the given query parameter name is handled by the PUI return.
   *
   * @param queryParamName the query parameter name
   * @return true if handled, false otherwise
   */
  public boolean handlesPuiReturn(final String queryParamName) {
    return ContextToken.QUERY_KEY_RET.equals(queryParamName);
  }

  /**
   * Confirms the validity of mandatory values.
   *
   * @return true if all mandatory values are valid, false otherwise
   */
  public boolean isValid() {
    return StringUtils.hasText(caseId)
        && StringUtils.hasText(providerId)
        && rulebaseId.intValue() > 0
        && ttl.longValue()
            > 0;      // only do basic TTL check, here. Other code gets more specific regards TTL.

  }

  /**
   * Returns a string representation of this context token.
   *
   * @return a string representation of this context token
   */
  @Override
  public String toString() {
    final StringBuilder sb = toStringForOpaSession();
    sb.append("ttl[").append(ttl).append("] ");
    try {
      sb.append(" {").append(new Date(ttl)).append("}");
    } catch (final Exception e) {
      sb.append(" {").append("!").append("}");
    }

    return sb.toString();
  }

  /**
   * Returns a string builder with the OPA session details.
   *
   * @return a StringBuilder with OPA session details
   */
  public StringBuilder toStringForOpaSession() {
    final StringBuilder sb = new StringBuilder();
    sb.append("caseId[").append(caseId).append("] ");
    sb.append("providerId[").append(providerId).append("] ");
    sb.append("RuleBase[").append(
        AssessmentRulebase.getPrePopAssessmentName(rulebaseId)).append("] ");
    return sb;
  }

  /**
   * Returns the hash code for this context token.
   *
   * @return the hash code value
   */
  public int hashcode() {
    return getTtl().intValue()
        * getCaseId().hashCode()
        * getProviderId().hashCode()
        * getRulebaseId().intValue();
  }

  /**
   * Compares this context token to the specified object.
   *
   * @param o the object to compare
   * @return true if the specified object is equal to this context token, false otherwise
   */
  public boolean equals(final Object o) {
    if (o == null) {
      return false;
    }
    if (o instanceof final ContextToken otherContextToken) {
      return getTtl().intValue() == otherContextToken.getTtl().intValue()
          && (getCaseId().equalsIgnoreCase(otherContextToken.getCaseId()))
          && (getProviderId().equalsIgnoreCase(otherContextToken.getProviderId()))
          && (getEzgovId().equalsIgnoreCase(otherContextToken.getEzgovId()))
          && getRulebaseId().intValue() == otherContextToken.getRulebaseId().intValue();
    } else {
      return false;
    }

  }

  /**
   * Checks if the TTL (Time to Live) has expired.
   *
   * @param currentTimeMillis the current time in milliseconds
   * @param elapseTimeMillis the elapsed time in milliseconds
   * @return true if TTL has expired, false otherwise
   */
  public boolean isTtlExpired(final long currentTimeMillis, final long elapseTimeMillis) {

    final boolean expired =
        currentTimeMillis < getTtl() | (currentTimeMillis > (getTtl() + elapseTimeMillis));

    // seed the Log with details of the attack
    if (expired) {
      final long expiredByMillis = currentTimeMillis - (getTtl() + elapseTimeMillis);
      // this is serious! - it represents a potential attack. 
      // We need to record some details in the log.
      log.error("isTTLExpired ********************************************");
      log.error("isTTLExpired ** WARNING - A T T A C K  ! detected      **");
      log.error("isTTLExpired ********************************************");
      log.error("isTTLExpired ** System clock is..........: " + System.currentTimeMillis());
      log.error("isTTLExpired ** check datum millis...... : " + currentTimeMillis);
      log.error("isTTLExpired ** elapseTimemMillis....... : " + elapseTimeMillis);
      log.error("isTTLExpired ** this context TTL Millis. : " + getTtl());
      log.error(
          "isTTLExpired ** Expired by millis....... : " + expiredByMillis + " ("
              + (expiredByMillis / 1000) + " sec. )");
      log.error("isTTLExpired ** Expired................. : true");
      log.error("isTTLExpired ** Context Details......... : " + this);
      log.error("isTTLExpired ********************************************");
    } else {
      log.debug("isTTLExpired [true] for context " + this);
    }

    return expired;
  }

}
