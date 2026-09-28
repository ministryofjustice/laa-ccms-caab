package uk.gov.laa.ccms.caab.service;

import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_NOTIFICATION_TYPE;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.model.NotificationSearchOptions;
import uk.gov.laa.ccms.data.model.BaseUser;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;

/** Loads and caches notification search options within the current HTTP session. */
@Component
@SessionScope
@RequiredArgsConstructor
@Slf4j
public class NotificationSearchOptionsCache {

  private final ProviderService providerService;
  private final LookupService lookupService;
  private final UserService userService;

  private NotificationSearchOptions cachedOptions;
  private Integer inFlightProviderId;
  private Mono<NotificationSearchOptions> inFlightLoad;
  private long generation;

  /**
   * Returns notification search options for a provider, sharing an active load and retaining only
   * complete results.
   *
   * @param providerId provider whose options are required
   * @return notification search options
   */
  public synchronized Mono<NotificationSearchOptions> get(Integer providerId) {
    if (cachedOptions != null && Objects.equals(cachedOptions.providerId(), providerId)) {
      return Mono.just(cachedOptions);
    }

    if (inFlightLoad != null && Objects.equals(inFlightProviderId, providerId)) {
      return inFlightLoad;
    }

    cachedOptions = null;
    long loadGeneration = ++generation;
    inFlightProviderId = providerId;
    inFlightLoad =
        load(providerId)
            .doOnNext(options -> cacheIfComplete(providerId, loadGeneration, options))
            .doFinally(signal -> clearInFlight(providerId, loadGeneration))
            .cache();
    return inFlightLoad;
  }

  /** Clears any cached or in-flight options for this session. */
  public synchronized void clear() {
    generation++;
    cachedOptions = null;
    inFlightProviderId = null;
    inFlightLoad = null;
  }

  private Mono<NotificationSearchOptions> load(Integer providerId) {
    Mono<LoadResult<ContactDetail>> feeEarners =
        providerService
            .getProvider(providerId)
            .map(providerService::getAllFeeEarners)
            .map(LoadResult::success)
            .switchIfEmpty(Mono.fromSupplier(LoadResult::failure))
            .onErrorResume(
                e -> {
                  log.error("Failed to retrieve fee earners", e);
                  return Mono.just(LoadResult.failure());
                });

    Mono<LoadResult<CommonLookupValueDetail>> notificationTypes =
        lookupService
            .getCommonValues(COMMON_VALUE_NOTIFICATION_TYPE)
            .map(detail -> Optional.ofNullable(detail.getContent()).orElse(Collections.emptyList()))
            .map(LoadResult::success)
            .switchIfEmpty(Mono.fromSupplier(LoadResult::failure))
            .onErrorResume(
                e -> {
                  log.error("Failed to retrieve notification types", e);
                  return Mono.just(LoadResult.failure());
                });

    Mono<LoadResult<BaseUser>> users =
        userService
            .getUsers(providerId)
            .map(
                details ->
                    Optional.ofNullable(details.getContent()).orElse(Collections.emptyList()))
            .map(LoadResult::success)
            .switchIfEmpty(Mono.fromSupplier(LoadResult::failure))
            .onErrorResume(
                e -> {
                  log.error("Failed to retrieve users", e);
                  return Mono.just(LoadResult.failure());
                });

    return Mono.zip(feeEarners, notificationTypes, users)
        .map(
            tuple ->
                new NotificationSearchOptions(
                    providerId,
                    tuple.getT1().values(),
                    tuple.getT2().values(),
                    tuple.getT3().values(),
                    tuple.getT1().available()
                        && tuple.getT2().available()
                        && tuple.getT3().available()));
  }

  private synchronized void cacheIfComplete(
      Integer providerId, long loadGeneration, NotificationSearchOptions options) {
    if (generation == loadGeneration
        && options.fullyAvailable()
        && Objects.equals(inFlightProviderId, providerId)) {
      cachedOptions = options;
    }
  }

  private synchronized void clearInFlight(Integer providerId, long loadGeneration) {
    if (generation == loadGeneration && Objects.equals(inFlightProviderId, providerId)) {
      inFlightProviderId = null;
      inFlightLoad = null;
    }
  }

  private record LoadResult<T>(List<T> values, boolean available) {

    private static <T> LoadResult<T> success(List<T> values) {
      return new LoadResult<>(values == null ? Collections.emptyList() : List.copyOf(values), true);
    }

    private static <T> LoadResult<T> failure() {
      return new LoadResult<>(Collections.emptyList(), false);
    }
  }
}
