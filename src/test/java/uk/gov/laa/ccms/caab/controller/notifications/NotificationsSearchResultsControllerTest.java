package uk.gov.laa.ccms.caab.controller.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import org.mockito.ArgumentCaptor;
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
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.NotificationInfo;
import uk.gov.laa.ccms.data.model.Notifications;
import uk.gov.laa.ccms.data.model.UserDetail;

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

    when(notificationService.getNotifications(any(), anyInt(), any(), any()))
        .thenReturn(Mono.just(notificationsMock));

    this.mockMvc.perform(get("/notifications/search-results")
            .sessionAttr("user", userDetails)
            .sessionAttr("notificationSearchCriteria", buildNotificationSearchCriteria())
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("notifications/actions-and-notifications"));
  }

  @Test
  void testGetSearchResults_noData() throws Exception {
    Notifications notificationsMock = new Notifications()
        .content(new ArrayList<>());

    when(notificationService.getNotifications(any(), anyInt(), any(), any()))
        .thenReturn(Mono.just(notificationsMock));

    this.mockMvc.perform(get("/notifications/search-results")
            .sessionAttr("user", userDetails)
            .sessionAttr("notificationSearchCriteria", buildNotificationSearchCriteria()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("notifications/actions-and-notifications-no-results"));
  }

  @Test
  void testSearchResults_WithoutAssignedUser_AndNoContent_ThrowsException() {
    NotificationSearchCriteria criteria = buildNotificationSearchCriteria();
    criteria.setAssignedToUserId("mildew@rot.com");

    Map<String, Object> flashMap = new HashMap<>();
    flashMap.put("user", userDetails);
    flashMap.put(NOTIFICATION_SEARCH_CRITERIA, criteria);
    when(notificationService.getNotifications(any(), anyInt(), any(), any()))
        .thenReturn(Mono.empty());

    Exception exception = assertThrows(Exception.class, () ->
        this.mockMvc.perform(get("/notifications/search-results")
            .flashAttrs(flashMap)));

    assertInstanceOf(CaabApplicationException.class, exception.getCause());
    assertEquals("Error retrieving notifications", exception.getCause().getMessage());

  }

  @Test
  void testGetSearchResults_SortIsApplied() throws Exception {
    Notifications notificationsMock = getNotificationsMock();

    when(notificationService.getNotifications(any(), anyInt(), any(), any()))
        .thenReturn(Mono.just(notificationsMock));

    NotificationSearchCriteria searchCriteria = buildNotificationSearchCriteria();
    searchCriteria.setSort("assignedDate,asc");

    ArgumentCaptor<NotificationSearchCriteria> criteriaArg =
        ArgumentCaptor.forClass(NotificationSearchCriteria.class);

    this.mockMvc.perform(get("/notifications/search-results")
            .sessionAttr("user", userDetails)
            .queryParam("pageSort", "assignedDate,asc")
            .sessionAttr("notificationSearchCriteria", searchCriteria))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("notifications/actions-and-notifications"));

    verify(notificationService).getNotifications(criteriaArg.capture(), anyInt(), any(), any());

    assertEquals("assignedDate,asc", criteriaArg.getValue().getSort());
  }

  @Test
  void testGetSearchResults_WhenSortCriteriaChanged_ResetPage() throws Exception {
    Notifications notificationsMock = getNotificationsMock();

    when(notificationService.getNotifications(any(), anyInt(), any(), any()))
        .thenReturn(Mono.just(notificationsMock));

    NotificationSearchCriteria searchCriteria = buildNotificationSearchCriteria();
    searchCriteria.setSort("assignedDate,asc");

    this.mockMvc.perform(get("/notifications/search-results")
            .sessionAttr("user", userDetails)
            .queryParam("page", "2")
            .queryParam("size", "10")
            .queryParam("pageSort", "assignedDate,desc")
            .sessionAttr("notificationSearchCriteria", searchCriteria))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("notifications/actions-and-notifications"));

    verify(notificationService).getNotifications(any(), anyInt(), eq(0), eq(10));
  }

  private static Notifications getNotificationsMock() {
    return new Notifications()
        .addContentItem(
            new NotificationInfo()
                .user(new UserDetail()
                    .loginId("user1")
                    .userType("user1"))
                .notificationId("234")
                .notificationType("N"));
  }

  private static final UserDetail userDetails = new UserDetail()
      .userId(1)
      .provider(new BaseProvider().id(10))
      .userType("testUserType")
      .loginId("testLoginId")
      .provider(new BaseProvider().id(1));

  private static NotificationSearchCriteria buildNotificationSearchCriteria() {
    NotificationSearchCriteria criteria = new NotificationSearchCriteria();
    criteria.setNotificationToDate("12/12/2022");
    return criteria;
  }
}
