package uk.gov.laa.ccms.caab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.laa.ccms.caab.constants.ClientActionConstants.ACTION_VIEW;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_LANGUAGE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_METHOD;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DISABILITY;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ETHNIC_ORIGIN;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_GENDER;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_MARITAL_STATUS;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataContactDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataMonitoringDetails;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupDetail;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

@ExtendWith(MockitoExtension.class)
public class LookupServiceTest {
  @Mock
  private EbsApiClient ebsApiClient;

  @InjectMocks
  private LookupService lookupService;

  @Test
  public void getCountries_returnsData() {
    final CommonLookupValueDetail commonValue = new CommonLookupValueDetail().code("GBR");
    final CommonLookupDetail commonValues = new CommonLookupDetail().addContentItem(commonValue);

    when(ebsApiClient.getCountries()).thenReturn(Mono.just(commonValues));

    final Mono<CommonLookupDetail> commonLookupDetailMono = lookupService.getCountries();
    StepVerifier.create(commonLookupDetailMono)
        .expectNext(commonValues)
        .verifyComplete();
  }

  @Test
  public void getCountry_returnsData() {
    final CommonLookupValueDetail commonValue = new CommonLookupValueDetail().code("GBR");
    final CommonLookupDetail commonValues = new CommonLookupDetail().addContentItem(commonValue);

    when(ebsApiClient.getCountries()).thenReturn(Mono.just(commonValues));

    final Mono<Optional<CommonLookupValueDetail>> commonLookupDetailMono =
        lookupService.getCountry("GBR");
    StepVerifier.create(commonLookupDetailMono)
        .expectNextMatches(result -> "GBR".equals(result.get().getCode()))
        .verifyComplete();
  }

  @Test
  public void getCategoriesOfLaw_returnsData() {
    final CategoryOfLawLookupValueDetail commonValue =
        new CategoryOfLawLookupValueDetail().code("CAT1");
    final CategoryOfLawLookupDetail commonValues =
        new CategoryOfLawLookupDetail().addContentItem(commonValue);

    when(ebsApiClient.getCategoriesOfLaw(null, null, null))
        .thenReturn(Mono.just(commonValues));

    final Mono<CategoryOfLawLookupDetail> commonLookupDetailMono =
        lookupService.getCategoriesOfLaw();
    StepVerifier.create(commonLookupDetailMono)
        .expectNextMatches(result -> result == commonValues)
        .verifyComplete();
  }

  @Test
  public void getCategoryOfLaw_returnsData() {
    final CategoryOfLawLookupValueDetail commonValue = new CategoryOfLawLookupValueDetail()
        .code("CAT1")
        .matterTypeDescription("DESC")
        .copyCostLimit(Boolean.TRUE);
    final CategoryOfLawLookupDetail commonValues =
        new CategoryOfLawLookupDetail().addContentItem(commonValue);

    when(ebsApiClient.getCategoriesOfLaw(commonValue.getCode(),
        null,
        null))
        .thenReturn(Mono.just(commonValues));

    final Mono<Optional<CategoryOfLawLookupValueDetail>> commonLookupDetailMono =
        lookupService.getCategoryOfLaw(commonValue.getCode());
    StepVerifier.create(commonLookupDetailMono)
        .expectNextMatches(result -> result.get() == commonValue)
        .verifyComplete();
  }

