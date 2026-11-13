package uk.gov.laa.ccms.caab.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import uk.gov.laa.ccms.caab.config.UserRole;

class UserRoleUtilTest {

  @Test
  void findMissingRolesReturnsRolesNotGrantedAsAuthorities() {
    List<SimpleGrantedAuthority> authorities =
        List.of(new SimpleGrantedAuthority("VIEWPROC"), new SimpleGrantedAuthority("other"));

    assertThat(
            UserRoleUtil.findMissingRoles(
                authorities, "VIEW_PROCEEDING, ADD_PROCEEDING,DELETE_PROCEEDING"))
        .containsExactly(UserRole.ADD_PROCEEDING, UserRole.DELETE_PROCEEDING);
  }

  @Test
  void findMissingRolesReturnsEmptyWhenAllGranted() {
    assertThat(
            UserRoleUtil.findMissingRoles(
                List.of(new SimpleGrantedAuthority("SUBAPP")), "SUBMIT_APPLICATION"))
        .isEmpty();
  }

  @Test
  void findMissingRolesRejectsUnknownRoleName() {
    assertThrows(
        IllegalArgumentException.class, () -> UserRoleUtil.findMissingRoles(List.of(), "SUBAPP"));
  }
}
