package uk.gov.laa.ccms.caab.controller;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.service.NotificationService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.Notification;
import uk.gov.laa.ccms.soa.gateway.model.Notifications;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
class ActionsAndNotificationsControllerTest {

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  ActionsAndNotificationsController actionsAndNotificationsController;

  @Autowired
  private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  private static final UserDetail userDetails = new UserDetail()
      .userId(1)
      .userType("testUserType")
      .loginId("testLoginId");

  @BeforeEach
  void setUp() {
    mockMvc = standaloneSetup(actionsAndNotificationsController).build();
  }

  @Test
  void testNotificationsEndpointAndViewName_Data() throws Exception {
    Notifications notificationsMock = getNotificationsMock();

    Mockito.when(notificationService.getNotifications(any(), any(), any())).thenReturn(Mono.just(notificationsMock));

    this.mockMvc.perform(get("/notifications").flashAttr("user", userDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("notifications/actions-and-notifications"));
  }

  @Test
  void testNotificationsEndpointAndViewName_NoData() throws Exception {

    Notifications notificationsMock = new Notifications()
        .content(new ArrayList<>());


    Mockito.when(notificationService.getNotifications(any(), any(), any())).thenReturn(Mono.just(notificationsMock));

    this.mockMvc.perform(get("/notifications").flashAttr("user", userDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("notifications/actions-and-notifications-no-results"));

  }

  @Test
  void testNotificationTypePropagatesThroughToModel() throws Exception {

    Notifications notificationsMock = getNotificationsMock();

    Mockito.when(notificationService.getNotifications(any(), any(), any())).thenReturn(Mono.just(notificationsMock));

    mockMvc.perform(get("/notifications?notification_type=A")
        .flashAttr("user", userDetails))
        .andDo(print())
        .andExpect(model().attribute("notificationSearchCriteria",
            hasProperty("notificationType", is("A"))))
        .andExpect(status().isOk());
  }

  private static Notifications getNotificationsMock() {
    return new Notifications()
        .addContentItem(
            new Notification()
                .user(new uk.gov.laa.ccms.soa.gateway.model.UserDetail()
                    .userLoginId("user1")
                    .userType("user1"))
                .notificationId("234")
                .notificationType("N"));

  }

  @Test
  void testSortFieldsSetCorrectlyInModel() throws Exception {
    Notifications notificationsMock = getNotificationsMock();
    Mockito.when(notificationService.getNotifications(any(), any(), any())).thenReturn(Mono.just(notificationsMock));
    mockMvc.perform(get("/notifications?notification_type=N&sort=subject,asc")
            .flashAttr("user", userDetails))
        .andDo(print())
        .andExpect(model().attribute("notificationSearchCriteria",
            hasProperty("notificationType", is("N"))))
        .andExpect(model().attribute("sortDirection", is("asc")))
        .andExpect(model().attribute("sortField", is("subject")))
        .andExpect(status().isOk());

  }
}
