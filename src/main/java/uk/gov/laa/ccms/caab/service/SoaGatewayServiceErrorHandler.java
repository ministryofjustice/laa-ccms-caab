package uk.gov.laa.ccms.caab.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.CopyCaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;

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

    public Mono<CaseDetails> handleCaseDetailsError(
        CopyCaseSearchCriteria copyCaseSearchCriteria, Throwable e) {
        log.error("Failed to retrieve CaseDetails for "
                + "caseReferenceNumber: {}, "
                + "providerCaseReference: {}, "
                + "caseStatus: {}, "
                + "feeEarnerId: {}, "
                + "officeId: {}, "
                + "clientSurname: {}",
            copyCaseSearchCriteria.getCaseReference(),
            copyCaseSearchCriteria.getProviderCaseReference(),
            copyCaseSearchCriteria.getActualStatus(),
            copyCaseSearchCriteria.getFeeEarnerId(),
            copyCaseSearchCriteria.getOfficeId(),
            copyCaseSearchCriteria.getClientSurname(), e);
        return Mono.empty();
    }


}
