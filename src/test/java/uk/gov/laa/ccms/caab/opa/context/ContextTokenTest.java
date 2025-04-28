package uk.gov.laa.ccms.caab.opa.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.opa.context.exception.ConnectorSecurityContextException;

class ContextTokenTest {

  private ContextToken contextToken;

  @BeforeEach
  void setUp() {
    contextToken = new ContextToken();
    contextToken.setCaseId("123");
    contextToken.setRulebaseId(1L);
    contextToken.setProviderId("provider");
    contextToken.setTtl(System.currentTimeMillis());
    contextToken.setInvokedForm("form");
    contextToken.setReturnUrl("http://return.url");
    contextToken.setEzgovId("ezgov123");
    contextToken.setUserId("user");
    contextToken.setUserSessionId("session123");
  }

  @Test
  void createJsonContextToken() {
    final String json = contextToken.createJsonContextToken();
    final JSONObject jsonObject = new JSONObject(json);

    assertEquals(contextToken.getCaseId(), jsonObject.getString(ContextToken.CASE_ID));
    assertEquals(contextToken.getRulebaseId(), jsonObject.getLong(ContextToken.RULE_BASE_ID));
    assertEquals(contextToken.getProviderId(), jsonObject.getString(ContextToken.PROVIDER_ID));
    assertEquals(contextToken.getTtl(), jsonObject.getLong(ContextToken.TIME_TO_LIVE));
    assertEquals(contextToken.getInvokedForm(), jsonObject.getString(ContextToken.INVOKED_FORM));
    assertEquals(contextToken.getReturnUrl(), jsonObject.getString(ContextToken.RETURN_URL));
    assertEquals(contextToken.getEzgovId(), jsonObject.getString(ContextToken.EZGOV_ID));
    assertEquals(contextToken.getUserId(), jsonObject.getString(ContextToken.USER_ID));
    assertEquals(contextToken.getUserSessionId(), jsonObject.getString(ContextToken.SESSION_ID));
  }

  @Test
  void createJsonContextTokenMissingMandatoryField() {
    contextToken.setCaseId(null);
    assertThrows(UnsupportedOperationException.class, () -> contextToken.createJsonContextToken());
  }

  @Test
  void parseJsonContextToken() throws ConnectorSecurityContextException {
    final String json = contextToken.createJsonContextToken();
    final ContextToken parsedToken = new ContextToken().parseJsonContextToken(json);

    assertEquals(contextToken.getCaseId(), parsedToken.getCaseId());
    assertEquals(contextToken.getRulebaseId(), parsedToken.getRulebaseId());
    assertEquals(contextToken.getProviderId(), parsedToken.getProviderId());
    assertEquals(contextToken.getTtl(), parsedToken.getTtl());
    assertEquals(contextToken.getInvokedForm(), parsedToken.getInvokedForm());
    assertEquals(contextToken.getReturnUrl(), parsedToken.getReturnUrl());
    assertEquals(contextToken.getEzgovId(), parsedToken.getEzgovId());
    assertEquals(contextToken.getUserId(), parsedToken.getUserId());
    assertEquals(contextToken.getUserSessionId(), parsedToken.getUserSessionId());
  }

  @Test
  void parseJsonContextTokenInvalidJson() {
    assertThrows(ConnectorSecurityContextException.class, () -> new ContextToken().parseJsonContextToken("{}"));
  }

  @Test
  void handlesConnectorLoad() {
    assertTrue(contextToken.handlesConnectorLoad(ContextToken.QUERY_KEY));
    assertFalse(contextToken.handlesConnectorLoad("invalid"));
  }

  @Test
  void handlesPuiReturn() {
    assertTrue(contextToken.handlesPuiReturn(ContextToken.QUERY_KEY_RET));
    assertFalse(contextToken.handlesPuiReturn("invalid"));
  }

  @Test
  void isValid() {
    assertTrue(contextToken.isValid());
    contextToken.setCaseId(null);
    assertFalse(contextToken.isValid());
  }

  @Test
  void testToString() {
    final String contextTokenString = contextToken.toString();
    assertTrue(contextTokenString.contains("caseId[123]"));
    assertTrue(contextTokenString.contains("providerId[provider]"));
  }

  @Test
  void toStringForOpaSession() {
    final StringBuilder sb = contextToken.toStringForOpaSession();
    assertTrue(sb.toString().contains("caseId[123]"));
    assertTrue(sb.toString().contains("providerId[provider]"));
  }

  @Test
  void testHashCode() {
    final int hashCode = contextToken.hashcode();
    assertEquals(hashCode, contextToken.getTtl().intValue()
        * contextToken.getCaseId().hashCode()
        * contextToken.getProviderId().hashCode()
        * contextToken.getRulebaseId().intValue());
  }

  @Test
  void equals() {
    final ContextToken sameToken = new ContextToken();
    sameToken.setCaseId("123");
    sameToken.setRulebaseId(1L);
    sameToken.setProviderId("provider");
    sameToken.setTtl(contextToken.getTtl());
    sameToken.setEzgovId("ezgov123");

    assertEquals(contextToken, sameToken);
    sameToken.setEzgovId("different");
    assertNotEquals(contextToken, sameToken);
  }

  @Test
  void isTtlExpired() {
    final long currentTime = System.currentTimeMillis();
    assertFalse(contextToken.isTtlExpired(currentTime, 1000));

    contextToken.setTtl(currentTime - 2000);
    assertTrue(contextToken.isTtlExpired(currentTime, 1000));
  }
}
