package uk.gov.laa.ccms.caab.controller.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.HamcrestCondition.matching;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DOCUMENT_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_NOTIFICATION_TYPE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.NOTIFICATIONS_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.util.ApplicationDetailUtils.buildFullApplicationDetail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.bean.notification.NotificationAttachmentUploadFormData;
import uk.gov.laa.ccms.caab.bean.notification.NotificationResponseFormData;
import uk.gov.laa.ccms.caab.bean.validators.notification.NotificationAttachmentUploadValidator;
import uk.gov.laa.ccms.caab.bean.validators.notification.NotificationResponseValidator;
import uk.gov.laa.ccms.caab.bean.validators.notification.NotificationSearchValidator;
import uk.gov.laa.ccms.caab.constants.SendBy;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.NotificationAttachmentMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.BaseNotificationAttachmentDetail;
import uk.gov.laa.ccms.caab.model.NotificationAttachmentDetail;
import uk.gov.laa.ccms.caab.model.NotificationAttachmentDetails;
import uk.gov.laa.ccms.caab.service.AvScanService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.NotificationService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.caab.service.UserService;
import uk.gov.laa.ccms.data.model.BaseOffice;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.BaseUser;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.Document;
import uk.gov.laa.ccms.data.model.Notification;
import uk.gov.laa.ccms.data.model.NotificationInfo;
import uk.gov.laa.ccms.data.model.Notifications;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.data.model.UserDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientTransactionResponse;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
class ActionsAndNotificationsControllerTest {

  private static final UserDetail userDetails =
      new UserDetail()
          .userId(1)
          .userType("testUserType")
          .loginId("testLoginId")
          .provider(buildBaseProvider());
  @InjectMocks ActionsAndNotificationsController actionsAndNotificationsController;
  @Mock private NotificationService notificationService;
  @Mock private ProviderService providerService;
  @Mock private LookupService lookupService;
  @Mock private UserService userService;
  @Mock private NotificationSearchValidator notificationSearchValidator;
  @Mock private NotificationAttachmentMapper notificationAttachmentMapper;
  @Mock private NotificationAttachmentUploadValidator notificationAttachmentUploadValidator;
  @Mock private NotificationResponseValidator notificationResponseValidator;
  @Mock private AvScanService avScanService;
  @Autowired private WebApplicationContext webApplicationContext;
  private MockMvc oldmockMvc;
  private MockMvcTester mockMvc;

  private static Notifications getNotificationsMock() {
    return new Notifications().addContentItem(buildNotificationInfo());
  }

  private static NotificationInfo buildNotificationInfo() {
    return new NotificationInfo()
        .user(new UserDetail().loginId("user1").userType("user1"))
        .notificationId("234")
        .notificationType("N");
  }

  private static Notification buildNotification() {
    return new Notification()
        .user(new UserDetail().loginId("user1").userType("user1"))
        .notificationId("234")
        .notificationType("N")
        .attachedDocuments(buildAttachedDocuments())
        .uploadedDocuments(buildUploadedDocuments());
  }

  private static List<Document> buildUploadedDocuments() {
    return List.of(new Document().documentId("890").channel("P").documentType("TST_DOC"));
  }

  private static List<uk.gov.laa.ccms.soa.gateway.model.Document> buildUploadedDocumentsSoa() {
    return List.of(
        new uk.gov.laa.ccms.soa.gateway.model.Document()
            .documentId("890")
            .channel("P")
            .documentType("TST_DOC"));
  }

  private static List<Document> buildAttachedDocuments() {
    return List.of(new Document().documentId("567").channel("E").documentType("TST_DOC"));
  }

  private static List<uk.gov.laa.ccms.soa.gateway.model.Document> buildAttachedDocumentsSoa() {
    return List.of(
        new uk.gov.laa.ccms.soa.gateway.model.Document()
            .documentId("567")
            .channel("E")
            .documentType("TST_DOC"));
  }

  private static NotificationSearchCriteria buildNotificationSearchCritieria() {
    NotificationSearchCriteria criteria = new NotificationSearchCriteria();
    criteria.setNotificationToDate("12/12/2025");
    return criteria;
  }

  private static BaseProvider buildBaseProvider() {
    return new BaseProvider().id(123).addOfficesItem(new BaseOffice().id(1).name("Office 1"));
  }

