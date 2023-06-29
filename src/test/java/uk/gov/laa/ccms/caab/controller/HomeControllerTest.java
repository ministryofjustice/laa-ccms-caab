package uk.gov.laa.ccms.caab.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
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
import uk.gov.laa.ccms.caab.service.SoaGatewayService;
import uk.gov.laa.ccms.data.model.UserResponse;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class HomeControllerTest {
  @Mock
  private DataService dataService;

  @Mock
  private SoaGatewayService soaGatewayService;

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
  public void testHomeRetrievesUserDetailsAndNotifications() throws Exception {
    final UserResponse userResponse = new UserResponse()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId");

    // Create a sample notification summary
    final NotificationSummary notificationSummary = new NotificationSummary()
            .notifications(1)
            .standardActions(2)
            .overdueActions(3);

    when(dataService.getUser(userResponse.getLoginId())).thenReturn(Mono.just(userResponse));

    // Mock the SOA Gateway service to return the notification summary
    when(soaGatewayService.getNotificationsSummary(userResponse.getLoginId(), userResponse.getUserType())).thenReturn(Mono.just(notificationSummary));

    this.mockMvc.perform(get("/").flashAttr("user", userResponse))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("home"))
        .andExpect(model().attribute("user", userResponse))
        .andExpect(model().attribute("showNotifications", true))
        .andExpect(model().attribute("actionsMsg", "2 Outstanding Actions (3 overdue)"))
        .andExpect(model().attribute("notificationsMsg", "View Notifications (1 outstanding)"));

    verify(dataService).getUser(userResponse.getLoginId());
  }

  @Test
  public void testHomeHandlesZeroOverdueNotifications() throws Exception {
    final UserResponse userResponse = new UserResponse()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId");

    // Create a sample notification summary
    final NotificationSummary notificationSummary = new NotificationSummary()
        .notifications(0)
        .standardActions(2)
        .overdueActions(0);

    when(dataService.getUser(userResponse.getLoginId())).thenReturn(Mono.just(userResponse));

    // Mock the SOA Gateway service to return the notification summary
    when(soaGatewayService.getNotificationsSummary(userResponse.getLoginId(), userResponse.getUserType())).thenReturn(Mono.just(notificationSummary));

    this.mockMvc.perform(get("/").flashAttr("user", userResponse))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("home"))
        .andExpect(model().attribute("user", userResponse))
        .andExpect(model().attribute("showNotifications", true))
        .andExpect(model().attribute("actionsMsg", "2 Outstanding Actions (none overdue)"))
        .andExpect(model().attribute("notificationsMsg", "View Notifications (none outstanding)"));

    verify(dataService).getUser(userResponse.getLoginId());
  }

  @Test
  public void testHomeHandlesNoActions() throws Exception {
    final UserResponse userResponse = new UserResponse()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId");

    // Create a sample notification summary
    final NotificationSummary notificationSummary = new NotificationSummary()
        .notifications(0)
        .standardActions(0)
        .overdueActions(5);

    when(dataService.getUser(userResponse.getLoginId())).thenReturn(Mono.just(userResponse));

    // Mock the SOA Gateway service to return the notification summary
    when(soaGatewayService.getNotificationsSummary(userResponse.getLoginId(), userResponse.getUserType())).thenReturn(Mono.just(notificationSummary));

    this.mockMvc.perform(get("/").flashAttr("user", userResponse))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("home"))
        .andExpect(model().attribute("user", userResponse))
        .andExpect(model().attribute("showNotifications", true))
        .andExpect(model().attribute("actionsMsg", HomeController.NO_OUTSTANDING_ACTIONS))
        .andExpect(model().attribute("notificationsMsg", "View Notifications (none outstanding)"));

    verify(dataService).getUser(userResponse.getLoginId());
  }

  @Test
  public void testHomeHandlesNullNotifications() throws Exception {
    final UserResponse userResponse = new UserResponse()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId");

    when(dataService.getUser(userResponse.getLoginId())).thenReturn(Mono.just(userResponse));

    // Mock the SOA Gateway service to return the notification summary
    when(soaGatewayService.getNotificationsSummary(userResponse.getLoginId(), userResponse.getUserType())).thenReturn(Mono.empty());

    this.mockMvc.perform(get("/").flashAttr("user", userResponse))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("home"))
        .andExpect(model().attribute("user", userResponse))
        .andExpect(model().attribute("showNotifications", false))
        .andExpect(model().attributeDoesNotExist("actionsMsg"))
        .andExpect(model().attributeDoesNotExist("notificationsMsg"));

    verify(dataService).getUser(userResponse.getLoginId());
  }
}


