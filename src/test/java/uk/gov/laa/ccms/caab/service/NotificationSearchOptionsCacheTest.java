package uk.gov.laa.ccms.caab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_NOTIFICATION_TYPE;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import uk.gov.laa.ccms.caab.model.NotificationSearchOptions;
import uk.gov.laa.ccms.data.model.BaseUser;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.UserDetails;

@ExtendWith(MockitoExtension.class)
class NotificationSearchOptionsCacheTest {

  private static final int PROVIDER_ID = 123;

  @Mock private ProviderService providerService;
  @Mock private LookupService lookupService;
  @Mock private UserService userService;

  private NotificationSearchOptionsCache cache;

  @BeforeEach
  void setUp() {
    cache = new NotificationSearchOptionsCache(providerService, lookupService, userService);
  }

  @Test
  void shouldReuseSuccessfulOptionsForTheProvider() {
    stubSuccessfulLoad(PROVIDER_ID);

    NotificationSearchOptions first = cache.get(PROVIDER_ID).block();
    NotificationSearchOptions second = cache.get(PROVIDER_ID).block();

    assertThat(first).isSameAs(second);
    assertThat(first.fullyAvailable()).isTrue();
    verify(providerService).getProvider(PROVIDER_ID);
    verify(lookupService).getCommonValues(COMMON_VALUE_NOTIFICATION_TYPE);
    verify(userService).getUsers(PROVIDER_ID);
  }

  @Test
  void shouldTreatSuccessfulEmptyListsAsCacheable() {
    when(providerService.getProvider(PROVIDER_ID)).thenReturn(Mono.just(new ProviderDetail()));
    when(providerService.getAllFeeEarners(any())).thenReturn(Collections.emptyList());
    when(lookupService.getCommonValues(COMMON_VALUE_NOTIFICATION_TYPE))
        .thenReturn(Mono.just(new CommonLookupDetail().content(Collections.emptyList())));
    when(userService.getUsers(PROVIDER_ID))
        .thenReturn(Mono.just(new UserDetails().content(Collections.emptyList())));

    cache.get(PROVIDER_ID).block();
    cache.get(PROVIDER_ID).block();

    verify(providerService).getProvider(PROVIDER_ID);
  }

  @Test
  void shouldRetryAfterAPartialFailure() {
    when(providerService.getProvider(PROVIDER_ID))
        .thenReturn(
            Mono.error(new RuntimeException("Unavailable")), Mono.just(new ProviderDetail()));
    when(providerService.getAllFeeEarners(any()))
        .thenReturn(List.of(new ContactDetail().id(1).name("Fee Earner")));
    when(lookupService.getCommonValues(COMMON_VALUE_NOTIFICATION_TYPE))
        .thenReturn(Mono.just(notificationTypes()));
    when(userService.getUsers(PROVIDER_ID)).thenReturn(Mono.just(users()));

    NotificationSearchOptions failed = cache.get(PROVIDER_ID).block();
    NotificationSearchOptions retried = cache.get(PROVIDER_ID).block();

    assertThat(failed.fullyAvailable()).isFalse();
    assertThat(retried.fullyAvailable()).isTrue();
    verify(providerService, times(2)).getProvider(PROVIDER_ID);
  }

  @Test
  void shouldCoalesceConcurrentLoads() {
    Sinks.One<ProviderDetail> provider = Sinks.one();
    when(providerService.getProvider(PROVIDER_ID)).thenReturn(provider.asMono());
    when(providerService.getAllFeeEarners(any())).thenReturn(Collections.emptyList());
    when(lookupService.getCommonValues(COMMON_VALUE_NOTIFICATION_TYPE))
        .thenReturn(Mono.just(notificationTypes()));
    when(userService.getUsers(PROVIDER_ID)).thenReturn(Mono.just(users()));

    Mono<NotificationSearchOptions> first = cache.get(PROVIDER_ID);
    Mono<NotificationSearchOptions> second = cache.get(PROVIDER_ID);

    assertThat(first).isSameAs(second);
    first.subscribe();
    second.subscribe();
    provider.tryEmitValue(new ProviderDetail());

    StepVerifier.create(first).expectNextCount(1).verifyComplete();
    verify(providerService).getProvider(PROVIDER_ID);
  }

  @Test
  void shouldReloadForAnotherProviderAndAfterClear() {
    stubSuccessfulLoad(PROVIDER_ID);
    stubSuccessfulLoad(456);

    cache.get(PROVIDER_ID).block();
    cache.get(456).block();
    cache.clear();
    cache.get(456).block();

    verify(providerService).getProvider(PROVIDER_ID);
    verify(providerService, times(2)).getProvider(456);
  }

  @Test
  void staleLoadShouldNotOverwriteOrClearNewLoadForSameProvider() {
    Sinks.One<ProviderDetail> staleProvider = Sinks.one();
    Sinks.One<ProviderDetail> currentProvider = Sinks.one();
    when(providerService.getProvider(PROVIDER_ID))
        .thenReturn(staleProvider.asMono(), currentProvider.asMono(), Mono.never());
    when(providerService.getAllFeeEarners(any())).thenReturn(Collections.emptyList());
    when(lookupService.getCommonValues(COMMON_VALUE_NOTIFICATION_TYPE))
        .thenReturn(Mono.just(notificationTypes()));
    when(userService.getUsers(PROVIDER_ID)).thenReturn(Mono.just(users()));

    Mono<NotificationSearchOptions> staleLoad = cache.get(PROVIDER_ID);
    staleLoad.subscribe();
    cache.clear();
    Mono<NotificationSearchOptions> currentLoad = cache.get(PROVIDER_ID);
    currentLoad.subscribe();

    staleProvider.tryEmitValue(new ProviderDetail());
    StepVerifier.create(staleLoad).expectNextCount(1).verifyComplete();

    assertThat(cache.get(PROVIDER_ID)).isSameAs(currentLoad);

    currentProvider.tryEmitValue(new ProviderDetail());
    NotificationSearchOptions currentOptions = currentLoad.block();

    assertThat(cache.get(PROVIDER_ID).block()).isSameAs(currentOptions);
    verify(providerService, times(2)).getProvider(PROVIDER_ID);
  }

  @Test
  void separateCacheInstancesShouldNotShareState() {
    stubSuccessfulLoad(PROVIDER_ID);
    NotificationSearchOptionsCache otherCache =
        new NotificationSearchOptionsCache(providerService, lookupService, userService);

    cache.get(PROVIDER_ID).block();
    otherCache.get(PROVIDER_ID).block();

    verify(providerService, times(2)).getProvider(PROVIDER_ID);
  }

  private void stubSuccessfulLoad(int providerId) {
    ProviderDetail provider = new ProviderDetail();
    when(providerService.getProvider(providerId)).thenReturn(Mono.just(provider));
    when(providerService.getAllFeeEarners(provider))
        .thenReturn(List.of(new ContactDetail().id(1).name("Fee Earner")));
    when(lookupService.getCommonValues(COMMON_VALUE_NOTIFICATION_TYPE))
        .thenReturn(Mono.just(notificationTypes()));
    when(userService.getUsers(providerId)).thenReturn(Mono.just(users()));
  }

  private CommonLookupDetail notificationTypes() {
    return new CommonLookupDetail()
        .content(
            List.of(
                new CommonLookupValueDetail()
                    .type("NOTIFICATION_TYPE")
                    .code("N")
                    .description("Notification")));
  }

  private UserDetails users() {
    return new UserDetails().content(List.of(new BaseUser().loginId("user@example.com")));
  }
}