  @BeforeEach
  void setUp() {
    oldmockMvc = standaloneSetup(actionsAndNotificationsController).build();
    mockMvc =
        MockMvcTester.create(
            MockMvcBuilders.standaloneSetup(actionsAndNotificationsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build());
    when(notificationSearchValidator.supports(any())).thenReturn(true);
  }

  @Nested
  @DisplayName("GET: /notifications/search")
  class GetNotificationsSearchTests {

    @Test
    @DisplayName("Should redirect to search results with notifications")
    void shouldRedirectToSearchResultsWithNotifications() {
      Notifications notificationsMock = getNotificationsMock();

      Mockito.when(notificationService.getNotifications(any(), anyInt(), any(), any()))
          .thenReturn(Mono.just(notificationsMock));

      assertThat(
              mockMvc.perform(
                  get("/notifications/search?notification_type=N").flashAttr("user", userDetails)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/notifications/search-results");
    }

    @Test
    @DisplayName("Should redirect to search results with all types of notifications")
    void shouldRedirectToSearchResultsWithAllTypesOfNotifications() {
      NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);
      flashMap.put("notificationSearchCriteria", criteria);

      assertThat(
              mockMvc.perform(
                  get("/notifications/search?notification_type=all").flashAttrs(flashMap)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/notifications/search-results");
    }

    @Test
    @DisplayName("Should return expected view when no criteria passed")
    void shouldReturnExpectedViewWhenNoCriteriaPassed() {

      NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);
      flashMap.put("notificationSearchCriteria", criteria);

      ProviderDetail providerDetail = new ProviderDetail();
      List<ContactDetail> feeEarners = buildFeeEarners();

      CommonLookupDetail notificationTypes = new CommonLookupDetail();
      notificationTypes.addContentItem(
          new CommonLookupValueDetail().type("N").code("code n").description("description"));

      UserDetails baseUsers =
          new UserDetails()
              .addContentItem(new BaseUser().userId(123).userType("type1").loginId("login1"));

      when(lookupService.getCommonValues(COMMON_VALUE_NOTIFICATION_TYPE))
          .thenReturn(Mono.just(notificationTypes));
      when(providerService.getProvider(userDetails.getProvider().getId()))
          .thenReturn(Mono.just(providerDetail));
      when(providerService.getAllFeeEarners(providerDetail)).thenReturn(feeEarners);
      when(userService.getUsers(any())).thenReturn(Mono.just(baseUsers));

      Notifications notificationsMock = new Notifications().content(new ArrayList<>());

      Mockito.when(notificationService.getNotifications(any(), anyInt(), any(), any()))
          .thenReturn(Mono.just(notificationsMock));

      assertThat(mockMvc.perform(get("/notifications/search").flashAttrs(flashMap)))
          .hasStatusOk()
          .hasViewName("notifications/actions-and-notifications-search");
    }

    @Test
    @DisplayName("Should return expected view when no criteria passed alternative")
    void shouldReturnExpectedViewWhenNoCriteriaPassedAlternative() {
      NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);
      flashMap.put("notificationSearchCriteria", criteria);

      ProviderDetail providerDetail = new ProviderDetail();
      List<ContactDetail> feeEarners = buildFeeEarners();

      CommonLookupDetail notificationTypes = new CommonLookupDetail();
      notificationTypes.addContentItem(
          new CommonLookupValueDetail().type("N").code("code n").description("description"));

      UserDetails baseUsers =
          new UserDetails()
              .addContentItem(
                  new BaseUser()
                      .userId(123)
                      .userType("type1")
                      .loginId("login1")
                      .username("login1"));

      when(lookupService.getCommonValues(COMMON_VALUE_NOTIFICATION_TYPE))
          .thenReturn(Mono.just(notificationTypes));
      when(providerService.getProvider(userDetails.getProvider().getId()))
          .thenReturn(Mono.just(providerDetail));
      when(providerService.getAllFeeEarners(providerDetail)).thenReturn(feeEarners);
      when(userService.getUsers(any())).thenReturn(Mono.just(baseUsers));
      assertThat(mockMvc.perform(get("/notifications/search").flashAttrs(flashMap)))
          .hasStatusOk()
          .hasViewName("notifications/actions-and-notifications-search")
          .model()
          .containsEntry("notificationSearchCriteria", criteria);
    }
  }

  @Nested
  @DisplayName("POST: /notifications/search")
  class PostNotificationSearchTests {

    @Test
    @DisplayName("Should have validation errors when date larger than 3 years")
    void shouldHaveValidationErrorswhenDateLargerThan3Years() {
      NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);
      flashMap.put("notificationSearchCriteria", criteria);

      ProviderDetail providerDetail = new ProviderDetail();
      List<ContactDetail> feeEarners = buildFeeEarners();

      CommonLookupDetail notificationTypes = new CommonLookupDetail();
      notificationTypes.addContentItem(
          new CommonLookupValueDetail().type("N").code("code n").description("description"));
      UserDetails baseUsers =
          new UserDetails()
              .addContentItem(
                  new BaseUser()
                      .userId(123)
                      .userType("type1")
                      .loginId("login1")
                      .username("login1"));

      when(lookupService.getCommonValues(COMMON_VALUE_NOTIFICATION_TYPE))
          .thenReturn(Mono.just(notificationTypes));
      when(providerService.getProvider(userDetails.getProvider().getId()))
          .thenReturn(Mono.just(providerDetail));
      when(providerService.getAllFeeEarners(providerDetail)).thenReturn(feeEarners);
      when(userService.getUsers(any())).thenReturn(Mono.just(baseUsers));

      doAnswer(
              invocation -> {
                Errors errors = (Errors) invocation.getArguments()[1];
                errors.rejectValue(
                    "notificationToDate",
                    "validation.date.range-exceeds-three-years.error-text",
                    "Your date range is invalid.");
                return null;
              })
          .when(notificationSearchValidator)
          .validate(any(), any());

      assertThat(mockMvc.perform(post("/notifications/search").flashAttrs(flashMap)))
          .hasForwardedUrl("notifications/actions-and-notifications-search")
          .model()
          .hasErrors()
          .containsEntry("notificationSearchCriteria", criteria);
    }

    @Test
    @DisplayName("Should redirect to search results")
    void shouldRedirectToSearchResults() throws Exception {
      NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);
      flashMap.put("notificationSearchCriteria", criteria);

      assertThat(mockMvc.perform(post("/notifications/search").flashAttrs(flashMap)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/notifications/search-results");
    }
  }

  @Nested
  @DisplayName("GET: /notifications/case-search")
  class GetNotificationCaseSearchTests {

    @Test
    @DisplayName("Should redirect to search results with notifications")
    void shouldRedirectToSearchResultsWithNotifications() {
      NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);
      flashMap.put("notificationSearchCriteria", criteria);

      Notifications notificationsMock = getNotificationsMock();

      Mockito.when(notificationService.getNotifications(any(), anyInt(), any(), any()))
          .thenReturn(Mono.just(notificationsMock));

      ApplicationDetail ebsCase = buildFullApplicationDetail();
      assertThat(
              mockMvc.perform(
                  get("/notifications/case-search")
                      .sessionAttr(CASE, ebsCase)
                      .flashAttrs(flashMap)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/notifications/search-results")
          .debug();
      assertThat(criteria.isOriginatesFromCase()).isTrue();
      assertThat(criteria.getCaseReference()).isEqualTo(ebsCase.getCaseReferenceNumber());
    }
  }

  @Nested
  @DisplayName("GET: /notifications/{notification-id}")
  class GetNotification {

    @Test
    @DisplayName("Should return expected result")
    void shouldReturnExpectedResult() throws Exception {
      NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
      Notification notification = buildNotification();
      Notifications notificationsMock = getNotificationsMock();

      when(notificationService.getNotification(
              "234", userDetails.getUserId(), userDetails.getProvider().getId()))
          .thenReturn(Mono.just(notification));

      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);
      flashMap.put("notificationSearchCriteria", criteria);
      flashMap.put("notificationsSearchResults", notificationsMock);

      assertThat(mockMvc.perform(get("/notifications/234").flashAttrs(flashMap)))
          .hasViewName("notifications/notification")
          .hasStatusOk()
          .model()
          .containsEntry("notification", notification);
      oldmockMvc
          .perform(get("/notifications/234").flashAttrs(flashMap))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(model().attributeExists("notification"));
    }

    @Test
    @DisplayName("Should throw exception when notification not found")
    void shouldThrowExceptionWhenNotificationNotFound() {
      NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
      Notifications notificationsMock = getNotificationsMock();
      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);
      flashMap.put("notificationSearchCriteria", criteria);
      flashMap.put("notificationsSearchResults", notificationsMock);
      when(notificationService.getNotification(
              "123", userDetails.getUserId(), userDetails.getProvider().getId()))
          .thenReturn(Mono.empty());
      Exception exception =
          assertThrows(
              Exception.class,
              () -> oldmockMvc.perform(get("/notifications/123").flashAttrs(flashMap)));
      assertInstanceOf(CaabApplicationException.class, exception.getCause());
      assertEquals("Notification with id 123 not found", exception.getCause().getMessage());

      assertThat(mockMvc.perform(get("/notifications/123").flashAttrs(flashMap)))
          .failure()
          .isInstanceOf(CaabApplicationException.class)
          .hasMessage("Notification with id 123 not found");
    }

    @Test
    @DisplayName("Should populate document URLs")
    void shouldPopulateDocumentUrls() {
      NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
      Notifications notificationsMock = getNotificationsMock();
      Notification notification = buildNotification();
      when(notificationService.getNotification(
              "234", userDetails.getUserId(), userDetails.getProvider().getId()))
          .thenReturn(Mono.just(notification));
      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);
      flashMap.put("notificationSearchCriteria", criteria);
      flashMap.put("notificationsSearchResults", notificationsMock);

      List<Document> documents = buildAttachedDocuments();

      when(notificationService.getDocumentLinks(documents)).thenReturn(Map.of("567", "doc-url"));

      assertThat(mockMvc.perform(get("/notifications/234").flashAttrs(flashMap)))
          .hasStatusOk()
          .model()
          .hasEntrySatisfying("documentLinks", matching(hasEntry("567", "doc-url")))
          .containsKey("notification");
    }
  }

