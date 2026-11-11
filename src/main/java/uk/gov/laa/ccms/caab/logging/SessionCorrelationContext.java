package uk.gov.laa.ccms.caab.logging;

import io.micrometer.context.ContextRegistry;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/** Propagates the opaque logging fields, leaving trace/span management to Micrometer. */
@Component
public class SessionCorrelationContext implements InitializingBean, DisposableBean {

  private static final String KEY_PREFIX = SessionCorrelationContext.class.getName() + ".";
  private static final List<String> FIELDS =
      List.of(
          "sessionID", "request_id", "source.ip", "network.forwarded_ip", UserIdentifier.MDC_KEY);

  @Override
  public void afterPropertiesSet() {
    FIELDS.forEach(
        field ->
            ContextRegistry.getInstance()
                .registerThreadLocalAccessor(
                    KEY_PREFIX + field,
                    () -> MDC.get(field),
                    value -> MDC.put(field, value),
                    () -> MDC.remove(field)));
  }

  @Override
  public void destroy() {
    FIELDS.forEach(
        field -> ContextRegistry.getInstance().removeThreadLocalAccessor(KEY_PREFIX + field));
  }
}
