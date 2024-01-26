package uk.gov.laa.ccms.caab.controller.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.NOTIFICATION_SEARCH_CRITERIA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.service.NotificationService;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.Notification;
import uk.gov.laa.ccms.soa.gateway.model.Notifications;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
class NotificationsSearchResultsControllerTest {

  @InjectMocks
  NotificationsSearchResultsController notificationsSearchResultsController;
  @Mock
  private NotificationService notificationService;

  @Autowired
  private WebApplicationContext webApplicationContext;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = standaloneSetup(notificationsSearchResultsController).build();
  }

  @Test
  void testGetSearchResults_returnsData() throws Exception {
    Notifications notificationsMock = getNotificationsMock();

    when(notificationService.getNotifications(any(), any(), any()))
        .thenReturn(Mono.just(notificationsMock));

    this.mockMvc.perform(get("/notifications/search-results")
            .sessionAttr("user", userDetails)
            .sessionAttr("notificationSearchCriteria", buildNotificationSearchCritieria())
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("notifications/actions-and-notifications"));
  }

  @Test
  void testGetSearchResults_noData() throws Exception {



    Notifications notificationsMock = new Notifications()
        .content(new ArrayList<>());

    when(notificationService.getNotifications(any(), any(), any()))
        .thenReturn(Mono.just(notificationsMock));

    this.mockMvc.perform(get("/notifications/search-results")
            .sessionAttr("user", userDetails)
            .sessionAttr("notificationSearchCriteria", buildNotificationSearchCritieria()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("notifications/actions-and-notifications-no-results"));
  }

  @Test
  void testSearchResults_WithoutAssignedUser_AndNoContent_ThrowsException() {
    Notifications notificationsMock = new Notifications();
    NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
    criteria.setAssignedToUserId("mildew@rot.com");

    Map<String, Object> flashMap = new HashMap<>();
    flashMap.put("user", userDetails);
    flashMap.put(NOTIFICATION_SEARCH_CRITERIA, criteria);
    when(notificationService.getNotifications(any(), any(), any()))
        .thenReturn(Mono.empty());

    Exception exception = assertThrows(Exception.class, () ->
        this.mockMvc.perform(get("/notifications/search-results")
            .flashAttrs(flashMap)));

    assertTrue(exception.getCause() instanceof CaabApplicationException);
    assertEquals("Error retrieving notifications", exception.getCause().getMessage());

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

  private static final UserDetail userDetails = new UserDetail()
      .userId(1)
      .userType("testUserType")
      .loginId("testLoginId");

  private static NotificationSearchCriteria buildNotificationSearchCritieria() {
    NotificationSearchCriteria criteria = new NotificationSearchCriteria();
    criteria.setNotificationToDateDay("12");
    criteria.setNotificationToDateMonth("12");
    criteria.setNotificationToDateYear("2022");
    return criteria;
  }
}