  @Nested
  @DisplayName("GET: /notifications")
  class GetNotificationsTests {

    @Test
    @DisplayName("Should return expected result")
    void testReturnToNotifications_Data_RedirectsToResultsPage() throws Exception {
      NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
      Notifications notificationsMock = getNotificationsMock();
      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("notificationSearchCriteria", criteria);
      assertThat(
              mockMvc.perform(
                  get("/notifications")
                      .sessionAttr(NOTIFICATIONS_SEARCH_RESULTS, notificationsMock)
                      .sessionAttr("user", userDetails)
                      .flashAttrs(flashMap)))
          .hasStatusOk()
          .hasViewName("notifications/actions-and-notifications");
    }

    @Test
    @DisplayName("Should redirect to results page when no data")
    void testReturnToNotifications_noData_redirectsToResultsPage() {
      NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("notificationSearchCriteria", criteria);
      assertThat(
              mockMvc.perform(
                  get("/notifications").sessionAttr("user", userDetails).flashAttrs(flashMap)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/notifications/search?notification_type=all");
    }
  }

  @Nested
  @DisplayName("GET: /notifications/{notification-id}/attachments/{attachment-id}/retrieve")
  class GetRetrieveAttachmentTests {

    @Test
    @DisplayName("Should redirect to notification page")
    void shouldRedirectToNotificationPage() {
      NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
      Notifications notificationsMock = getNotificationsMock();
      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);
      flashMap.put("notificationSearchCriteria", criteria);
      flashMap.put("notificationsSearchResults", notificationsMock);
      assertThat(
              mockMvc.perform(
                  get("/notifications/234/attachments/567/retrieve")
                      .sessionAttr("user", userDetails)
                      .header(HttpHeaders.REFERER, "/notifications/234")
                      .flashAttrs(flashMap)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/notifications/234");
    }

    @Test
    @DisplayName("Should redirect to provide document or evidence page")
    void shouldRedirectToProvideDocumentOrEvidencePage() {
      NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
      Notifications notificationsMock = getNotificationsMock();
      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);
      flashMap.put("notificationSearchCriteria", criteria);
      flashMap.put("notificationsSearchResults", notificationsMock);
      assertThat(
              mockMvc.perform(
                  get("/notifications/234/attachments/567/retrieve")
                      .sessionAttr("user", userDetails)
                      .header(
                          HttpHeaders.REFERER, "/notifications/234/provide-documents-or-evidence")
                      .flashAttrs(flashMap)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/notifications/234/provide-documents-or-evidence");
    }
  }

  @Nested
  @DisplayName("GET: /notifications/{notification-id}/attachments/{attachment-id}/remove")
  class RemoveAttachmentTests {

    @Test
    @DisplayName("Should redirect to provide documents or evidence page")
    void shouldRedirectToProvideDocumentOrEvidencePage() {

      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);

      assertThat(
              mockMvc.perform(
                  get("/notifications/234/attachments/567/remove")
                      .sessionAttr("user", userDetails)
                      .flashAttrs(flashMap)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/notifications/234/provide-documents-or-evidence");

      verify(notificationService)
          .removeDraftNotificationAttachment(
              "234", 567, userDetails.getLoginId(), userDetails.getUserId());
    }
  }

  @Nested
  @DisplayName("GET: /notifications/234/attachments/567/retrieveDraft")
  class GetRetrieveDraftTests {

    @Test
    @DisplayName("Should redirect to provide documents or evidence page")
    void shouldRedirectToProvideDocumentOrEvidencePage() {
      NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
      Notifications notificationsMock = getNotificationsMock();
      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);
      flashMap.put("notificationSearchCriteria", criteria);
      flashMap.put("notificationsSearchResults", notificationsMock);
      assertThat(
              mockMvc.perform(
                  get("/notifications/234/attachments/567/retrieveDraft")
                      .sessionAttr("user", userDetails)
                      .flashAttrs(flashMap)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/notifications/234/provide-documents-or-evidence");
    }
  }

  @Nested
  @DisplayName("GET: /notifications/{notification-id}/attachments/{attachment-id}/edit")
  class GetEditAttachmentTests {

    @Test
    @DisplayName("Should redirect to upload page")
    void shouldRedirectToUploadPage() throws Exception {

      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);

      NotificationAttachmentDetail notificationAttachmentDetail =
          new NotificationAttachmentDetail();
      notificationAttachmentDetail.setSendBy("E");

      NotificationAttachmentUploadFormData notificationAttachmentUploadFormData =
          new NotificationAttachmentUploadFormData();
      notificationAttachmentUploadFormData.setSendBy(SendBy.ELECTRONIC);

      when(notificationService.getDraftNotificationAttachment(567))
          .thenReturn(Mono.just(notificationAttachmentDetail));
      when(notificationAttachmentMapper.toNotificationAttachmentUploadFormData(
              notificationAttachmentDetail))
          .thenReturn(notificationAttachmentUploadFormData);

      assertThat(
              mockMvc.perform(get("/notifications/234/attachments/567/edit").flashAttrs(flashMap)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/notifications/234/attachments/upload?sendBy=ELECTRONIC")
          .flash()
          .containsEntry(
              "notificationAttachmentUploadFormData", notificationAttachmentUploadFormData);
    }
  }

  @Nested
  @DisplayName("GET: /notifications/{notification-id}/attachments/upload")
  class GetUploadAttachmentTests {

    @Test
    @DisplayName("Should return expected result")
    void shouldReturnExpectedResult() {

      CommonLookupValueDetail documentType =
          new CommonLookupValueDetail()
              .type(COMMON_VALUE_DOCUMENT_TYPES)
              .code("TST_DOC")
              .description("Test Document");

      CommonLookupDetail documentTypes = new CommonLookupDetail();
      documentTypes.addContentItem(documentType);

      List<String> validExtensions = List.of("ext");
      String maxFileSize = "8MB";

      when(lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
          .thenReturn(Mono.just(documentTypes));
      when(notificationAttachmentUploadValidator.getValidExtensions()).thenReturn(validExtensions);
      when(notificationAttachmentUploadValidator.getMaxFileSize()).thenReturn(maxFileSize);

      Notification notification = buildNotification();
      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);

      assertThat(
              mockMvc.perform(
                  get("/notifications/234/attachments/upload")
                      .sessionAttr("notification", notification)
                      .queryParam("sendBy", "ELECTRONIC")
                      .flashAttrs(flashMap)))
          .hasStatusOk()
          .hasViewName("notifications/upload-notification-attachment")
          .model()
          .containsEntry("documentTypes", documentTypes.getContent())
          .containsEntry("validExtensions", validExtensions.getFirst())
          .containsEntry("maxFileSize", maxFileSize);
    }
  }

  @Nested
  @DisplayName("POST: /notifications/{notification-id}/attachments/upload")
  class PostUploadAttachmentTests {

    @Test
    @DisplayName("Should retrieve draft notification and redirect to provide documents page")
    void shouldRetrieveDraftNotificationAndRedirectToProvideDocumentsPge() throws Exception {

      NotificationAttachmentUploadFormData attachmentUploadFormData =
          new NotificationAttachmentUploadFormData();
      attachmentUploadFormData.setSendBy(SendBy.POSTAL);

      NotificationAttachmentDetail notificationAttachment = new NotificationAttachmentDetail();
      notificationAttachment.setSendBy("P");

      BaseNotificationAttachmentDetail baseNotificationAttachment =
          new BaseNotificationAttachmentDetail();
      baseNotificationAttachment.setSendBy("P");

      NotificationAttachmentDetails notificationAttachmentDetails =
          new NotificationAttachmentDetails();
      notificationAttachmentDetails.setContent(List.of(baseNotificationAttachment));

      when(notificationAttachmentMapper.toNotificationAttachmentDetail(attachmentUploadFormData))
          .thenReturn(notificationAttachment);
      when(notificationService.getDraftNotificationAttachments("234", userDetails.getUserId()))
          .thenReturn(Mono.just(notificationAttachmentDetails));

      Notification notification = buildNotification();
      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);
      flashMap.put("attachmentUploadFormData", attachmentUploadFormData);

      assertThat(
              mockMvc.perform(
                  post("/notifications/234/attachments/upload")
                      .sessionAttr("notification", notification)
                      .flashAttrs(flashMap)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/notifications/234/provide-documents-or-evidence");

      verify(notificationService)
          .addDraftNotificationAttachment(notificationAttachment, userDetails.getLoginId());
    }

    @Test
    @DisplayName(
        "Should redirect back to provide documents or evidence page when edit post " + "attachment")
    void shouldRedirectToProviderDocumentsPageWhenEditPostAttachment() throws Exception {

      NotificationAttachmentUploadFormData attachmentUploadFormData =
          new NotificationAttachmentUploadFormData();
      attachmentUploadFormData.setSendBy(SendBy.POSTAL);
      attachmentUploadFormData.setDocumentId(123);

      NotificationAttachmentDetail notificationAttachment = new NotificationAttachmentDetail();
      notificationAttachment.setId(123);
      notificationAttachment.setSendBy("P");

      BaseNotificationAttachmentDetail baseNotificationAttachment =
          new BaseNotificationAttachmentDetail();
      baseNotificationAttachment.setSendBy("P");

      NotificationAttachmentDetails notificationAttachmentDetails =
          new NotificationAttachmentDetails();
      notificationAttachmentDetails.setContent(List.of(baseNotificationAttachment));

      when(notificationAttachmentMapper.toNotificationAttachmentDetail(attachmentUploadFormData))
          .thenReturn(notificationAttachment);
      when(notificationService.getDraftNotificationAttachments("234", userDetails.getUserId()))
          .thenReturn(Mono.just(notificationAttachmentDetails));

      Notification notification = buildNotification();
      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);
      flashMap.put("attachmentUploadFormData", attachmentUploadFormData);

      assertThat(
              mockMvc.perform(
                  post("/notifications/234/attachments/upload")
                      .sessionAttr("notification", notification)
                      .flashAttrs(flashMap)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/notifications/234/provide-documents-or-evidence");

      verify(notificationService)
          .updateDraftNotificationAttachment(notificationAttachment, userDetails.getLoginId());
    }

    @Test
    @DisplayName("Should upload and perform virus scan then redirect to provide documents page")
    void shouldUploadAndPerformVirusScanThenRedirectToProvideDocumentsPage() throws Exception {

      String filename = "filename";
      byte[] content = "content".getBytes();

      MockMultipartFile file = new MockMultipartFile("name", filename, null, content);

      NotificationAttachmentUploadFormData attachmentUploadFormData =
          new NotificationAttachmentUploadFormData();
      attachmentUploadFormData.setSendBy(SendBy.ELECTRONIC);
      attachmentUploadFormData.setFile(file);

      NotificationAttachmentDetail notificationAttachment = new NotificationAttachmentDetail();
      notificationAttachment.setSendBy("E");

      BaseNotificationAttachmentDetail baseNotificationAttachment =
          new BaseNotificationAttachmentDetail();
      baseNotificationAttachment.setSendBy("E");

      NotificationAttachmentDetails notificationAttachmentDetails =
          new NotificationAttachmentDetails();
      notificationAttachmentDetails.setContent(List.of(baseNotificationAttachment));

      when(notificationAttachmentMapper.toNotificationAttachmentDetail(attachmentUploadFormData))
          .thenReturn(notificationAttachment);
      when(notificationService.getDraftNotificationAttachments("234", userDetails.getUserId()))
          .thenReturn(Mono.just(notificationAttachmentDetails));

      Notification notification = buildNotification();
      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);
      flashMap.put("attachmentUploadFormData", attachmentUploadFormData);

      assertThat(
              mockMvc.perform(
                  post("/notifications/234/attachments/upload")
                      .sessionAttr("notification", notification)
                      .flashAttrs(flashMap)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/notifications/234/provide-documents-or-evidence");

      verify(avScanService)
          .performAvScan(any(), any(), any(), any(), eq(file.getOriginalFilename()), any());
      verify(notificationService)
          .addDraftNotificationAttachment(notificationAttachment, userDetails.getLoginId());
    }
  }

  @Nested
  @DisplayName("GET: /notifications/{notification-id}/provide-documents-or-evidence")
  class GetProvideDocumentsOrEvidencePageTests {

    @Test
    @DisplayName("Should return expected result")
    void shouldReturnExpectedResult() {

      Notification notification = buildNotification();

      BaseNotificationAttachmentDetail baseNotificationAttachment =
          new BaseNotificationAttachmentDetail();
      baseNotificationAttachment.setSendBy("E");

      NotificationAttachmentDetails notificationAttachmentDetails =
          new NotificationAttachmentDetails();
      notificationAttachmentDetails.setContent(List.of(baseNotificationAttachment));

      Map<String, String> draftDocumentLinks = Map.of("draft doc 1", "link 1");
      Map<String, String> documentLinks = Map.of("uploaded doc 1", "link 1");

      CommonLookupValueDetail documentType =
          new CommonLookupValueDetail()
              .type(COMMON_VALUE_DOCUMENT_TYPES)
              .code("TST_DOC")
              .description("Test Document");

      CommonLookupDetail documentTypes = new CommonLookupDetail();
      documentTypes.addContentItem(documentType);

      when(lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
          .thenReturn(Mono.just(documentTypes));

      when(notificationService.getDraftNotificationAttachments(
              notification.getNotificationId(), userDetails.getUserId()))
          .thenReturn(Mono.just(notificationAttachmentDetails));
      when(notificationService.getDraftDocumentLinks(List.of(baseNotificationAttachment)))
          .thenReturn(draftDocumentLinks);
      when(notificationService.getDocumentLinks(notification.getUploadedDocuments()))
          .thenReturn(documentLinks);
      when(notificationAttachmentMapper.toBaseNotificationAttachmentDetail(
              any(uk.gov.laa.ccms.soa.gateway.model.Document.class), eq("Test Document")))
          .thenReturn(new BaseNotificationAttachmentDetail());

      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);

      assertThat(
              mockMvc.perform(
                  get("/notifications/234/provide-documents-or-evidence")
                      .sessionAttr("notification", notification)
                      .flashAttrs(flashMap)))
          .hasStatusOk()
          .hasViewName("notifications/provide-documents-or-evidence")
          .model()
          .hasEntrySatisfying("notificationAttachments", matching(hasSize(2)))
          .hasEntrySatisfying("documentLinks", matching(hasEntry("draft doc 1", "link 1")))
          .hasEntrySatisfying("documentLinks", matching(hasEntry("uploaded doc 1", "link 1")));

      verify(notificationService)
          .getDraftNotificationAttachments(
              notification.getNotificationId(), userDetails.getUserId());
      verify(notificationService).getDraftDocumentLinks(List.of(baseNotificationAttachment));
      verify(notificationService).getDocumentLinks(notification.getUploadedDocuments());
    }
  }

  @Nested
  @DisplayName("GET: /notifications/{notification-id}/provide-documents-or-evidence")
  class PostProvideDocumentsOrEvidenceTests {

    @Test
    @DisplayName("Should submit draft attachment and redirect to confirm page")
    void shouldSubmitAndRedirectToConfirmedPage() throws Exception {

      Notification notification = buildNotification();

      BaseNotificationAttachmentDetail baseNotificationAttachment =
          new BaseNotificationAttachmentDetail();
      baseNotificationAttachment.setSendBy("E");

      NotificationAttachmentDetails notificationAttachmentDetails =
          new NotificationAttachmentDetails();
      notificationAttachmentDetails.setContent(List.of(baseNotificationAttachment));

      when(notificationService.getDraftNotificationAttachments(
              notification.getNotificationId(), userDetails.getUserId()))
          .thenReturn(Mono.just(notificationAttachmentDetails));

      when(notificationAttachmentMapper.toBaseNotificationAttachmentDetail(
              any(uk.gov.laa.ccms.soa.gateway.model.Document.class), eq("Test Document")))
          .thenReturn(new BaseNotificationAttachmentDetail());

      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);

      assertThat(
              mockMvc.perform(
                  post("/notifications/234/provide-documents-or-evidence")
                      .sessionAttr("notification", notification)
                      .flashAttrs(flashMap)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/application/notification-attachments/confirmed");

      verify(notificationService)
          .submitNotificationAttachments(
              notification.getNotificationId(),
              userDetails.getLoginId(),
              userDetails.getUserType(),
              userDetails.getUserId());
    }

    @Test
    @DisplayName("Should have error when no documents submitted")
    void shouldHaveErrorWhenNoDocumentsSubmitted() {

      Notification notification = buildNotification();

      NotificationAttachmentDetails notificationAttachmentDetails =
          new NotificationAttachmentDetails();
      notificationAttachmentDetails.setContent(Collections.emptyList());

      CommonLookupValueDetail documentType =
          new CommonLookupValueDetail()
              .type(COMMON_VALUE_DOCUMENT_TYPES)
              .code("TST_DOC")
              .description("Test Document");

      CommonLookupDetail documentTypes = new CommonLookupDetail();
      documentTypes.addContentItem(documentType);

      when(lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
          .thenReturn(Mono.just(documentTypes));

      when(notificationService.getDraftNotificationAttachments(
              notification.getNotificationId(), userDetails.getUserId()))
          .thenReturn(Mono.just(notificationAttachmentDetails));
      when(notificationAttachmentMapper.toBaseNotificationAttachmentDetail(
              any(uk.gov.laa.ccms.soa.gateway.model.Document.class), eq("Test Document")))
          .thenReturn(new BaseNotificationAttachmentDetail());

      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);

      assertThat(
              mockMvc.perform(
                  post("/notifications/234/provide-documents-or-evidence")
                      .sessionAttr("notification", notification)
                      .flashAttrs(flashMap)))
          .hasViewName("notifications/provide-documents-or-evidence")
          .model()
          .containsKey("errorMessage");

      verify(notificationService, times(2))
          .getDraftNotificationAttachments(
              notification.getNotificationId(), userDetails.getUserId());
    }
  }

  @Nested
  @DisplayName("POST: /application/notification-attachments/confirmed")
  class PostNotificationAttachmentConfirmTests {

    @Test
    @DisplayName("Should redirect to provide documents page")
    void shouldRedirectToProvideDocumentsPage() {

      Notification notification = buildNotification();

      Map<String, Object> flashMap = new HashMap<>();
      flashMap.put("user", userDetails);
      when(notificationService.getNotification(
              "234", userDetails.getUserId(), userDetails.getProvider().getId()))
          .thenReturn(Mono.just(notification));

      assertThat(
              mockMvc.perform(
                  post("/application/notification-attachments/confirmed")
                      .sessionAttr("notification", notification)
                      .flashAttrs(flashMap)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/notifications/234/provide-documents-or-evidence");
    }
  }

  @Nested
  @DisplayName("POST: /notifications/{notification-id}")
  class PostNotificationTests {

    @Test
    @DisplayName("Should return expected view")
    void testSubmitNotificationResponse_success() throws Exception {
      Map<String, Object> flashMap = new HashMap<>();

      NotificationResponseFormData formData = new NotificationResponseFormData();
      formData.setAction("action");
      formData.setMessage("message");

      flashMap.put("user", userDetails);
      flashMap.put("notificationResponseFormData", formData);

      Notification notification = buildNotification();

      String notificationId = "12345";

      when(notificationService.submitNotificationResponse(
              notificationId,
              formData.getAction(),
              formData.getMessage(),
              userDetails.getLoginId(),
              userDetails.getUserType()))
          .thenReturn(Mono.just(new ClientTransactionResponse()));

      assertThat(
              mockMvc.perform(
                  post("/notifications/" + notificationId)
                      .sessionAttr("user", userDetails)
                      .sessionAttr("notification", notification)
                      .flashAttrs(flashMap)))
          .hasStatusOk()
          .hasViewName("notifications/notification");

      verify(notificationService)
          .submitNotificationResponse(
              notificationId,
              formData.getAction(),
              formData.getMessage(),
              userDetails.getLoginId(),
              userDetails.getUserType());
    }

    @Test
    @DisplayName("Should throw exception")
    void testSubmitNotificationResponse_throwsException_whenSubmitNotificationFails() {
      Map<String, Object> flashMap = new HashMap<>();

      NotificationResponseFormData formData = new NotificationResponseFormData();
      formData.setAction("action");
      formData.setMessage("message");

      flashMap.put("user", userDetails);
      flashMap.put("notificationResponseFormData", formData);

      Notification notification = buildNotification();

      String notificationId = "12345";

      when(notificationService.submitNotificationResponse(
              notificationId,
              formData.getAction(),
              formData.getMessage(),
              userDetails.getLoginId(),
              userDetails.getUserType()))
          .thenReturn(Mono.empty());

      assertThat(
              mockMvc.perform(
                  post("/notifications/" + notificationId)
                      .sessionAttr("user", userDetails)
                      .sessionAttr("notification", notification)
                      .flashAttrs(flashMap)))
          .failure()
          .isInstanceOf(CaabApplicationException.class)
          .hasMessage("Failed to submit notification response");
    }

    @Test
    @DisplayName("Should handle validation errors")
    void shouldHandleValidationError() {
      Map<String, Object> flashMap = new HashMap<>();

      NotificationResponseFormData formData = new NotificationResponseFormData();

      flashMap.put("user", userDetails);
      flashMap.put("notificationResponseFormData", formData);

      Notification notification = buildNotification();

      String notificationId = "12345";

      doAnswer(
              invocation -> {
                Errors errors = (Errors) invocation.getArguments()[1];

                errors.rejectValue(
                    "action", "required.action", "Please complete 'Notification response action'.");
                return null;
              })
          .when(notificationResponseValidator)
          .validate(any(), any());

      assertThat(
              mockMvc.perform(
                  post("/notifications/" + notificationId)
                      .sessionAttr("user", userDetails)
                      .sessionAttr("notification", notification)
                      .flashAttrs(flashMap)))
          .hasStatusOk()
          .hasViewName("notifications/notification")
          .model()
          .hasErrors()
          .hasAttributeErrors("notificationResponseFormData");

      verify(notificationService, never())
          .submitNotificationResponse(any(), any(), any(), any(), any());
    }
  }

  private List<ContactDetail> buildFeeEarners() {
    List<ContactDetail> feeEarners = new ArrayList<>();
    feeEarners.add(new ContactDetail().id(1).name("FeeEarner1"));
    feeEarners.add(new ContactDetail().id(2).name("FeeEarner2"));
    return feeEarners;
  }
}
