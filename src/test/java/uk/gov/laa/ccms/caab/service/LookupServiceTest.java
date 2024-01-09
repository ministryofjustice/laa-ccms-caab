package uk.gov.laa.ccms.caab.service;

import static org.mockito.Mockito.when;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_TYPE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CASE_ADDRESS_OPTION;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CLIENT_INVOLVEMENT_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_LANGUAGE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_METHOD;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DISABILITY;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ETHNIC_ORIGIN;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_GENDER;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_LEVEL_OF_SERVICE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_MARITAL_STATUS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_MATTER_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_NOTIFICATION_TYPE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_SCOPE_LIMITATIONS;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_UNIQUE_IDENTIFIER_TYPE;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
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
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupDetail;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

@SuppressWarnings({"unchecked"})
@ExtendWith(MockitoExtension.class)
public class LookupServiceTest {
  @Mock
  private EbsApiClient ebsApiClient;

  @InjectMocks
  private LookupService lookupService;

  private static Stream<Arguments> getCommonLookupArguments() {
    return Stream.of(
        Arguments.of("getApplicationTypes", COMMON_VALUE_APPLICATION_TYPE),
        Arguments.of("getGenders", COMMON_VALUE_GENDER),
        Arguments.of("getUniqueIdentifierTypes", COMMON_VALUE_UNIQUE_IDENTIFIER_TYPE),
        Arguments.of("getContactTitles", COMMON_VALUE_CONTACT_TITLE),
        Arguments.of("getMaritalStatuses", COMMON_VALUE_MARITAL_STATUS),
        Arguments.of("getCorrespondenceMethods", COMMON_VALUE_CORRESPONDENCE_METHOD),
        Arguments.of("getCorrespondenceLanguages", COMMON_VALUE_CORRESPONDENCE_LANGUAGE),
        Arguments.of("getCaseAddressOptions", COMMON_VALUE_CASE_ADDRESS_OPTION),
        Arguments.of("getEthnicOrigins", COMMON_VALUE_ETHNIC_ORIGIN),
        Arguments.of("getDisabilities", COMMON_VALUE_DISABILITY),
        Arguments.of("getNotificationTypes", COMMON_VALUE_NOTIFICATION_TYPE),
        Arguments.of("getLevelsOfService", COMMON_VALUE_LEVEL_OF_SERVICE),
        Arguments.of("getMatterTypes", COMMON_VALUE_MATTER_TYPES),
        Arguments.of("getClientInvolvementTypes", COMMON_VALUE_CLIENT_INVOLVEMENT_TYPES),
        Arguments.of("getScopeLimitations", COMMON_VALUE_SCOPE_LIMITATIONS)
    );
  }

  @ParameterizedTest
  @MethodSource("getCommonLookupArguments")
  public void getCommonValues_returnsData(String methodCall, String constantValue)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

    // Retrieve the method with its argument types if any
    Method method = lookupService.getClass().getDeclaredMethod(methodCall);
    CommonLookupDetail commonValues = new CommonLookupDetail();

    // Mock the behavior
    when(ebsApiClient.getCommonValues(constantValue)).thenReturn(Mono.just(commonValues));

    // Invoke the method on the correct instance
    Mono<CommonLookupDetail> commonLookupDetailMono =
        (Mono<CommonLookupDetail>) method.invoke(lookupService);

