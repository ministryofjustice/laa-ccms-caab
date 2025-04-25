package uk.gov.laa.ccms.caab.controller.submission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Controller for confirmed submissions.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class SubmissionConfirmedController {

  /**
   * Handles the GET request for all confirmed submissions screen.
   *
   * @return The view name for a completed submission.
   */
  @GetMapping("/application/{submissionType}/confirmed")
  public String submissionsConfirmed(
      @PathVariable String submissionType,
      Model model) {

    model.addAttribute("submissionType", submissionType);

    return "submissions/submissionConfirmed";
  }
}
