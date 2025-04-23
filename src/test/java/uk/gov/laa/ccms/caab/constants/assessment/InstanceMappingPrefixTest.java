package uk.gov.laa.ccms.caab.constants.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InstanceMappingPrefixTest {

  @Test
  void proceedingPrefix() {
    assertEquals("P_", InstanceMappingPrefix.PROCEEDING.getPrefix());
  }

  @Test
  void opponentPrefix() {
    assertEquals("OPPONENT_", InstanceMappingPrefix.OPPONENT.getPrefix());
  }
}
