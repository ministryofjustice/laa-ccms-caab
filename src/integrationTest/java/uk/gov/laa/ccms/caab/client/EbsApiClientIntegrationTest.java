package uk.gov.laa.ccms.caab.client;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.AbstractIntegrationTest;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.BaseOffice;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.BaseUser;
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.NotificationSummary;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.data.model.UserDetails;

public class EbsApiClientIntegrationTest extends AbstractIntegrationTest {

  @RegisterExtension
  protected static WireMockExtension wiremock = WireMockExtension.newInstance()
      .options(wireMockConfig().dynamicPort())
      .build();

  @DynamicPropertySource
  public static void properties(final DynamicPropertyRegistry registry) {
    registry.add("laa.ccms.ebs-api.port", wiremock::getPort);
  }

  @Autowired
  private EbsApiClient ebsApiClient;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final String USER_ERROR_MESSAGE = "Failed to retrieve User with login id: %s";

  @Test
  public void testGetUser_returnData() throws Exception {
    final UserDetail expectedUserDetail = buildUserDetail();
    final String userJson = objectMapper.writeValueAsString(expectedUserDetail);

    wiremock.stubFor(get(String.format("/users/%s", expectedUserDetail.getLoginId()))
        .willReturn(okJson(userJson)));

    final Mono<UserDetail> userDetailsMono = ebsApiClient.getUser(expectedUserDetail.getLoginId());

    final UserDetail userDetails = userDetailsMono.block();

    assertEquals(userJson, objectMapper.writeValueAsString(userDetails));
  }

  @Test
  public void testGetUser_notFound() {
    final String loginId = "user1";
    final String expectedMessage = String.format(USER_ERROR_MESSAGE, loginId);

    wiremock.stubFor(get(String.format("/users/%s", loginId))
        .willReturn(notFound()));

    final Mono<UserDetail> userDetailsMono = ebsApiClient.getUser(loginId);

    StepVerifier.create(userDetailsMono)
        .expectErrorMatches(throwable -> throwable instanceof EbsApiClientException
            && throwable.getMessage().equals(expectedMessage)
        ).verify();
  }

  @Test
  public void testGetCommonValues_returnData() throws Exception {
    final CommonLookupDetail expectedCommonValues = buildCommonLookupDetail();
    final String commonValuesJson = objectMapper.writeValueAsString(expectedCommonValues);

    final String type = "testType";
    final String code = "testCode";
    final String descr = "testDescr";
    final String sort = "testSort";

    wiremock.stubFor(get(urlPathMatching("/lookup/common.*"))
        .withQueryParam("type", equalTo(type))
        .withQueryParam("code", equalTo(code))
        .withQueryParam("description", equalTo(descr))
        .withQueryParam("sort", equalTo(sort))
        .willReturn(okJson(commonValuesJson)));

    final Mono<CommonLookupDetail> commonValuesMono = ebsApiClient.getCommonValues(type, code, descr, sort);

    final CommonLookupDetail commonValues = commonValuesMono.block();

    assertEquals(commonValuesJson, objectMapper.writeValueAsString(commonValues));
  }

  @Test
  public void testGetCaseStatusValues_returnData() throws Exception {
    final String caseStatusValuesJson = objectMapper.writeValueAsString(buildCaseStatusLookupDetail());

    final Boolean copyAllowed = true;

    wiremock.stubFor(get(urlPathMatching("/lookup/case-status.*"))
        .withQueryParam("copy-allowed", equalTo(copyAllowed.toString()))
        .willReturn(okJson(caseStatusValuesJson)));

    final Mono<CaseStatusLookupDetail> lookupDetailMono = ebsApiClient.getCaseStatusValues(copyAllowed);

    final CaseStatusLookupDetail response = lookupDetailMono.block();

    assertEquals(caseStatusValuesJson, objectMapper.writeValueAsString(response));
  }

  @Test
  public void testGetProvider() throws Exception {
    final ProviderDetail provider = buildProviderDetail();
    final String providerJson = objectMapper.writeValueAsString(provider);

    wiremock.stubFor(get(String.format("/providers/%s", provider.getId()))
        .willReturn(okJson(providerJson)));

    final ProviderDetail result = ebsApiClient.getProvider(provider.getId()).block();

    assertNotNull(result);
    assertEquals(providerJson, objectMapper.writeValueAsString(result));
  }

  @Test
  public void testGetAmendmentTypes_returnData() throws Exception {
    final AmendmentTypeLookupDetail expectedAmendmentTypes = buildAmendmentTypeLookupDetail();
    final String amendmentTypesJson = objectMapper.writeValueAsString(expectedAmendmentTypes);
    final String applicationType = "testApplicationType";

    wiremock.stubFor(get(urlPathEqualTo("/lookup/amendment-types"))
        .withQueryParam("application-type", equalTo(applicationType))
        .willReturn(okJson(amendmentTypesJson)));

    final Mono<AmendmentTypeLookupDetail> amendmentTypesMono =
        ebsApiClient.getAmendmentTypes(applicationType);

    final AmendmentTypeLookupDetail amendmentTypes = amendmentTypesMono.block();

    assertEquals(amendmentTypesJson, objectMapper.writeValueAsString(amendmentTypes));
  }

