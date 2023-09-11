package uk.gov.laa.ccms.caab.controller.application.client;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_DETAILS;

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
 * Controller for handling contact client details selection during the new application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({
    CLIENT_DETAILS
})
@SuppressWarnings({"unchecked"})
public class ClientContactDetailsController {

  private final CommonLookupService commonLookupService;

  private final ClientContactDetailsValidator clientContactDetailsValidator;

  /**
   * Handles the GET request for client contact details page.
   *
   * @param clientDetails The details of the client.
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("application/client/details/contact")
  public String clientDetailsContact(
          @ModelAttribute(CLIENT_DETAILS) ClientDetails clientDetails,
          Model model) {
    log.info("GET /application/client/details/contact");
    populateDropdowns(model);
    return "application/client/contact-client-details";
  }

  /**
   * Handles the client contact details results submission.
   *
   * @param clientDetails The details of the client.
   * @param bindingResult Validation result.
   * @param model The model for the view.
   * @return A redirect string to the agreement page.
   */
  @PostMapping("/application/client/details/contact")
  public String clientDetailsContact(
      @ModelAttribute(CLIENT_DETAILS) ClientDetails clientDetails,
      BindingResult bindingResult,
      Model model) {
    log.info("POST /application/client/details/contact");

    clientContactDetailsValidator.validate(clientDetails, bindingResult);
    model.addAttribute(CLIENT_DETAILS, clientDetails);

    if (bindingResult.hasErrors()) {
      populateDropdowns(model);
      return "application/client/contact-client-details";
    }

    log.info("clientDetails: {}", clientDetails);
    return "redirect:/application/client/details/address";
  }

  private void populateDropdowns(Model model) {

    Mono<CommonLookupDetail> correspondenceMethodMono =
        commonLookupService.getCorrespondenceMethods();

    // Asynchronously fetch marital statuses
    Mono<CommonLookupDetail> correspondenceLanguageMono =
        commonLookupService.getCorrespondenceLanguagess();

    // Zip all Monos and populate the model once all results are available
    Mono.zip(correspondenceMethodMono, correspondenceLanguageMono)
        .doOnNext(tuple -> {
          model.addAttribute("correspondenceMethods", tuple.getT1().getContent());
          model.addAttribute("correspondenceLanguages", tuple.getT2().getContent());
        })
        .block();

  }
}
