package uk.gov.laa.ccms.caab.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.annotation.Contract;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.CopyCaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.soa.gateway.model.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.*;

@Service
@Slf4j
public class SoaGatewayService {
    private final WebClient soaGatewayWebClient;

    private final SoaGatewayServiceErrorHandler soaGatewayServiceErrorHandler;

    public SoaGatewayService(@Qualifier("soaGatewayWebClient") WebClient soaGatewayWebClient,
        SoaGatewayServiceErrorHandler soaGatewayServiceErrorHandler) {
        this.soaGatewayWebClient = soaGatewayWebClient;
        this.soaGatewayServiceErrorHandler = soaGatewayServiceErrorHandler;
    }

    public Mono<NotificationSummary> getNotificationsSummary(String loginId, String userType){
        return soaGatewayWebClient
                .get()
                .uri("/users/{loginId}/notifications/summary", loginId)
                .header("SoaGateway-User-Login-Id", loginId)
                .header("SoaGateway-User-Role", userType)
                .retrieve()
                .bodyToMono(NotificationSummary.class)
                .onErrorResume(e -> soaGatewayServiceErrorHandler.handleNotificationSummaryError(loginId, e));
    }

    public List<String> getCategoryOfLawCodes(Integer providerFirmId, Integer officeId,
        String loginId, String userType, Boolean initialApplication) {
        ContractDetails contractDetails = this.getContractDetails(
            providerFirmId,
            officeId,
            loginId,
            userType).block();

        // Process and filter the response
        return Optional.ofNullable(contractDetails)
            .map(cd -> filterCategoriesOfLaw(cd.getContracts(), initialApplication))
            .orElse(Collections.emptyList());
    }

    public String getContractualDevolvedPowers(Integer providerFirmId, Integer officeId,
                                               String loginId, String userType, String categoryOfLaw) {

        ContractDetails contractDetails = this.getContractDetails(
                providerFirmId,
                officeId,
                loginId,
                userType).block();

        // Process and filter the contracts to get devolved powers
        return Optional.ofNullable(contractDetails)
                .map(cd -> filterContractualDevolvedPowers(cd.getContracts(), categoryOfLaw))
                .orElse(null);
    }



    public Mono<ContractDetails> getContractDetails(Integer providerFirmId, Integer officeId,
        String loginId, String userType){
        return soaGatewayWebClient
                .get()
                .uri(builder -> builder.path("/contract-details")
                        .queryParam("providerFirmId", providerFirmId)
                        .queryParam("officeId", officeId)
                        .build())
                .header("SoaGateway-User-Login-Id", loginId)
                .header("SoaGateway-User-Role", userType)
                .retrieve()
                .bodyToMono(ContractDetails.class)
                .onErrorResume(e -> soaGatewayServiceErrorHandler.handleContractDetailsError(providerFirmId, officeId, e));
    }

