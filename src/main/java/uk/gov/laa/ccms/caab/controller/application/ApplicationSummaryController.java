package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Controller for handling application type selection during the application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ApplicationSummaryController {

  @GetMapping("/application/summary")
  public String selectOffice(
      Model model) {

    return "application/summary-task-page";
  }

}
