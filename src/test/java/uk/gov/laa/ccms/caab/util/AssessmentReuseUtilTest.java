package uk.gov.laa.ccms.caab.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;

@DisplayName("AssessmentReuseUtil test")
class AssessmentReuseUtilTest {

  @Test
  void meritsListContainsTheDoNotReuseAttributes() {
    final Set<String> attributes =
        AssessmentReuseUtil.getNonReusableAttributes(AssessmentRulebase.MERITS);

    // Extracted from old PUI's MeritsDoNotReuseAttributes (BR100, "Do Not Reuse"). The exact size
    // is a checksum over that transcription: lines silently lost from the resource would leave the
    // attributes reused on an amendment (the provider is never re-asked) with nothing failing at
    // runtime. Update it only when the BR100 list itself changes - that is a behaviour change.
    assertThat(attributes).hasSize(565);
    assertThat(attributes)
        .contains(
            "MERITS_EVIDENCE_REQD",
            "ADDITIONAL_EVIDENCE_COLLECTED",
            "MANDATORY_EVIDENCE_COLLECTED",
            "REQUIRED_EVIDENCE_IDENTIFIED",
            "AMENDMENT");
  }

  @Test
  void meansListContainsTheShortTermReuseAttributes() {
    final Set<String> attributes =
        AssessmentReuseUtil.getNonReusableAttributes(AssessmentRulebase.MEANS);

    // Extracted from old PUI's MeansShortTermReuseAttributes (BR100, "Short Term Reuse"). The exact
    // size is a checksum over that transcription - see the merits test above.
    assertThat(attributes).hasSize(890);
    assertThat(attributes).contains("MEANS_EVIDENCE_REQD", "BANKACC_SMOD_FLAG", "CLIENT_PRISONER");
  }

  @Test
  void meansAndMeritsListsAreDistinct() {
    assertThat(AssessmentReuseUtil.getNonReusableAttributes(AssessmentRulebase.MEANS))
        .isNotEqualTo(AssessmentReuseUtil.getNonReusableAttributes(AssessmentRulebase.MERITS));
  }

  @Test
  void financialRulebasesHaveNoNonReusableAttributes() {
    assertThat(AssessmentReuseUtil.getNonReusableAttributes(AssessmentRulebase.BILLING)).isEmpty();
    assertThat(AssessmentReuseUtil.getNonReusableAttributes(AssessmentRulebase.POA)).isEmpty();
  }

  @Test
  void nullRulebaseReturnsEmpty() {
    assertThat(AssessmentReuseUtil.getNonReusableAttributes(null)).isEmpty();
  }
}
