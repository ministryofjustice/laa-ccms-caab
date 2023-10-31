package uk.gov.laa.ccms.caab.controller.application.summary;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SubmissionConstants.SUBMISSION_CREATE_EDIT;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientBasicDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientContactDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientEqualOpportunitiesMonitoringDetailsValidator;
import uk.gov.laa.ccms.caab.controller.application.client.AbstractClientSummaryController;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.ClientDetailMapper;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.caab.service.CommonLookupService;
import uk.gov.laa.ccms.caab.util.ReflectionUtils;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientCreated;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

/**
 * Controller for handling edits to client summary details during the application summary process.
 */
@Controller
@Slf4j
@SessionAttributes({
    CLIENT_DETAILS
})
public class EditClientSummaryController extends AbstractClientSummaryController {

  public EditClientSummaryController(
      CommonLookupService commonLookupService,
      ClientService clientService,
      ClientBasicDetailsValidator basicValidator,
      ClientContactDetailsValidator contactValidator,
      ClientAddressDetailsValidator addressValidator,
      ClientEqualOpportunitiesMonitoringDetailsValidator opportunitiesValidator,
      ClientDetailMapper clientDetailsMapper) {
    super(commonLookupService,
        clientService,
        basicValidator,
        contactValidator,
        addressValidator,
        opportunitiesValidator,
        clientDetailsMapper);
  }


  /**
   * Handles the GET request for the client summary page.
   *
   * @return The view name for the client summary details
   */
  @GetMapping("/application/summary/client/details/summary")
  public String clientDetailsSummary(
      @SessionAttribute(USER_DETAILS) UserDetail user,
      @SessionAttribute(ACTIVE_CASE) ActiveCase activeCase,
      Model model,
      HttpSession session) {

    ClientDetails clientDetails;

    if (session.getAttribute(CLIENT_DETAILS) != null) {
      clientDetails = (ClientDetails) session.getAttribute(CLIENT_DETAILS);
    } else {
      //if session contains clientDetails
      ClientDetail clientInformation = clientService.getClient(
          activeCase.getClientReferenceNumber(),
          user.getLoginId(),
          user.getUserType()).block();

      //map data to the view
      clientDetails = clientDetailsMapper.toClientDetails(clientInformation);
      session.setAttribute(CLIENT_DETAILS, clientDetails);
    }

    populateSummaryListLookups(clientDetails, model);

    model.addAttribute(activeCase);
    model.addAttribute(CLIENT_DETAILS, clientDetails);

    return "application/summary/client-summary-details";
  }

  /**
   * Handles the POST request for the client summary page.
   *
   * @return The view name for the client summary details
   */
  @PostMapping("/application/client/details/summary")
  public String clientDetailsSummary(
      @ModelAttribute(CLIENT_DETAILS) ClientDetails clientDetails,
      @SessionAttribute(USER_DETAILS) UserDetail user,
      BindingResult bindingResult,
      HttpSession session) {

    validateClientDetails(clientDetails, bindingResult);

    //TODO AMEND TO UPDATE CLIENT
    ClientCreated response =
        clientService.createClient(
            clientDetails,
            user).block();

    session.setAttribute(SUBMISSION_TRANSACTION_ID, response.getTransactionId());

    return String.format("redirect:/submissions/%s", SUBMISSION_CREATE_EDIT);
  }

}
