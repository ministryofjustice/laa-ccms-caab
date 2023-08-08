package uk.gov.laa.ccms.caab.controller.application;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.*;
import uk.gov.laa.ccms.caab.service.CaabApiService;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.caab.service.SoaGatewayService;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

import java.text.ParseException;

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

    private final DataService dataService;

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

            if (caseReferenceSummary.getCaseReferenceNumber() == null) {
                throw new RuntimeException("No case reference number was created, unable to continue");
            } else {
                String caseReference = caseReferenceSummary.getCaseReferenceNumber();
                ApplicationDetailProvider provider = new ApplicationDetailProvider(user.getProvider().getId().toString())
                        .displayValue(user.getProvider().getName());

                ApplicationDetailClient client = new ApplicationDetailClient()
                        .firstName(clientInformation.getDetails().getName().getFirstName())
                        .surname(clientInformation.getDetails().getName().getSurname())
                        .reference(clientInformation.getClientReferenceNumber());

                StringDisplayValue categoryOfLaw = new StringDisplayValue()
                        .id(applicationDetails.getCategoryOfLawId())
                        .displayValue(applicationDetails.getCategoryOfLawDisplayValue());

                ApplicationDetail application = new ApplicationDetail(caseReference, provider, categoryOfLaw, client);

                IntDisplayValue office = new IntDisplayValue()
                        .id(applicationDetails.getOfficeId())
                        .displayValue(applicationDetails.getOfficeDisplayValue());
                application.setOffice(office);

                //get devolved powers
                String contractualDevolvedPower = soaGatewayService.getContractualDevolvedPowers(user.getProvider().getId(),
                        applicationDetails.getOfficeId(),
                        user.getLoginId(),
                        user.getUserType(),
                        application.getCategoryOfLaw().getId());

                //Delegated functions/devolved powers
                ApplicationDetailDevolvedPowers devolvedPowers = new ApplicationDetailDevolvedPowers();
                devolvedPowers.setContractFlag(contractualDevolvedPower);
                devolvedPowers.setUsed(applicationDetails.isDelegatedFunctions());
                if (applicationDetails.isDelegatedFunctions()){
                    devolvedPowers.setDateUsed(applicationDetails.getDelegatedFunctionDate());
                }
                application.setDevolvedPowers(devolvedPowers);

                //Application type
                StringDisplayValue applicationType = new StringDisplayValue()
                        .id(applicationDetails.getApplicationTypeId())
                        .displayValue(applicationDetails.getApplicationTypeDisplayValue());
                application.setApplicationType(applicationType);

                //call data api for amendment types - LAR SCOPE Flag
                AmendmentTypeLookupDetail amendmentTypes = dataService.getAmendmentTypes(applicationDetails.getApplicationTypeId(), null).block();
                if (amendmentTypes.getContent() != null){
                    AmendmentTypeLookupValueDetail amendmentType = amendmentTypes.getContent().get(0);
                    application.setLarScopeFlag(amendmentType.getDefaultLarScopeFlag());
                } else {
                    throw new RuntimeException("No amendment type available, unable to continue");
                }

                //Status
                StringDisplayValue status = new StringDisplayValue()
                        .id(STATUS_UNSUBMITTED_ACTUAL_VALUE)
                        .displayValue(STATUS_UNSUBMITTED_ACTUAL_VALUE_DISPLAY);
                application.setStatus(status);

                caabApiService.createApplication(user.getLoginId(), application).block();

                log.info("Application details to submit: {}", application);
            }

            return "redirect:TODO";
        }

        throw new RuntimeException("Client information does not match");
    }
}
