package uk.gov.laa.ccms.caab.controller.application.client;

import static uk.gov.laa.ccms.caab.constants.ClientActionConstants.ACTION_CREATE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_SEARCH_CRITERIA;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;

/**
 * Controller for handling the cancellation of creating new client details during the new
 * application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {
    CLIENT_SEARCH_CRITERIA,
    CLIENT_FLOW_FORM_DATA})
public class CancelClientRegistrationController {

  /**
   * Handles the GET request for cancelling the creation of a new client page.
   *
   * @return The view name for cancelling the creation of a new client page
   */
  @GetMapping("/application/client/details/cancel")
  public String clientDetailsCancel() {
    return "application/client/cancel-client";
  }

  /**
   * Handles the client search results submission.
   *
   * @param model The model for the view.
   * @return A redirect string to the agreement page.
   */
  @PostMapping("/application/client/details/cancel")
  public String postClientDetailsCancel(
      Model model) {

    model.addAttribute(CLIENT_SEARCH_CRITERIA, new ClientSearchCriteria());
    model.addAttribute(CLIENT_FLOW_FORM_DATA, new ClientFlowFormData(ACTION_CREATE));

    return "redirect:/application/client/search";
  }

}
