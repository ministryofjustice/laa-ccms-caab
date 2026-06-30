package uk.gov.laa.ccms.caab.controller.submission;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.constants.CaseContext;

/** Controller for confirmed submissions. */
@Controller
@RequiredArgsConstructor
@Slf4j
public class SubmissionConfirmedController {

  /** Submission type for a provider request: either a case query or a general enquiry. */
  private static final String PROVIDER_REQUEST = "provider-request";

  /**
   * Handles the GET request for all confirmed submissions screen.
   *
   * @return The view name for a completed submission.
   */
  @GetMapping("/{caseContext}/{submissionType}/confirmed")
  public String submissionsConfirmed(
      @PathVariable("caseContext") CaseContext caseContext,
      @PathVariable String submissionType,
      @RequestParam(required = false) final String caseReferenceNumber,
      Model model) {

    model.addAttribute("submissionType", submissionType);
    model.addAttribute("caseContext", caseContext);

    applyProviderRequestCaseBanner(model, submissionType, caseReferenceNumber);

    return "submissions/submissionConfirmed";
  }

  /**
   * Hides the case context banner when a provider request's confirmation page is not about the case
   * the banner is for. A provider request carries its case reference on this URL when, and only
   * when, it is linked to a case, so an absent one means a general enquiry — and the banner, which
   * comes from the shared {@code ACTIVE_CASE} session attribute, would be showing an unrelated case
   * the user happened to visit earlier in the session.
   *
   * <p>Only the model is touched: {@code ACTIVE_CASE} is shared with the amendment journey and must
   * not be removed from the session. Other submission types are left alone — they do not carry a
   * case reference here, so absence tells us nothing about them.
   */
  private void applyProviderRequestCaseBanner(
      final Model model, final String submissionType, final String caseReferenceNumber) {

    if (!PROVIDER_REQUEST.equals(submissionType)) {
      return;
    }

    if (model.asMap().get(ACTIVE_CASE) instanceof ActiveCase activeCase) {
      final boolean bannerBelongsToPage =
          caseReferenceNumber != null
              && caseReferenceNumber.equals(activeCase.getCaseReferenceNumber());
      if (!bannerBelongsToPage) {
        model.asMap().remove(ACTIVE_CASE);
      }
    }
  }

  @GetMapping("/submissions/alreadySubmitted")
  public String alreadySubmitted() {
    return "submissions/alreadySubmitted";
  }
}
