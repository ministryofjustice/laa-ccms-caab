package uk.gov.laa.ccms.caab.controller.application.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.CaseSearchCriteria;
import uk.gov.laa.ccms.caab.constants.SearchConstants;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.mapper.ApplicationMapper;
import uk.gov.laa.ccms.caab.model.BaseApplication;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.data.model.CaseStatusLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class CopyCaseSearchResultsControllerTest {

  @Mock
  private ApplicationService applicationService;

  @Mock
  private ApplicationMapper applicationMapper;

  @Mock
  private SearchConstants searchConstants;

  @InjectMocks
  private CopyCaseSearchResultsController copyCaseSearchResultsController;

  private MockMvc mockMvc;

  private UserDetail user;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(copyCaseSearchResultsController).build();
    this.user = buildUser();

    when(searchConstants.getMaxSearchResultsCases()).thenReturn(200);
  }

  @Test
  public void testGetCopyCaseSearchResults_NoCopyCaseStatus() throws Exception {
    List<BaseApplication> baseApplications = new ArrayList<>();

    when(applicationService.getCases(any(), any(), any())).thenReturn(baseApplications);

    CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    this.mockMvc.perform(get("/application/copy-case/results")
            .sessionAttr(USER_DETAILS, user)
            .sessionAttr(CASE_SEARCH_CRITERIA, caseSearchCriteria))
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-copy-case-search-no-results"));

    verify(applicationService).getCopyCaseStatus();
    verify(applicationService).getCases(eq(caseSearchCriteria), any(), any());
    assertNull(caseSearchCriteria.getStatus());
  }

  @Test
  public void testGetCopyCaseSearchResults_NoResults() throws Exception {
    List<BaseApplication> baseApplications = new ArrayList<>();

    when(applicationService.getCases(any(), any(), any())).thenReturn(baseApplications);

    String COPY_STATUS_CODE = "APP";
    when(applicationService.getCopyCaseStatus()).thenReturn(
        new CaseStatusLookupValueDetail().code(COPY_STATUS_CODE));

    CaseSearchCriteria caseSearchCriteria = new CaseSearchCriteria();
    this.mockMvc.perform(get("/application/copy-case/results")
            .sessionAttr(USER_DETAILS, user)
            .sessionAttr(CASE_SEARCH_CRITERIA, caseSearchCriteria))
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-copy-case-search-no-results"));

    verify(applicationService).getCopyCaseStatus();
    verify(applicationService).getCases(eq(caseSearchCriteria), any(), any());
    assertEquals(COPY_STATUS_CODE, caseSearchCriteria.getStatus());
  }

  @Test
  public void testGetCopyCaseSearchResults_WithTooManyResults() throws Exception {

    when(applicationService.getCases(any(), any(), any())).thenThrow(
        new TooManyResultsException(""));

    this.mockMvc.perform(get("/application/copy-case/results")
            .sessionAttr(USER_DETAILS, user)
            .sessionAttr(CASE_SEARCH_CRITERIA, new CaseSearchCriteria()))
        .andExpect(status().isOk())
        .andExpect(view().name(
            "application/application-copy-case-search-too-many-results"));
  }

  @Test
  public void testGetCopyCaseSearchResults_WithResults() throws Exception {
    List<BaseApplication> baseApplications = List.of(new BaseApplication());

    when(applicationService.getCases(any(), any(), any())).thenReturn(baseApplications);

    this.mockMvc.perform(get("/application/copy-case/results")
            .sessionAttr(USER_DETAILS, user)
            .sessionAttr(CASE_SEARCH_CRITERIA, new CaseSearchCriteria()))
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-copy-case-search-results"));
  }

  @Test
  public void testSelectCopyCaseReferenceNumber_InvalidCaseRef() {
    Exception exception = assertThrows(ServletException.class, () ->
        this.mockMvc.perform(get("/application/copy-case/{caseRef}/confirm", "123")
            .sessionAttr(CASE_SEARCH_RESULTS, new ArrayList<BaseApplication>())
            .sessionAttr(APPLICATION_FORM_DATA, new ApplicationFormData())));

    assertInstanceOf(CaabApplicationException.class, exception.getCause());

    String expectedMessage = "Invalid copyCaseReferenceNumber supplied";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }

  @Test
  public void testSelectCopyCaseReferenceNumber_ValidCaseRef() throws Exception {
    List<BaseApplication> baseApplications =
        List.of(new BaseApplication().caseReferenceNumber("123"));

    when(applicationService.getCases(any(), any(), any())).thenReturn(baseApplications);

    ApplicationFormData applicationFormData = new ApplicationFormData();
    this.mockMvc.perform(get("/application/copy-case/{caseRef}/confirm", "123")
            .sessionAttr("caseSearchResults", baseApplications)
            .sessionAttr(APPLICATION_FORM_DATA, applicationFormData))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/search"));

    assertEquals("123", applicationFormData.getCopyCaseReferenceNumber());
  }

  private UserDetail buildUser() {
    return new UserDetail()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId");
  }
}
