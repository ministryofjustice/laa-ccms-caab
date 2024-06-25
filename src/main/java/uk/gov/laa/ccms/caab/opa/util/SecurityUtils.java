package uk.gov.laa.ccms.caab.opa.util;

import java.io.UnsupportedEncodingException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.laa.ccms.caab.opa.context.ContextToken;
import uk.gov.laa.ccms.caab.opa.security.ContextUrlEncoder;
import uk.gov.laa.ccms.caab.opa.security.Encryptor;

/**
 * Utility class for security-related operations.
 */
@Setter
@Getter
@Component
@Slf4j
public class SecurityUtils {

  public static final String ERROR_WITH_CREATE_FEEDBACK_HUB_CONTEXT =
      "error with createFeedbackHubContext";
  public static final String UTF_8 = "UTF-8";
  public static final String ERROR_WITH_CREATE_HUB_CONTEXT = "error with createHubContext";

  private String returnUrl;
  private String password;

  public SecurityUtils(
      @Value("${laa.ccms.oracle-web-determination-server.redirect.url}") final String returnUrl,
      @Value("${laa.ccms.caab.opa.security.password}") final String password) {
    this.returnUrl = returnUrl;
    this.password = password;
  }

  /**
   * Create a URL Encoded(http legal) - Encryted Json String.
   *
   * <p>The Json structure contains the CCMS->OWD context values together with other values This
   * Json structure is encrypted, and finally converted to legal Http characters as the excryption
   * process creates whitespace characters and is unsafe to transmit raw. where: LscCaseRef +
   * RuleBaseId Provider used by the connector to retrieve the OPASessoin from TDS puiSessionId is
   * user Session Id of PUI - used to correlate connector log activity for support</p>
   *
   * @return String
   */
  public String createHubContext(
      final String lscCaseReference,
      final Long ruleBaseId,
      final String userId,
      final Long providerId,
      final String puiUserSessionId,
      final String invokedForm,
      final String ezgovId) {
    final ContextToken contextToken = new ContextToken();
    log.debug("createHubContext() based on supplied details caseId:[{}], "
        + "ruleBaseName:[{}]", lscCaseReference, ruleBaseId);
    log.debug("returnURL: {}", returnUrl);

    // Establish ContextToken POJO
    contextToken.setCaseId(lscCaseReference);
    contextToken.setProviderId("" + providerId);
    contextToken.setTtl(System.currentTimeMillis());
    contextToken.setRulebaseId(ruleBaseId);
    contextToken.setUserId(userId);
    contextToken.setUserSessionId(puiUserSessionId);
    contextToken.setInvokedForm(invokedForm);
    contextToken.setReturnUrl(returnUrl);
    contextToken.setEzgovId(ezgovId);

    // create a String with the Json structure for connector context
    final String jsonStr = contextToken.createJsonContextToken();
    String uriLegalTokenValue = "";
    final boolean isEncrypted = true;
    final Encryptor encryptor = new Encryptor(password);
    if (isEncrypted) {
      try {
        log.info("createHubContext() Json: ...........[{}]", jsonStr);

        final String encryptedJsonStr = encryptor.encrypt(jsonStr);
        log.info("createHubContext() encrypted Json...[{}]", uriLegalTokenValue);

        uriLegalTokenValue = ContextUrlEncoder.encode(encryptedJsonStr, UTF_8);
        log.info("createHubContext() UrlEncoded ......[{}]", uriLegalTokenValue);

      } catch (UnsupportedEncodingException | SecurityException e) {
        log.error(ERROR_WITH_CREATE_HUB_CONTEXT, e);
      }


    } else {
      try {
        uriLegalTokenValue = ContextUrlEncoder.encode(jsonStr, UTF_8);
      } catch (UnsupportedEncodingException e) {
        log.error(ERROR_WITH_CREATE_HUB_CONTEXT, e);
      }
      log.debug("createHubContext() created (clear) legal Token: [{}]", uriLegalTokenValue);

    }
    log.debug("========> ContextToken ===========> {}", uriLegalTokenValue);

    return uriLegalTokenValue;
  }


  /**
   * Creates a feedback hub context based on the supplied username and provider name.
   *
   * @param username the username to include in the context
   * @param providerName the provider name to include in the context
   * @return the URI-encoded legal token value for the feedback hub context
   */
  public String createFeedbackHubContext(final String username, final String providerName) {

    log.debug("createFeedbackHubContext() based on supplied details username:[{}], "
        + "providerName:[{}]", username, providerName);

    final Encryptor encryptor = new Encryptor(password);

    // create a String with the Json structure for connector context
    final String jsonStr = createJsonToken(username, providerName);
    String uriLegalTokenValue = "";

    try {
      log.info("createHubContext() Json: ...........[{}]", jsonStr);

      final String encryptedJsonStr = encryptor.encrypt(jsonStr);

      uriLegalTokenValue = ContextUrlEncoder.encode(encryptedJsonStr, UTF_8);

      log.info("createHubContext() UrlEncoded ......[{}]", uriLegalTokenValue);

    } catch (final Exception e) {
      log.error(ERROR_WITH_CREATE_FEEDBACK_HUB_CONTEXT, e);
    }


    return uriLegalTokenValue;
  }

  /**
   * Form a json structure that will be passed to OWD (connector), with values necessary for OWD to
   * invoke its data connector with a valid context.
   *
   * @return String value of json context structure
   */
  private String createJsonToken(final String username, final String providerName) {
    final JSONObject jsonObj = new JSONObject();

    // check manadatory items.
    if (username == null) {
      throw new UnsupportedOperationException("username is null cannot create ContextToken");
    }
    if (providerName == null) {
      throw new UnsupportedOperationException("providerName is null cannot create ContextToken");
    }

    jsonObj.put("_SYSTEM_USERID", username);
    jsonObj.put("VENDOR_SITE_NAME", providerName);

    return jsonObj.toString();
  }

}