    /**
     * Build a filtered list of Category Of Law.
     * Include the Category code only if
     * - CreateNewMatters is true
     * or
     * - This is not an initial Application and RemainderAuthorisation is true
     *
     * @param contractDetails The List of contract details to process
     * @param initialApplication if it is an initial application
     * @return List of Category Of Law Codes
     */
    private List<String> filterCategoriesOfLaw(List<ContractDetail> contractDetails,
        final Boolean initialApplication) {
        return contractDetails.stream()
            .filter(c -> Boolean.TRUE.equals(c.isCreateNewMatters()) || (!initialApplication
                && Boolean.TRUE.equals(c.isRemainderAuthorisation())))
            .map(ContractDetail::getCategoryofLaw)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Get a String for the contractual devolved powers.
     * @param contractDetails The List of contract details to process
     * @param categoryOfLaw the category of law to filter out
     * @return String of the first contractual devolved power.
     */
    public String filterContractualDevolvedPowers(List<ContractDetail> contractDetails, String categoryOfLaw) {
        return contractDetails != null ? contractDetails.stream()
                .filter(contract -> categoryOfLaw.equals(contract.getCategoryofLaw()))
                .map(ContractDetail::getContractualDevolvedPowers)
                .findFirst()
                .orElse(null)
                : null;
    }

    public Mono<ClientDetails> getClients(ClientSearchCriteria clientSearchCriteria, String loginId,
                                           String userType, Integer page, Integer size){
        return soaGatewayWebClient
                .get()
                .uri(builder -> builder.path("/clients")
                        .queryParamIfPresent("first-name", Optional.ofNullable(clientSearchCriteria.getForename()))
                        .queryParamIfPresent("surname", Optional.ofNullable(clientSearchCriteria.getSurname()))
                        .queryParamIfPresent("date-of-birth", Optional.ofNullable(clientSearchCriteria.getDateOfBirth()))
                        .queryParamIfPresent("home-office-reference", Optional.ofNullable(clientSearchCriteria.getUniqueIdentifier(UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE)))
                        .queryParamIfPresent("national-insurance_number", Optional.ofNullable(clientSearchCriteria.getUniqueIdentifier(UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER)))
                        .queryParamIfPresent("case-reference-number", Optional.ofNullable(clientSearchCriteria.getUniqueIdentifier(UNIQUE_IDENTIFIER_CASE_REFERENCE_NUMBER)))
                        .queryParamIfPresent("page", Optional.ofNullable(page))
                        .queryParamIfPresent("size", Optional.ofNullable(size))
                        .build())
                .header("SoaGateway-User-Login-Id", loginId)
                .header("SoaGateway-User-Role", userType)
                .retrieve()
                .bodyToMono(ClientDetails.class)
                .onErrorResume(e -> soaGatewayServiceErrorHandler.handleClientDetailsError(clientSearchCriteria,e));

    }

    public Mono<ClientDetail> getClient(String clientReferenceNumber, String loginId,
        String userType){
        return soaGatewayWebClient
            .get()
            .uri("/clients/{clientReferenceNumber}", clientReferenceNumber)
            .header("SoaGateway-User-Login-Id", loginId)
            .header("SoaGateway-User-Role", userType)
            .retrieve()
            .bodyToMono(ClientDetail.class)
            .onErrorResume(e -> soaGatewayServiceErrorHandler.handleClientDetailError(clientReferenceNumber,e));

    }

    public Mono<CaseDetails> getCases(CopyCaseSearchCriteria copyCaseSearchCriteria, String loginId,
        String userType, Integer page, Integer size){
        return soaGatewayWebClient
            .get()
            .uri(builder -> builder.path("/cases")
                .queryParamIfPresent("case-reference-number", Optional.ofNullable(
                    copyCaseSearchCriteria.getCaseReference()))
                .queryParamIfPresent("provider-case-reference", Optional.ofNullable(
                    copyCaseSearchCriteria.getProviderCaseReference()))
                .queryParamIfPresent("case-status", Optional.ofNullable(copyCaseSearchCriteria.getActualStatus()))
                .queryParamIfPresent("fee-earner-id", Optional.ofNullable(copyCaseSearchCriteria.getFeeEarnerId()))
                .queryParamIfPresent("office-id", Optional.ofNullable(copyCaseSearchCriteria.getOfficeId()))
                .queryParamIfPresent("client-surname", Optional.ofNullable(copyCaseSearchCriteria.getClientSurname()))
                .queryParamIfPresent("page", Optional.ofNullable(page))
                .queryParamIfPresent("size", Optional.ofNullable(size))
                .build())
            .header("SoaGateway-User-Login-Id", loginId)
            .header("SoaGateway-User-Role", userType)
            .retrieve()
            .bodyToMono(CaseDetails.class)
            .onErrorResume(e -> soaGatewayServiceErrorHandler.handleCaseDetailsError(
                copyCaseSearchCriteria,e));

    }

    public Mono<CaseReferenceSummary> getCaseReference(String loginId,
                                        String userType){
        return soaGatewayWebClient
                .get()
                .uri("/case-reference")
                .header("SoaGateway-User-Login-Id", loginId)
                .header("SoaGateway-User-Role", userType)
                .retrieve()
                .bodyToMono(CaseReferenceSummary.class)
                .onErrorResume(e -> soaGatewayServiceErrorHandler.handleCaseReferenceError(e));

    }

}