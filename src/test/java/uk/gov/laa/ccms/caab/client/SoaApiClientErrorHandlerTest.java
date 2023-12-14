package uk.gov.laa.ccms.caab.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.bean.CopyCaseSearchCriteria;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetail;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetails;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.ClientCreated;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientStatus;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;
import uk.gov.laa.ccms.soa.gateway.model.Notifications;

@ExtendWith(MockitoExtension.class)
class SoaApiClientErrorHandlerTest {
  @Mock
  private Logger loggerMock;
  @InjectMocks
  private SoaApiClientErrorHandler soaApiClientErrorHandler;

  @Test
  public void testHandleNotificationSummaryError() {
    String loginId = "testLoginId";
    Throwable throwable = new RuntimeException("Error");

    Mono<NotificationSummary> result =
        soaApiClientErrorHandler.handleNotificationSummaryError(loginId, throwable);

    StepVerifier.create(result)
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  public void testHandleContractDetailsError() {
    Integer providerFirmId = 123;
    Integer officeId = 4567;
    Throwable throwable = new RuntimeException("Error");

    Mono<ContractDetails> result =
        soaApiClientErrorHandler.handleContractDetailsError(providerFirmId, officeId,
            throwable);

    StepVerifier.create(result)
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  public void testHandleClientDetailsError() {
    ClientSearchCriteria clientSearchCriteria = new ClientSearchCriteria();
    clientSearchCriteria.setForename("John");
    clientSearchCriteria.setSurname("Doe");
    clientSearchCriteria.setDobYear("1990");
    clientSearchCriteria.setDobMonth("02");
    clientSearchCriteria.setDobDay("01");
    clientSearchCriteria.setUniqueIdentifierType(1);
    clientSearchCriteria.setUniqueIdentifierValue("ABC123");

    Throwable throwable = new RuntimeException("Error");

    Mono<ClientDetails> result =
        soaApiClientErrorHandler.handleClientDetailsError(clientSearchCriteria, throwable);

    StepVerifier.create(result)
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  public void testHandleCaseDetailsError() {
    CopyCaseSearchCriteria copyCaseSearchCriteria = new CopyCaseSearchCriteria();
    copyCaseSearchCriteria.setCaseReference("caseRef123");
    copyCaseSearchCriteria.setProviderCaseReference("provCaseRef456");
    copyCaseSearchCriteria.setActualStatus("status1");
    copyCaseSearchCriteria.setFeeEarnerId(123);
    copyCaseSearchCriteria.setOfficeId(456);
    copyCaseSearchCriteria.setClientSurname("Doe");

    Throwable throwable = new RuntimeException("Error");

    Mono<CaseDetails> result =
        soaApiClientErrorHandler.handleCaseDetailsError(copyCaseSearchCriteria, throwable);

    StepVerifier.create(result)
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  public void testHandleCaseDetailError() {
    Throwable throwable = new RuntimeException("Error");

    Mono<CaseDetail> result =
        soaApiClientErrorHandler.handleCaseDetailError("123", throwable);

    StepVerifier.create(result)
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  public void testHandleClientDetailError() {
    String clientReferenceNumber = "testClientRefNumber";
    Throwable throwable = new RuntimeException("Error");

    Mono<ClientDetail> result =
        soaApiClientErrorHandler.handleClientDetailError(clientReferenceNumber, throwable);

    StepVerifier.create(result)
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  public void testHandleCaseReferenceError() {
    Throwable throwable = new RuntimeException("Error");

    Mono<CaseReferenceSummary> result =
        soaApiClientErrorHandler.handleCaseReferenceError(throwable);

    StepVerifier.create(result)
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  public void testHandleClientStatusError() {
    String transactionId = "testTransactionId";
    Throwable throwable = new RuntimeException("Error");

    Mono<ClientStatus> result =
        soaApiClientErrorHandler.handleClientStatusError(transactionId, throwable);

    StepVerifier.create(result)
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  public void testHandleClientCreatedError() {
    String fullName = "John Doe";
    Throwable throwable = new RuntimeException("Error");

    Mono<ClientCreated> result =
        soaApiClientErrorHandler.handleClientCreatedError(fullName, throwable);

    StepVerifier.create(result)
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  public void testHandleNotificationsError() {
    NotificationSearchCriteria criteria = new NotificationSearchCriteria();
    criteria.setCaseReference("caseRef123");
    criteria.setProviderCaseReference("provCaseRef456");
    criteria.setAssignedToUserId("userId789");
    criteria.setClientSurname("Doe");
    criteria.setFeeEarnerId(123);
    criteria.setIncludeClosed(true);
    criteria.setNotificationType("typeABC");
    criteria.setDateFrom("2023-01-01");
    criteria.setDateTo("2023-12-31");

    Throwable throwable = new RuntimeException("Error");

    Mono<Notifications> result =
        soaApiClientErrorHandler.handleNotificationsError(criteria, throwable);

    StepVerifier.create(result)
        .expectNextCount(0)
        .verifyComplete();
  }

}