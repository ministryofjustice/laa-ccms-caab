package uk.gov.laa.ccms.caab.security;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * An authenticated EntraID user, named by their CCMS username rather than by their EntraID
 * identity.
 *
 * <p>Everything downstream of authentication - the user functions that gate each route, the user
 * details put on the model and session, every EBS and SOA call made on the user's behalf - works in
 * terms of a CCMS username. EntraID does not know that username; it knows an email address, or for
 * an external user a SiLAS identifier. This wraps the user EntraID authenticated so that {@code
 * getName()} answers with the CCMS username the identity was resolved to, leaving the original
 * claims intact for anything that wants them.
 */
public class CcmsOidcUser implements OidcUser {

  @Serial private static final long serialVersionUID = 1L;

  private final OidcUser delegate;
  private final String ccmsLoginId;
  private final Set<GrantedAuthority> authorities;

  /**
   * Constructs a CCMS-named view of an authenticated EntraID user.
   *
   * @param delegate the user as EntraID authenticated them.
   * @param ccmsLoginId the CCMS username that identity resolved to.
   * @param authorities the authorities granted to the user.
   */
  public CcmsOidcUser(
      final OidcUser delegate,
      final String ccmsLoginId,
      final Collection<? extends GrantedAuthority> authorities) {
    this.delegate = delegate;
    this.ccmsLoginId = ccmsLoginId;
    this.authorities = Collections.unmodifiableSet(Set.copyOf(authorities));
  }

  /**
   * The CCMS username this EntraID identity resolved to.
   *
   * @return the CCMS username.
   */
  @Override
  public String getName() {
    return ccmsLoginId;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return delegate.getAttributes();
  }

  @Override
  public Map<String, Object> getClaims() {
    return delegate.getClaims();
  }

  @Override
  public OidcUserInfo getUserInfo() {
    return delegate.getUserInfo();
  }

  @Override
  public OidcIdToken getIdToken() {
    return delegate.getIdToken();
  }
}
