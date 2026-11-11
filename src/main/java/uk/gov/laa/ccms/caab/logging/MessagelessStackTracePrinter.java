package uk.gov.laa.ccms.caab.logging;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;
import org.springframework.boot.logging.StackTracePrinter;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Prints an exception as its class chain and code locations, without any message. Exception
 * messages routinely echo what was being processed, such as the request URI with its query string
 * or a rejected value, so they must not reach the production logs.
 */
public class MessagelessStackTracePrinter implements StackTracePrinter {

  private static final int MAX_CAUSES = 10;

  @Override
  public void printStackTrace(Throwable throwable, Appendable out) throws IOException {
    Map<Throwable, Boolean> seen = new IdentityHashMap<>();
    Throwable current = throwable;
    for (int depth = 0; current != null && seen.put(current, true) == null; depth++) {
      if (depth == MAX_CAUSES) {
        out.append("Caused by: ...\n");
        return;
      }
      out.append(depth == 0 ? "" : "Caused by: ").append(describe(current)).append('\n');
      for (StackTraceElement frame : current.getStackTrace()) {
        out.append("\tat ").append(frame.toString()).append('\n');
      }
      current = current.getCause();
    }
  }

  private static String describe(Throwable throwable) {
    String name = throwable.getClass().getName();
    return throwable instanceof WebClientResponseException response
        ? name + " [HTTP " + response.getStatusCode().value() + "]"
        : name;
  }
}
