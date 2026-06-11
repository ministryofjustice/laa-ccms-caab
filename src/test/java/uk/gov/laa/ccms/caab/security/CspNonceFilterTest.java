package uk.gov.laa.ccms.caab.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CspNonceFilterTest {

  @Test
  void shouldIncludeUnsafeInlineInStyleSrcForGetAssessmentPath() throws Exception {
    CspNonceFilter filter =
        new CspNonceFilter(true, true, false, "https://opa.oraclecloud.com/opa");
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("GET");
    request.setRequestURI("/application/assessments");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (servletRequest, servletResponse) -> {});

    String nonce = (String) request.getAttribute(CspNonceFilter.CSP_NONCE_ATTRIBUTE);
    assertThat(response.getHeader("Content-Security-Policy-Report-Only"))
        .contains("style-src 'self' 'unsafe-inline' https://opa.oraclecloud.com")
        .doesNotContain("style-src 'nonce-" + nonce + "'");
  }

  @Test
  void shouldIncludeUnsafeInlineInStyleSrcForGetAssessmentFramePath() throws Exception {
    CspNonceFilter filter =
        new CspNonceFilter(true, true, false, "https://opa.oraclecloud.com/opa");
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("GET");
    request.setRequestURI("/application/assessments/frame");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (servletRequest, servletResponse) -> {});

    String nonce = (String) request.getAttribute(CspNonceFilter.CSP_NONCE_ATTRIBUTE);
    assertThat(response.getHeader("Content-Security-Policy-Report-Only"))
        .contains("style-src 'self' 'unsafe-inline' https://opa.oraclecloud.com")
        .doesNotContain("style-src 'nonce-" + nonce + "'");
  }

  @Test
  void shouldIncludeNonceInStyleSrcForPostAssessmentPath() throws Exception {
    CspNonceFilter filter =
        new CspNonceFilter(true, true, false, "https://opa.oraclecloud.com/opa");
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("POST");
    request.setRequestURI("/application/assessments");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (servletRequest, servletResponse) -> {});

    String nonce = (String) request.getAttribute(CspNonceFilter.CSP_NONCE_ATTRIBUTE);
    assertThat(response.getHeader("Content-Security-Policy-Report-Only"))
        .contains("style-src 'nonce-" + nonce + "' 'self' https://opa.oraclecloud.com")
        .doesNotContain("'unsafe-inline'");
  }

  @Test
  void shouldIncludeNonceInStyleSrcForAssessmentConfirmPath() throws Exception {
    CspNonceFilter filter =
        new CspNonceFilter(true, true, false, "https://opa.oraclecloud.com/opa");
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("GET");
    request.setRequestURI("/application/assessments/confirm");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (servletRequest, servletResponse) -> {});

    String nonce = (String) request.getAttribute(CspNonceFilter.CSP_NONCE_ATTRIBUTE);
    assertThat(response.getHeader("Content-Security-Policy-Report-Only"))
        .contains("style-src 'nonce-" + nonce + "' 'self' https://opa.oraclecloud.com")
        .doesNotContain("'unsafe-inline'");
  }

  @Test
  void shouldSetReportOnlyHeaderWithMatchingNonce() throws Exception {
    CspNonceFilter filter =
        new CspNonceFilter(true, true, false, "https://opa.oraclecloud.com/opa");
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (servletRequest, servletResponse) -> {});

    String nonce = (String) request.getAttribute(CspNonceFilter.CSP_NONCE_ATTRIBUTE);
    assertThat(nonce).isNotBlank();
    assertThat(response.getHeader("Content-Security-Policy")).isNull();
    assertThat(response.getHeader("Content-Security-Policy-Report-Only"))
        .contains(
            "'nonce-" + nonce + "'",
            "'strict-dynamic'",
            "default-src 'self'",
            "https://www.googletagmanager.com",
            "https://opa.oraclecloud.com",
            "https://www.google-analytics.com",
            "frame-ancestors 'self'",
            "style-src 'nonce-" + nonce + "' 'self' https://opa.oraclecloud.com",
            "img-src 'self' data: https://www.googletagmanager.com https://opa.oraclecloud.com",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "font-src 'self' data: https://opa.oraclecloud.com",
            "report-uri /civil/csp/report")
        .doesNotContain("{nonce}", "*", "upgrade-insecure-requests");
  }

  @Test
  void shouldSetEnforcementHeaderWhenReportOnlyDisabled() throws Exception {
    CspNonceFilter filter =
        new CspNonceFilter(true, false, false, "https://opa.oraclecloud.com/opa");
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (servletRequest, servletResponse) -> {});

    assertThat(response.getHeader("Content-Security-Policy")).isNotBlank();
    assertThat(response.getHeader("Content-Security-Policy-Report-Only")).isNull();
  }

  @Test
  void shouldGenerateNewNonceForEachRequest() throws Exception {
    CspNonceFilter filter =
        new CspNonceFilter(true, true, false, "https://opa.oraclecloud.com/opa");
    MockHttpServletRequest firstRequest = new MockHttpServletRequest();
    MockHttpServletRequest secondRequest = new MockHttpServletRequest();

    filter.doFilter(firstRequest, new MockHttpServletResponse(), (request, response) -> {});
    filter.doFilter(secondRequest, new MockHttpServletResponse(), (request, response) -> {});

    assertThat(firstRequest.getAttribute(CspNonceFilter.CSP_NONCE_ATTRIBUTE))
        .isNotEqualTo(secondRequest.getAttribute(CspNonceFilter.CSP_NONCE_ATTRIBUTE));
  }

  @Test
  void shouldIncludeConfiguredOpaOriginWhenDifferentFromDefault() throws Exception {
    CspNonceFilter filter =
        new CspNonceFilter(true, true, false, "http://localhost:8082/opa/web-determinations");
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (servletRequest, servletResponse) -> {});

    assertThat(response.getHeader("Content-Security-Policy-Report-Only"))
        .contains("https://opa.oraclecloud.com", "http://localhost:8082")
        .doesNotContain("http://localhost:8082/opa");
  }

  @Test
  void shouldIncludeUpgradeInsecureRequestsWhenEnabled() throws Exception {
    CspNonceFilter filter = new CspNonceFilter(true, true, true, "https://opa.oraclecloud.com/opa");
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (servletRequest, servletResponse) -> {});

    assertThat(response.getHeader("Content-Security-Policy-Report-Only"))
        .contains("upgrade-insecure-requests");
  }

  @Test
  void shouldNotIncludeReportUriWhenDisabled() throws Exception {
    CspNonceFilter filter =
        new CspNonceFilter(false, true, false, "https://opa.oraclecloud.com/opa");
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (servletRequest, servletResponse) -> {});

    assertThat(response.getHeader("Content-Security-Policy-Report-Only"))
        .doesNotContain("report-uri");
  }
}
