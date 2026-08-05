package uk.gov.laa.ccms.caab.controller.submission;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_RESULT;
import static uk.gov.laa.ccms.caab.constants.SubmissionConstants.SUBMISSION_SUBMIT_CASE;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.laa.ccms.caab.constants.CaseContext;

/** Controller for confirmed submissions. */
@Controller
@RequiredArgsConstructor
@Slf4j
public class SubmissionConfirmedController {

  /**
   * Handles the GET request for all confirmed submissions screen.
   *
   * @return The view name for a completed submission.
   */
  @GetMapping("/{caseContext}/{submissionType}/confirmed")
  public String submissionsConfirmed(
      @PathVariable("caseContext") CaseContext caseContext,
      @PathVariable String submissionType,
      final HttpSession session,
      Model model) {
    if (!"confirmed".equals(session.getAttribute(SUBMISSION_RESULT))) {
      return "redirect:/submissions/alreadySubmitted?returnUrl="
          + resolveReturnUrl(caseContext, submissionType);
    }

    model.addAttribute("submissionType", submissionType);
    model.addAttribute("caseContext", caseContext);

    return "submissions/submissionConfirmed";
  }

  @GetMapping("/submissions/alreadySubmitted")
  public String alreadySubmitted(
      @RequestParam(defaultValue = "/case/overview") final String returnUrl, final Model model) {
    model.addAttribute("returnUrl", returnUrl);
    return "submissions/alreadySubmitted";
  }

  private String resolveReturnUrl(final CaseContext caseContext, final String submissionType) {
    if (caseContext.isAmendment()) {
      return "/case/overview";
    }
    return SUBMISSION_SUBMIT_CASE.equals(submissionType) ? "/home" : "/application/sections";
  }
}
