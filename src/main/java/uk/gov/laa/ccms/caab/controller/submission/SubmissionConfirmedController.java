package uk.gov.laa.ccms.caab.controller.submission;

import static uk.gov.laa.ccms.caab.constants.SubmissionConstants.SUBMISSION_SUBMIT_CASE;
import static uk.gov.laa.ccms.caab.util.SubmissionUtil.isAlreadySubmitted;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.laa.ccms.caab.constants.CaseContext;

/** Controller for confirmed submissions. */
@Controller
@RequiredArgsConstructor
@Slf4j
public class SubmissionConfirmedController {
  private static final String DEFAULT_RETURN_URL = "/case/overview";

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
    if (!isAlreadySubmitted(session)) {
      return "redirect:/submissions/alreadySubmitted?returnUrl="
          + resolveReturnUrl(caseContext, submissionType);
    }

    model.addAttribute("submissionType", submissionType);
    model.addAttribute("caseContext", caseContext);

    return "submissions/submissionConfirmed";
  }

  @GetMapping("/submissions/alreadySubmitted")
  public String alreadySubmitted(
      @RequestParam(required = false) final String returnUrl, final Model model) {
    model.addAttribute("returnUrl", sanitizeReturnUrl(returnUrl));
    return "submissions/alreadySubmitted";
  }

  private String resolveReturnUrl(final CaseContext caseContext, final String submissionType) {
    if (caseContext.isAmendment()) {
      return "/case/overview";
    }
    return SUBMISSION_SUBMIT_CASE.equals(submissionType) ? "/home" : "/application/sections";
  }

  private String sanitizeReturnUrl(final String returnUrl) {
    if (!StringUtils.hasText(returnUrl)
        || !returnUrl.startsWith("/")
        || returnUrl.startsWith("//")
        || returnUrl.contains("\\")
        || returnUrl.chars().anyMatch(Character::isISOControl)) {
      return DEFAULT_RETURN_URL;
    }
    return returnUrl;
  }
}
