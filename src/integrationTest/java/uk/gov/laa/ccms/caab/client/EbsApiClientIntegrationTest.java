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
import static uk.gov.laa.ccms.caab.client.EbsApiClientErrorHandler.USER_ERROR_MESSAGE;
import static uk.gov.laa.ccms.caab.client.EbsApiClientErrorHandler.USERS_ERROR_MESSAGE;

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
  public static void properties(DynamicPropertyRegistry registry) {
    registry.add("laa.ccms.ebs-api.port", wiremock::getPort);
  }

  @Autowired
  private EbsApiClient ebsApiClient;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testGetUser_returnData() throws Exception {
    UserDetail expectedUserDetail = buildUserDetail();
    String userJson = objectMapper.writeValueAsString(expectedUserDetail);

    wiremock.stubFor(get(String.format("/users/%s", expectedUserDetail.getLoginId()))
        .willReturn(okJson(userJson)));

    Mono<UserDetail> userDetailsMono = ebsApiClient.getUser(expectedUserDetail.getLoginId());

    UserDetail userDetails = userDetailsMono.block();

    assertEquals(userJson, objectMapper.writeValueAsString(userDetails));
  }

  @Test
  public void testGetUser_notFound() {
    String loginId = "user1";
    String expectedMessage = String.format(USER_ERROR_MESSAGE, loginId);

    wiremock.stubFor(get(String.format("/users/%s", loginId))
        .willReturn(notFound()));

    Mono<UserDetail> userDetailsMono = ebsApiClient.getUser(loginId);

    StepVerifier.create(userDetailsMono)
        .expectErrorMatches(throwable -> throwable instanceof EbsApiClientException
            && throwable.getMessage().equals(expectedMessage)
        ).verify();
  }

  @Test
  public void testGetCommonValues_returnData() throws Exception {
    CommonLookupDetail expectedCommonValues = buildCommonLookupDetail();
    String commonValuesJson = objectMapper.writeValueAsString(expectedCommonValues);

    String type = "testType";
    String code = "testCode";
    String sort = "testSort";

    wiremock.stubFor(get(urlPathMatching("/lookup/common.*"))
        .withQueryParam("type", equalTo(type))
        .withQueryParam("code", equalTo(code))
        .withQueryParam("sort", equalTo(sort))
        .willReturn(okJson(commonValuesJson)));

    Mono<CommonLookupDetail> commonValuesMono = ebsApiClient.getCommonValues(type, code, sort);

    CommonLookupDetail commonValues = commonValuesMono.block();

    assertEquals(commonValuesJson, objectMapper.writeValueAsString(commonValues));
  }

  @Test
  public void testGetCaseStatusValues_returnData() throws Exception {
    String caseStatusValuesJson = objectMapper.writeValueAsString(buildCaseStatusLookupDetail());

    Boolean copyAllowed = true;

    wiremock.stubFor(get(urlPathMatching("/lookup/case-status.*"))
        .withQueryParam("copy-allowed", equalTo(copyAllowed.toString()))
        .willReturn(okJson(caseStatusValuesJson)));

    Mono<CaseStatusLookupDetail> lookupDetailMono = ebsApiClient.getCaseStatusValues(copyAllowed);

    CaseStatusLookupDetail response = lookupDetailMono.block();

    assertEquals(caseStatusValuesJson, objectMapper.writeValueAsString(response));
  }

  @Test
  public void testGetProvider() throws Exception {
    ProviderDetail provider = buildProviderDetail();
    String providerJson = objectMapper.writeValueAsString(provider);

    wiremock.stubFor(get(String.format("/providers/%s", provider.getId()))
        .willReturn(okJson(providerJson)));

    ProviderDetail result = ebsApiClient.getProvider(provider.getId()).block();

    assertNotNull(result);
    assertEquals(providerJson, objectMapper.writeValueAsString(result));
  }

  @Test
  public void testGetAmendmentTypes_returnData() throws Exception {
    AmendmentTypeLookupDetail expectedAmendmentTypes = buildAmendmentTypeLookupDetail();
    String amendmentTypesJson = objectMapper.writeValueAsString(expectedAmendmentTypes);
    String applicationType = "testApplicationType";

    wiremock.stubFor(get(urlPathEqualTo("/lookup/amendment-types"))
        .withQueryParam("application-type", equalTo(applicationType))
        .willReturn(okJson(amendmentTypesJson)));

    Mono<AmendmentTypeLookupDetail> amendmentTypesMono =
        ebsApiClient.getAmendmentTypes(applicationType);

    AmendmentTypeLookupDetail amendmentTypes = amendmentTypesMono.block();

    assertEquals(amendmentTypesJson, objectMapper.writeValueAsString(amendmentTypes));
  }

  @Test
  void testGetUsers_returnsData() throws JsonProcessingException {
    String username = "user1";
    Integer providerId = 123;
    BaseUser user = new BaseUser()
        .username(username)
        .userId(123);
    UserDetails userDetails = new UserDetails()
        .addContentItem(user);

    String userDetailsJson = objectMapper.writeValueAsString(userDetails);
    wiremock.stubFor(get(String.format("/users?provider-id=%s", providerId))
        .willReturn(okJson(userDetailsJson)));
    UserDetails result = ebsApiClient.getUsers(providerId).block();

    assertNotNull(result);
    assertEquals(userDetailsJson, objectMapper.writeValueAsString(result));

  }
  @Test
  public void testGetUsers_notFound() {
    Integer providerId = 123;
    String expectedMessage = String.format(USERS_ERROR_MESSAGE, providerId);
    wiremock.stubFor(get(String.format("/users/provider-id=%s", providerId))
        .willReturn(notFound()));
    Mono<UserDetails> userDetailsMono = ebsApiClient.getUsers(providerId);

    StepVerifier.create(userDetailsMono)
        .expectErrorMatches(throwable -> throwable instanceof EbsApiClientException
            && throwable.getMessage().equals(expectedMessage)
        ).verify();
  }


  // You may need to build the AmendmentTypeLookupDetail for the test
  private AmendmentTypeLookupDetail buildAmendmentTypeLookupDetail() {
    AmendmentTypeLookupDetail detail = new AmendmentTypeLookupDetail();
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

}
