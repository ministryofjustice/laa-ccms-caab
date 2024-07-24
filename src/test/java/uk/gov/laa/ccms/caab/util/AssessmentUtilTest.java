package uk.gov.laa.ccms.caab.util;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MEANS;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MEANS_PREPOP;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MERITS;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MERITS_PREPOP;

import java.util.List;
import org.junit.jupiter.api.Test;

class AssessmentUtilTest {

  @Test
  void testGetNonFinancialAssessmentNamesIncludingPrepop_returnsCorrectNames() {
    final List<String> expectedAssessmentNames = List.of(
        MEANS.getName(),
        MEANS_PREPOP.getName(),
        MERITS.getName(),
        MERITS_PREPOP.getName());

    final List<String> result = AssessmentUtil.getNonFinancialAssessmentNamesIncludingPrepop();

    assertNotNull(result);
    assertEquals(expectedAssessmentNames, result);
  }
}