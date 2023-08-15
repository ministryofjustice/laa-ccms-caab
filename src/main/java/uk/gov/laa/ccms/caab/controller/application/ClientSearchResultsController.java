package uk.gov.laa.ccms.caab.controller.application;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.constants.SearchConstants;
import uk.gov.laa.ccms.caab.mapper.ClientResultDisplayMapper;
import uk.gov.laa.ccms.caab.model.ClientResultsDisplay;
import uk.gov.laa.ccms.caab.service.SoaGatewayService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;

/**
 * Controller responsible for managing the search operations related to clients.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {APPLICATION_DETAILS, CLIENT_SEARCH_CRITERIA, CLIENT_SEARCH_RESULTS})
public class ClientSearchResultsController {

  private final SoaGatewayService soaGatewayService;

  private final ClientResultDisplayMapper clientResultDisplayMapper;

  private final SearchConstants searchConstants;

  /**
   * Displays the search results for clients based on specified criteria.
   *
   * @param page                Current page for pagination.
   * @param size                Size of a page for pagination.
   * @param clientSearchCriteria Search criteria for finding clients.
   * @param user                The details of the currently authenticated user.
   * @param request             The HttpServletRequest.
   * @param model               Model to pass data to the view.
   * @return The view name for client search results or the appropriate error view.
   */
  @GetMapping("/application/client/results")
  public String clientSearchResults(
          @RequestParam(value = "page", defaultValue = "0") int page,
          @RequestParam(value = "size", defaultValue = "10") int size,
          @ModelAttribute(CLIENT_SEARCH_CRITERIA) ClientSearchCriteria clientSearchCriteria,
          @SessionAttribute(USER_DETAILS) UserDetail user,
          HttpServletRequest request,
          Model model) {
    log.info("GET /application/client/results");

    ClientDetails clientSearchResults = soaGatewayService.getClients(
            clientSearchCriteria,
            user.getLoginId(),
            user.getUserType(),
            page,
            size).block();

    if (clientSearchResults != null && clientSearchResults.getContent() != null
            && clientSearchResults.getTotalElements() > 0) {
      if (clientSearchResults.getTotalElements() > searchConstants.getMaxSearchResultsClients()) {
        return "/application/application-client-search-too-many-results";
      }
      String currentUrl = request.getRequestURL().toString();
      model.addAttribute("currentUrl", currentUrl);

      model.addAttribute(
              CLIENT_SEARCH_RESULTS,
              clientResultDisplayMapper.toClientResultsDisplay(clientSearchResults));

      return "/application/application-client-search-results";
    } else {
      return "/application/application-client-search-no-results";
    }
  }

  /**
   * Handles the client search results submission.
   *
   * @param clientSearchResults The display of client search results.
   * @param applicationDetails The details of the application.
   * @return A redirect string to the agreement page.
   */
  @PostMapping("/application/client/results")
  public String clientSearch(
          @ModelAttribute(CLIENT_SEARCH_RESULTS) ClientResultsDisplay clientSearchResults,
          @ModelAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails) {
    log.info("POST /application/client/results");

    // a post only occurs when register new client has been clicked
    // if so we want to amend application created to false, so they get
    // redirected correctly after the privacy notice
    applicationDetails.setApplicationCreated(false);

    return "redirect:/application/agreement";
  }
}

