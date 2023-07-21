package uk.gov.laa.ccms.caab.controller.application;

import jakarta.validation.Valid;
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
import uk.gov.laa.ccms.caab.bean.ClientSearchDetails;
import uk.gov.laa.ccms.caab.bean.ClientSearchDetailsValidator;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.caab.service.SoaGatewayService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class ClientSearchControllerTest {

    @Mock
    private DataService dataService;

    @Mock
    private SoaGatewayService soaGatewayService;

    @Mock
    private ClientSearchDetailsValidator clientSearchDetailsValidator;

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
        ClientSearchController clientSearchController = new ClientSearchController(dataService, soaGatewayService, clientSearchDetailsValidator);
        ClientSearchDetails clientSearchDetails = clientSearchController.getClientSearchDetails();
        assertNotNull(clientSearchDetails);
    }

    @Test
    public void testClientSearch_Get() throws Exception {
        this.mockMvc.perform(get("/application/client-search")
                        .flashAttr("applicationDetails", new ApplicationDetails())
                        .sessionAttr("clientSearchDetails", new ClientSearchDetails()))
                .andExpect(status().isOk())
                .andExpect(view().name("/application/application-client-search"));

        verify(dataService).getGenders();
        verify(dataService).getUniqueIdentifierTypes();
    }

    @Test
    public void testClientSearch_Post_WithErrors() throws Exception {
        final ClientSearchDetails clientSearchDetails = new ClientSearchDetails();

        doAnswer(invocation -> {
            Errors errors = (Errors) invocation.getArguments()[1];

            errors.rejectValue("forename", "required.forename",
                    "Please complete 'First name'.");
            return null;
        }).when(clientSearchDetailsValidator).validate(any(), any());
        this.mockMvc.perform(post("/application/client-search")
                        .flashAttr("clientSearchDetails", clientSearchDetails)
                        .sessionAttr("user", user))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("/application/application-client-search"));

        verify(dataService).getGenders();
        verify(dataService).getUniqueIdentifierTypes();
    }

    @Test
    public void testClientSearch_Post_Successful() throws Exception {
        ClientSearchDetails clientSearchDetails = buildClientSearchDetails();

        ClientDetail client = new ClientDetail();
        List<ClientDetail> clients = new ArrayList<>();
        clients.add(client);
        ClientDetails mockClientDetails = new ClientDetails();
        mockClientDetails.setClients(clients);

        when(soaGatewayService.getClients(clientSearchDetails, user.getLoginId(), user.getUserType()))
                .thenReturn(Mono.just(mockClientDetails));

        this.mockMvc.perform(post("/application/client-search")
                        .flashAttr("clientSearchDetails", clientSearchDetails)
                        .sessionAttr("user", user))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/application/client-search/results"))
                .andExpect(flash().attributeExists("clientSearchResults"));

        verify(soaGatewayService).getClients(clientSearchDetails, user.getLoginId(), user.getUserType());
    }

    @Test
    public void testClientSearch_Post_NoResults() throws Exception {
        ClientSearchDetails clientSearchDetails = buildClientSearchDetails();

        when(clientSearchDetailsValidator.supports(ClientSearchDetails.class)).thenReturn(true);

        when(soaGatewayService.getClients(clientSearchDetails, user.getLoginId(), user.getUserType()))
                .thenReturn(Mono.just(new ClientDetails())); // Empty client search results

        this.mockMvc.perform(post("/application/client-search")
                        .flashAttr("clientSearchDetails", clientSearchDetails)
                        .sessionAttr("user", user))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/application/client-search/no-results"));

        verify(soaGatewayService).getClients(clientSearchDetails, user.getLoginId(), user.getUserType());
    }

    private UserDetail buildUser() {
        return new UserDetail()
                .userId(1)
                .userType("testUserType")
                .loginId("testLoginId");
    }

    private ClientSearchDetails buildClientSearchDetails() {
        ClientSearchDetails clientSearchDetails = new ClientSearchDetails();
        clientSearchDetails.setForename("Test");
        clientSearchDetails.setSurname("User");
        clientSearchDetails.setDobDay("01");
        clientSearchDetails.setDobMonth("01");
        clientSearchDetails.setDobYear("2000");
        return clientSearchDetails;
    }
}