  @Test
  void testGetUsers_returnsData() throws JsonProcessingException {
    final String username = "user1";
    final Integer providerId = 123;
    final BaseUser user = new BaseUser()
        .username(username)
        .userId(123);
    final UserDetails userDetails = new UserDetails()
        .addContentItem(user);

    final String userDetailsJson = objectMapper.writeValueAsString(userDetails);
    wiremock.stubFor(get(String.format("/users?size=1000&provider-id=%s", providerId))
        .willReturn(okJson(userDetailsJson)));
    final UserDetails result = ebsApiClient.getUsers(providerId).block();

    assertNotNull(result);
    assertEquals(userDetailsJson, objectMapper.writeValueAsString(result));

  }

  @Test
  public void testGetUsers_notFound() {
    final Integer providerId = 123;
    final String expectedMessage = "Failed to retrieve Users with parameters: size=1000, provider-id=123";
    wiremock.stubFor(get(String.format("/users?size=1000&provider-id=%s", providerId))
        .willReturn(notFound()));
    final Mono<UserDetails> userDetailsMono = ebsApiClient.getUsers(providerId);

    StepVerifier.create(userDetailsMono)
        .expectErrorMatches(throwable -> throwable instanceof EbsApiClientException
            && throwable.getMessage().equals(expectedMessage)
        ).verify();
  }


  @Test
  public void testGetUserNotificationSummary_returnData() throws Exception {
    final String loginId = "user1";
    final NotificationSummary expectedNotificationsummary = buildUserNotificationSummary();
    final String notificationSummaryJson = objectMapper.writeValueAsString(
        expectedNotificationsummary);

    wiremock.stubFor(get(String.format("/users/%s/notifications/summary", loginId))
        .willReturn(okJson(notificationSummaryJson)));

    final Mono<NotificationSummary> userNotificationSummary = ebsApiClient.getUserNotificationSummary(
        loginId);

    final NotificationSummary userDetails = userNotificationSummary.block();

    assertEquals(notificationSummaryJson, objectMapper.writeValueAsString(userDetails));
  }

  @Test
  public void testGetUserNotificationSummary_notFound() {
    final String loginId = "user1";
    final String expectedMessage = String.format(USER_ERROR_MESSAGE, loginId);

    wiremock.stubFor(get(String.format("/users/%s/notifications/summary", loginId))
        .willReturn(notFound()));

    final Mono<NotificationSummary> notificationSummary = ebsApiClient.getUserNotificationSummary(
        loginId);

    StepVerifier.create(notificationSummary)
        .expectErrorMatches(throwable -> throwable instanceof EbsApiClientException
            && throwable.getMessage().equals(expectedMessage)
        ).verify();
  }

  // You may need to build the AmendmentTypeLookupDetail for the test
  private AmendmentTypeLookupDetail buildAmendmentTypeLookupDetail() {
    final AmendmentTypeLookupDetail detail = new AmendmentTypeLookupDetail();
    detail.setContent(new ArrayList<>());

    detail.getContent().add(new AmendmentTypeLookupValueDetail()
        .applicationTypeCode("DP")
        .applicationTypeDescription("Del. Functions")
        .costLimitCap("1350.00")
        .devolvedPowersIndicator("Y")
        .defaultLarScopeFlag("Y"));

    detail.getContent().add(new AmendmentTypeLookupValueDetail()
        .applicationTypeCode("ECF")
        .applicationTypeDescription("ECF")
        .defaultLarScopeFlag("Y"));

    return detail;
  }

  private CaseStatusLookupDetail buildCaseStatusLookupDetail() {
    return new CaseStatusLookupDetail()
        .addContentItem(
            new CaseStatusLookupValueDetail().code("CODE1").description("Description 1")
                .copyAllowed(Boolean.FALSE))
        .addContentItem(
            new CaseStatusLookupValueDetail().code("CODE2").description("Description 2")
                .copyAllowed(Boolean.TRUE));
  }

  private CommonLookupDetail buildCommonLookupDetail() {
    return new CommonLookupDetail()
        .addContentItem(new CommonLookupValueDetail().code("CODE1").description("Description 1"))
        .addContentItem(new CommonLookupValueDetail().code("CODE2").description("Description 2"))
        // Add details in the excluded list
        .addContentItem(new CommonLookupValueDetail().code("DP").description("Description DP"))
        .addContentItem(new CommonLookupValueDetail().code("ECF").description("Description ECF"));
  }

  private UserDetail buildUserDetail() {
    return new UserDetail()
        .userId(1)
        .userType("testUserType")
        .loginId("user1")
        .addFirmsItem(
            new BaseProvider()
                .id(1)
                .name("testProvider"))
        .provider(new BaseProvider()
            .id(2)
            .addOfficesItem(new BaseOffice()
                .id(1)
                .name("Office 1")))
        .addFunctionsItem("testFunction");
  }

  private ProviderDetail buildProviderDetail() {
    return new ProviderDetail()
        .id(123)
        .name("provider1")
        .addOfficesItem(new OfficeDetail()
            .id(10)
            .name("Office 1")
            .addFeeEarnersItem(new ContactDetail()
                .id(1)
                .name("FeeEarner1")));
  }

  private NotificationSummary buildUserNotificationSummary() {
    return new NotificationSummary().notifications(5).overdueActions(3).standardActions(7);
  }

}
