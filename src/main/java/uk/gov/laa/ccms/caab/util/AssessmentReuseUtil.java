package uk.gov.laa.ccms.caab.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;

/**
 * Holds the assessment attributes that must not be reused when an assessment is run as part of an
 * amendment.
 *
 * <p>The rulebases mark each attribute with a reassessment option. Attributes marked "Do Not Reuse"
 * (merits) or "Short Term Reuse" (means) - the evidence families among them - must be cleared from
 * an amendment's assessment data before the interview runs, so the provider is asked for them again
 * rather than inheriting the answers given on the original application. Old PUI does this in {@code
 * IframeHelper.removeAttributes}, from the lists extracted from the BR100; the two resource files
 * here are those lists.
 */
public final class AssessmentReuseUtil {

  private static final String MERITS_ATTRIBUTES = "assessment/merits-do-not-reuse-attributes.txt";
  private static final String MEANS_ATTRIBUTES = "assessment/means-short-term-reuse-attributes.txt";

  private static final Set<String> MERITS_NON_REUSABLE = load(MERITS_ATTRIBUTES);
  private static final Set<String> MEANS_NON_REUSABLE = load(MEANS_ATTRIBUTES);

  private AssessmentReuseUtil() {}

  /**
   * Returns the attributes that must not be reused on an amendment for the given rulebase.
   *
   * @param assessmentRulebase the rulebase being run
   * @return the attribute names to clear, empty for rulebases with no such list
   */
  public static Set<String> getNonReusableAttributes(final AssessmentRulebase assessmentRulebase) {
    if (assessmentRulebase == null) {
      return Collections.emptySet();
    }

    return switch (assessmentRulebase) {
      case MEANS -> MEANS_NON_REUSABLE;
      case MERITS -> MERITS_NON_REUSABLE;
      default -> Collections.emptySet();
    };
  }

  private static Set<String> load(final String resource) {
    try (InputStream inputStream =
        AssessmentReuseUtil.class.getClassLoader().getResourceAsStream(resource)) {

      if (inputStream == null) {
        throw new CaabApplicationException("Failed to load assessment attributes from " + resource);
      }

      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
        return reader
            .lines()
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
      }
    } catch (final IOException e) {
      throw new CaabApplicationException(
          "Failed to load assessment attributes from " + resource, e);
    }
  }
}
