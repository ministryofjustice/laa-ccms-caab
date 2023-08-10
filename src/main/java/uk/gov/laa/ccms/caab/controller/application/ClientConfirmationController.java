package uk.gov.laa.ccms.caab.controller.application;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple4;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.mapper.ClientResultDisplayMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.service.CaabApiService;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.caab.service.SoaGatewayService;
import uk.gov.laa.ccms.caab.util.ApplicationBuilder;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;

import java.text.ParseException;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CATEGORY_OF_LAW;
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

    private final ClientResultDisplayMapper clientResultDisplayMapper;

    @GetMapping("/application/client/{client-reference-number}/confirm")
    public String clientConfirm(@PathVariable("client-reference-number") String clientReferenceNumber,
                                @SessionAttribute(USER_DETAILS) UserDetail user,
                                Model model, HttpSession session) {
        log.info("GET /application/client/{}/confirm", clientReferenceNumber);

        ClientDetail clientInformation = soaGatewayService.getClient(clientReferenceNumber, user.getLoginId(),
                user.getUserType()).block();

        session.setAttribute("clientInformation", clientInformation);
        model.addAttribute("clientReferenceNumber", clientReferenceNumber);
        model.addAttribute("client", clientResultDisplayMapper.toClientResultRowDisplay(clientInformation));

        return "/application/application-client-confirmation";
    }


    @PostMapping("/application/client/confirmed")
    public Mono<String> clientConfirmed(String confirmedClientReference,
                                        @SessionAttribute(APPLICATION_DETAILS) ApplicationDetails applicationDetails,
                                        @SessionAttribute("clientInformation") ClientDetail clientInformation,
                                        @SessionAttribute(USER_DETAILS) UserDetail user) {
        log.info("POST /application/client/confirmed: {}", applicationDetails);

        if (!confirmedClientReference.equals(clientInformation.getClientReferenceNumber())) {
            throw new RuntimeException("Client information does not match");
        }

        //need to do this first in order to get amendment types
        ApplicationDetail baseApplication = new ApplicationBuilder()
                .applicationType(applicationDetails.getApplicationTypeCategory(), applicationDetails.isDelegatedFunctions())
                .build();

        // get case reference Number, category of law value, contractual devolved powers, amendment types
        Mono<Tuple4<CaseReferenceSummary, CommonLookupDetail, ContractDetails, AmendmentTypeLookupDetail>> combinedResult =
                Mono.zip(soaGatewayService.getCaseReference(user.getLoginId(), user.getUserType()),
                        dataService.getCommonValues(COMMON_VALUE_CATEGORY_OF_LAW, null, null),
                        soaGatewayService.getContractDetails(user.getProvider().getId(), applicationDetails.getOfficeId(), user.getLoginId(), user.getUserType()),
                        dataService.getAmendmentTypes(baseApplication.getApplicationType().getId()));

        return combinedResult.flatMap(tuple -> {
            CaseReferenceSummary caseReferenceSummary = tuple.getT1();
            CommonLookupDetail categoryOfLawLookupDetail = tuple.getT2();
            ContractDetails contractDetails = tuple.getT3();
            AmendmentTypeLookupDetail amendmentTypes = tuple.getT4();

            try {
                ApplicationDetail application = new ApplicationBuilder(baseApplication)
                        .caseReference(caseReferenceSummary)
                        .provider(user)
                        .client(clientInformation)
                        .categoryOfLaw(applicationDetails.getCategoryOfLawId(), categoryOfLawLookupDetail)
                        .office(applicationDetails.getOfficeId(), user.getProvider().getOffices())
                        .devolvedPowers(contractDetails.getContracts(), applicationDetails)
                        .larScopeFlag(amendmentTypes)
                        .status()
                        .build();

                // Create the application and block until it's done
                return caabApiService.createApplication(user.getLoginId(), application)
                        .doOnNext(createdApplication -> log.info("Application details submitted: {}", createdApplication))
                        .thenReturn("redirect:/application/agreement");

            } catch (ParseException e) {
                return Mono.error(new RuntimeException(e));
            }
        });
    }

}
