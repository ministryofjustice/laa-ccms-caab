package uk.gov.laa.ccms.caab.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter responsible for generating a cryptographically strong nonce for Content Security Policy.
 * The nonce is added to the request attributes for use in Thymeleaf templates.
 */
public class CspNonceFilter extends OncePerRequestFilter {

  /** The attribute name used to store the CSP nonce in the request. */
  public static final String CSP_NONCE_ATTRIBUTE = "cspNonce";

  private final SecureRandom secureRandom = new SecureRandom();

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    
    byte[] nonceBytes = new byte[16];
    secureRandom.nextBytes(nonceBytes);
    String nonce = Base64.getEncoder().encodeToString(nonceBytes);
    
    request.setAttribute(CSP_NONCE_ATTRIBUTE, nonce);
    
    filterChain.doFilter(request, response);
  }
}
