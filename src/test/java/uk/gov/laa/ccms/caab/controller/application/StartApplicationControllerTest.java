package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_SECTIONS_BACK_URL;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.util.ConversionServiceUtils.getConversionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationType;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration
@WebAppConfiguration
@DisplayName("StartApplicationController test")
class StartApplicationControllerTest {

  private MockMvcTester mockMvc;

  @InjectMocks private StartApplicationController startApplicationController;

  @BeforeEach
  void setup() {
    mockMvc =
        MockMvcTester.create(
            standaloneSetup(startApplicationController)
                .setConversionService(getConversionService())
                .build());
  }

  @Test
  @DisplayName("GET: /application/new should create new application and redirect")
  void shouldCreateNewApplication() {
    assertThat(mockMvc.perform(get("/application/new")))
        .hasStatus3xxRedirection()
        .hasRedirectedUrl("/application/office");
  }

  @Test
  @DisplayName("GET: /application/new should keep the sections back link for browser back")
  void shouldKeepSectionsBackUrlForNewApplication() {
    assertThat(
            mockMvc.perform(
                get("/application/new").sessionAttr(APPLICATION_SECTIONS_BACK_URL, "searchUrl")))
        .request()
        .sessionAttributes()
        .containsEntry(APPLICATION_SECTIONS_BACK_URL, "searchUrl");
  }

  @Test
  @DisplayName("GET: /application/new starts the journey's searches empty")
  void shouldClearSearchCriteriaForNewApplication() {
    CaseSearchCriteria caseCriteria = new CaseSearchCriteria();
    caseCriteria.setCaseReference("300001");
    ClientSearchCriteria clientCriteria = new ClientSearchCriteria();
    clientCriteria.setSurname("WALL");

    assertThat(
            mockMvc.perform(
                get("/application/new")
                    .sessionAttr(CASE_SEARCH_CRITERIA, caseCriteria)
                    .sessionAttr(CLIENT_SEARCH_CRITERIA, clientCriteria)))
        .request()
        .sessionAttributes()
        .doesNotContainKey(CASE_SEARCH_CRITERIA)
        .doesNotContainKey(CLIENT_SEARCH_CRITERIA);
  }

  @Test
  @DisplayName("GET: /amendments/new should create new application and redirect")
  void shouldCreateNewAmendment() {
    assertThat(mockMvc.perform(get("/amendments/new")))
        .hasStatus3xxRedirection()
        .hasRedirectedUrl("/amendments/application-type");
  }

  @Test
  @DisplayName("GET: /amendments/new for an ECF case skips the application-type screen")
  void shouldSkipApplicationTypeForExceptionalCaseFunding() {
    final ApplicationDetail ecfCase =
        new ApplicationDetail()
            .applicationType(
                new ApplicationType().id(APP_TYPE_EXCEPTIONAL_CASE_FUNDING).displayValue("ECF"));

    assertThat(mockMvc.perform(get("/amendments/new").sessionAttr(CASE, ecfCase)))
        .hasStatus3xxRedirection()
        .hasRedirectedUrl("/amendments/create");
  }

  @Test
  @DisplayName("GET: /amendments/new for a non-ECF case still shows the application-type screen")
  void shouldShowApplicationTypeForNonExceptionalCaseFunding() {
    final ApplicationDetail substantiveCase =
        new ApplicationDetail()
            .applicationType(new ApplicationType().id("SUB").displayValue("Substantive"));

    assertThat(mockMvc.perform(get("/amendments/new").sessionAttr(CASE, substantiveCase)))
        .hasStatus3xxRedirection()
        .hasRedirectedUrl("/amendments/application-type");
  }
}
