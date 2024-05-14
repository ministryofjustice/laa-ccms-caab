package uk.gov.laa.ccms.caab.controller;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentName;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.service.AssessmentService;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller handling assessment requests.
 */
@Controller
@RequiredArgsConstructor
public class AssessmentController {

  private final AssessmentService assessmentService;

  /**
   * Displays the page to confirm the removal of an assessment.
   *
   * @param assessmentCategory the category of the assessment to remove.
   * @param model the model to populate with data for the view.
   * @return the name of the view to render for removing a prior authority.
   */
  @GetMapping("/assessments/{assessmentCategory}/remove")
  public String assessmentRemove(
      @PathVariable("assessmentCategory") final String assessmentCategory,
      final Model model) {

    model.addAttribute("assessmentCategory", assessmentCategory);

    return "application/assessments/assessment-remove";
  }

  /**
   * Handles the removal of an assessment.
   *
   * @param assessmentCategory the category of the assessment to remove.
   * @param user the user making the request.
   * @param activeCase the active case for which the assessment is being removed.
   * @return the name of the view to render after the assessment has been removed.
   */
  @PostMapping("/assessments/{assessmentCategory}/remove")
  public String assessmentRemovePost(
      @PathVariable("assessmentCategory") final String assessmentCategory,
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase) {

    final List<String> assessmentCategories =
        AssessmentName.findAssessmentNamesByCategory(assessmentCategory);

    if (assessmentCategories.isEmpty()) {
      throw new CaabApplicationException("Invalid assessment type");
    } else {
      assessmentService.deleteAssessments(
          user,
          assessmentCategories,
          activeCase.getCaseReferenceNumber(),
          null)
          .block();

    }

    return "redirect:/application/summary";
  }


}
