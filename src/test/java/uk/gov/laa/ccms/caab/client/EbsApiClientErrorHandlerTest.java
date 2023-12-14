package uk.gov.laa.ccms.caab.client;


import static uk.gov.laa.ccms.caab.client.EbsApiClientErrorHandler.AWARD_TYPE_ERROR_MESSAGE;
import static uk.gov.laa.ccms.caab.client.EbsApiClientErrorHandler.OUTCOME_RESULTS_ERROR_MESSAGE;
import static uk.gov.laa.ccms.caab.client.EbsApiClientErrorHandler.PRIOR_AUTHORITY_TYPE_ERROR_MESSAGE;
import static uk.gov.laa.ccms.caab.client.EbsApiClientErrorHandler.SCOPE_LIMITATIONS_ERROR_MESSAGE;
import static uk.gov.laa.ccms.caab.client.EbsApiClientErrorHandler.STAGE_END_ERROR_MESSAGE;
import static uk.gov.laa.ccms.caab.client.EbsApiClientErrorHandler.USERS_ERROR_MESSAGE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AwardTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.OutcomeResultLookupDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetails;
import uk.gov.laa.ccms.data.model.ProceedingDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetails;
import uk.gov.laa.ccms.data.model.StageEndLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.data.model.UserDetails;

@ExtendWith(MockitoExtension.class)
public class EbsApiClientErrorHandlerTest {
  @InjectMocks
  private EbsApiClientErrorHandler ebsApiClientErrorHandler;

  @BeforeEach
  public void setUp() {
    ebsApiClientErrorHandler = new EbsApiClientErrorHandler();
  }

  @Test
  public void testHandleUserError() {
    Throwable throwable = new RuntimeException("Error");

    Mono<UserDetail> result = ebsApiClientErrorHandler.handleUserError("testLoginId", throwable);

    StepVerifier.create(result)
        .verifyErrorMatches(e -> e instanceof EbsApiClientException
            && e.getMessage().equals("Failed to retrieve User with loginId: testLoginId")
            && e.getCause() == throwable);
  }

  @Test
  public void testHandleCommonValuesError() {
    Throwable throwable = new RuntimeException("Error");

    Mono<CommonLookupDetail> result =
        ebsApiClientErrorHandler.handleCommonValuesError("testType", "testCode", "testSort",
            throwable);

    StepVerifier.create(result)
        .verifyErrorMatches(e -> e instanceof EbsApiClientException
            && e.getMessage().equals(
            "Failed to retrieve Common Values: (type: testType, code: testCode, sort: testSort)")
            && e.getCause() == throwable);
  }

  @Test
  public void testHandleCountryLookupError() {
    Throwable throwable = new RuntimeException("Error");

    Mono<CommonLookupDetail> result = ebsApiClientErrorHandler.handleCountryLookupError(throwable);

    StepVerifier.create(result)
        .verifyErrorMatches(e -> e instanceof EbsApiClientException
            && e.getMessage().equals("Failed to retrieve Countries")
            && e.getCause() == throwable);
  }

  @Test
  public void testHandleCaseStatusValuesError() {
    Throwable throwable = new RuntimeException("Error");

    Mono<CaseStatusLookupDetail> result =
        ebsApiClientErrorHandler.handleCaseStatusValuesError(true, throwable);

    StepVerifier.create(result)
        .verifyErrorMatches(e -> e instanceof EbsApiClientException
            && e.getMessage().equals("Failed to retrieve Case Status Values: (copyAllowed: true)")
            && e.getCause() == throwable);
  }

  @Test
  public void testHandleToCaseRelationshipValuesError() {
    Throwable throwable = new RuntimeException("Error");

    Mono<RelationshipToCaseLookupDetail> result =
        ebsApiClientErrorHandler.handleToCaseRelationshipValuesError(throwable);

    StepVerifier.create(result)
        .verifyErrorMatches(e -> e instanceof EbsApiClientException
            && e.getMessage().equals("Failed to retrieve relationship to case")
            && e.getCause() == throwable);
  }

  @Test
  public void testHandleAmendmentTypeLookupError() {
    Throwable throwable = new RuntimeException("Error");
    String applicationType = "testApplicationType";

    Mono<AmendmentTypeLookupDetail> result =
        ebsApiClientErrorHandler.handleAmendmentTypeLookupError(applicationType, throwable);

    final String expectedMessage =
        String.format("Failed to retrieve Amendment Types: (applicationType: %s)", applicationType);

    StepVerifier.create(result)
        .verifyErrorMatches(e -> e instanceof EbsApiClientException
            && e.getMessage().equals(expectedMessage)
            && e.getCause() == throwable);
  }

