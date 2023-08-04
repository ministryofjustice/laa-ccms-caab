package uk.gov.laa.ccms.caab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.AbstractIntegrationTest;
import uk.gov.laa.ccms.data.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.EXCLUDED_APPLICATION_TYPE_CODES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_TYPE;
import static uk.gov.laa.ccms.caab.service.DataServiceErrorHandler.USER_ERROR_MESSAGE;

public class DataServiceIntegrationTest extends AbstractIntegrationTest {

    @RegisterExtension
    protected static WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    public static void properties(DynamicPropertyRegistry registry) {
        registry.add("laa.ccms.data-api.port", wiremock::getPort);
    }

    @Autowired
    private DataService dataService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testGetUser_returnData() throws Exception{
        UserDetail expectedUserDetail = buildUserDetail();
        String userJson = objectMapper.writeValueAsString(expectedUserDetail);

        wiremock.stubFor(get(String.format("/users/%s", expectedUserDetail.getLoginId()))
            .willReturn(okJson(userJson)));

        Mono<UserDetail> userDetailsMono = dataService.getUser(expectedUserDetail.getLoginId());

        UserDetail userDetails = userDetailsMono.block();

        assertEquals(userJson, objectMapper.writeValueAsString(userDetails));
    }

    @Test
    public void testGetUser_notFound() {
        String loginId = "user1";
        String expectedMessage = String.format(USER_ERROR_MESSAGE, loginId);

        wiremock.stubFor(get(String.format("/users/%s", loginId))
            .willReturn(notFound()));

        Mono<UserDetail> userDetailsMono = dataService.getUser(loginId);

        StepVerifier.create(userDetailsMono)
                .expectErrorMatches(throwable -> throwable instanceof DataServiceException &&
                        throwable.getMessage().equals(expectedMessage)
                ).verify();
     }

    @Test
    public void testGetCommonValues_returnData() throws Exception {
        CommonLookupDetail expectedCommonValues = buildCommonLookupDetail();
        String commonValuesJson = objectMapper.writeValueAsString(expectedCommonValues);

        String type = "testType";
        String code = "testCode";
        String sort = "testSort";

        wiremock.stubFor(get(urlPathMatching("/common-lookup-values.*"))
                .withQueryParam("type", equalTo(type))
                .withQueryParam("code", equalTo(code))
                .withQueryParam("sort", equalTo(sort))
                .willReturn(okJson(commonValuesJson)));

        Mono<CommonLookupDetail> commonValuesMono = dataService.getCommonValues(type, code, sort);

        CommonLookupDetail commonValues = commonValuesMono.block();

        assertEquals(commonValuesJson, objectMapper.writeValueAsString(commonValues));
    }

    @Test
    public void testGetApplicationTypes() throws Exception {
        CommonLookupDetail allApplicationTypes = buildCommonLookupDetail();
        String applicationTypesJson = objectMapper.writeValueAsString(allApplicationTypes);

        wiremock.stubFor(get(urlPathEqualTo("/common-lookup-values"))
                .withQueryParam("type", equalTo(COMMON_VALUE_APPLICATION_TYPE))
                .willReturn(okJson(applicationTypesJson)));

        List<CommonLookupValueDetail> applicationTypes = dataService.getApplicationTypes();

        List<CommonLookupValueDetail> expectedApplicationTypes = allApplicationTypes.getContent().stream()
                .filter(applicationType -> !EXCLUDED_APPLICATION_TYPE_CODES.contains(applicationType.getCode().toUpperCase()))
                .collect(Collectors.toList());

        assertEquals(expectedApplicationTypes, applicationTypes);
    }

    @Test
    public void testGetFeeEarners() throws Exception {
        FeeEarnerDetail feeEarners = buildFeeEarnerDetail();
        String feeEarnersJson = objectMapper.writeValueAsString(feeEarners);

        wiremock.stubFor(get(urlPathEqualTo("/fee-earners"))
            .withQueryParam("provider-id", equalTo("1"))
            .willReturn(okJson(feeEarnersJson)));

        FeeEarnerDetail result = dataService.getFeeEarners(1).block();

        assertNotNull(result);
        assertEquals(feeEarnersJson, objectMapper.writeValueAsString(result));
    }



    private CommonLookupDetail buildCommonLookupDetail() {
        CommonLookupDetail commonLookupValueListDetails = new CommonLookupDetail();
        commonLookupValueListDetails.setContent(new ArrayList<>());

        // Add details not in the excluded list
        commonLookupValueListDetails.getContent().add(new CommonLookupValueDetail().code("CODE1").description("Description 1"));
        commonLookupValueListDetails.getContent().add(new CommonLookupValueDetail().code("CODE2").description("Description 2"));

        // Add details in the excluded list
        commonLookupValueListDetails.getContent().add(new CommonLookupValueDetail().code("DP").description("Description DP"));
        commonLookupValueListDetails.getContent().add(new CommonLookupValueDetail().code("ECF").description("Description ECF"));

        return commonLookupValueListDetails;
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
