package uk.gov.laa.ccms.caab.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import uk.gov.laa.ccms.data.model.UserDetails;
import uk.gov.laa.ccms.soa.gateway.model.NotificationSummary;

import java.util.stream.Stream;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

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

  private static Stream<Arguments> userDetailsAndNotificationParameters() {
    return Stream.of(
            Arguments.of(1, 2, 3, true, "2 Outstanding Actions (3 overdue)", "View Notifications (1 outstanding)"),
            Arguments.of(0, 2, 0, true, "2 Outstanding Actions (none overdue)", "View Notifications (none outstanding)"),
            Arguments.of(0, 0, 5, true, HomeController.NO_OUTSTANDING_ACTIONS, "View Notifications (none outstanding)")
    );
  }

  private static final UserDetails userDetails = new UserDetails()
          .userId(1)
          .userType("testUserType")
          .loginId("testLoginId");

  @ParameterizedTest
  @MethodSource("userDetailsAndNotificationParameters")
  public void testHomeRetrievesUserDetailsAndNotifications(int notifications, int standardActions, int overdueActions,
                                                           boolean expectedShowNotifications, String expectedActionMsg,
                                                           String expectedNotificationMsg) throws Exception {

    // Create a sample notification summary
    final NotificationSummary notificationSummary = new NotificationSummary()
            .notifications(notifications)
            .standardActions(standardActions)
            .overdueActions(overdueActions);

    // Mock the SOA Gateway service to return the notification summary
    when(soaGatewayService.getNotificationsSummary(userDetails.getLoginId(), userDetails.getUserType())).thenReturn(Mono.just(notificationSummary));

    this.mockMvc.perform(get("/").flashAttr("user", userDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("home"))
        .andExpect(model().attribute("user", userDetails))
        .andExpect(model().attribute("showNotifications", expectedShowNotifications))
        .andExpect(model().attribute("actionsMsg", expectedActionMsg))
        .andExpect(model().attribute("notificationsMsg", expectedNotificationMsg));

  }

  @Test
  public void testHomeHandlesNullNotifications() throws Exception {

    // Mock the SOA Gateway service to return the notification summary
    when(soaGatewayService.getNotificationsSummary(userDetails.getLoginId(), userDetails.getUserType())).thenReturn(Mono.empty());

    this.mockMvc.perform(get("/").flashAttr("user", userDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("home"))
        .andExpect(model().attribute("user", userDetails))
        .andExpect(model().attribute("showNotifications", false))
        .andExpect(model().attributeDoesNotExist("actionsMsg"))
        .andExpect(model().attributeDoesNotExist("notificationsMsg"));
  }
}

