package uk.gov.laa.ccms.caab.controller.application;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.*;
import uk.gov.laa.ccms.caab.service.CaabApiService;
import uk.gov.laa.ccms.caab.service.SoaGatewayService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;


@Controller
@RequiredArgsConstructor
@Slf4j
@SessionAttributes(value = {"clientInformation"})
public class ClientConfirmationController {

    private final SoaGatewayService soaGatewayService;

    private final CaabApiService caabApiService;

    @GetMapping("/application/client/{client-reference-number}/confirm")
    public String clientConfirm(@PathVariable("client-reference-number") String clientReferenceNumber,
                                @SessionAttribute(USER_DETAILS) UserDetail user,
                                Model model) {
        log.info("GET /application/client/{}/confirm", clientReferenceNumber);

        ClientDetail clientInformation = soaGatewayService.getClient(clientReferenceNumber, user.getLoginId(),
                user.getUserType()).block();

        model.addAttribute("clientInformation", clientInformation);
        model.addAttribute("clientReferenceNumber", clientReferenceNumber);

        return "/application/application-client-confirmation";
    }


    @PostMapping("/application/client/confirmed")
    public String clientConfirmed(String confirmedClientReference,
                                  @SessionAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails,
                                  @SessionAttribute("clientInformation") ClientDetail clientInformation,
                                  @SessionAttribute(USER_DETAILS) UserDetail user) throws ParseException {
        log.info("POST /application/client/confirmed: {}", applicationDetails);

        if (confirmedClientReference.equals(clientInformation.getClientReferenceNumber())){
            applicationDetails.setClient(clientInformation);

            //get case reference Number
            CaseReferenceSummary caseReferenceSummary = soaGatewayService.getCaseReference(user.getLoginId(),
                    user.getUserType()).block();

            //get the case reference
            String caseReference = caseReferenceSummary.getCaseReferenceNumber();
            //todo if null then error

            ApplicationDetailProvider provider = new ApplicationDetailProvider()
                    .id(user.getProvider().getId().toString())
                    .displayValue(user.getProvider().getName());

            ApplicationDetailClient client = new ApplicationDetailClient()
                    .firstName(clientInformation.getDetails().getName().getFirstName())
                    .surname(clientInformation.getDetails().getName().getSurname())
                    .reference(clientInformation.getClientReferenceNumber());

            IntDisplayValue office = new IntDisplayValue()
                    .id(applicationDetails.getOfficeId())
                    .displayValue(applicationDetails.getOfficeDisplayValue());

            StringDisplayValue categoryOfLaw = new StringDisplayValue()
                    .id(applicationDetails.getCategoryOfLawId())
                    .displayValue(applicationDetails.getCategoryOfLawDisplayValue());

            ApplicationDetail application = new ApplicationDetail(caseReference, provider, categoryOfLaw, client);

            //get devolved powers
            String contractualDevolvedPower = soaGatewayService.getContractualDevolvedPowers(user.getProvider().getId(),
                    applicationDetails.getOfficeId(),
                    user.getLoginId(),
                    user.getUserType(),
                    application.getCategoryOfLaw().getId());

            //Delegated functions/devolved powers
            ApplicationDetailDevolvedPowers devolvedPowers = new ApplicationDetailDevolvedPowers();
            devolvedPowers.setContractFlag(contractualDevolvedPower);
            if (applicationDetails.isDelegatedFunctions()){
                devolvedPowers.setUsed(true);
                devolvedPowers.setDateUsed(applicationDetails.getDelegatedFunctionDate());
            }
            application.setDevolvedPowers(devolvedPowers);

            //Application type
            StringDisplayValue applicationType = new StringDisplayValue()
                    .id(applicationDetails.getApplicationTypeId())
                    .displayValue(applicationDetails.getApplicationTypeDisplayValue());
            application.setApplicationType(applicationType);

            //TODO LAR SCOPE flag
            //TODO - SELECT DEFAULT_LAR_SCOPE_FLAG FROM XXCCMS_APP_AMEND_TYPES_V WHERE APP_TYPE_CODE = ?
            //TODO - DATA API CALL - CCLS-1733

            //Status
            StringDisplayValue status = new StringDisplayValue()
                    .id(STATUS_UNSUBMITTED_ACTUAL_VALUE)
                    .displayValue(STATUS_UNSUBMITTED_ACTUAL_VALUE_DISPLAY);
            application.setStatus(status);

            //default cost limitation
            ApplicationDetailCosts costs = new ApplicationDetailCosts();
            costs.grantedCostLimitation(new BigDecimal(0));

            //correspondence address
            application.setCorrespondenceAddress(new ApplicationDetailCorrespondenceAddress());

            //cost - costStructure
            application.setCosts(new ApplicationDetailCosts());

            caabApiService.createApplication(user.getLoginId(), application).block();

            log.info("Application details to submit: {}", applicationDetails);
        }

        return "redirect:TODO";
    }
}
