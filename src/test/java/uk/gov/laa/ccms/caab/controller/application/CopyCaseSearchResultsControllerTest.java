package uk.gov.laa.ccms.caab.controller.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.util.ArrayList;
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
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.CopyCaseSearchCriteria;
import uk.gov.laa.ccms.caab.constants.SearchConstants;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.caab.service.SoaGatewayService;
import uk.gov.laa.ccms.data.model.CaseStatusLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetails;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class CopyCaseSearchResultsControllerTest {

    @Mock
    private SoaGatewayService soaGatewayService;

    @Mock
    private DataService dataService;

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
    public void testControllerHandlesNoCopyCaseStatus() throws Exception {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setTotalElements(0);

        when(soaGatewayService.getCases(any(), any(), any(), any(), any())).thenReturn(Mono.just(caseDetails));

        CopyCaseSearchCriteria copyCaseSearchCriteria = new CopyCaseSearchCriteria();
        this.mockMvc.perform(get("/application/copy-case-search/results")
                .sessionAttr("user", user)
                .sessionAttr("copyCaseSearchCriteria", copyCaseSearchCriteria))
            .andExpect(status().isOk())
            .andExpect(view().name("/application/application-copy-case-search-no-results"));

        verify(dataService).getCopyCaseStatus();
        verify(soaGatewayService).getCases(eq(copyCaseSearchCriteria), any(), any(), any(), any());
        assertNull(copyCaseSearchCriteria.getActualStatus());
    }

    @Test
    public void testCopyCaseSearchResults_NoResults() throws Exception {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setTotalElements(0);

        when(soaGatewayService.getCases(any(), any(), any(), any(), any())).thenReturn(Mono.just(caseDetails));
        String COPY_STATUS_CODE = "APP";
        when(dataService.getCopyCaseStatus()).thenReturn(new CaseStatusLookupValueDetail().code(COPY_STATUS_CODE));

        CopyCaseSearchCriteria copyCaseSearchCriteria = new CopyCaseSearchCriteria();
        this.mockMvc.perform(get("/application/copy-case-search/results")
                        .sessionAttr("user", user)
                        .sessionAttr("copyCaseSearchCriteria", copyCaseSearchCriteria))
                .andExpect(status().isOk())
                .andExpect(view().name("/application/application-copy-case-search-no-results"));

        verify(dataService).getCopyCaseStatus();
        verify(soaGatewayService).getCases(eq(copyCaseSearchCriteria), any(), any(), any(), any());
        assertEquals(COPY_STATUS_CODE, copyCaseSearchCriteria.getActualStatus());
    }

    @Test
    public void testCopyCaseSearchResults_WithTooManyResults() throws Exception {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setContent(new ArrayList<>());
        caseDetails.setTotalElements(300);

        when(soaGatewayService.getCases(any(), any(), any(), any(), any())).thenReturn(Mono.just(caseDetails));

        this.mockMvc.perform(get("/application/copy-case-search/results")
                        .sessionAttr("user", user)
                        .sessionAttr("copyCaseSearchCriteria", new CopyCaseSearchCriteria()))
                .andExpect(status().isOk())
                .andExpect(view().name("/application/application-copy-case-search-too-many-results"));
    }

    @Test
    public void testCopyCaseSearchResults_WithResults() throws Exception {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setContent(new ArrayList<>());
        caseDetails.setTotalElements(100);

        when(soaGatewayService.getCases(any(), any(), any(), any(), any())).thenReturn(Mono.just(caseDetails));

        this.mockMvc.perform(get("/application/copy-case-search/results")
                        .sessionAttr("user", user)
                        .sessionAttr("copyCaseSearchCriteria", new CopyCaseSearchCriteria()))
                .andExpect(status().isOk())
                .andExpect(view().name("/application/application-copy-case-search-results"));
    }

    @Test
    public void testCopyCaseSearch_Post() throws Exception {
        this.mockMvc.perform(post("/application/copy-case-search/results")
                        .sessionAttr("copyCaseSearchResults", new CaseDetails()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/application/TODO"));
    }

    private UserDetail buildUser() {
        return new UserDetail()
                .userId(1)
                .userType("testUserType")
                .loginId("testLoginId");
    }
}
