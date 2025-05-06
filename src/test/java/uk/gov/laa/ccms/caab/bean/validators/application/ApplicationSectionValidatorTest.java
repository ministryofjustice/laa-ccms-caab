package uk.gov.laa.ccms.caab.bean.validators.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.SECTION_STATUS_COMPLETE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionStatusDisplay;
import uk.gov.laa.ccms.caab.model.sections.GeneralDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.OpponentsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.ProceedingsAndCostsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.ProviderSectionDisplay;

@ExtendWith(SpringExtension.class)
class ApplicationSectionValidatorTest {

  @InjectMocks
  private ApplicationSectionValidator applicationSectionValidator;

  private ApplicationSectionDisplay sectionData;

  private Errors errors;

  @BeforeEach
  public void setUp() {
    sectionData = ApplicationSectionDisplay.builder().build();
    errors = new BeanPropertyBindingResult(sectionData, "sectionData");
  }

  @Test
  void supports_ReturnsTrueForApplicationSectionDisplayClass() {
    assertTrue(applicationSectionValidator.supports(ApplicationSectionDisplay.class));
  }

  @Test
  void supports_ReturnsFalseForOtherClasses() {
    assertFalse(applicationSectionValidator.supports(Object.class));
  }

  @Test
  void validate_ProviderNotComplete_HasErrors() {
    sectionData.setProvider(ProviderSectionDisplay.builder().status("Incomplete").build());

    applicationSectionValidator.validate(sectionData, errors);

    assertTrue(errors.hasErrors());
    assertTrue(errors.hasGlobalErrors());
    assertTrue("provider.required".equals(errors.getGlobalError().getCode()));
  }

  @Test
  void validate_GeneralDetailsNotComplete_HasErrors() {
    sectionData.setProvider(ProviderSectionDisplay.builder().status(SECTION_STATUS_COMPLETE).build());
    sectionData.setGeneralDetails(GeneralDetailsSectionDisplay.builder().status("Incomplete").build());

    applicationSectionValidator.validate(sectionData, errors);

    assertTrue(errors.hasErrors());
    assertTrue(errors.hasGlobalErrors());
    assertTrue("generalDetails.required".equals(errors.getGlobalError().getCode()));
  }

  @Test
  void validate_ProceedingsAndCostsNotComplete_HasErrors() {
    sectionData.setProvider(ProviderSectionDisplay.builder().status(SECTION_STATUS_COMPLETE).build());
    sectionData.setGeneralDetails(GeneralDetailsSectionDisplay.builder().status(SECTION_STATUS_COMPLETE).build());
    sectionData.setProceedingsAndCosts(ProceedingsAndCostsSectionDisplay.builder().status("Incomplete").build());

    applicationSectionValidator.validate(sectionData, errors);

    assertTrue(errors.hasErrors());
    assertTrue(errors.hasGlobalErrors());
    assertTrue("proceedingsAndCosts.required".equals(errors.getGlobalError().getCode()));
  }

  @Test
  void validate_OpponentsNotComplete_HasErrors() {
    sectionData.setProvider(ProviderSectionDisplay.builder().status(SECTION_STATUS_COMPLETE).build());
    sectionData.setGeneralDetails(GeneralDetailsSectionDisplay.builder().status(SECTION_STATUS_COMPLETE).build());
    sectionData.setProceedingsAndCosts(ProceedingsAndCostsSectionDisplay.builder().status(SECTION_STATUS_COMPLETE).build());
    sectionData.setOpponentsAndOtherParties(OpponentsSectionDisplay.builder().status("Incomplete").build());

    applicationSectionValidator.validate(sectionData, errors);

    assertTrue(errors.hasErrors());
    assertTrue(errors.hasGlobalErrors());
    assertTrue("opponentsAndOtherParties.required".equals(errors.getGlobalError().getCode()));
  }

  @Test
  void validate_MeansAssessmentNotComplete_HasErrors() {
    sectionData.setProvider(ProviderSectionDisplay.builder().status(SECTION_STATUS_COMPLETE).build());
    sectionData.setGeneralDetails(GeneralDetailsSectionDisplay.builder().status(SECTION_STATUS_COMPLETE).build());
    sectionData.setProceedingsAndCosts(ProceedingsAndCostsSectionDisplay.builder().status(SECTION_STATUS_COMPLETE).build());
    sectionData.setOpponentsAndOtherParties(OpponentsSectionDisplay.builder().status(SECTION_STATUS_COMPLETE).build());
    sectionData.setMeansAssessment(ApplicationSectionStatusDisplay.builder().status("Incomplete").build());

    applicationSectionValidator.validate(sectionData, errors);

    assertTrue(errors.hasErrors());
    assertTrue(errors.hasGlobalErrors());
    assertTrue("meansAssessment.required".equals(errors.getGlobalError().getCode()));
  }

  @Test
  void validate_MeritsAssessmentNotComplete_HasErrors() {
    sectionData.setProvider(ProviderSectionDisplay.builder().status(SECTION_STATUS_COMPLETE).build());
    sectionData.setGeneralDetails(GeneralDetailsSectionDisplay.builder().status(SECTION_STATUS_COMPLETE).build());
    sectionData.setProceedingsAndCosts(ProceedingsAndCostsSectionDisplay.builder().status(SECTION_STATUS_COMPLETE).build());
    sectionData.setOpponentsAndOtherParties(
        OpponentsSectionDisplay.builder().status(SECTION_STATUS_COMPLETE).build());
    sectionData.setMeansAssessment(ApplicationSectionStatusDisplay.builder().status(SECTION_STATUS_COMPLETE).build());
    sectionData.setMeritsAssessment(ApplicationSectionStatusDisplay.builder().status("Incomplete").build());

    applicationSectionValidator.validate(sectionData, errors);

    assertTrue(errors.hasErrors());
    assertTrue(errors.hasGlobalErrors());
    assertTrue("meritsAssessment.required".equals(errors.getGlobalError().getCode()));
  }

  @Test
  void validate_AllSectionsComplete_NoErrors() {
    sectionData.setProvider(ProviderSectionDisplay.builder().status(SECTION_STATUS_COMPLETE).build());
    sectionData.setGeneralDetails(GeneralDetailsSectionDisplay.builder().status(SECTION_STATUS_COMPLETE).build());
    sectionData.setProceedingsAndCosts(
        ProceedingsAndCostsSectionDisplay.builder().status(SECTION_STATUS_COMPLETE).build());
    sectionData.setOpponentsAndOtherParties(OpponentsSectionDisplay.builder().status(SECTION_STATUS_COMPLETE).build());
    sectionData.setMeansAssessment(ApplicationSectionStatusDisplay.builder().status(SECTION_STATUS_COMPLETE).build());
    sectionData.setMeritsAssessment(ApplicationSectionStatusDisplay.builder().status(SECTION_STATUS_COMPLETE).build());

    applicationSectionValidator.validate(sectionData, errors);

    assertFalse(errors.hasErrors());
  }
}
