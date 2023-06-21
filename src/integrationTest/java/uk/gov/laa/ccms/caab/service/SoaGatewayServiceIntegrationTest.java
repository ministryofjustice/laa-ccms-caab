//package uk.gov.laa.ccms.caab.service;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//import uk.gov.laa.ccms.caab.AbstractIntegrationTest;
//import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;
//
//import static com.github.tomakehurst.wiremock.client.WireMock.*;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//public class SoaGatewayServiceIntegrationTest extends AbstractIntegrationTest {
//
//
//    @Autowired
//    private SoaGatewayService soaGatewayService;
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    @Test
//    public void testGetNotificationsSummary_returnData() throws Exception {
//        String loginId = "user1";
//        NotificationSummary expectedSummary = buildNotificationSummary();
//        String summaryJson = objectMapper.writeValueAsString(expectedSummary);
//
//        wiremock.stubFor(get(String.format("/users/%s/notifications/summary", loginId))
//                .withHeader("SoaGateway-User-Login-Id", equalTo(loginId))
//                .withHeader("SoaGateway-User-Role", equalTo("EXTERNAL"))
//                .willReturn(okJson(summaryJson)));
//
//        Mono<NotificationSummary> summaryMono = soaGatewayService.getNotificationsSummary(loginId);
//
//        NotificationSummary summary = summaryMono.block();
//
//        assertEquals(summaryJson, objectMapper.writeValueAsString(summary));
//    }
//
//    @Test
//    public void testGetNotificationsSummary_notFound() {
//        String loginId = "user1";
//        String expectedMessage = String.format(NOTIFICATION_SUMMARY_ERROR_MESSAGE, loginId);
//
//        wiremock.stubFor(get(String.format("/users/%s/notifications/summary", loginId))
//                .withHeader("SoaGateway-User-Login-Id", equalTo(loginId))
//                .withHeader("SoaGateway-User-Role", equalTo("EXTERNAL"))
//                .willReturn(notFound()));
//
//        Mono<NotificationSummary> summaryMono = soaGatewayService.getNotificationsSummary(loginId);
//
//        StepVerifier.create(summaryMono)
//                .expectErrorMatches(throwable ->
//                        throwable instanceof SoaGatewayServiceException &&
//                                throwable.getMessage().equals(expectedMessage))
//                .verify();
//    }
//
//    private NotificationSummary buildNotificationSummary() {
//        return new NotificationSummary()
//                .notifications(10)
//                .standardActions(5)
//                .overdueActions(2);
//    }
//}
