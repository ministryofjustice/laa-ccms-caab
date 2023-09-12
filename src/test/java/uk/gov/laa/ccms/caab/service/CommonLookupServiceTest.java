package uk.gov.laa.ccms.caab.service;

import static org.mockito.Mockito.when;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_TYPE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CATEGORY_OF_LAW;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_GENDER;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_MARITAL_STATUS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_UNIQUE_IDENTIFIER_TYPE;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

@ExtendWith(MockitoExtension.class)
public class CommonLookupServiceTest {
  @Mock
  private EbsApiClient ebsApiClient;

  @InjectMocks
  private CommonLookupService commonLookupService;

  @Test
  public void getApplicationTypes_returnsData() {
    CommonLookupDetail commonValues = new CommonLookupDetail();

    when(ebsApiClient.getCommonValues(COMMON_VALUE_APPLICATION_TYPE)).thenReturn(Mono.just(commonValues));

    Mono<CommonLookupDetail> commonLookupDetailMono = commonLookupService.getApplicationTypes();
    StepVerifier.create(commonLookupDetailMono)
        .expectNext(commonValues)
        .verifyComplete();
  }

  @Test
  public void getCategoriesOfLaw_returnsData() {
    CommonLookupDetail commonValues = new CommonLookupDetail();

    when(ebsApiClient.getCommonValues(COMMON_VALUE_CATEGORY_OF_LAW)).thenReturn(Mono.just(commonValues));

    Mono<CommonLookupDetail> commonLookupDetailMono = commonLookupService.getCategoriesOfLaw();
    StepVerifier.create(commonLookupDetailMono)
        .expectNext(commonValues)
        .verifyComplete();
  }

  @Test
  public void getGenders_returnsData() {
    CommonLookupDetail commonValues = new CommonLookupDetail();

    when(ebsApiClient.getCommonValues(COMMON_VALUE_GENDER)).thenReturn(Mono.just(commonValues));

    Mono<CommonLookupDetail> commonLookupDetailMono = commonLookupService.getGenders();
    StepVerifier.create(commonLookupDetailMono)
        .expectNext(commonValues)
        .verifyComplete();
  }

  @Test
  public void getUniqueIdentifierTypes_returnsData() {
    CommonLookupDetail commonValues = new CommonLookupDetail();

    when(ebsApiClient.getCommonValues(COMMON_VALUE_UNIQUE_IDENTIFIER_TYPE)).thenReturn(Mono.just(commonValues));

    Mono<CommonLookupDetail> commonLookupDetailMono = commonLookupService.getUniqueIdentifierTypes();
    StepVerifier.create(commonLookupDetailMono)
        .expectNext(commonValues)
        .verifyComplete();
  }

  @Test
  public void getContactTitles_returnsData() {
    CommonLookupDetail commonValues = new CommonLookupDetail();

    when(ebsApiClient.getCommonValues(COMMON_VALUE_CONTACT_TITLE)).thenReturn(Mono.just(commonValues));

    Mono<CommonLookupDetail> commonLookupDetailMono = commonLookupService.getContactTitles();
    StepVerifier.create(commonLookupDetailMono)
        .expectNext(commonValues)
        .verifyComplete();
  }

  @Test
  public void getMaritalStatuses_returnsData() {
    CommonLookupDetail commonValues = new CommonLookupDetail();

    when(ebsApiClient.getCommonValues(COMMON_VALUE_MARITAL_STATUS)).thenReturn(Mono.just(commonValues));

    Mono<CommonLookupDetail> commonLookupDetailMono = commonLookupService.getMaritalStatuses();
    StepVerifier.create(commonLookupDetailMono)
        .expectNext(commonValues)
        .verifyComplete();
  }

  @Test
  public void getCountries_returnsData() {
    CommonLookupValueDetail commonValue = new CommonLookupValueDetail().code("GBR");
    CommonLookupDetail commonValues = new CommonLookupDetail().addContentItem(commonValue);

    when(ebsApiClient.getCountries()).thenReturn(Mono.just(commonValues));

    Mono<CommonLookupDetail> commonLookupDetailMono = commonLookupService.getCountries();
    StepVerifier.create(commonLookupDetailMono)
        .expectNext(commonValues)
        .verifyComplete();
  }
}
