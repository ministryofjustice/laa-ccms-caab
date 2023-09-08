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
import uk.gov.laa.ccms.data.model.CaseStatusLookupDetail;
import uk.gov.laa.ccms.data.model.CaseStatusLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.FeeEarnerDetail;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

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
  public void testGetFeeEarners() throws Exception {
    FeeEarnerDetail feeEarners = buildFeeEarnerDetail();
    String feeEarnersJson = objectMapper.writeValueAsString(feeEarners);

    wiremock.stubFor(get(urlPathEqualTo("/fee-earners"))
        .withQueryParam("provider-id", equalTo("1"))
        .willReturn(okJson(feeEarnersJson)));

    FeeEarnerDetail result = ebsApiClient.getFeeEarners(1).block();

    assertNotNull(result);
    assertEquals(feeEarnersJson, objectMapper.writeValueAsString(result));
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
            new ProviderDetail()
                .id(1)
                .name("testProvider"))
        .provider(new ProviderDetail()
            .id(2)
            .addOfficesItem(new OfficeDetail()
                .id(1)
                .name("Office 1")))
        .addFunctionsItem("testFunction");
  }

  private FeeEarnerDetail buildFeeEarnerDetail() {
    return new FeeEarnerDetail()
        .addContentItem(new ContactDetail().id(1).name("feeEarner1"))
        .addContentItem(new ContactDetail().id(2).name("feeEarner2"));
  }

}