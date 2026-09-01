package uk.gov.laa.ccms.caab.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OpaRelationshipUtil test")
class OpaRelationshipUtilTest {

  private static final String RESOURCE = "assessment/opa-relationship-names.txt";

  private static final List<String> ENTITY_NAMES = entityNames();

  @Test
  void mapsTheEntitiesEbsReturnsButCaabNeverBuilds() {
    // The merits statements of case and the means collections only ever reach an assessment from
    // EBS, so nothing else pins these names down.
    assertThat(OpaRelationshipUtil.getRelationshipName("NON_FAMILY_STATEMENT"))
        .contains("nonfamilystatementofcase");
    assertThat(OpaRelationshipUtil.getRelationshipName("FAMILY_STATEMENT"))
        .contains("familystatementofcase");
    assertThat(OpaRelationshipUtil.getRelationshipName("ASSET_IN_DISPUTE")).contains("asset");
    assertThat(OpaRelationshipUtil.getRelationshipName("ADDPROPERTY"))
        .contains("additionalproperties");
    assertThat(OpaRelationshipUtil.getRelationshipName("BANKACC")).contains("bankaccounts");
  }

  @Test
  void mapsTheEntitiesCaabBuildsItself() {
    assertThat(OpaRelationshipUtil.getRelationshipName("PROCEEDING")).contains("proceeding");
    assertThat(OpaRelationshipUtil.getRelationshipName("OPPONENT_OTHER_PARTIES"))
        .contains("opponentotherparties");
    assertThat(OpaRelationshipUtil.getRelationshipName("LINKED_CASES")).contains("linkedcases");
    assertThat(OpaRelationshipUtil.getRelationshipName("BILL_HISTORY")).contains("billhistory");
  }

  @Test
  void relationshipNamesCannotBeDerivedFromEntityNames() {
    // This is the whole reason the mapping is held as data. Lowercasing the entity name and
    // dropping its underscores - what this code used to do - is right for only a small minority of
    // entities, and every entity it is wrong for reaches OPA with no link to its parent.
    final long derivable = ENTITY_NAMES.stream().filter(this::derivationMatches).count();

    assertThat(derivable).isLessThan(ENTITY_NAMES.size() / 2);
  }

  @Test
  void isNotCaseOrWhitespaceSensitive() {
    assertThat(OpaRelationshipUtil.getRelationshipName("  non_family_statement  "))
        .contains("nonfamilystatementofcase");
  }

  @Test
  void returnsEmptyForAnythingItDoesNotKnow() {
    assertThat(OpaRelationshipUtil.getRelationshipName("NOT_AN_ENTITY")).isEmpty();
    assertThat(OpaRelationshipUtil.getRelationshipName("")).isEmpty();
    assertThat(OpaRelationshipUtil.getRelationshipName(null)).isEmpty();
  }

  @Test
  void holdsEveryMappingTheConnectorHolds() {
    // A checksum over the transcription from the connector's CcmsOpaRelationshipMap. A mapping
    // silently lost from the resource costs nothing at build time and breaks the interview for
    // whichever journey supplies that entity, so the count is asserted rather than left to drift.
    assertThat(ENTITY_NAMES).hasSize(120).doesNotHaveDuplicates();
  }

  private boolean derivationMatches(final String entityName) {
    return OpaRelationshipUtil.getRelationshipName(entityName)
        .filter(entityName.toLowerCase(Locale.ROOT).replace("_", "")::equals)
        .isPresent();
  }

  private static List<String> entityNames() {
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(
                OpaRelationshipUtilTest.class.getClassLoader().getResourceAsStream(RESOURCE),
                StandardCharsets.UTF_8))) {
      return reader
          .lines()
          .map(String::trim)
          .filter(line -> !line.isEmpty() && !line.startsWith("#"))
          .map(line -> line.substring(0, line.indexOf('=')))
          .toList();
    } catch (final Exception e) {
      throw new IllegalStateException("Could not read " + RESOURCE, e);
    }
  }
}