  @Test
  @DisplayName("Test getClientLookups method")
  void testGetClientLookups() {
    final String TITLE = "Mr.";
    final String COUNTRY_OF_ORIGIN = "UK";
    final String MARITAL_STATUS = "Single";
    final String GENDER = "Male";
    final String CORRESPONDENCE_METHOD = "Email";
    final String ETHNIC_ORIGIN = "Asian";
    final String DISABILITY = "None";
    final String COUNTRY = "USA";
    final String CORRESPONDENCE_LANGUAGE = "English";

    final ClientFlowFormData clientFlowFormData = new ClientFlowFormData(ACTION_VIEW);
    final ClientFormDataBasicDetails basicDetails = new ClientFormDataBasicDetails();
    basicDetails.setTitle(TITLE);
    basicDetails.setCountryOfOrigin(COUNTRY_OF_ORIGIN);
    basicDetails.setMaritalStatus(MARITAL_STATUS);
    basicDetails.setGender(GENDER);
    clientFlowFormData.setBasicDetails(basicDetails);

    final ClientFormDataContactDetails contactDetails = new ClientFormDataContactDetails();
    contactDetails.setCorrespondenceMethod(CORRESPONDENCE_METHOD);
    contactDetails.setCorrespondenceLanguage(CORRESPONDENCE_LANGUAGE);
    clientFlowFormData.setContactDetails(contactDetails);

    final ClientFormDataMonitoringDetails monitoringDetails = new ClientFormDataMonitoringDetails();
    monitoringDetails.setEthnicOrigin(ETHNIC_ORIGIN);
    monitoringDetails.setDisability(DISABILITY);
    clientFlowFormData.setMonitoringDetails(monitoringDetails);

    final ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();
    addressDetails.setCountry(COUNTRY);
    clientFlowFormData.setAddressDetails(addressDetails);

    final CommonLookupValueDetail titleLookupValueDetail = new CommonLookupValueDetail();
    final CommonLookupValueDetail countryLookupValueDetail = new CommonLookupValueDetail();
    final CommonLookupValueDetail maritalStatusLookupValueDetail = new CommonLookupValueDetail();
    final CommonLookupValueDetail genderLookupValueDetail = new CommonLookupValueDetail();
    final CommonLookupValueDetail correspondenceMethodLookupValueDetail = new CommonLookupValueDetail();
    final CommonLookupValueDetail ethnicityLookupValueDetail = new CommonLookupValueDetail();
    final CommonLookupValueDetail disabilityLookupValueDetail = new CommonLookupValueDetail();
    final CommonLookupValueDetail correspondenceLanguageLookupValueDetail = new CommonLookupValueDetail();

    final CommonLookupDetail commonLookupDetailWithCountry = new CommonLookupDetail();
    commonLookupDetailWithCountry.setContent(List.of(countryLookupValueDetail));

    final CommonLookupDetail commonLookupDetailWithValues = new CommonLookupDetail();
    commonLookupDetailWithValues.setContent(List.of(
        titleLookupValueDetail,
        maritalStatusLookupValueDetail,
        genderLookupValueDetail,
        correspondenceMethodLookupValueDetail,
        ethnicityLookupValueDetail,
        disabilityLookupValueDetail,
        correspondenceLanguageLookupValueDetail
    ));

    when(ebsApiClient.getCommonValues(COMMON_VALUE_CONTACT_TITLE, TITLE)).thenReturn(Mono.just(commonLookupDetailWithCountry));
    when(ebsApiClient.getCommonValues(COMMON_VALUE_MARITAL_STATUS, MARITAL_STATUS)).thenReturn(Mono.just(commonLookupDetailWithCountry));
    when(ebsApiClient.getCommonValues(COMMON_VALUE_GENDER, GENDER)).thenReturn(Mono.just(commonLookupDetailWithCountry));
    when(ebsApiClient.getCommonValues(COMMON_VALUE_CORRESPONDENCE_METHOD, CORRESPONDENCE_METHOD)).thenReturn(Mono.just(commonLookupDetailWithCountry));
    when(ebsApiClient.getCommonValues(COMMON_VALUE_ETHNIC_ORIGIN, ETHNIC_ORIGIN)).thenReturn(Mono.just(commonLookupDetailWithCountry));
    when(ebsApiClient.getCommonValues(COMMON_VALUE_DISABILITY, DISABILITY)).thenReturn(Mono.just(commonLookupDetailWithCountry));
    when(ebsApiClient.getCommonValues(COMMON_VALUE_CORRESPONDENCE_LANGUAGE, CORRESPONDENCE_LANGUAGE)).thenReturn(Mono.just(commonLookupDetailWithCountry));

    when(ebsApiClient.getCountries()).thenReturn(Mono.just(commonLookupDetailWithCountry));

    List<Pair<String, Mono<Optional<CommonLookupValueDetail>>>> lookups =
        lookupService.getClientLookups(clientFlowFormData);

    assertEquals(9, lookups.size());

    assertEquals("contactTitle", lookups.get(0).getLeft());
    assertEquals(titleLookupValueDetail, lookups.get(0).getRight().block().get());

    assertEquals("countryOfOrigin", lookups.get(1).getLeft());

    assertEquals("maritalStatus", lookups.get(2).getLeft());
    assertEquals(maritalStatusLookupValueDetail, lookups.get(2).getRight().block().get());

    assertEquals("gender", lookups.get(3).getLeft());
    assertEquals(genderLookupValueDetail, lookups.get(3).getRight().block().get());

    assertEquals("correspondenceMethod", lookups.get(4).getLeft());
    assertEquals(correspondenceMethodLookupValueDetail, lookups.get(4).getRight().block().get());

    assertEquals("ethnicity", lookups.get(5).getLeft());
    assertEquals(ethnicityLookupValueDetail, lookups.get(5).getRight().block().get());

    assertEquals("disability", lookups.get(6).getLeft());
    assertEquals(disabilityLookupValueDetail, lookups.get(6).getRight().block().get());

    assertEquals("country", lookups.get(7).getLeft());

    assertEquals("correspondenceLanguage", lookups.get(8).getLeft());
    assertEquals(correspondenceLanguageLookupValueDetail, lookups.get(8).getRight().block().get());
  }

}