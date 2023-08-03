package uk.gov.laa.ccms.caab.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.soa.gateway.model.*;

import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.*;

@Slf4j
@Component
public class SoaGatewayServiceErrorHandler {

    public Mono<NotificationSummary> handleNotificationSummaryError(String loginId, Throwable e) {
        log.error("Failed to retrieve Notification count for loginId: {}", loginId, e);
        return Mono.empty();
    }

    public Mono<ContractDetails> handleContractDetailsError(
        Integer providerFirmId, Integer officeId, Throwable e) {
        log.error("Failed to retrieve ContractDetails for providerFirmId: {}, officeId: {}",
            providerFirmId, officeId, e);
        return Mono.empty();
    }

    public Mono<ClientDetails> handleClientDetailsError(
            ClientSearchCriteria clientSearchCriteria, Throwable e) {
        log.error("Failed to retrieve ClientDetails for firstName: {}, surname: {}, dob: {}, homeOfficeReference: {}," +
                " nationalInsuranceNumber: {}, caseReferenceNumber: {}",
                clientSearchCriteria.getForename(), clientSearchCriteria.getSurname(), clientSearchCriteria.getDateOfBirth(),
                clientSearchCriteria.getUniqueIdentifier(UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE),
                clientSearchCriteria.getUniqueIdentifier(UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER),
                clientSearchCriteria.getUniqueIdentifier(UNIQUE_IDENTIFIER_CASE_REFERENCE_NUMBER), e);
        return Mono.empty();
    }

    public Mono<ClientDetail> handleClientDetailError(
            String clientReferenceNumber, Throwable e) {
        log.error("Failed to retrieve ClientDetail for clientReferenceNumber: {}",clientReferenceNumber, e);
        return Mono.empty();
    }

    public Mono<CaseReferenceSummary> handleCaseReferenceError(Throwable e) {
        log.error("Failed to retrieve CaseReferenceSummary", e);
        return Mono.empty();
    }


}