  @Test
  public void testHandleProviderError() {
    Throwable throwable = new RuntimeException("Error");
    Integer providerId = 1234;

    Mono<ProviderDetail> result =
        ebsApiClientErrorHandler.handleProviderError(providerId, throwable);

    StepVerifier.create(result)
        .verifyErrorMatches(e -> e instanceof EbsApiClientException
            && e.getMessage().equals("Failed to retrieve Provider: (id: 1234)")
            && e.getCause() == throwable);
  }

  @Test
  public void testHandleProceedingError() {
    Throwable throwable = new RuntimeException("Error");
    String proceedingCode = "PROC1";

    Mono<ProceedingDetail> result =
        ebsApiClientErrorHandler.handleProceedingError(proceedingCode, throwable);

    StepVerifier.create(result)
        .verifyErrorMatches(e -> e instanceof EbsApiClientException
            && e.getMessage().equals("Failed to retrieve Proceeding: (code: PROC1)")
            && e.getCause() == throwable);
  }

  @Test
  public void testHandleScopeLimitationsError() {
    Throwable throwable = new RuntimeException("Error");
    ScopeLimitationDetail scopeLimitationDetail = new ScopeLimitationDetail();

    Mono<ScopeLimitationDetails> result =
        ebsApiClientErrorHandler.handleScopeLimitationsError(scopeLimitationDetail, throwable);

    StepVerifier.create(result)
        .verifyErrorMatches(e -> e instanceof EbsApiClientException
            && e.getMessage().equals(
                String.format(SCOPE_LIMITATIONS_ERROR_MESSAGE, scopeLimitationDetail))
            && e.getCause() == throwable);
  }

  @Test
  public void testHandleOutcomeResultsError() {
    Throwable throwable = new RuntimeException("Error");
    String proceedingCode = "code1";
    String outcomeResult = "result1";

    Mono<OutcomeResultLookupDetail> result =
        ebsApiClientErrorHandler.handleOutcomeResultsError(
            proceedingCode, outcomeResult, throwable);

    StepVerifier.create(result)
        .verifyErrorMatches(e -> e instanceof EbsApiClientException
            && e.getMessage().equals(
            String.format(OUTCOME_RESULTS_ERROR_MESSAGE, proceedingCode, outcomeResult))
            && e.getCause() == throwable);
  }

  @Test
  public void testHandleStageEndError() {
    Throwable throwable = new RuntimeException("Error");
    String proceedingCode = "code1";
    String stageEnd = "end";

    Mono<StageEndLookupDetail> result =
        ebsApiClientErrorHandler.handleStageEndError(
            proceedingCode, stageEnd, throwable);

    StepVerifier.create(result)
        .verifyErrorMatches(e -> e instanceof EbsApiClientException
            && e.getMessage().equals(
            String.format(STAGE_END_ERROR_MESSAGE, proceedingCode, stageEnd))
            && e.getCause() == throwable);
  }

  @Test
  public void testHandlePriorAuthorityTypeError() {
    Throwable throwable = new RuntimeException("Error");
    String code = "code1";
    Boolean valueRequired = Boolean.TRUE;

    Mono<PriorAuthorityTypeDetails> result =
        ebsApiClientErrorHandler.handlePriorAuthorityTypeError(
            code, valueRequired, throwable);

    StepVerifier.create(result)
        .verifyErrorMatches(e -> e instanceof EbsApiClientException
            && e.getMessage().equals(
            String.format(PRIOR_AUTHORITY_TYPE_ERROR_MESSAGE, code, valueRequired))
            && e.getCause() == throwable);
  }

  @Test
  public void testHandleAwardTypeError() {
    Throwable throwable = new RuntimeException("Error");
    String code = "code1";
    String awardType = "type1";

    Mono<AwardTypeLookupDetail> result =
        ebsApiClientErrorHandler.handleAwardTypeError(
            code, awardType, throwable);

    StepVerifier.create(result)
        .verifyErrorMatches(e -> e instanceof EbsApiClientException
            && e.getMessage().equals(
            String.format(AWARD_TYPE_ERROR_MESSAGE, code, awardType))
            && e.getCause() == throwable);
  }

  @Test
  public void testHandleUsersError() {
    Throwable throwable = new RuntimeException("Error");
    String providerId = "prov1";

    Mono<UserDetails> result =
        ebsApiClientErrorHandler.handleUsersError(
            providerId, throwable);

    StepVerifier.create(result)
        .verifyErrorMatches(e -> e instanceof EbsApiClientException
            && e.getMessage().equals(
            String.format(USERS_ERROR_MESSAGE, providerId))
            && e.getCause() == throwable);
  }
}
