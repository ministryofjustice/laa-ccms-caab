package uk.gov.laa.ccms.caab.util;

import java.util.Collections;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentAttributeDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityDetail;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentEntityTypeDetail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MEANS;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MEANS_PREPOP;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MERITS;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentName.MERITS_PREPOP;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentAttribute.APPLICATION_CASE_REF;
import static uk.gov.laa.ccms.caab.constants.assessment.AssessmentEntityType.GLOBAL;

import java.util.List;
import org.junit.jupiter.api.Test;

public class AssessmentUtilTest {

  @ParameterizedTest
  @CsvSource({
      "CASE123, CASE123, CASE123, true",        // valid case reference
      "CASE123, DIFFERENT_CASE, DIFFERENT_CASE, false", // invalid case reference
      "CASE123, CASE123, DIFFERENT_CASE, false", // mismatched attributes
      "CASE123, , , false" // no attributes
  })
  void testIsAssessmentReferenceConsistent(
      final String caseReferenceNumber,
      final String entityName,
      final String attributeValue,
      final boolean expected) {
    final AssessmentDetail assessment = new AssessmentDetail();
    assessment.setCaseReferenceNumber(caseReferenceNumber);

    final AssessmentEntityDetail globalEntity = new AssessmentEntityDetail();
    globalEntity.setName(entityName);

    if (attributeValue != null && !attributeValue.isEmpty()) {
      final AssessmentAttributeDetail attributeDetail = new AssessmentAttributeDetail();
      attributeDetail.setName(APPLICATION_CASE_REF.name());
      attributeDetail.setValue(attributeValue);
      globalEntity.setAttributes(Collections.singletonList(attributeDetail));
    } else {
      globalEntity.setAttributes(Collections.emptyList());
    }

    final AssessmentEntityTypeDetail globalEntityType = new AssessmentEntityTypeDetail();
    globalEntityType.setName(GLOBAL.getType());
    globalEntityType.setEntities(Collections.singletonList(globalEntity));

    assessment.setEntityTypes(Collections.singletonList(globalEntityType));

    final boolean result = AssessmentUtil.isAssessmentReferenceConsistent(assessment);
    assertEquals(expected, result);
  }

  @ParameterizedTest
  @CsvSource({
      "UNCERTAIN_VALUE_STRING, TEXT, ",
      "invalidDate, DATE, invalidDate",
      "invalidCurrency, CURRENCY, invalidCurrency",
      "invalidNumber, NUMBER, invalidNumber",
  })
  void testGetFormattedAttributeValue_CatchBlocks(
      final String value, final String type, final String expected) {
    final AssessmentAttributeDetail attribute = new AssessmentAttributeDetail();
    attribute.setValue(value);
    attribute.setType(type);

    final String result = AssessmentUtil.getFormattedAttributeValue(attribute);
    assertEquals(expected, result);
  }
  
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
