package uk.gov.laa.ccms.caab.bean.validators.application;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.SECTION_STATUS_COMPLETE;

import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.validators.AbstractValidator;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;

/** Validates the application sections. */
@Component
public class ApplicationSectionValidator extends AbstractValidator {

  private static final String validationMessage =
      "You cannot submit this Application until all application sections are marked Completed. "
          + "Please complete the relevant sections and try again.";

  @Override
  public boolean supports(final Class<?> clazz) {
    return ApplicationSectionDisplay.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(final Object target, final Errors errors) {
    final ApplicationSectionDisplay applicationSectionDisplay = (ApplicationSectionDisplay) target;

    // check provider
    if (!Objects.equals(
        applicationSectionDisplay.getProvider().getStatus(), SECTION_STATUS_COMPLETE)) {
      errors.reject("provider.required", validationMessage);

      // check general details
    } else if (!Objects.equals(
        applicationSectionDisplay.getGeneralDetails().getStatus(), SECTION_STATUS_COMPLETE)) {
      errors.reject("generalDetails.required", validationMessage);

      // check proceedings and costs
    } else if (!Objects.equals(
        applicationSectionDisplay.getProceedingsAndCosts().getStatus(), SECTION_STATUS_COMPLETE)) {
      errors.reject("proceedingsAndCosts.required", validationMessage);

      // check opponents
    } else if (!Objects.equals(
        applicationSectionDisplay.getOpponentsAndOtherParties().getStatus(),
        SECTION_STATUS_COMPLETE)) {
      errors.reject("opponentsAndOtherParties.required", validationMessage);

      // check means assessment
    } else if (!Objects.equals(
        applicationSectionDisplay.getMeansAssessment().getStatus(), SECTION_STATUS_COMPLETE)) {
      errors.reject("meansAssessment.required", validationMessage);

      // check merits assessment
    } else if (!Objects.equals(
        applicationSectionDisplay.getMeritsAssessment().getStatus(), SECTION_STATUS_COMPLETE)) {
      errors.reject("meritsAssessment.required", validationMessage);
    }
  }
}
