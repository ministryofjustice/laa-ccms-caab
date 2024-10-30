package uk.gov.laa.ccms.caab.controller.provider;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

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
import uk.gov.laa.ccms.caab.service.UserService;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class ProviderControllerTest {

  @Mock
  UserService userService;

  @InjectMocks
  ProviderController providerController;

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(providerController).build();
  }

  @Test
  public void testGetProviderSwitchScreen() throws Exception {

    BaseProvider providerFirm = new BaseProvider().name("providerFirm");

    UserDetail userDetails = new UserDetail()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId")
        .firms(List.of(providerFirm));

    this.mockMvc.perform(get("/provider-switch")
            .sessionAttr("user", userDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("provider/provider-switch"))
        .andExpect(model().attribute("userFirms", List.of(providerFirm)));

  }

  @Test
  public void testUpdateProviderOptions() throws Exception {

    BaseProvider providerFirm = new BaseProvider().name("providerFirm");

    UserDetail userDetails = new UserDetail()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId")
        .firms(List.of(providerFirm));

    this.mockMvc.perform(get("/provider-switch")
            .sessionAttr("user", userDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("provider/provider-switch"))
        .andExpect(model().attribute("userFirms", List.of(providerFirm)));

  }


}
