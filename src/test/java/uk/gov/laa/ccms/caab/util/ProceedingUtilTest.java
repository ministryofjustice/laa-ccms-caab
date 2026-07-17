package uk.gov.laa.ccms.caab.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;

/** Tests for {@link ProceedingUtil}. */
class ProceedingUtilTest {

  private static ProceedingDetail proceedingWith(final ScopeLimitationDetail... scopeLimitations) {
    final ProceedingDetail proceeding = new ProceedingDetail();
    proceeding.setScopeLimitations(new ArrayList<>(Arrays.asList(scopeLimitations)));
    return proceeding;
  }

  private static ScopeLimitationDetail scopeLimitation(final Boolean defaultInd) {
    final ScopeLimitationDetail scopeLimitation = new ScopeLimitationDetail();
    scopeLimitation.setDefaultInd(defaultInd);
    return scopeLimitation;
  }

  @Test
  @DisplayName("Returns true when a scope limitation is marked as default")
  void isScopeLimitDefault_trueWhenADefaultExists() {
    assertTrue(ProceedingUtil.isScopeLimitDefault(proceedingWith(scopeLimitation(true))));
  }

  @Test
  @DisplayName("Returns true when only one of several scope limitations is default")
  void isScopeLimitDefault_trueWhenAnyIsDefault() {
    assertTrue(
        ProceedingUtil.isScopeLimitDefault(
            proceedingWith(scopeLimitation(false), scopeLimitation(true))));
  }

  @Test
  @DisplayName("Returns false when scope limitations exist but none is default")
  void isScopeLimitDefault_falseWhenNoneIsDefault() {
    assertFalse(
        ProceedingUtil.isScopeLimitDefault(
            proceedingWith(scopeLimitation(false), scopeLimitation(false))));
  }

  @Test
  @DisplayName("Returns false when the default indicator is not set")
  void isScopeLimitDefault_falseWhenDefaultIndIsNull() {
    assertFalse(ProceedingUtil.isScopeLimitDefault(proceedingWith(scopeLimitation(null))));
  }

  @Test
  @DisplayName("Returns false when the scope limitation list is empty")
  void isScopeLimitDefault_falseWhenEmpty() {
    assertFalse(ProceedingUtil.isScopeLimitDefault(proceedingWith()));
  }

  @Test
  @DisplayName("Returns false when the proceeding has no scope limitations")
  void isScopeLimitDefault_falseWhenNull() {
    final ProceedingDetail proceeding = new ProceedingDetail();
    proceeding.setScopeLimitations(null);
    assertFalse(ProceedingUtil.isScopeLimitDefault(proceeding));
  }

  @Test
  @DisplayName("Ignores null entries in the scope limitation list")
  void isScopeLimitDefault_ignoresNullEntries() {
    final ProceedingDetail proceeding = new ProceedingDetail();
    final List<ScopeLimitationDetail> scopeLimitations = new ArrayList<>();
    scopeLimitations.add(null);
    scopeLimitations.add(scopeLimitation(true));
    proceeding.setScopeLimitations(scopeLimitations);

    assertTrue(ProceedingUtil.isScopeLimitDefault(proceeding));
  }
}
