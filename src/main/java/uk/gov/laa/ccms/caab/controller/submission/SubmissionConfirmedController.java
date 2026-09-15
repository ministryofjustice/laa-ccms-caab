package uk.gov.laa.ccms.caab.controller.submission;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.GENERAL_PROVIDER_REQUEST_CONFIRMATION_ID;
import static uk.gov.laa.ccms.caab.constants.SubmissionConstants.SUBMISSION_SUBMIT_CASE;
import static uk.gov.laa.ccms.caab.constants.SubmissionConstants.SUBMISSION_SUBMIT_CASE_PROVIDER_REQUEST;
import static uk.gov.laa.ccms.caab.constants.SubmissionConstants.SUBMISSION_SUBMIT_GENERAL_PROVIDER_REQUEST;
import static uk.gov.laa.ccms.caab.util.SubmissionUtil.isAlreadySubmitted;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
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
  private static final String RETURN_DESTINATION = "returnDestination";
  private static final ArrayList<String> HOME_RETURN_URL_SUBMISSION_TYPES =
      new ArrayList<>(List.of(SUBMISSION_SUBMIT_CASE, SUBMISSION_SUBMIT_GENERAL_PROVIDER_REQUEST));
  private static final ArrayList<String> CASE_OVERVIEW_RETURN_URL_SUBMISSION_TYPES =
      new ArrayList<>(List.of(SUBMISSION_SUBMIT_CASE_PROVIDER_REQUEST));

  /**
   * Handles the GET request for all confirmed submissions screen.
   *
   * @return The view name for a completed submission.
   */
  @GetMapping("/{caseContext}/{submissionType}/confirmed")
  public String submissionsConfirmed(
      @PathVariable("caseContext") CaseContext caseContext,
      @PathVariable String submissionType,
      @RequestParam(required = false) final String submissionId,
      final HttpSession session,
      Model model) {
    final boolean hasConfirmedSubmission =
        isAlreadySubmitted(session)
            && (!isGeneralProviderRequest(submissionType)
                || submissionId == null
                || hasValidGeneralConfirmationId(session, submissionId));
    if (!hasConfirmedSubmission) {
      return "redirect:/submissions/alreadySubmitted?returnUrl="
          + resolveReturnUrl(caseContext, submissionType);
    }

    model.addAttribute("submissionType", submissionType);
    model.addAttribute("caseContext", caseContext);
    model.addAttribute("submissionId", submissionId);

    return "submissions/submissionConfirmed";
  }

  @GetMapping("/submissions/alreadySubmitted")
  public String alreadySubmitted(
      @RequestParam(required = false) final String returnUrl, final Model model) {
    final String safeReturnUrl = sanitizeReturnUrl(returnUrl);
    model.addAttribute("returnUrl", safeReturnUrl);
    model.addAttribute(RETURN_DESTINATION, resolveReturnDestination(safeReturnUrl).name());
    return "submissions/alreadySubmitted";
  }

  private String resolveReturnUrl(final CaseContext caseContext, final String submissionType) {
    if (caseContext.isAmendment()
        || CASE_OVERVIEW_RETURN_URL_SUBMISSION_TYPES.contains(submissionType)) {
      return "/case/overview";
    }
    return HOME_RETURN_URL_SUBMISSION_TYPES.contains(submissionType)
        ? "/home"
        : "/application/sections";
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

  private ReturnDestination resolveReturnDestination(final String returnUrl) {
    final String pathOnly = returnUrl == null ? null : returnUrl.split("[?#]", 2)[0];
    return switch (pathOnly) {
      case "/home" -> ReturnDestination.HOME;
      case "/application/sections" -> ReturnDestination.APPLICATION_SECTIONS;
      default -> ReturnDestination.CASE_OVERVIEW;
    };
  }

  private boolean isGeneralProviderRequest(final String submissionType) {
    return SUBMISSION_SUBMIT_GENERAL_PROVIDER_REQUEST.equals(submissionType);
  }

  private boolean hasValidGeneralConfirmationId(
      final HttpSession session, final String submissionId) {
    final Object activeConfirmationId =
        session.getAttribute(GENERAL_PROVIDER_REQUEST_CONFIRMATION_ID);
    return submissionId != null && submissionId.equals(activeConfirmationId);
  }

  private enum ReturnDestination {
    HOME,
    CASE_OVERVIEW,
    APPLICATION_SECTIONS
  }
}
