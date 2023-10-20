package uk.gov.laa.ccms.caab.controller.application.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.service.CommonLookupService;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class ClientSearchControllerTest {

  @Mock
  private CommonLookupService commonLookupService;
  
  @Mock
  private ClientSearchCriteriaValidator clientSearchCriteriaValidator;

  @InjectMocks
  private ClientSearchController clientSearchController;

  private MockMvc mockMvc;

  private UserDetail user;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(clientSearchController).build();
    this.user = buildUser();
  }

  @Test
  public void testGetClientSearchDetails() {
    ClientSearchController clientSearchController =
        new ClientSearchController(commonLookupService, clientSearchCriteriaValidator);
    ClientSearchCriteria clientSearchCriteria = clientSearchController.getClientSearchDetails();
    assertNotNull(clientSearchCriteria);
  }

  @Test
  public void testClientSearch_Get() throws Exception {
    when(commonLookupService.getGenders()).thenReturn(Mono.empty());
    when(commonLookupService.getUniqueIdentifierTypes()).thenReturn(Mono.empty());

    this.mockMvc.perform(get("/application/client/search")
            .flashAttr("applicationDetails", new ApplicationDetails())
            .sessionAttr("clientSearchCriteria", new ClientSearchCriteria()))
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-client-search"));

    verify(commonLookupService).getGenders();
    verify(commonLookupService).getUniqueIdentifierTypes();
  }

  @Test
  public void testClientSearch_Post_WithErrors() throws Exception {
    final ClientSearchCriteria clientSearchCriteria = new ClientSearchCriteria();

    when(commonLookupService.getGenders()).thenReturn(Mono.empty());
    when(commonLookupService.getUniqueIdentifierTypes()).thenReturn(Mono.empty());

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];

      errors.rejectValue("forename", "required.forename",
          "Please complete 'First name'.");
      return null;
    }).when(clientSearchCriteriaValidator).validate(any(), any());
    this.mockMvc.perform(post("/application/client/search")
            .flashAttr("clientSearchCriteria", clientSearchCriteria)
            .sessionAttr("user", user))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-client-search"));

    verify(commonLookupService).getGenders();
    verify(commonLookupService).getUniqueIdentifierTypes();
  }

  @Test
  public void testClientSearch_Post_Successful() throws Exception {
    ClientSearchCriteria clientSearchCriteria = buildClientSearchDetails();

    this.mockMvc.perform(post("/application/client/search")
            .flashAttr("clientSearchCriteria", clientSearchCriteria)
            .sessionAttr("user", user))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/results"));
  }

  private UserDetail buildUser() {
    return new UserDetail()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId");
  }

  private ClientSearchCriteria buildClientSearchDetails() {
    ClientSearchCriteria clientSearchCriteria = new ClientSearchCriteria();
    clientSearchCriteria.setForename("Test");
    clientSearchCriteria.setSurname("User");
    clientSearchCriteria.setDobDay("01");
    clientSearchCriteria.setDobMonth("01");
    clientSearchCriteria.setDobYear("2000");
    return clientSearchCriteria;
  }
}
