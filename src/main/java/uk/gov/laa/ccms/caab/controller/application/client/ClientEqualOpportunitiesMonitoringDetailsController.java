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
import uk.gov.laa.ccms.caab.bean.validators.client.ClientEqualOpportunitiesMonitoringDetailsValidator;
import uk.gov.laa.ccms.caab.service.CommonLookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;

/**
 * Controller for handling equal opportunities monitoring client details selection during the
 * new application process.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes({
    CLIENT_DETAILS
})
@SuppressWarnings({"unchecked"})
public class ClientEqualOpportunitiesMonitoringDetailsController {

  private final CommonLookupService commonLookupService;

  private final ClientEqualOpportunitiesMonitoringDetailsValidator validator;

  /**
   * Handles the GET request for client equal opportunities monitoring details page.
   *
   * @param clientDetails The details of the client.
   * @param model The model for the view.
   * @return The view name for the client equal opportunities monitoring details page
   */
  @GetMapping("application/client/details/equal-opportunities-monitoring")
  public String clientDetailsEqualOpportunitiesMonitoring(
          @ModelAttribute(CLIENT_DETAILS) ClientDetails clientDetails,
          Model model) {
    populateDropdowns(model);
    return "application/client/equal-opportunities-monitoring-client-details";
  }

  /**
   * Handles the client equal opportunities monitoring details results submission.
   *
   * @param clientDetails The details of the client.
   * @param bindingResult Validation result.
   * @param model The model for the view.
   * @return A redirect string to the client summary page.
   */
  @PostMapping("/application/client/details/equal-opportunities-monitoring")
  public String clientDetailsEqualOpportunitiesMonitoring(
      @ModelAttribute(CLIENT_DETAILS) ClientDetails clientDetails,
      BindingResult bindingResult,
      Model model) {

    validator.validate(clientDetails, bindingResult);
    model.addAttribute(CLIENT_DETAILS, clientDetails);

    if (bindingResult.hasErrors()) {
      populateDropdowns(model);
      return "application/client/equal-opportunities-monitoring-client-details";
    }

    log.info("clientDetails: {}", clientDetails);
    return "redirect:/application/client/details/summary";
  }

  private void populateDropdowns(Model model) {

    // Asynchronously fetch ethnic origins
    Mono<CommonLookupDetail> ethnicOriginsMono =
        commonLookupService.getEthnicOrigins();

    // Asynchronously fetch disabilities
    Mono<CommonLookupDetail> disabilitiesMono =
        commonLookupService.getDisabilities();

    // Zip all Monos and populate the model once all results are available
    Mono.zip(ethnicOriginsMono, disabilitiesMono)
        .doOnNext(tuple -> {
          model.addAttribute("ethnicOrigins", tuple.getT1().getContent());
          model.addAttribute("disabilities", tuple.getT2().getContent());
        })
        .block();

  }
}
