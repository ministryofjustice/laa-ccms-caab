package uk.gov.laa.ccms.caab.controller.application.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COURT_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SELECTED_COURT;

import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.ActiveCaseModelAdvice;
import uk.gov.laa.ccms.caab.bean.validators.application.CourtSearchValidator;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

@ExtendWith(MockitoExtension.class)
@DisplayName("Court Search Controller Tests")
class CourtSearchControllerTest {

  @Mock protected LookupService lookupService;

  protected CourtSearchValidator courtSearchValidator = new CourtSearchValidator();

  protected MockMvc mockMvc;

  private MockMvcTester mockMvcTester;

  @BeforeEach
  public void setUp() {
    mockMvc =
        standaloneSetup(new CourtSearchController(courtSearchValidator, lookupService))
            .setControllerAdvice(new ActiveCaseModelAdvice())
            .build();
    mockMvcTester = MockMvcTester.create(mockMvc);
  }

  @Test
  @DisplayName("GET /court/search displays court search form with proceeding index")
  public void courtSearchDisplaysForm() throws Exception {
    assertThat(
            mockMvc
                .perform(get("/court/search").param("proceedingIndex", "0"))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(200);
  }

  @Test
  @DisplayName("GET /court/search returns court search view")
  public void courtSearchReturnsView() throws Exception {
    assertThat(mockMvcTester.perform(get("/court/search").param("proceedingIndex", "1")))
        .hasViewName("application/court-search");
  }

  @Test
  @DisplayName("GET /court/search passes proceeding index to model")
  public void courtSearchPassesProceedingIndexToModel() throws Exception {
    assertThat(mockMvcTester.perform(get("/court/search").param("proceedingIndex", "2")))
        .hasModelAttribute("proceedingIndex", 2);
  }

  @Test
  @DisplayName("POST /court/search with validation errors returns search view")
  public void courtSearchPostWithValidationErrorsReturnsSearchView() throws Exception {
    assertThat(
            mockMvcTester.perform(
                post("/court/search")
                    .param("proceedingIndex", "0")
                    .param("courtCode", "")
                    .param("courtName", "")))
        .hasViewName("application/court-search")
        .hasModelAttribute("proceedingIndex", 0);
  }

  @Test
  @DisplayName("POST /court/search with no results returns no results view")
  public void courtSearchPostWithNoResultsReturnsNoResultsView() throws Exception {
    CommonLookupDetail emptyResult = new CommonLookupDetail().content(Collections.emptyList());

    when(lookupService.getCourts(anyString(), anyString())).thenReturn(Mono.just(emptyResult));

    assertThat(
            mockMvcTester.perform(
                post("/court/search")
                    .param("proceedingIndex", "0")
                    .param("courtCode", "123")
                    .param("courtName", "NonExistent")))
        .hasViewName("application/court-search-no-results")
        .hasModelAttribute("proceedingIndex", 0);
  }

  @Test
  @DisplayName("POST /court/search with results redirects to court results page")
  public void courtSearchPostWithResultsRedirectsToResults() throws Exception {
    CommonLookupValueDetail court1 =
        new CommonLookupValueDetail().code("001").description("High Court");
    CommonLookupValueDetail court2 =
        new CommonLookupValueDetail().code("002").description("Crown Court");

    CommonLookupDetail result = new CommonLookupDetail().content(List.of(court1, court2));

    when(lookupService.getCourts(anyString(), anyString())).thenReturn(Mono.just(result));

    assertThat(
            mockMvcTester.perform(
                post("/court/search")
                    .param("proceedingIndex", "0")
                    .param("courtCode", "")
                    .param("courtName", "Court")))
        .hasStatus3xxRedirection()
        .hasRedirectedUrl("/court/results?proceedingIndex=0");
  }

  @Test
  @DisplayName("POST /court/search stores results in session")
  public void courtSearchPostStoresResultsInSession() throws Exception {
    CommonLookupValueDetail court1 =
        new CommonLookupValueDetail().code("001").description("High Court");

    CommonLookupDetail result = new CommonLookupDetail().content(List.of(court1));

    when(lookupService.getCourts(anyString(), anyString())).thenReturn(Mono.just(result));

    HttpSession session =
        mockMvc
            .perform(
                post("/court/search")
                    .param("proceedingIndex", "0")
                    .param("courtCode", "")
                    .param("courtName", "High"))
            .andReturn()
            .getRequest()
            .getSession();

    assertThat(session).isNotNull();
    @SuppressWarnings("unchecked")
    List<CommonLookupValueDetail> storedResults =
        (List<CommonLookupValueDetail>) session.getAttribute(COURT_SEARCH_RESULTS);
    assertThat(storedResults).isNotNull().hasSize(1).contains(court1);
  }

  @Test
  @DisplayName("GET /court/results without session redirects to search")
  public void courtResultsWithoutSessionRedirectsToSearch() throws Exception {
    assertThat(mockMvcTester.perform(get("/court/results").param("proceedingIndex", "0")))
        .hasStatus3xxRedirection()
        .hasRedirectedUrl("/court/search");
  }

  @Test
  @DisplayName("GET /court/results displays results page with proceeding index")
  public void courtResultsDisplaysPageWithProceedingIndex() throws Exception {
    CommonLookupValueDetail court1 =
        new CommonLookupValueDetail().code("001").description("High Court");
    CommonLookupValueDetail court2 =
        new CommonLookupValueDetail().code("002").description("Crown Court");

    HttpSession session =
        mockMvc
            .perform(get("/court/results").param("proceedingIndex", "1"))
            .andReturn()
            .getRequest()
            .getSession();

    session.setAttribute(COURT_SEARCH_RESULTS, List.of(court1, court2));

    assertThat(
            mockMvcTester.perform(
                get("/court/results").param("proceedingIndex", "1").session((HttpSession) session)))
        .hasViewName("application/court-search-results")
        .hasModelAttribute("proceedingIndex", 1);
  }

  @Test
  @DisplayName("GET /court/results returns paginated results")
  public void courtResultsReturnsPaginatedResults() throws Exception {
    List<CommonLookupValueDetail> courts = new java.util.ArrayList<>();
    for (int i = 1; i <= 15; i++) {
      courts.add(new CommonLookupValueDetail().code("00" + i).description("Court " + i));
    }

    HttpSession session =
        mockMvc
            .perform(get("/court/results").param("proceedingIndex", "0"))
            .andReturn()
            .getRequest()
            .getSession();

    session.setAttribute(COURT_SEARCH_RESULTS, courts);

    assertThat(
            mockMvcTester.perform(
                get("/court/results")
                    .param("proceedingIndex", "0")
                    .param("page", "0")
                    .param("size", "10")
                    .session((HttpSession) session)))
        .hasViewName("application/court-search-results");
  }

  @Test
  @DisplayName("GET /court/select with valid index sets selected court in session")
  public void courtSelectWithValidIndexSetsSelectedCourt() throws Exception {
    CommonLookupValueDetail court1 =
        new CommonLookupValueDetail().code("001").description("High Court");
    CommonLookupValueDetail court2 =
        new CommonLookupValueDetail().code("002").description("Crown Court");

    HttpSession session =
        mockMvc
            .perform(get("/court/results").param("proceedingIndex", "0"))
            .andReturn()
            .getRequest()
            .getSession();

    session.setAttribute(COURT_SEARCH_RESULTS, List.of(court1, court2));

    HttpSession resultSession =
        mockMvc
            .perform(
                get("/court/select")
                    .param("index", "0")
                    .param("proceedingIndex", "0")
                    .session((HttpSession) session))
            .andReturn()
            .getRequest()
            .getSession();

    CommonLookupValueDetail selectedCourt =
        (CommonLookupValueDetail) resultSession.getAttribute(SELECTED_COURT);
    assertThat(selectedCourt).isNotNull().isEqualTo(court1);
  }

  @Test
  @DisplayName("GET /court/select redirects to proceeding outcome with proceeding index")
  public void courtSelectRedirectsToProceedingOutcome() throws Exception {
    CommonLookupValueDetail court1 =
        new CommonLookupValueDetail().code("001").description("High Court");

    HttpSession session =
        mockMvc
            .perform(get("/court/results").param("proceedingIndex", "2"))
            .andReturn()
            .getRequest()
            .getSession();

    session.setAttribute(COURT_SEARCH_RESULTS, List.of(court1));

    assertThat(
            mockMvcTester.perform(
                get("/court/select")
                    .param("index", "0")
                    .param("proceedingIndex", "2")
                    .session((HttpSession) session)))
        .hasStatus3xxRedirection()
        .hasRedirectedUrl("/case/outcome-and-awards/proceeding/2/outcome");
  }

  @Test
  @DisplayName("GET /court/select with invalid index does not set selected court")
  public void courtSelectWithInvalidIndexDoesNotSetSelectedCourt() throws Exception {
    CommonLookupValueDetail court1 =
        new CommonLookupValueDetail().code("001").description("High Court");

    HttpSession session =
        mockMvc
            .perform(get("/court/results").param("proceedingIndex", "0"))
            .andReturn()
            .getRequest()
            .getSession();

    session.setAttribute(COURT_SEARCH_RESULTS, List.of(court1));

    HttpSession resultSession =
        mockMvc
            .perform(
                get("/court/select")
                    .param("index", "99")
                    .param("proceedingIndex", "0")
                    .session((HttpSession) session))
            .andReturn()
            .getRequest()
            .getSession();

    CommonLookupValueDetail selectedCourt =
        (CommonLookupValueDetail) resultSession.getAttribute(SELECTED_COURT);
    assertThat(selectedCourt).isNull();
  }

  @Test
  @DisplayName("GET /court/select with out of bounds index does not set selected court")
  public void courtSelectWithOutOfBoundsIndexDoesNotSetSelectedCourt() throws Exception {
    CommonLookupValueDetail court1 =
        new CommonLookupValueDetail().code("001").description("High Court");

    HttpSession session =
        mockMvc
            .perform(get("/court/results").param("proceedingIndex", "0"))
            .andReturn()
            .getRequest()
            .getSession();

    session.setAttribute(COURT_SEARCH_RESULTS, List.of(court1));

    HttpSession resultSession =
        mockMvc
            .perform(
                get("/court/select")
                    .param("index", "5")
                    .param("proceedingIndex", "0")
                    .session((HttpSession) session))
            .andReturn()
            .getRequest()
            .getSession();

    CommonLookupValueDetail selectedCourt =
        (CommonLookupValueDetail) resultSession.getAttribute(SELECTED_COURT);
    assertThat(selectedCourt).isNull();
  }

  @Test
  @DisplayName("GET /court/select with negative index does not set selected court")
  public void courtSelectWithNegativeIndexDoesNotSetSelectedCourt() throws Exception {
    CommonLookupValueDetail court1 =
        new CommonLookupValueDetail().code("001").description("High Court");

    HttpSession session =
        mockMvc
            .perform(get("/court/results").param("proceedingIndex", "0"))
            .andReturn()
            .getRequest()
            .getSession();

    session.setAttribute(COURT_SEARCH_RESULTS, List.of(court1));

    HttpSession resultSession =
        mockMvc
            .perform(
                get("/court/select")
                    .param("index", "-1")
                    .param("proceedingIndex", "0")
                    .session((HttpSession) session))
            .andReturn()
            .getRequest()
            .getSession();

    CommonLookupValueDetail selectedCourt =
        (CommonLookupValueDetail) resultSession.getAttribute(SELECTED_COURT);
    assertThat(selectedCourt).isNull();
  }
}
