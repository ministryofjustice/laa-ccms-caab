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
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetails;

import java.util.ArrayList;

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
        ClientDetails clientDetails = new ClientDetails();
        clientDetails.setContent(new ArrayList<>());
        clientDetails.getContent().add(new ClientDetail()); // Add as many ClientDetail instances as needed

        this.mockMvc.perform(get("/application/client/{id}/confirm", 0)
                        .sessionAttr("user", user)
                        .sessionAttr("clientSearchResults", clientDetails))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("clientInformation"))
                .andExpect(view().name("/application/application-client-confirmation"));
    }

    @Test
    public void testClientConfirmed() throws Exception {
        ClientDetail clientDetail = new ClientDetail();

        this.mockMvc.perform(post("/application/client/confirmed")
                        .sessionAttr("user", user)
                        .sessionAttr("clientInformation", clientDetail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("TODO"));
    }

    private UserDetail buildUser() {
        return new UserDetail()
                .userId(1)
                .userType("testUserType")
                .loginId("testLoginId");
    }
}
