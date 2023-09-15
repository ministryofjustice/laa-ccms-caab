package uk.gov.laa.ccms.caab.service;

import static org.mockito.Mockito.when;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_TYPE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CATEGORY_OF_LAW;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_LANGUAGE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_METHOD;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DISABILITY;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ETHNIC_ORIGIN;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_GENDER;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_MARITAL_STATUS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_UNIQUE_IDENTIFIER_TYPE;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.client.EbsApiClient;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

@SuppressWarnings({"unchecked"})
@ExtendWith(MockitoExtension.class)
public class CommonLookupServiceTest {
  @Mock
  private EbsApiClient ebsApiClient;

  @InjectMocks
  private CommonLookupService commonLookupService;

  private static Stream<Arguments> getCommonValuesArguments() {
    return Stream.of(
        Arguments.of("getApplicationTypes", COMMON_VALUE_APPLICATION_TYPE),
        Arguments.of("getCategoriesOfLaw", COMMON_VALUE_CATEGORY_OF_LAW),
        Arguments.of("getGenders", COMMON_VALUE_GENDER),
        Arguments.of("getUniqueIdentifierTypes", COMMON_VALUE_UNIQUE_IDENTIFIER_TYPE),
        Arguments.of("getContactTitles", COMMON_VALUE_CONTACT_TITLE),
        Arguments.of("getMaritalStatuses", COMMON_VALUE_MARITAL_STATUS),
        Arguments.of("getCorrespondenceMethods", COMMON_VALUE_CORRESPONDENCE_METHOD),
        Arguments.of("getCorrespondenceLanguages", COMMON_VALUE_CORRESPONDENCE_LANGUAGE),
        Arguments.of("getEthnicOrigins", COMMON_VALUE_ETHNIC_ORIGIN),
        Arguments.of("getDisabilities", COMMON_VALUE_DISABILITY)
    );
  }

  @ParameterizedTest
  @MethodSource("getCommonValuesArguments")
  public void getCommonValues_returnsData(String methodCall, String constantValue)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

    // Retrieve the method with its argument types if any
    Method method = commonLookupService.getClass().getDeclaredMethod(methodCall);
    CommonLookupDetail commonValues = new CommonLookupDetail();

    // Mock the behavior
    when(ebsApiClient.getCommonValues(constantValue)).thenReturn(Mono.just(commonValues));

    // Invoke the method on the correct instance
    Mono<CommonLookupDetail> commonLookupDetailMono =
        (Mono<CommonLookupDetail>) method.invoke(commonLookupService);

    // Verify
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
