package uk.gov.laa.ccms.caab.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildOpponent;
import static uk.gov.laa.ccms.caab.util.OpponentUtil.getPartyName;

import java.util.Date;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

class OpponentUtilTest {

  @Test
  void testGetPartyName_buildsFullName() {
    final OpponentDetail opponent = buildOpponent(new Date());

    final CommonLookupValueDetail titleLookup =
        new CommonLookupValueDetail().code(opponent.getTitle()).description("test");

    final String fullName = getPartyName(opponent, titleLookup);

    assertNotNull(fullName);
    final String expectedResult =
        titleLookup.getDescription() + " " + opponent.getFirstName() + " " + opponent.getSurname();
    assertEquals(expectedResult, fullName);
  }

  @Test
  void testGetPartyName_noTitleMatchReturnsCode() {
    final OpponentDetail opponent = buildOpponent(new Date());

    final CommonLookupValueDetail titleLookup =
        new CommonLookupValueDetail().code(opponent.getTitle());

    final String fullName = getPartyName(opponent, titleLookup);

    assertNotNull(fullName);
    final String expectedResult =
        opponent.getTitle() + " " + opponent.getFirstName() + " " + opponent.getSurname();
    assertEquals(expectedResult, fullName);
  }

  @Test
  void testGetPartyName_noNameElementsReturnsUndefined() {
    final OpponentDetail opponent = new OpponentDetail();

    final CommonLookupValueDetail titleLookup = new CommonLookupValueDetail();

    final String fullName = getPartyName(opponent, titleLookup);

    assertNotNull(fullName);
    final String expectedResult = "undefined";
    assertEquals(expectedResult, fullName);
  }

  @Test
  void testGetPartyName_noFirstnameReturnsCorrectly() {
    final OpponentDetail opponent = buildOpponent(new Date());
    opponent.setFirstName(null);

    final CommonLookupValueDetail titleLookup =
        new CommonLookupValueDetail().code(opponent.getTitle());

    final String fullName = getPartyName(opponent, titleLookup);

    assertNotNull(fullName);
    final String expectedResult = opponent.getTitle() + " " + opponent.getSurname();
    assertEquals(expectedResult, fullName);
  }

  @Test
  void testGetPartyName_noSurnameReturnsCorrectly() {
    final OpponentDetail opponent = buildOpponent(new Date());
    opponent.setSurname(null);

    final CommonLookupValueDetail titleLookup =
        new CommonLookupValueDetail().code(opponent.getTitle());

    final String fullName = getPartyName(opponent, titleLookup);

    assertNotNull(fullName);
    final String expectedResult = opponent.getTitle() + " " + opponent.getFirstName();
    assertEquals(expectedResult, fullName);
  }

  @Test
  void testGetPartyName_noFirstnameSurnameReturnsTitleOnly() {
    final OpponentDetail opponent = buildOpponent(new Date());
    opponent.setFirstName(null);
    opponent.setSurname(null);

    final CommonLookupValueDetail titleLookup =
        new CommonLookupValueDetail().code(opponent.getTitle());

    final String fullName = getPartyName(opponent, titleLookup);

    assertNotNull(fullName);
    final String expectedResult = opponent.getTitle();
    assertEquals(expectedResult, fullName);
  }
}
