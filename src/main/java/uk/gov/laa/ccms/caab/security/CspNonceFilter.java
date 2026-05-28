package uk.gov.laa.ccms.caab.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter responsible for generating a cryptographically strong nonce for Content Security Policy.
 * The nonce is added to the request attributes for use in Thymeleaf templates.
 */
public class CspNonceFilter extends OncePerRequestFilter {

  /** The attribute name used to store the CSP nonce in the request. */
  public static final String CSP_NONCE_ATTRIBUTE = "cspNonce";

  private static final String CSP_REPORT_ONLY_HEADER = "Content-Security-Policy-Report-Only";
  private static final String CSP_ENFORCE_HEADER = "Content-Security-Policy";
  private static final String CSP_REPORT_URI = "/civil/csp/report";
  private static final String DEFAULT_OPA_ORIGIN = "https://opa.oraclecloud.com";

  private final SecureRandom secureRandom = new SecureRandom();
  private final boolean reportEnabled;
  private final boolean reportOnly;
  private final boolean upgradeInsecureRequests;
  private final String opaSources;

  /**
   * Creates a CSP nonce filter with the configured rollout mode and external OPA source.
   *
   * @param reportEnabled whether the browser should send CSP reports
   * @param reportOnly whether the policy should be sent as report-only instead of enforced
   * @param upgradeInsecureRequests whether to add the upgrade-insecure-requests directive
   * @param owdUrl configured Oracle Web Determinations URL used to derive the OPA origin
   */
  public CspNonceFilter(
      boolean reportEnabled, boolean reportOnly, boolean upgradeInsecureRequests, String owdUrl) {
    this.reportEnabled = reportEnabled;
    this.reportOnly = reportOnly;
    this.upgradeInsecureRequests = upgradeInsecureRequests;
    this.opaSources = buildOpaSources(owdUrl);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    byte[] nonceBytes = new byte[16];
    secureRandom.nextBytes(nonceBytes);
    String nonce = Base64.getEncoder().encodeToString(nonceBytes);

    request.setAttribute(CSP_NONCE_ATTRIBUTE, nonce);
    response.setHeader(
        reportOnly ? CSP_REPORT_ONLY_HEADER : CSP_ENFORCE_HEADER,
        buildPolicy(nonce, request.getMethod(), request.getRequestURI()));

    filterChain.doFilter(request, response);
  }

  private String buildPolicy(String nonce, String method, String requestUri) {
    boolean isAssessmentPath =
        "GET".equalsIgnoreCase(method) && requestUri != null && requestUri.endsWith("/assessments");

    StringBuilder policy = new StringBuilder();
    policy.append("default-src 'self'; ");
    policy
        .append("script-src 'nonce-")
        .append(nonce)
        .append("' 'strict-dynamic' 'self' ")
        .append("https://www.googletagmanager.com ")
        .append(opaSources)
        .append("; ");

    policy.append("style-src ");
    if (isAssessmentPath) {
      policy.append("'self' 'unsafe-inline' ").append(opaSources);
    } else {
      policy.append("'nonce-").append(nonce).append("' 'self' ").append(opaSources);
    }
    policy.append("; ");

    policy
        .append("img-src 'self' data: https://www.googletagmanager.com ")
        .append(opaSources)
        .append("; ");
    policy
        .append("connect-src 'self' https://www.google-analytics.com ")
        .append(opaSources)
        .append("; ");
    policy.append("font-src 'self' data: ").append(opaSources).append("; ");
    policy.append("frame-src 'self' ").append(opaSources).append("; ");
    policy.append("frame-ancestors 'self'; ");

    if (upgradeInsecureRequests) {
      policy.append("upgrade-insecure-requests; ");
    }

    policy.append("object-src 'none'; ");
    policy.append("base-uri 'self'; ");
    policy.append("form-action 'self';");

    if (reportEnabled) {
      policy.append(" report-uri ").append(CSP_REPORT_URI).append(";");
    }

    return policy.toString();
  }

  private String buildOpaSources(String owdUrl) {
    Set<String> opaOrigins = new LinkedHashSet<>();
    opaOrigins.add(DEFAULT_OPA_ORIGIN);
    parseOrigin(owdUrl).ifPresent(opaOrigins::add);
    return String.join(" ", opaOrigins);
  }

  private Optional<String> parseOrigin(String url) {
    if (url == null || url.isBlank()) {
      return Optional.empty();
    }

    try {
      URI uri = new URI(url);
      if (uri.getScheme() == null || uri.getHost() == null) {
        return Optional.empty();
      }

      StringBuilder origin = new StringBuilder(uri.getScheme()).append("://").append(uri.getHost());
      if (uri.getPort() != -1) {
        origin.append(":").append(uri.getPort());
      }
      return Optional.of(origin.toString());
    } catch (URISyntaxException e) {
      return Optional.empty();
    }
  }
}