    // Verify
    StepVerifier.create(commonLookupDetailMono)
        .expectNext(commonValues)
        .verifyComplete();
  }

  private static Stream<Arguments> getCommonLookupValueArguments() {
    return Stream.of(
        Arguments.of("getGender", COMMON_VALUE_GENDER),
        Arguments.of("getContactTitle", COMMON_VALUE_CONTACT_TITLE),
        Arguments.of("getMaritalStatus", COMMON_VALUE_MARITAL_STATUS),
        Arguments.of("getCorrespondenceMethod", COMMON_VALUE_CORRESPONDENCE_METHOD),
        Arguments.of("getCorrespondenceLanguage", COMMON_VALUE_CORRESPONDENCE_LANGUAGE),
        Arguments.of("getEthnicOrigin", COMMON_VALUE_ETHNIC_ORIGIN),
        Arguments.of("getDisability", COMMON_VALUE_DISABILITY)
    );
  }

  @ParameterizedTest
  @MethodSource("getCommonLookupValueArguments")
  public void getCommonValue_returnsData(String methodCall, String constantValue)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

    String code = "CODE";

    // Prepare the expected value
    CommonLookupDetail commonValues = new CommonLookupDetail();
    CommonLookupValueDetail commonValue = new CommonLookupValueDetail()
        .code(code)
        .description("DESCRIPTION");

    // Add the commonValue to the commonValues object
    commonValues.setContent(Arrays.asList(commonValue));

    // Mock the behavior
    when(ebsApiClient.getCommonValues(constantValue, code)).thenReturn(Mono.just(commonValues));

    // Retrieve the method using reflection
    Method method = lookupService.getClass().getMethod(methodCall, String.class);

    // Invoke the method
    Mono<CommonLookupValueDetail> commonLookupDetailMono =
        (Mono<CommonLookupValueDetail>) method.invoke(lookupService, code);

    StepVerifier.create(commonLookupDetailMono)
        .expectNextMatches(result -> code.equals(result.getCode())
            && "DESCRIPTION".equals(result.getDescription()))
        .verifyComplete();
  }


  @Test
  public void getCountries_returnsData() {
    CommonLookupValueDetail commonValue = new CommonLookupValueDetail().code("GBR");
    CommonLookupDetail commonValues = new CommonLookupDetail().addContentItem(commonValue);

    when(ebsApiClient.getCountries()).thenReturn(Mono.just(commonValues));

    Mono<CommonLookupDetail> commonLookupDetailMono = lookupService.getCountries();
    StepVerifier.create(commonLookupDetailMono)
        .expectNext(commonValues)
        .verifyComplete();
  }

  @Test
  public void getCountry_returnsData() {
    CommonLookupValueDetail commonValue = new CommonLookupValueDetail().code("GBR");
    CommonLookupDetail commonValues = new CommonLookupDetail().addContentItem(commonValue);

    when(ebsApiClient.getCountries()).thenReturn(Mono.just(commonValues));

    Mono<CommonLookupValueDetail> commonLookupDetailMono = lookupService.getCountry("GBR");
    StepVerifier.create(commonLookupDetailMono)
        .expectNextMatches(result -> "GBR".equals(result.getCode()))
        .verifyComplete();
  }

  @Test
  public void getCategoriesOfLaw_returnsData() {
    CategoryOfLawLookupValueDetail commonValue = new CategoryOfLawLookupValueDetail().code("CAT1");
    CategoryOfLawLookupDetail commonValues =
        new CategoryOfLawLookupDetail().addContentItem(commonValue);

    when(ebsApiClient.getCategoriesOfLaw(null, null, null))
        .thenReturn(Mono.just(commonValues));

    Mono<CategoryOfLawLookupDetail> commonLookupDetailMono = lookupService.getCategoriesOfLaw();
    StepVerifier.create(commonLookupDetailMono)
        .expectNextMatches(result -> result == commonValues)
        .verifyComplete();
  }

  @Test
  public void getCategoryOfLaw_returnsData() {
    CategoryOfLawLookupValueDetail commonValue = new CategoryOfLawLookupValueDetail()
        .code("CAT1")
        .matterTypeDescription("DESC")
        .copyCostLimit(Boolean.TRUE);
    CategoryOfLawLookupDetail commonValues =
        new CategoryOfLawLookupDetail().addContentItem(commonValue);

    when(ebsApiClient.getCategoriesOfLaw(commonValue.getCode(),
        null,
        null))
        .thenReturn(Mono.just(commonValues));

    Mono<CategoryOfLawLookupValueDetail> commonLookupDetailMono =
        lookupService.getCategoryOfLaw(commonValue.getCode());
    StepVerifier.create(commonLookupDetailMono)
        .expectNextMatches(result -> result == commonValue)
        .verifyComplete();
  }

}
