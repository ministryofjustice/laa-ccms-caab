package uk.gov.laa.ccms.caab.security;

import io.micrometer.context.ContextRegistry;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Hooks;
import reactor.util.context.ContextView;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Holds the active CCMS provider firm identifier for the current request and bridges it across
 * Reactor/Netty threads via Micrometer context propagation.
 */
@Component
public class ProviderContext implements InitializingBean, DisposableBean {

  public static final String HEADER_NAME = "Caab-Provider-Id";

  static final String CONTEXT_KEY = ProviderContext.class.getName() + ".providerId";

  private static final ThreadLocal<String> PROVIDER_ID = new ThreadLocal<>();

  /**
   * Registers the {@link ThreadLocal} accessor with Micrometer's {@link ContextRegistry} and
   * enables Reactor automatic context propagation.
   */
  public static void registerAccessor() {
    ContextRegistry.getInstance()
        .registerThreadLocalAccessor(
            CONTEXT_KEY,
            ProviderContext::getProviderId,
            ProviderContext::setProviderId,
            ProviderContext::clear);
    Hooks.enableAutomaticContextPropagation();
  }

  /** Returns the active provider firm identifier on the current thread, or {@code null}. */
  public static String getProviderId() {
    return PROVIDER_ID.get();
  }

  /** Sets the active provider firm identifier, clearing when blank or {@code "null"}. */
  public static void setProviderId(final String providerId) {
    final String normalised = normalise(providerId);
    if (normalised == null) {
      PROVIDER_ID.remove();
    } else {
      PROVIDER_ID.set(normalised);
    }
  }

  /** Sets the active provider firm identifier from a numeric ID, clearing when {@code null}. */
  public static void setProviderId(final Integer providerId) {
    if (providerId == null) {
      PROVIDER_ID.remove();
    } else {
      PROVIDER_ID.set(String.valueOf(providerId));
    }
  }

  /** Seeds or clears the active provider firm identifier from the supplied {@link UserDetail}. */
  public static void setFromUser(final UserDetail user) {
    if (user != null && user.getProvider() != null && user.getProvider().getId() != null) {
      setProviderId(user.getProvider().getId());
    } else {
      clear();
    }
  }

  /** Clears the active provider firm identifier on the current thread. */
  public static void clear() {
    PROVIDER_ID.remove();
  }

  /**
   * Resolves the active provider firm identifier from the current thread or Reactor {@link
   * ContextView}.
   */
  public static String resolveProviderId(final ContextView contextView) {
    final String threadLocalValue = normalise(PROVIDER_ID.get());
    if (threadLocalValue != null) {
      return threadLocalValue;
    }
    if (contextView != null && contextView.hasKey(CONTEXT_KEY)) {
      final Object contextual = contextView.get(CONTEXT_KEY);
      if (contextual instanceof String value) {
        return normalise(value);
      }
    }
    return null;
  }

  private static String normalise(final String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    final String trimmed = value.trim();
    return "null".equalsIgnoreCase(trimmed) ? null : trimmed;
  }

  @Override
  public void afterPropertiesSet() {
    registerAccessor();
  }

  @Override
  public void destroy() {
    ContextRegistry.getInstance().removeThreadLocalAccessor(CONTEXT_KEY);
  }
}
