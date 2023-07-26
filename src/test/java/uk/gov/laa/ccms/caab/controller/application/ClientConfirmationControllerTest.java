package uk.gov.laa.ccms.caab.controller.application;

import jakarta.servlet.http.HttpSession;
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
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;

import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class ClientConfirmationControllerTest {

    @Mock
    private HttpSession httpSession;

    @InjectMocks
    private ClientConfirmationController clientConfirmationController;

    private MockMvc mockMvc;

    private UserDetail user;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    public void setup() {
        mockMvc = standaloneSetup(clientConfirmationController).build();
        this.user = buildUser();
    }

    @Test
    public void testClientConfirm() throws Exception {
        ClientDetails clientSearchResults = new ClientDetails();
        clientSearchResults.setContent(new ArrayList<>());
        clientSearchResults.getContent().add(new ClientDetail());

        this.mockMvc.perform(get("/application/client/{id}/confirm", 0)
                        .sessionAttr("user", user)
                        .sessionAttr("clientSearchResults", clientSearchResults))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("clientInformation"))
                .andExpect(view().name("/application/application-client-confirmation"));
    }

    @Test
    public void testClientConfirmed() throws Exception {
        ApplicationDetails applicationDetails = new ApplicationDetails();
        ClientDetails clientSearchResults = new ClientDetails();
        clientSearchResults.setContent(new ArrayList<>());
        clientSearchResults.getContent().add(new ClientDetail());

        int confirmedClientId = 0; // The confirmedClientId value from the form

        this.mockMvc.perform(post("/application/client/confirmed")
                        .param("confirmedClientId", String.valueOf(confirmedClientId)) // Pass the confirmedClientId as a form parameter
                        .sessionAttr("user", user)
                        .sessionAttr("applicationDetails", applicationDetails)
                        .sessionAttr("clientSearchResults", clientSearchResults)) // Use the correct attribute name for clientSearchResults
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/application/agreement"));
    }

    private UserDetail buildUser() {
        return new UserDetail()
                .userId(1)
                .userType("testUserType")
                .loginId("testLoginId");
    }
}
