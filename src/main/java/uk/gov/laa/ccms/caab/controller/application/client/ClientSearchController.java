package uk.gov.laa.ccms.caab.controller.application.client;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_SEARCH_CRITERIA;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.service.CommonLookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;

/**
 * Controller for handling client search operations.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {APPLICATION_DETAILS, CLIENT_SEARCH_CRITERIA})
public class ClientSearchController {

  private final CommonLookupService commonLookupService;

  private final ClientSearchCriteriaValidator clientSearchCriteriaValidator;

  /**
   * Provides default values for client search criteria.
   *
   * @return A new instance of ClientSearchCriteria.
   */
  @ModelAttribute(CLIENT_SEARCH_CRITERIA)
  public ClientSearchCriteria getClientSearchDetails() {
    return new ClientSearchCriteria();
  }

  /**
   * Handles the GET request for client search page.
   *
   * @param applicationDetails The details of the application.
   * @param clientSearchCriteria The criteria for client search.
   * @param model The model for the view.
   * @return The view name for the client search page.
   */
  @GetMapping("/application/client/search")
  public String clientSearch(
          @ModelAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails,
          @ModelAttribute(CLIENT_SEARCH_CRITERIA) ClientSearchCriteria clientSearchCriteria,
          Model model) {
    applicationDetails.setApplicationCreated(false);
    applicationDetails.setAgreementAccepted(false);

    populateDropdowns(model);
    return "application/application-client-search";
  }

  /**
   * Handles the POST request for client search form submission.
   *
   * @param clientSearchCriteria The criteria for client search.
   * @param bindingResult The result of data binding/validation.
   * @param model The model for the view.
   * @return A redirect string to the client search results page or the search page on errors.
   */
  @PostMapping("/application/client/search")
  public String clientSearch(
          @ModelAttribute(CLIENT_SEARCH_CRITERIA) ClientSearchCriteria clientSearchCriteria,
          BindingResult bindingResult,
          Model model) {

    clientSearchCriteriaValidator.validate(clientSearchCriteria, bindingResult);

    if (bindingResult.hasErrors()) {
      populateDropdowns(model);
      return "application/application-client-search";
    }

    return "redirect:/application/client/results";
  }

  /**
   * Populates dropdown options for the client search form.
   *
   * @param model The model for the view.
   */
  private void populateDropdowns(Model model) {
    CommonLookupDetail genders =
        Optional.ofNullable(commonLookupService.getGenders().block())
            .orElse(new CommonLookupDetail());
    model.addAttribute("genders", genders.getContent());

    CommonLookupDetail uniqueIdentifierTypes =
        Optional.ofNullable(commonLookupService.getUniqueIdentifierTypes().block())
            .orElse(new CommonLookupDetail());
    model.addAttribute("uniqueIdentifierTypes", uniqueIdentifierTypes.getContent());
  }
}
