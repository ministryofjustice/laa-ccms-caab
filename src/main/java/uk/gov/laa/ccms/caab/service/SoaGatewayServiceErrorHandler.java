package uk.gov.laa.ccms.caab.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientSearchDetails;
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
            ClientSearchDetails clientSearchDetails, Throwable e) {
        log.error("Failed to retrieve ClientDetails for firstName: {}, surname: {}, dob: {}, homeOfficeReference: {}," +
                " nationalInsuranceNumber: {}, caseReferenceNumber: {}",
                clientSearchDetails.getForename(), clientSearchDetails.getSurname(), clientSearchDetails.getDateOfBirth(),
                clientSearchDetails.getUniqueIdentifier(UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE),
                clientSearchDetails.getUniqueIdentifier(UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER),
                clientSearchDetails.getUniqueIdentifier(UNIQUE_IDENTIFIER_CASE_REFERENCE_NUMBER), e);
        return Mono.empty();
    }


}
