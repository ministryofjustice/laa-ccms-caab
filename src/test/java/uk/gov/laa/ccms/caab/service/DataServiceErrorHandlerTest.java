package uk.gov.laa.ccms.caab.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.FeeEarnerDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
public class DataServiceErrorHandlerTest {

    @InjectMocks
    private DataServiceErrorHandler dataServiceErrorHandler;

    @BeforeEach
    public void setUp() {
        dataServiceErrorHandler = new DataServiceErrorHandler();
    }

    @Test
    public void testHandleUserError() {
        Throwable throwable = new RuntimeException("Error");

        Mono<UserDetail> result = dataServiceErrorHandler.handleUserError("testLoginId", throwable);

        StepVerifier.create(result)
                .verifyErrorMatches(e -> e instanceof DataServiceException
                        && e.getMessage().equals("Failed to retrieve User with loginId: testLoginId")
                        && e.getCause() == throwable);
    }

    @Test
    public void testHandleCommonValuesError() {
        Throwable throwable = new RuntimeException("Error");

        Mono<CommonLookupDetail> result = dataServiceErrorHandler.handleCommonValuesError("testType", "testCode", "testSort", throwable);

        StepVerifier.create(result)
                .verifyErrorMatches(e -> e instanceof DataServiceException
                        && e.getMessage().equals("Failed to retrieve Common Values: (type: testType, code: testCode, sort: testSort)")
                        && e.getCause() == throwable);
    }

    @Test
    public void testHandleCaseStatusValuesError() {
        Throwable throwable = new RuntimeException("Error");

        Mono<CaseStatusLookupDetail> result = dataServiceErrorHandler.handleCaseStatusValuesError(true, throwable);

        StepVerifier.create(result)
            .verifyErrorMatches(e -> e instanceof DataServiceException
                && e.getMessage().equals("Failed to retrieve Case Status Values: (copyAllowed: true)")
                && e.getCause() == throwable);
    }

    @Test
    public void testHandleFeeEarnersError() {
        Throwable throwable = new RuntimeException("Error");
        Integer providerId = 1234;

        Mono<FeeEarnerDetail> result = dataServiceErrorHandler.handleFeeEarnersError(providerId, throwable);

        StepVerifier.create(result)
                .verifyErrorMatches(e -> e instanceof DataServiceException
                        && e.getMessage().equals("Failed to retrieve Fee Earners: (providerId: 1234)")
                        && e.getCause() == throwable);
    }
}
