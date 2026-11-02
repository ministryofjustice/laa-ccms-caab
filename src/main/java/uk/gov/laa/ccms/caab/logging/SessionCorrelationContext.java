package uk.gov.laa.ccms.caab.logging;

import io.micrometer.context.ContextRegistry;
import org.slf4j.MDC;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/** Propagates only the opaque logging ID, leaving trace/span management to Micrometer. */
@Component
public class SessionCorrelationContext implements InitializingBean, DisposableBean {

  private static final String KEY = SessionCorrelationContext.class.getName();

  @Override
  public void afterPropertiesSet() {
    ContextRegistry.getInstance()
        .registerThreadLocalAccessor(
            KEY,
            () -> MDC.get("sessionID"),
            value -> MDC.put("sessionID", value),
            () -> MDC.remove("sessionID"));
  }

  @Override
  public void destroy() {
    ContextRegistry.getInstance().removeThreadLocalAccessor(KEY);
  }
}
