package uk.gov.laa.ccms.caab.constants.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class InstanceMappingPrefixTest {

  @Test
  void testProceedingPrefix() {
    assertEquals("P_", InstanceMappingPrefix.PROCEEDING.getPrefix());
  }

  @Test
  void testOpponentPrefix() {
    assertEquals("OPPONENT_", InstanceMappingPrefix.OPPONENT.getPrefix());
  }
}
