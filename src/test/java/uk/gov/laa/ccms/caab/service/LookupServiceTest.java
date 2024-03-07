package uk.gov.laa.ccms.caab.service;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
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

    final Mono<CommonLookupValueDetail> commonLookupDetailMono = lookupService.getCountry("GBR");
    StepVerifier.create(commonLookupDetailMono)
        .expectNextMatches(result -> "GBR".equals(result.getCode()))
        .verifyComplete();
  }

  @Test
  public void getCategoriesOfLaw_returnsData() {
    final CategoryOfLawLookupValueDetail commonValue = new CategoryOfLawLookupValueDetail().code("CAT1");
    final CategoryOfLawLookupDetail commonValues =
        new CategoryOfLawLookupDetail().addContentItem(commonValue);

    when(ebsApiClient.getCategoriesOfLaw(null, null, null))
        .thenReturn(Mono.just(commonValues));

    final Mono<CategoryOfLawLookupDetail> commonLookupDetailMono = lookupService.getCategoriesOfLaw();
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

    final Mono<CategoryOfLawLookupValueDetail> commonLookupDetailMono =
        lookupService.getCategoryOfLaw(commonValue.getCode());
    StepVerifier.create(commonLookupDetailMono)
        .expectNextMatches(result -> result == commonValue)
        .verifyComplete();
  }

}
