package uk.gov.laa.ccms.caab.controller.notifications;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.notification.NotificationSearchValidator;
import uk.gov.laa.ccms.caab.service.CommonLookupService;
import uk.gov.laa.ccms.caab.service.NotificationService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.caab.service.UserService;
import uk.gov.laa.ccms.data.model.BaseOffice;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.BaseUser;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.data.model.UserDetails;
import uk.gov.laa.ccms.soa.gateway.model.Notification;
import uk.gov.laa.ccms.soa.gateway.model.Notifications;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
class ActionsAndNotificationsControllerTest {

  private static final UserDetail userDetails = new UserDetail()
      .userId(1)
      .userType("testUserType")
      .loginId("testLoginId")
      .provider(buildBaseProvider());
  @InjectMocks
  ActionsAndNotificationsController actionsAndNotificationsController;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ProviderService providerService;
  @Mock
  private CommonLookupService commonLookupService;
  @Mock
  private UserService userService;
  @Mock
  private NotificationSearchValidator notificationSearchValidator;
  @Autowired
  private WebApplicationContext webApplicationContext;
  private MockMvc mockMvc;

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

  private static NotificationSearchCriteria buildNotificationSearchCritieria() {
    NotificationSearchCriteria criteria = new NotificationSearchCriteria();
    criteria.setNotificationToDateDay("12");
    criteria.setNotificationToDateMonth("12");
    criteria.setNotificationToDateYear("2025");
    return criteria;
  }

  private static BaseProvider buildBaseProvider() {
    return new BaseProvider()
        .id(123)
        .addOfficesItem(
            new BaseOffice()
                .id(1)
                .name("Office 1"));
  }

  @BeforeEach
  void setUp() {
    mockMvc = standaloneSetup(actionsAndNotificationsController).build();
  }

  @Test
  void testNotificationsEndpointAndViewName_Data() throws Exception {
    Notifications notificationsMock = getNotificationsMock();

    Mockito.when(notificationService.getNotifications(any(), any(), any()))
        .thenReturn(Mono.just(notificationsMock));

    this.mockMvc.perform(get("/notifications").flashAttr("user", userDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("notifications/actions-and-notifications"));
  }

  @Test
  void testNotificationsEndpointAndViewName_NoData() throws Exception {

    Notifications notificationsMock = new Notifications()
        .content(new ArrayList<>());

    Mockito.when(notificationService.getNotifications(any(), any(), any()))
        .thenReturn(Mono.just(notificationsMock));

    this.mockMvc.perform(get("/notifications").flashAttr("user", userDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("notifications/actions-and-notifications-no-results"));

  }

  @Test
  void testNotificationsFromHeaderEndpointAndViewName_NoData() throws Exception {

    Notifications notificationsMock = new Notifications()
        .content(new ArrayList<>());

    Mockito.when(notificationService.getNotifications(any(), any(), any()))
        .thenReturn(Mono.just(notificationsMock));

    this.mockMvc.perform(get("/notifications?notification_type=all").flashAttr("user", userDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("notifications/actions-and-notifications-no-results"));

  }
  @Test
  void testNotificationTypePropagatesThroughToModel() throws Exception {

    Notifications notificationsMock = getNotificationsMock();

    Mockito.when(notificationService.getNotifications(any(), any(), any()))
        .thenReturn(Mono.just(notificationsMock));

    mockMvc.perform(get("/notifications?notification_type=A")
            .flashAttr("user", userDetails))
        .andDo(print())
        .andExpect(model().attribute("notificationSearchCriteria",
            hasProperty("notificationType", is("A"))))
        .andExpect(status().isOk());
  }

  @Test
  void testSortFieldsSetCorrectlyInModel() throws Exception {
    Notifications notificationsMock = getNotificationsMock();
    Mockito.when(notificationService.getNotifications(any(), any(), any()))
        .thenReturn(Mono.just(notificationsMock));
    mockMvc.perform(get("/notifications?notification_type=N&sort=subject,asc")
            .flashAttr("user", userDetails))
        .andDo(print())
        .andExpect(model().attribute("notificationSearchCriteria",
            hasProperty("notificationType", is("N"))))
        .andExpect(model().attribute("sortDirection", is("asc")))
        .andExpect(model().attribute("sortField", is("subject")))
        .andExpect(status().isOk());
  }

  @Test
  void testNotificationsSearchValidationForcesReturnToSearchPage() throws Exception {
    NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
    Map<String, Object> flashMap = new HashMap<>();
    flashMap.put("user", userDetails);
    flashMap.put("notificationSearchCriteria", criteria);

    ProviderDetail providerDetail = new ProviderDetail();
    List<ContactDetail> feeEarners = buildFeeEarners();

    CommonLookupDetail notificationTypes = new CommonLookupDetail();
    notificationTypes
        .addContentItem(
            new CommonLookupValueDetail().type("N").code("code n").description("description")
        );

    UserDetails baseUsers = new UserDetails()
        .addContentItem(new BaseUser()
            .userId(123)
            .userType("type1")
            .loginId("login1"));

    when(commonLookupService.getNotificationTypes()).thenReturn(Mono.just(notificationTypes));
    when(providerService.getProvider(userDetails.getProvider().getId()))
        .thenReturn(Mono.just(providerDetail));
    when(providerService.getAllFeeEarners(providerDetail)).thenReturn(feeEarners);
    when(userService.getUsers(any())).thenReturn(Mono.just(baseUsers));

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("notificationToDateYear", "invalid.input",
          "Your date range is invalid."
              + " Please amend your entry for the year field");
      return null;
    }).when(notificationSearchValidator).validate(any(), any());

    mockMvc.perform(post("/notifications/search")
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(model().attribute("criteria", hasProperty("notificationToDateDay")))
        .andExpect(model().hasErrors())
        .andExpect(forwardedUrl("notifications/actions-and-notifications-search"));
  }


  @Test
  void testGetNotificationsFromPhaseBanner_RetunsData() throws Exception {
    Notifications notificationsMock = getNotificationsMock();

    Mockito.when(notificationService.getNotifications(any(), any(), any()))
        .thenReturn(Mono.just(notificationsMock));

    this.mockMvc.perform(get("/notifications?notification_type=all")
            .sessionAttr("user", userDetails)
            .sessionAttr("notificationSearchCriteria", buildNotificationSearchCritieria())
        )
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("notifications/actions-and-notifications"));
  }

  private List<ContactDetail> buildFeeEarners() {
    List<ContactDetail> feeEarners = new ArrayList<>();
    feeEarners.add(new ContactDetail()
        .id(1)
        .name("FeeEarner1"));
    feeEarners.add(new ContactDetail()
        .id(2)
        .name("FeeEarner2"));
    return feeEarners;
  }
}
