package uk.gov.laa.ccms.caab.controller.application.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;

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
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.constants.SearchConstants;
import uk.gov.laa.ccms.caab.controller.client.ClientSearchResultsController;
import uk.gov.laa.ccms.caab.mapper.ResultDisplayMapper;
import uk.gov.laa.ccms.caab.model.ClientResultsDisplay;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.data.model.ClientDetails;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class ClientSearchResultsControllerTest {

  @Mock
  private ClientService clientService;

  @Mock
  private ResultDisplayMapper resultDisplayMapper;

  @Mock
  private SearchConstants searchConstants;

  @InjectMocks
  private ClientSearchResultsController clientSearchResultsController;

  private MockMvc mockMvc;

  private UserDetail user;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(clientSearchResultsController).build();
    this.user = buildUser();

    when(searchConstants.getMaxSearchResultsClients()).thenReturn(200);
  }

  @Test
  public void testClientSearchResults_NoResults() throws Exception {
    ClientDetails clientDetails = new ClientDetails();
    clientDetails.setTotalElements(0);

    when(clientService.getClients(any(), any(), any())).thenReturn(
        Mono.just(clientDetails));

    this.mockMvc.perform(get("/application/client/results")
            .sessionAttr("user", user)
            .sessionAttr("clientSearchCriteria", new ClientSearchCriteria()))
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-client-search-no-results"));
  }

  @Test
  public void testClientSearchResults_WithManyResults() throws Exception {
    ClientDetails clientDetails = new ClientDetails();
    clientDetails.setContent(new ArrayList<>());
    clientDetails.setTotalElements(300);

    when(clientService.getClients(any(), any(), any())).thenReturn(
        Mono.just(clientDetails));

    this.mockMvc.perform(get("/application/client/results")
            .sessionAttr("user", user)
            .sessionAttr("clientSearchCriteria", new ClientSearchCriteria()))
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-client-search-too-many-results"));
  }

  @Test
  public void testClientSearchResults_WithResults() throws Exception {
    ClientDetails clientDetails = new ClientDetails();
    clientDetails.setContent(new ArrayList<>());
    clientDetails.setTotalElements(100);

    when(clientService.getClients(any(), any(), any())).thenReturn(
        Mono.just(clientDetails));

    this.mockMvc.perform(get("/application/client/results")
            .sessionAttr("user", user)
            .sessionAttr("clientSearchCriteria", new ClientSearchCriteria()))
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-client-search-results"));
  }

  @Test
  public void testClientSearch_Post() throws Exception {
    this.mockMvc.perform(post("/application/client/results")
            .sessionAttr(APPLICATION_FORM_DATA, new ApplicationFormData())
            .sessionAttr("clientSearchResults", new ClientResultsDisplay()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/agreement"));
  }

  @Test
  public void testClientSearchResults_NullResults() throws Exception {
    when(clientService.getClients(any(), any(), any())).thenReturn(Mono.empty());

    this.mockMvc.perform(get("/application/client/results")
            .sessionAttr("user", user)
            .sessionAttr("clientSearchCriteria", new ClientSearchCriteria()))
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-client-search-no-results"));
  }

  @Test
  public void testClientSearchResults_ZeroTotalElements() throws Exception {
    ClientDetails clientDetails = new ClientDetails();
    clientDetails.setContent(new ArrayList<>());
    clientDetails.setTotalElements(0);

    when(clientService.getClients(any(), any(), any())).thenReturn(
        Mono.just(clientDetails));

    this.mockMvc.perform(get("/application/client/results")
            .sessionAttr("user", user)
            .sessionAttr("clientSearchCriteria", new ClientSearchCriteria()))
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-client-search-no-results"));
  }

  private UserDetail buildUser() {
    return new UserDetail()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId");
  }
}
