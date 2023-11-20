package uk.gov.laa.ccms.caab.controller.application.client;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SUBMISSION_TRANSACTION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SubmissionConstants.SUBMISSION_CREATE_CLIENT;

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
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientBasicDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientContactDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientEqualOpportunitiesMonitoringDetailsValidator;
import uk.gov.laa.ccms.caab.mapper.ClientDetailMapper;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.caab.service.CommonLookupService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientCreated;

/**
 * Controller for handling client summary details during the new application process.
 */
@Controller
@Slf4j
@SessionAttributes({CLIENT_FLOW_FORM_DATA})
public class ClientSummaryController extends AbstractClientSummaryController {

  /**
   * Default constructor method implementing the abstract controller's constructor.
   */
  public ClientSummaryController(
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
  @GetMapping("/application/client/details/summary")
  public String clientDetailsSummary(
      @ModelAttribute(CLIENT_FLOW_FORM_DATA) ClientFlowFormData clientFlowFormData,
      Model model) {

    populateSummaryListLookups(clientFlowFormData, model);

    return "application/client/client-summary-details";
  }

  /**
   * Handles the POST request for the client summary page.
   *
   * @return The view name for the client summary details
   */
  @PostMapping("/application/client/details/summary")
  public String clientDetailsSummary(
      @ModelAttribute(CLIENT_FLOW_FORM_DATA) ClientFlowFormData clientFlowFormData,
      @SessionAttribute(USER_DETAILS) UserDetail user,
      BindingResult bindingResult,
      HttpSession session) {

    validateClientFlowFormData(clientFlowFormData, bindingResult);

    ClientCreated response =
        clientService.createClient(
            clientFlowFormData,
            user).block();

    session.setAttribute(SUBMISSION_TRANSACTION_ID, response.getTransactionId());

    return String.format("redirect:/submissions/%s", SUBMISSION_CREATE_CLIENT);
  }
}
