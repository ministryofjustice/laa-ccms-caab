package uk.gov.laa.ccms.caab.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@DisplayName("Refused action page interceptor")
class RefusedActionPageInterceptorTest {

  private final RefusedActionPageInterceptor interceptor = new RefusedActionPageInterceptor();

  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
  }

  private MockHttpServletRequest request(String method, String uri, String query) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
    request.setQueryString(query);
    request.setSession(session);
    return request;
  }

  private ModelAndView render(MockHttpServletRequest request, ModelAndView modelAndView) {
    interceptor.postHandle(request, new MockHttpServletResponse(), new Object(), modelAndView);
    return modelAndView;
  }

  @Test
  @DisplayName("A page rendered by a plain page load is recorded with its query")
  void recordsRenderedGetPage() {
    render(request("GET", "/civil/case/details", "tab=costs"), new ModelAndView("details"));

    assertThat(RefusedActionSession.findRenderedPage(session, "/civil/case/details")).isPresent();
    assertThat(RefusedActionSession.lastRenderedPage(session))
        .contains("/civil/case/details?tab=costs");
  }

  @Test
  @DisplayName("A page rendered in answer to a form post is not recorded")
  void doesNotRecordPostedPage() {
    render(request("POST", "/civil/application/client/confirmed", null), new ModelAndView("x"));

    assertThat(RefusedActionSession.lastRenderedPage(session)).isEmpty();
  }

  @Test
  @DisplayName("A background request is not recorded")
  void doesNotRecordBackgroundRequest() {
    MockHttpServletRequest request = request("GET", "/civil/fragment", null);
    request.addHeader("Sec-Fetch-Mode", "cors");

    render(request, new ModelAndView("fragment"));

    assertThat(RefusedActionSession.lastRenderedPage(session)).isEmpty();
  }

  @Test
  @DisplayName("A redirect neither records the page nor uses up the waiting message")
  void redirectKeepsMessageWaiting() {
    RefusedActionSession.markNotAuthorised(session);

    ModelAndView byName =
        render(request("GET", "/civil/notifications", null), new ModelAndView("redirect:/x"));
    ModelAndView byView =
        render(
            request("GET", "/civil/notifications", null), new ModelAndView(new RedirectView("/x")));

    assertThat(byName.getModel())
        .doesNotContainKey(NotAuthorisedAccessDeniedHandler.NOT_AUTHORISED_ATTRIBUTE);
    assertThat(byView.getModel())
        .doesNotContainKey(NotAuthorisedAccessDeniedHandler.NOT_AUTHORISED_ATTRIBUTE);
    assertThat(RefusedActionSession.lastRenderedPage(session)).isEmpty();
    assertThat(RefusedActionSession.consumeNotAuthorised(session)).isTrue();
  }

  @Test
  @DisplayName("The waiting message is shown on the next rendered page, and only once")
  void showsWaitingMessageOnce() {
    RefusedActionSession.markNotAuthorised(session);

    ModelAndView first = render(request("POST", "/civil/form", null), new ModelAndView("form"));
    ModelAndView second = render(request("GET", "/civil/other", null), new ModelAndView("other"));

    assertThat(first.getModel())
        .containsEntry(NotAuthorisedAccessDeniedHandler.NOT_AUTHORISED_ATTRIBUTE, true);
    assertThat(second.getModel())
        .doesNotContainKey(NotAuthorisedAccessDeniedHandler.NOT_AUTHORISED_ATTRIBUTE);
  }

  @Test
  @DisplayName("A page that could be read as another host is not recorded")
  void doesNotRecordNonLocalPage() {
    render(request("GET", "//evil.example/page", null), new ModelAndView("page"));

    assertThat(RefusedActionSession.lastRenderedPage(session)).isEmpty();
  }

  @Test
  @DisplayName("A referring page is matched to the recorded page, preferring the same query")
  void findsRecordedPageForReferer() {
    RefusedActionSession.recordRenderedPage(session, "/civil/search?page=1");
    RefusedActionSession.recordRenderedPage(session, "/civil/search?page=2");

    assertThat(RefusedActionSession.findRenderedPage(session, "/civil/search?page=1"))
        .contains("/civil/search?page=1");
    assertThat(RefusedActionSession.findRenderedPage(session, "/civil/search?page=9"))
        .contains("/civil/search?page=2");
  }

  @Test
  @DisplayName("Only the most recent pages are kept, without duplicates")
  void keepsRecentPagesWithoutDuplicates() {
    for (int i = 0; i < 60; i++) {
      RefusedActionSession.recordRenderedPage(session, "/civil/page/" + i);
    }
    RefusedActionSession.recordRenderedPage(session, "/civil/page/20");

    assertThat(RefusedActionSession.findRenderedPage(session, "/civil/page/5")).isEmpty();
    assertThat(RefusedActionSession.findRenderedPage(session, "/civil/page/59")).isPresent();
    assertThat(RefusedActionSession.lastRenderedPage(session)).contains("/civil/page/20");
  }
}
