package uk.gov.laa.ccms.caab.controller.application.summary;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_DETAILS;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientContactDetailsValidator;
import uk.gov.laa.ccms.caab.service.CommonLookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;

/**
 * Controller for handling edits to client basic details during the application summary process.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
@SessionAttributes({CLIENT_DETAILS})
public class EditClientContactDetailsController {

  private final CommonLookupService commonLookupService;

  private final ClientContactDetailsValidator clientContactDetailsValidator;

  @ModelAttribute("editedClientDetails")
  public ClientDetails getClientDetails(HttpSession session) {
    return (ClientDetails) session.getAttribute(CLIENT_DETAILS);
  }

  /**
   * Handles the GET request for edit client contact details page.
   *
   * @param clientDetails The details of the client.
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("/application/summary/client/details/contact")
  public String getClientDetailsBasic(
      @ModelAttribute("editedClientDetails") ClientDetails clientDetails,
      Model model) {

    populateDropdowns(model);
    model.addAttribute("editedClientDetails", clientDetails);

    return "application/summary/client-contact-details";
  }

  /**
   * Handles the edit contact client details submission.
   *
   * @param clientDetails The details of the client.
   * @param bindingResult Validation result.
   * @param model The model for the view.
   * @return A redirect string to the agreement page.
   */
  @PostMapping("/application/summary/client/details/contact")
  public String postClientDetailsBasic(
      @ModelAttribute("editedClientDetails") ClientDetails clientDetails,
      BindingResult bindingResult,
      Model model,
      HttpSession session) {

    clientContactDetailsValidator.validate(clientDetails, bindingResult);

    if (bindingResult.hasErrors()) {
      populateDropdowns(model);
      model.addAttribute("editedClientDetails", clientDetails);
      return "application/summary/client-contact-details";
    }

    session.setAttribute(CLIENT_DETAILS, clientDetails);
    return "redirect:/application/summary/client/details/summary";
  }

  /**
   * Populates dropdown options for the client contact details form.
   *
   * @param model The model for the view.
   */
  private void populateDropdowns(Model model) {
    Mono<CommonLookupDetail> correspondenceMethodMono =
        commonLookupService.getCorrespondenceMethods();

    // Asynchronously fetch marital statuses
    Mono<CommonLookupDetail> correspondenceLanguageMono =
        commonLookupService.getCorrespondenceLanguages();

    // Zip all Monos and populate the model once all results are available
    Mono.zip(correspondenceMethodMono, correspondenceLanguageMono)
        .doOnNext(tuple -> {
          model.addAttribute("correspondenceMethods", tuple.getT1().getContent());
          model.addAttribute("correspondenceLanguages", tuple.getT2().getContent());
        })
        .block();
  }

}
