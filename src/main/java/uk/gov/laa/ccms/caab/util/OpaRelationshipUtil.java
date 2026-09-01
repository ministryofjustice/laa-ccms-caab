package uk.gov.laa.ccms.caab.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

/**
 * Maps an OPA entity name to the relationship name that entity is published under.
 *
 * <p>The two are not related by any rule. An entity's name and the relationship the rulebase
 * publishes it under were authored separately, so {@code NON_FAMILY_STATEMENT} is reached through
 * {@code nonfamilystatementofcase} and {@code ADDPROPERTY} through {@code additionalproperties}.
 * Lowercasing the entity name and dropping its underscores gives the right answer for only 14 of
 * the 120 entities here - it happens to hold for every entity CAAB builds itself and for the
 * billing collections, which is why it survived as an assumption for as long as it did.
 *
 * <p>Getting it wrong is not visible from CAAB. The connector resolves the relationship name in
 * {@code HubTdsService.constructHubLinks}; a name it cannot resolve is logged and skipped, so the
 * entity reaches OPA with no link to its parent. OPA then rejects the whole seed transaction - "No
 * parent link reference specified for row '...' in table '...'" - and the provider is shown a
 * generic error instead of the interview.
 *
 * <p>The mapping is the connector's {@code CcmsOpaRelationshipMap}, which took it from the TDS view
 * {@code xxccms_opaentities_v} ({@code ENTITY_CODE}, {@code RELATIONSHIP_PUBLIC_NAME}). It is held
 * here as a resource rather than derived so that the entities EBS returns but CAAB never builds -
 * the merits statements of case, and most of the means collections - are linked under the names the
 * rulebases actually publish.
 */
public final class OpaRelationshipUtil {

  private static final String RELATIONSHIP_NAMES = "assessment/opa-relationship-names.txt";

  private static final Map<String, String> RELATIONSHIP_BY_ENTITY = load(RELATIONSHIP_NAMES);

  private OpaRelationshipUtil() {}

  /**
   * Returns the relationship name the given entity is published under.
   *
   * @param entityName the OPA entity name, as EBS and the rulebases spell it
   * @return the relationship name, or empty when the entity is not one of the mapped entities
   */
  public static Optional<String> getRelationshipName(final String entityName) {
    if (entityName == null || entityName.isBlank()) {
      return Optional.empty();
    }

    return Optional.ofNullable(
        RELATIONSHIP_BY_ENTITY.get(entityName.trim().toUpperCase(Locale.ROOT)));
  }

  private static Map<String, String> load(final String resource) {
    try (InputStream inputStream =
        OpaRelationshipUtil.class.getClassLoader().getResourceAsStream(resource)) {

      if (inputStream == null) {
        throw new CaabApplicationException(
            "Failed to load OPA relationship names from " + resource);
      }

      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

        final Map<String, String> relationshipByEntity = new LinkedHashMap<>();

        reader
            .lines()
            .map(String::trim)
            .filter(line -> !line.isEmpty() && !line.startsWith("#"))
            .forEach(line -> putMapping(relationshipByEntity, line, resource));

        return Map.copyOf(relationshipByEntity);
      }
    } catch (final IOException e) {
      throw new CaabApplicationException(
          "Failed to load OPA relationship names from " + resource, e);
    }
  }

  private static void putMapping(
      final Map<String, String> relationshipByEntity, final String line, final String resource) {

    final int separator = line.indexOf('=');
    if (separator < 1 || separator == line.length() - 1) {
      throw new CaabApplicationException(
          "Expected ENTITY_NAME=relationshipname in %s but found '%s'".formatted(resource, line));
    }

    final String entityName = line.substring(0, separator).trim().toUpperCase(Locale.ROOT);
    final String existing =
        relationshipByEntity.putIfAbsent(entityName, line.substring(separator + 1).trim());

    // A duplicate would silently take whichever relationship name came last, so refuse to load.
    if (existing != null) {
      throw new CaabApplicationException(
          "Duplicate entity %s in %s".formatted(entityName, resource));
    }
  }
}
