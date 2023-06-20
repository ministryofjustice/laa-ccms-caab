package uk.gov.laa.ccms.caab.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

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
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.data.model.UserResponse;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class HomeControllerTest {
  @Mock
  private DataService dataService;

  @InjectMocks
  private HomeController homeController;

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(homeController).build();
  }

  @Test
  public void testHomeRetrievesUserDetails() throws Exception {
    final UserResponse userResponse = new UserResponse()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId");

    when(dataService.getUser(userResponse.getLoginId())).thenReturn(Mono.just(userResponse));

    this.mockMvc.perform(get("/").flashAttr("user", userResponse))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(model().attribute("user", userResponse));

    verify(dataService).getUser(userResponse.getLoginId());
  }
}
