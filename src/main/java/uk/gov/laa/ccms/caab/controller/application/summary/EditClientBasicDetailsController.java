package uk.gov.laa.ccms.caab.controller.application.summary;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SubmissionConstants.SUBMISSION_CREATE_EDIT;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientBasicDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientContactDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientEqualOpportunitiesMonitoringDetailsValidator;
import uk.gov.laa.ccms.caab.controller.application.client.AbstractClientSummaryController;
import uk.gov.laa.ccms.caab.mapper.ClientDetailMapper;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.caab.service.CommonLookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientCreated;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

/**
 * Controller for handling edits to client basic details during the application summary process.
 */
@Controller
@Slf4j
@SessionAttributes({
    CLIENT_DETAILS
})
@RequiredArgsConstructor
public class EditClientBasicDetailsController {

  private final CommonLookupService commonLookupService;

  private final ClientBasicDetailsValidator clientBasicDetailsValidator;

  /**
   * Handles the GET request for edit client basic details page.
   *
   * @param clientDetails The details of the client.
   * @param model The model for the view.
   * @return The view name for the client basic details page
   */
  @GetMapping("/application/summary/client/details/basic")
  public String getClientDetailsBasic(
      @ModelAttribute(CLIENT_DETAILS) ClientDetails clientDetails,
      Model model) {

    populateDropdowns(model);

    return "application/summary/client-basic-details";
  }

  /**
   * Handles the edit basic client details submission.
   *
   * @param clientDetails The details of the client.
   * @param bindingResult Validation result.
   * @param model The model for the view.
   * @return A redirect string to the agreement page.
   */
  @PostMapping("/application/summary/client/details/basic")
  public String postClientDetailsBasic(
      @ModelAttribute(CLIENT_DETAILS) ClientDetails clientDetails,
      BindingResult bindingResult,
      Model model) {

    clientBasicDetailsValidator.validate(clientDetails, bindingResult);

    if (bindingResult.hasErrors()) {
      populateDropdowns(model);
      model.addAttribute(CLIENT_DETAILS, clientDetails);
      return "application/summary/client-basic-details";
    }

    model.addAttribute(CLIENT_DETAILS, clientDetails);

    return "redirect:/application/summary/client/details/summary";
  }

  /**
   * Populates dropdown options for the client basic details form.
   *
   * @param model The model for the view.
   */
  private void populateDropdowns(Model model) {
    // Asynchronously fetch titles
    Mono<CommonLookupDetail> titlesMono = commonLookupService.getContactTitles();

    // Asynchronously fetch countries
    Mono<CommonLookupDetail> countriesMono = commonLookupService.getCountries();

    // Asynchronously fetch genders
    Mono<CommonLookupDetail> gendersMono = commonLookupService.getGenders();

    // Asynchronously fetch marital statuses
    Mono<CommonLookupDetail> maritalStatusMono = commonLookupService.getMaritalStatuses();

    // Zip all Monos and populate the model once all results are available
    Mono.zip(titlesMono, countriesMono, gendersMono, maritalStatusMono)
        .doOnNext(tuple -> {
          model.addAttribute("titles", tuple.getT1().getContent());
          model.addAttribute("countries", tuple.getT2().getContent());
          model.addAttribute("genders", tuple.getT3().getContent());
          model.addAttribute("maritalStatusList", tuple.getT4().getContent());
        })
        .block();
  }

}
