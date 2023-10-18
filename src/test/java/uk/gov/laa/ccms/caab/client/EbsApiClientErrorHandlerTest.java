package uk.gov.laa.ccms.caab.client;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.ProceedingDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

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
}
