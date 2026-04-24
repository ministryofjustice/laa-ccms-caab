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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.NotificationConstants.SORT_DIRECTION;
import static uk.gov.laa.ccms.caab.constants.NotificationConstants.SORT_FIELD;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.NOTIFICATIONS_SEARCH_RESULTS;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.service.NotificationService;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.NotificationInfo;
import uk.gov.laa.ccms.data.model.Notifications;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration
@WebAppConfiguration
class NotificationsSearchResultsControllerTest {

  @InjectMocks NotificationsSearchResultsController notificationsSearchResultsController;
  @Mock private NotificationService notificationService;

  @Autowired private WebApplicationContext webApplicationContext;
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

    this.mockMvc
        .perform(
            get("/notifications/search-results")
                .sessionAttr("user", userDetails)
                .queryParam("page", "0")
                .queryParam("size", "10")
                .queryParam("pageSort", "dateAssigned,asc")
                .sessionAttr("notificationSearchCriteria", buildNotificationSearchCriteria()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("notifications/actions-and-notifications"));
  }

  @Test
  void testGetSearchResults_noData() throws Exception {
    Notifications notificationsMock = new Notifications().content(new ArrayList<>());

    when(notificationService.getNotifications(any(), anyInt(), any(), any()))
        .thenReturn(Mono.just(notificationsMock));

    this.mockMvc
        .perform(
            get("/notifications/search-results")
                .sessionAttr("user", userDetails)
                .queryParam("page", "0")
                .queryParam("size", "10")
                .queryParam("pageSort", "dateAssigned,asc")
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

    Exception exception =
        assertThrows(
            Exception.class,
            () ->
                this.mockMvc.perform(
                    get("/notifications/search-results")
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .queryParam("pageSort", "dateAssigned,asc")
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

    this.mockMvc
        .perform(
            get("/notifications/search-results")
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

    this.mockMvc
        .perform(
            get("/notifications/search-results")
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

  @Test
  void testGetSearchResults_UsesCachedResults_WhenParamsMissing_Redirects() throws Exception {
    Notifications cachedNotifications = getNotificationsMock();
    NotificationSearchCriteria criteria = buildNotificationSearchCriteria();
    criteria.setPage(2);
    criteria.setSize(10);
    criteria.setSort("dateAssigned,asc");

    this.mockMvc
        .perform(
            get("/notifications/search-results")
                .sessionAttr("user", userDetails)
                // No query parameters -> should redirect
                .sessionAttr("notificationSearchCriteria", criteria)
                .sessionAttr("notificationsSearchResults", cachedNotifications))
        .andExpect(status().is3xxRedirection())
        .andExpect(
            redirectedUrl(
                "/notifications/search-results?page=2&size=10&pageSort=dateAssigned,asc"));
  }

  @Test
  void testGetSearchResults_Redirects_WhenParamsMissing() throws Exception {
    NotificationSearchCriteria criteria = buildNotificationSearchCriteria();
    criteria.setPage(3);
    criteria.setSize(20);
    criteria.setSort("caseReference,desc");

    this.mockMvc
        .perform(
            get("/notifications/search-results")
                .sessionAttr("user", userDetails)
                .sessionAttr("notificationSearchCriteria", criteria))
        .andExpect(status().is3xxRedirection())
        .andExpect(
            redirectedUrl(
                "/notifications/search-results?page=3&size=20&pageSort=caseReference,desc"));
  }

  @Test
  void testGetSearchResults_WithEmptySortParam_DoesNotThrowException() throws Exception {
    Notifications cachedNotifications = getNotificationsMock();
    NotificationSearchCriteria criteria = buildNotificationSearchCriteria();
    criteria.setSort("otherField,asc"); // Different from default
    criteria.setPage(0);
    criteria.setSize(10);

    // Default to dateAssigned,asc when empty
    Notifications defaultSortNotifications = getNotificationsMock();
    defaultSortNotifications.getContent().get(0).setNotificationId("default-sort-id");

    when(notificationService.getNotifications(any(), anyInt(), any(), any()))
        .thenReturn(Mono.just(defaultSortNotifications));

    this.mockMvc
        .perform(
            get("/notifications/search-results")
                .sessionAttr("user", userDetails)
                .queryParam(
                    "pageSort", "") // Invalid sort in request -> will default to dateAssigned,asc
                .queryParam("page", "0")
                .queryParam("size", "10")
                .sessionAttr("notificationSearchCriteria", criteria))
        .andExpect(status().isOk())
        .andExpect(view().name("notifications/actions-and-notifications"))
        .andExpect(model().attribute(SORT_FIELD, "dateAssigned"))
        .andExpect(model().attribute(SORT_DIRECTION, "asc"))
        .andExpect(model().attribute(NOTIFICATIONS_SEARCH_RESULTS, defaultSortNotifications));

    verify(notificationService).getNotifications(any(), anyInt(), any(), any());
  }

  @Test
  void testGetSearchResults_FetchesNewResults_WhenPageChanges() throws Exception {
    Notifications cachedNotifications = getNotificationsMock();
    NotificationSearchCriteria criteria = buildNotificationSearchCriteria();
    criteria.setPage(0);
    criteria.setSize(10);
    criteria.setSort("dateAssigned,asc");

    Notifications nextPageNotifications = getNotificationsMock();
    nextPageNotifications.getContent().get(0).setNotificationId("next-page-id");

    // Expect call for page 1
    when(notificationService.getNotifications(any(), anyInt(), eq(1), eq(10)))
        .thenReturn(Mono.just(nextPageNotifications));

    // Clear session criteria to simulate fresh binding with new page 1
    this.mockMvc
        .perform(
            get("/notifications/search-results")
                .sessionAttr("user", userDetails)
                .queryParam("page", "1") // Change page to 1
                .queryParam("size", "10")
                .queryParam("pageSort", "dateAssigned,asc")
                // By providing a criteria with page 0, and a request with page 1,
                // the controller should detect the change even if Spring binds page=1 into
                // criteria.
                .sessionAttr(NOTIFICATION_SEARCH_CRITERIA, criteria)
                .sessionAttr("notificationsSearchResults", cachedNotifications))
        .andExpect(status().isOk())
        .andExpect(view().name("notifications/actions-and-notifications"))
        .andExpect(model().attribute(NOTIFICATIONS_SEARCH_RESULTS, nextPageNotifications));

    verify(notificationService).getNotifications(any(), anyInt(), eq(1), eq(10));
  }

  private static Notifications getNotificationsMock() {
    return new Notifications()
        .addContentItem(
            new NotificationInfo()
                .user(new UserDetail().loginId("user1").userType("user1"))
                .notificationId("234")
                .notificationType("N"));
  }

  private static final UserDetail userDetails =
      new UserDetail()
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
