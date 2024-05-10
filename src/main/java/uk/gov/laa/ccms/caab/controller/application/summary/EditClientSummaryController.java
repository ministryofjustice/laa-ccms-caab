package uk.gov.laa.ccms.caab.controller.application.summary;

import static uk.gov.laa.ccms.caab.constants.ClientActionConstants.ACTION_EDIT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_CLIENT_NAMES;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SubmissionConstants.SUBMISSION_UPDATE_CLIENT;

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
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientBasicDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientContactDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientEqualOpportunitiesMonitoringDetailsValidator;
import uk.gov.laa.ccms.caab.controller.application.client.AbstractClientSummaryController;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.ClientDetailMapper;
import uk.gov.laa.ccms.caab.model.BaseClientDetail;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientTransactionResponse;

/**
 * Controller for handling edits to client summary details during the application summary process.
 */
@Controller
@Slf4j
@SessionAttributes({CLIENT_FLOW_FORM_DATA})
public class EditClientSummaryController extends AbstractClientSummaryController {

  /**
   * Default constructor method implementing the abstract controller's constructor.
   */
  public EditClientSummaryController(
      final LookupService lookupService,
      final ClientService clientService,
      final ClientBasicDetailsValidator basicValidator,
      final ClientContactDetailsValidator contactValidator,
      final ClientAddressDetailsValidator addressValidator,
      final ClientEqualOpportunitiesMonitoringDetailsValidator opportunitiesValidator,
      final ClientDetailMapper clientDetailsMapper) {
    super(lookupService,
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
  public String getClientDetailsSummary(
      @SessionAttribute(USER_DETAILS) final UserDetail user,
      @SessionAttribute(ACTIVE_CASE) final ActiveCase activeCase,
      final Model model,
      final HttpSession session) {

    final ClientFlowFormData clientFlowFormData;

    if (session.getAttribute(CLIENT_FLOW_FORM_DATA) != null) {
      clientFlowFormData = (ClientFlowFormData) session.getAttribute(CLIENT_FLOW_FORM_DATA);
    } else {
      //if session contains clientDetails
      final ClientDetail clientInformation = clientService.getClient(
          activeCase.getClientReferenceNumber(),
          user.getLoginId(),
          user.getUserType())
          .blockOptional()
          .orElseThrow(() -> new CaabApplicationException("Failed to retrieve client"));

      //map data to the view
      clientFlowFormData = clientDetailsMapper.toClientFlowFormData(clientInformation.getDetails());
      clientFlowFormData.setAction(ACTION_EDIT);
      session.setAttribute(CLIENT_FLOW_FORM_DATA, clientFlowFormData);
    }

    populateSummaryListLookups(clientFlowFormData, model);
    model.addAttribute(CLIENT_FLOW_FORM_DATA, clientFlowFormData);

    return "application/summary/client-summary-details";
  }

  /**
   * Handles the POST request for the edit client summary page.
   *
   * @return The view name for the edit client summary details
   */
  @PostMapping("/application/summary/client/details/summary")
  public String postClientDetailsSummary(
      final @ModelAttribute(CLIENT_FLOW_FORM_DATA) ClientFlowFormData clientFlowFormData,
      final @SessionAttribute(USER_DETAILS) UserDetail user,
      final @SessionAttribute(ACTIVE_CASE) ActiveCase activeCase,
      final BindingResult bindingResult,
      final HttpSession session) {

    validateClientFlowFormData(clientFlowFormData, bindingResult);

    final ClientTransactionResponse response =
        clientService.updateClient(
            activeCase.getClientReferenceNumber(),
            clientFlowFormData,
            user)
            .blockOptional()
            .orElseThrow(() -> new CaabApplicationException("Failed to update Client"));


    final BaseClientDetail applicationClientNames =
        clientDetailsMapper.toBaseClient(clientFlowFormData);

    session.setAttribute(SUBMISSION_TRANSACTION_ID, response.getTransactionId());
    session.setAttribute(APPLICATION_CLIENT_NAMES, applicationClientNames);

    return String.format("redirect:/submissions/%s", SUBMISSION_UPDATE_CLIENT);
  }

}
