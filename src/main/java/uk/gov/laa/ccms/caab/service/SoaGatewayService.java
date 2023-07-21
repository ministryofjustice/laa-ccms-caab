package uk.gov.laa.ccms.caab.service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientSearchDetails;
import uk.gov.laa.ccms.soa.gateway.model.*;

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

    private Mono<ContractDetails> getContractDetails(Integer providerFirmId, Integer officeId,
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

    public Mono<ClientDetails> getClients(ClientSearchDetails clientSearchDetails, String loginId,
                                          String userType){
        return soaGatewayWebClient
                .get()
                .uri(builder -> builder.path("/clients")
                        .queryParamIfPresent("first-name", Optional.ofNullable(clientSearchDetails.getForename()))
                        .queryParamIfPresent("surname", Optional.ofNullable(clientSearchDetails.getSurname()))
                        .queryParamIfPresent("date-of-birth", Optional.ofNullable(clientSearchDetails.getDateOfBirth()))
                        .queryParamIfPresent("home-office-reference", Optional.ofNullable(clientSearchDetails.getUniqueIdentifier(UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE)))
                        .queryParamIfPresent("national-insurance_number", Optional.ofNullable(clientSearchDetails.getUniqueIdentifier(UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER)))
                        .queryParamIfPresent("client-reference-number", Optional.ofNullable(clientSearchDetails.getUniqueIdentifier(UNIQUE_IDENTIFIER_CASE_REFERENCE_NUMBER)))
                        .build())
                .header("SoaGateway-User-Login-Id", loginId)
                .header("SoaGateway-User-Role", userType)
                .retrieve()
                .bodyToMono(ClientDetails.class)
                .onErrorResume(e -> soaGatewayServiceErrorHandler.handleClientDetailsError(clientSearchDetails,e));

    }

}