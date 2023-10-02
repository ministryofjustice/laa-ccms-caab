package uk.gov.laa.ccms.caab.controller.application.client;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_SEARCH_CRITERIA;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
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
    CLIENT_DETAILS})
public class CancelClientRegistrationController {

  /**
   * Handles the GET request for cancelling the creation of a new client page.
   *
   * @return The view name for cancelling the creation of a new client page
   */
  @GetMapping("/application/client/details/cancel")
  public String clientDetailsBasic() {
    return "application/client/cancel-client";
  }

  /**
   * Handles the client search results submission.
   *
   * @param model The model for the view.
   * @return A redirect string to the agreement page.
   */
  @PostMapping("/application/client/details/cancel")
  public String clientDetailsBasic(
      Model model) {

    model.addAttribute(CLIENT_SEARCH_CRITERIA, new ClientSearchCriteria());
    model.addAttribute(CLIENT_DETAILS, new ClientDetails());

    return "redirect:/application/client/search";
  }

}
