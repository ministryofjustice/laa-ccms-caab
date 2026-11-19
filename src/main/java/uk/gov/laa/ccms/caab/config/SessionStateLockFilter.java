package uk.gov.laa.ccms.caab.config;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

/**
 * Coordinates stateful requests through MVC session write-back, including resets in other journeys.
 * The permit is local to the servlet session: replicated sessions require distributed coordination.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class SessionStateLockFilter extends OncePerRequestFilter {

  private static final String PERMIT_ATTRIBUTE = SessionStateLockFilter.class.getName() + ".permit";

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    String path = request.getServletPath();
    return path != null
        && (path.equals("/favicon.ico")
            || path.startsWith("/assets/")
            || path.startsWith("/ccms/")
            || path.startsWith("/govuk-dialect/"));
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {
    HttpSession session = request.getSession(false);
    if (session == null) {
      // Never create a session for a stateless request just to coordinate it.
      chain.doFilter(request, response);
      return;
    }
    Semaphore permit;
    try {
      synchronized (WebUtils.getSessionMutex(session)) {
        Object value = session.getAttribute(PERMIT_ATTRIBUTE);
        if (value instanceof Semaphore existing) {
          permit = existing;
        } else {
          permit = new Semaphore(1, true);
          session.setAttribute(PERMIT_ATTRIBUTE, permit);
        }
      }
    } catch (IllegalStateException invalidated) {
      response.sendError(HttpServletResponse.SC_CONFLICT, "The session has expired.");
      return;
    }
    try {
      if (!permit.tryAcquire(30, TimeUnit.SECONDS)) {
        response.sendError(
            HttpServletResponse.SC_SERVICE_UNAVAILABLE,
            "Another request is still using this session. Please try again.");
        return;
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new ServletException("Interrupted while waiting for session state", interrupted);
    }
    Completion completion = new Completion(permit);
    try {
      chain.doFilter(request, response);
    } finally {
      if (request.isAsyncStarted()) {
        try {
          request.getAsyncContext().addListener(completion);
        } catch (IllegalStateException alreadyCompleted) {
          completion.release();
        }
      } else {
        completion.release();
      }
    }
  }

  private static final class Completion implements AsyncListener {
    private final Semaphore permit;
    private final AtomicBoolean released = new AtomicBoolean();

    private Completion(Semaphore permit) {
      this.permit = permit;
    }

    private void release() {
      if (released.compareAndSet(false, true)) {
        permit.release();
      }
    }

    @Override
    public void onComplete(AsyncEvent event) {
      release();
    }

    @Override
    public void onStartAsync(AsyncEvent event) {
      event.getAsyncContext().addListener(this);
    }

    @Override
    public void onTimeout(AsyncEvent event) {
      // Keep ownership until container completion; timeout/error handling can still mutate state.
    }

    @Override
    public void onError(AsyncEvent event) {
      // Keep ownership until container completion, including error redispatch and session
      // write-back.
    }
  }
}
