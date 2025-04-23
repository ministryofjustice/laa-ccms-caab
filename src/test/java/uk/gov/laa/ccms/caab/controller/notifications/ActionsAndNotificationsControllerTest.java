package uk.gov.laa.ccms.caab.controller.notifications;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_DOCUMENT_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_NOTIFICATION_TYPE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.NOTIFICATIONS_SEARCH_RESULTS;

import java.util.ArrayList;
import java.util.Collections;
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
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.NotificationSearchCriteria;
import uk.gov.laa.ccms.caab.bean.notification.NotificationAttachmentUploadFormData;
import uk.gov.laa.ccms.caab.bean.notification.NotificationResponseFormData;
import uk.gov.laa.ccms.caab.bean.validators.notification.NotificationAttachmentUploadValidator;
import uk.gov.laa.ccms.caab.bean.validators.notification.NotificationResponseValidator;
import uk.gov.laa.ccms.caab.bean.validators.notification.NotificationSearchValidator;
import uk.gov.laa.ccms.caab.constants.SendBy;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.NotificationAttachmentMapper;
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
  private LookupService lookupService;
  @Mock
  private UserService userService;
  @Mock
  private NotificationSearchValidator notificationSearchValidator;
  @Mock
  private NotificationAttachmentMapper notificationAttachmentMapper;
  @Mock
  private NotificationAttachmentUploadValidator notificationAttachmentUploadValidator;
  @Mock
  private NotificationResponseValidator notificationResponseValidator;
  @Mock
  private AvScanService avScanService;
  @Autowired
  private WebApplicationContext webApplicationContext;
  private MockMvc mockMvc;

  private static Notifications getNotificationsMock() {
    return new Notifications()
        .addContentItem(buildNotificationInfo());
  }

  private static NotificationInfo buildNotificationInfo() {
    return new NotificationInfo()
        .user(new UserDetail()
            .loginId("user1")
            .userType("user1"))
        .notificationId("234")
        .notificationType("N");
  }

  private static Notification buildNotification() {
    return new Notification()
        .user(new UserDetail()
            .loginId("user1")
            .userType("user1"))
        .notificationId("234")
        .notificationType("N")
        .attachedDocuments(buildAttachedDocuments())
        .uploadedDocuments(buildUploadedDocuments());
  }

  private static List<Document> buildUploadedDocuments() {
    return List.of(new Document().documentId("890").channel("P").documentType("TST_DOC"));
  }

  private static List<uk.gov.laa.ccms.soa.gateway.model.Document> buildUploadedDocumentsSoa() {
    return List.of(new uk.gov.laa.ccms.soa.gateway.model.Document().documentId("890").channel("P")
        .documentType("TST_DOC"));
  }

  private static List<Document> buildAttachedDocuments() {
    return List.of(new Document().documentId("567").channel("E").documentType("TST_DOC"));
  }

  private static List<uk.gov.laa.ccms.soa.gateway.model.Document> buildAttachedDocumentsSoa() {
    return List.of(new uk.gov.laa.ccms.soa.gateway.model.Document().documentId("567").channel("E")
        .documentType("TST_DOC"));
  }

  private static NotificationSearchCriteria buildNotificationSearchCritieria() {
    NotificationSearchCriteria criteria = new NotificationSearchCriteria();
    criteria.setNotificationToDate("12/12/2025");
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
    when(notificationSearchValidator.supports(any())).thenReturn(true);
  }

  @Test
  void notificationsEndpointAndViewNameWhenNotificationTypeSetData() throws Exception {
    Notifications notificationsMock = getNotificationsMock();

    Mockito.when(notificationService.getNotifications(any(), anyInt(), any(), any()))
        .thenReturn(Mono.just(notificationsMock));

    this.mockMvc.perform(
            get("/notifications/search?notification_type=N").flashAttr("user", userDetails))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(view().name("redirect:/notifications/search-results"));
  }

  @Test
  void notificationsEndpointFromHeaderURLNotificationTypeALLRedirectsToResultsEndpoint()
      throws Exception {
    NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
    Map<String, Object> flashMap = new HashMap<>();
    flashMap.put("user", userDetails);
    flashMap.put("notificationSearchCriteria", criteria);

    this.mockMvc.perform(get("/notifications/search?notification_type=all")
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(view().name("redirect:/notifications/search-results"));
  }


  @Test
  void notificationsFromHeaderEndpointDropdownCodeExecutesAndRedirectsToSearchResultsEndpoint()
      throws Exception {

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

    when(lookupService.getCommonValues(COMMON_VALUE_NOTIFICATION_TYPE)).thenReturn(
        Mono.just(notificationTypes));
    when(providerService.getProvider(userDetails.getProvider().getId()))
        .thenReturn(Mono.just(providerDetail));
    when(providerService.getAllFeeEarners(providerDetail)).thenReturn(feeEarners);
    when(userService.getUsers(any())).thenReturn(Mono.just(baseUsers));
    /**/

    Notifications notificationsMock = new Notifications()
        .content(new ArrayList<>());

    Mockito.when(notificationService.getNotifications(any(), anyInt(), any(), any()))
        .thenReturn(Mono.just(notificationsMock));

    this.mockMvc.perform(get("/notifications/search")
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("notifications/actions-and-notifications-search"));

  }


  @Test
  void notificationsSearchValidationForcesReturnToSearchPage() throws Exception {
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

    when(lookupService.getCommonValues(COMMON_VALUE_NOTIFICATION_TYPE)).thenReturn(
        Mono.just(notificationTypes));
    when(providerService.getProvider(userDetails.getProvider().getId()))
        .thenReturn(Mono.just(providerDetail));
    when(providerService.getAllFeeEarners(providerDetail)).thenReturn(feeEarners);
    when(userService.getUsers(any())).thenReturn(Mono.just(baseUsers));

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("notificationToDate",
          "validation.date.range-exceeds-three-years.error-text",
          "Your date range is invalid.");
      return null;
    }).when(notificationSearchValidator).validate(any(), any());

    mockMvc.perform(post("/notifications/search")
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(
            model().attribute("notificationSearchCriteria", hasProperty("notificationToDate")))
        .andExpect(model().hasErrors())
        .andExpect(forwardedUrl("notifications/actions-and-notifications-search"));
  }

  @Test
  void searchEndpointCalledFromRefineSearchExecuteDropdownCodeRedirectToSearchPage()
      throws Exception {
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
            .loginId("login1")
            .username("login1"));

    when(lookupService.getCommonValues(COMMON_VALUE_NOTIFICATION_TYPE)).thenReturn(
        Mono.just(notificationTypes));
    when(providerService.getProvider(userDetails.getProvider().getId()))
        .thenReturn(Mono.just(providerDetail));
    when(providerService.getAllFeeEarners(providerDetail)).thenReturn(feeEarners);
    when(userService.getUsers(any())).thenReturn(Mono.just(baseUsers));
    mockMvc.perform(get("/notifications/search")
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(
            model().attribute("notificationSearchCriteria", hasProperty("notificationToDate")))
        .andExpect(forwardedUrl("notifications/actions-and-notifications-search"));
  }

  @Test
  void notificationsSearchEndpointValidCriteriaRedirectsToSearchResults() throws Exception {
    NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
    Map<String, Object> flashMap = new HashMap<>();
    flashMap.put("user", userDetails);
    flashMap.put("notificationSearchCriteria", criteria);

    mockMvc.perform(post("/notifications/search")
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(view().name("redirect:/notifications/search-results"));
  }

  @Test
  void getNotification() throws Exception {
    NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
    Notification notification = buildNotification();
    Notifications notificationsMock = getNotificationsMock();

    when(notificationService.getNotification("234", userDetails.getUserId(), userDetails.getProvider().getId()))
        .thenReturn(Mono.just(notification));

    Map<String, Object> flashMap = new HashMap<>();
    flashMap.put("user", userDetails);
    flashMap.put("notificationSearchCriteria", criteria);
    flashMap.put("notificationsSearchResults", notificationsMock);
    mockMvc.perform(get("/notifications/234")
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(model().attributeExists("notification"));
  }

  @Test
  void getNotificationThrowsExceptionWhenNotificationNotFound() {
    NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
    Notifications notificationsMock = getNotificationsMock();
    Map<String, Object> flashMap = new HashMap<>();
    flashMap.put("user", userDetails);
    flashMap.put("notificationSearchCriteria", criteria);
    flashMap.put("notificationsSearchResults", notificationsMock);
    when(notificationService.getNotification("123", userDetails.getUserId(), userDetails.getProvider().getId()))
        .thenReturn(Mono.empty());
    Exception exception = assertThrows(Exception.class, () ->
        mockMvc.perform(get("/notifications/123")
            .flashAttrs(flashMap)));
    assertInstanceOf(CaabApplicationException.class, exception.getCause());
    assertEquals("Notification with id 123 not found", exception.getCause().getMessage());

  }

  @Test
  void returnToNotificationsDataRedirectsToResultsPage() throws Exception {
    NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
    Notifications notificationsMock = getNotificationsMock();
    Map<String, Object> flashMap = new HashMap<>();
    flashMap.put("notificationSearchCriteria", criteria);
    mockMvc.perform(get("/notifications")
            .sessionAttr(NOTIFICATIONS_SEARCH_RESULTS, notificationsMock)
            .sessionAttr("user", userDetails)
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("notifications/actions-and-notifications"));
  }


  @Test
  void returnToNotificationsNoDataRedirectsToResultsPage() throws Exception {
    NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
    Map<String, Object> flashMap = new HashMap<>();
    flashMap.put("notificationSearchCriteria", criteria);
    mockMvc.perform(get("/notifications")
            .sessionAttr("user", userDetails)
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/notifications/search?notification_type=all"));
  }

  @Test
  void retrieveNotificationAttachmentRedirectsToNotificationPage() throws Exception {
    NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
    Notifications notificationsMock = getNotificationsMock();
    Map<String, Object> flashMap = new HashMap<>();
    flashMap.put("user", userDetails);
    flashMap.put("notificationSearchCriteria", criteria);
    flashMap.put("notificationsSearchResults", notificationsMock);
    mockMvc.perform(get("/notifications/234/attachments/567/retrieve")
            .sessionAttr("user", userDetails)
            .header(HttpHeaders.REFERER, "/notifications/234")
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/notifications/234"));
  }

  @Test
  void retrieveNotificationAttachmentRedirectsToProvideDocumentsPage() throws Exception {
    NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
    Notifications notificationsMock = getNotificationsMock();
    Map<String, Object> flashMap = new HashMap<>();
    flashMap.put("user", userDetails);
    flashMap.put("notificationSearchCriteria", criteria);
    flashMap.put("notificationsSearchResults", notificationsMock);
    mockMvc.perform(get("/notifications/234/attachments/567/retrieve")
            .sessionAttr("user", userDetails)
            .header(HttpHeaders.REFERER, "/notifications/234/provide-documents-or-evidence")
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/notifications/234/provide-documents-or-evidence"));
  }

  @Test
  void remmoveNotificationAttachmentRemovesAttachmentAndRedirectsToProvideDocumentsPage()
      throws Exception {

    Map<String, Object> flashMap = new HashMap<>();
    flashMap.put("user", userDetails);

    mockMvc.perform(get("/notifications/234/attachments/567/remove")
            .sessionAttr("user", userDetails)
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/notifications/234/provide-documents-or-evidence"));

    verify(notificationService).removeDraftNotificationAttachment("234", 567,
        userDetails.getLoginId(), userDetails.getUserId());
  }

  @Test
  void retrieveDraftNotificationAttachmentRedirectsToProvideDocumentsPage() throws Exception {
    NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
    Notifications notificationsMock = getNotificationsMock();
    Map<String, Object> flashMap = new HashMap<>();
    flashMap.put("user", userDetails);
    flashMap.put("notificationSearchCriteria", criteria);
    flashMap.put("notificationsSearchResults", notificationsMock);
    mockMvc.perform(get("/notifications/234/attachments/567/retrieveDraft")
            .sessionAttr("user", userDetails)
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/notifications/234/provide-documents-or-evidence"));
  }

  @Test
  void editDraftNotificationAttachmentRedirectsToUploadPage() throws Exception {

    Map<String, Object> flashMap = new HashMap<>();
    flashMap.put("user", userDetails);

    NotificationAttachmentDetail notificationAttachmentDetail =
        new NotificationAttachmentDetail();
    notificationAttachmentDetail.setSendBy("E");

    NotificationAttachmentUploadFormData notificationAttachmentUploadFormData =
        new NotificationAttachmentUploadFormData();
    notificationAttachmentUploadFormData.setSendBy(SendBy.ELECTRONIC);

    when(notificationService.getDraftNotificationAttachment(567)).thenReturn(
        Mono.just(notificationAttachmentDetail));
    when(notificationAttachmentMapper.toNotificationAttachmentUploadFormData(
        notificationAttachmentDetail))
        .thenReturn(notificationAttachmentUploadFormData);

    mockMvc.perform(get("/notifications/234/attachments/567/edit")
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(flash().attribute("notificationAttachmentUploadFormData",
            notificationAttachmentUploadFormData))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/notifications/234/attachments/upload?sendBy=ELECTRONIC"));
  }

  @Test
  void getUploadNotificationAttachmentCreatesDropsDownAndDisplaysUploadForm()
      throws Exception {

    CommonLookupValueDetail documentType = new CommonLookupValueDetail()
        .type(COMMON_VALUE_DOCUMENT_TYPES).code("TST_DOC").description(
            "Test Document");

    CommonLookupDetail documentTypes = new CommonLookupDetail();
    documentTypes
        .addContentItem(documentType);

    List<String> validExtensions = List.of("ext");
    String maxFileSize = "8MB";

    when(lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
        .thenReturn(Mono.just(documentTypes));
    when(notificationAttachmentUploadValidator.getValidExtensions())
        .thenReturn(validExtensions);
    when(notificationAttachmentUploadValidator.getMaxFileSize())
        .thenReturn(maxFileSize);

    Notification notification = buildNotification();
    Map<String, Object> flashMap = new HashMap<>();
    flashMap.put("user", userDetails);

    mockMvc.perform(get("/notifications/234/attachments/upload")
            .sessionAttr("notification", notification)
            .queryParam("sendBy", "ELECTRONIC")
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(model().attribute("documentTypes", documentTypes.getContent()))
        .andExpect(model().attribute("validExtensions", validExtensions.getFirst()))
        .andExpect(model().attribute("maxFileSize", maxFileSize))
        .andExpect(view().name("notifications/upload-notification-attachment"));
  }

  @Test
  void postUploadNotificationAttachmentNewPostalAttachmentUploadsAndReturnsToProvideDocumentsPage()
      throws Exception {

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

    mockMvc.perform(post("/notifications/234/attachments/upload")
            .sessionAttr("notification", notification)
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/notifications/234/provide-documents-or-evidence"));

    verify(notificationService).addDraftNotificationAttachment(notificationAttachment,
        userDetails.getLoginId());
  }

  @Test
  void postUploadNotificationAttachmentEditPostalAttachmentUpdatesAndReturnsToProvideDocumentsPage()
      throws Exception {

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

    mockMvc.perform(post("/notifications/234/attachments/upload")
            .sessionAttr("notification", notification)
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/notifications/234/provide-documents-or-evidence"));

    verify(notificationService).updateDraftNotificationAttachment(notificationAttachment,
        userDetails.getLoginId());
  }

  @Test
  void postUploadNotificationAttachmentNewElectronicAttachmentRunsVirusScanUploadsAndReturnsToProvideDocumentsPage()
      throws Exception {

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

    mockMvc.perform(post("/notifications/234/attachments/upload")
            .sessionAttr("notification", notification)
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/notifications/234/provide-documents-or-evidence"));

    verify(avScanService).performAvScan(any(), any(), any(), any(), eq(file.getOriginalFilename()),
        any());
    verify(notificationService).addDraftNotificationAttachment(notificationAttachment,
        userDetails.getLoginId());
  }

  @Test
  void getProvideDocumentsOrEvidencePopulatesDraftAttachmentsAndDisplaysPage()
      throws Exception {

    Notification notification = buildNotification();

    BaseNotificationAttachmentDetail baseNotificationAttachment =
        new BaseNotificationAttachmentDetail();
    baseNotificationAttachment.setSendBy("E");

    NotificationAttachmentDetails notificationAttachmentDetails =
        new NotificationAttachmentDetails();
    notificationAttachmentDetails.setContent(List.of(baseNotificationAttachment));

    Map<String, String> draftDocumentLinks = Map.of("draft doc 1", "link 1");
    Map<String, String> documentLinks = Map.of("uploaded doc 1", "link 1");

    CommonLookupValueDetail documentType = new CommonLookupValueDetail()
        .type(COMMON_VALUE_DOCUMENT_TYPES).code("TST_DOC").description(
            "Test Document");

    CommonLookupDetail documentTypes = new CommonLookupDetail();
    documentTypes
        .addContentItem(documentType);

    when(lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
        .thenReturn(Mono.just(documentTypes));

    when(notificationService.getDraftNotificationAttachments(notification.getNotificationId(),
        userDetails.getUserId())).thenReturn(Mono.just(notificationAttachmentDetails));
    when(notificationService.getDraftDocumentLinks(List.of(baseNotificationAttachment))).thenReturn(
        draftDocumentLinks);
    when(notificationService.getDocumentLinks(notification.getUploadedDocuments())).thenReturn(documentLinks);
    when(notificationAttachmentMapper.toBaseNotificationAttachmentDetail(
        any(uk.gov.laa.ccms.soa.gateway.model.Document.class), eq("Test Document")))
        .thenReturn(new BaseNotificationAttachmentDetail());

    Map<String, Object> flashMap = new HashMap<>();
    flashMap.put("user", userDetails);

    mockMvc.perform(get("/notifications/234/provide-documents-or-evidence")
            .sessionAttr("notification", notification)
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(model().attribute("notificationAttachments", hasSize(2)))
        .andExpect(model().attribute("documentLinks", hasEntry("draft doc 1", "link 1")))
        .andExpect(model().attribute("documentLinks", hasEntry("uploaded doc 1", "link 1")))
        .andExpect(view().name("notifications/provide-documents-or-evidence"));

    verify(notificationService).getDraftNotificationAttachments(notification.getNotificationId(),
        userDetails.getUserId());
    verify(notificationService).getDraftDocumentLinks(List.of(baseNotificationAttachment));
    verify(notificationService).getDocumentLinks(notification.getUploadedDocuments());
  }

  @Test
  void postProvideDocumentsOrEvidenceSubmitsDraftAttachmentsAndRedirectsToSubmissionConfirmation()
      throws Exception {

    Notification notification = buildNotification();

    BaseNotificationAttachmentDetail baseNotificationAttachment =
        new BaseNotificationAttachmentDetail();
    baseNotificationAttachment.setSendBy("E");

    NotificationAttachmentDetails notificationAttachmentDetails =
        new NotificationAttachmentDetails();
    notificationAttachmentDetails.setContent(List.of(baseNotificationAttachment));

    when(notificationService.getDraftNotificationAttachments(notification.getNotificationId(),
        userDetails.getUserId())).thenReturn(Mono.just(notificationAttachmentDetails));

    when(notificationAttachmentMapper.toBaseNotificationAttachmentDetail(
        any(uk.gov.laa.ccms.soa.gateway.model.Document.class), eq("Test Document")))
        .thenReturn(new BaseNotificationAttachmentDetail());

    Map<String, Object> flashMap = new HashMap<>();
    flashMap.put("user", userDetails);

    mockMvc.perform(post("/notifications/234/provide-documents-or-evidence")
            .sessionAttr("notification", notification)
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/submissions/notification-attachments/confirmed"));

    verify(notificationService).submitNotificationAttachments(notification.getNotificationId(),
        userDetails.getLoginId(), userDetails.getUserType(), userDetails.getUserId());
  }

  @Test
  void postNotificationAttachmentsSubmissionConfirmedRedirectsToProvideDocumentsOrEvidencePage()
      throws Exception {

    Notification notification = buildNotification();

    Map<String, Object> flashMap = new HashMap<>();
    flashMap.put("user", userDetails);
    when(notificationService.getNotification("234", userDetails.getUserId(), userDetails.getProvider().getId()))
        .thenReturn(Mono.just(notification));

    mockMvc.perform(post("/submissions/notification-attachments/confirmed")
            .sessionAttr("notification", notification)
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/notifications/234/provide-documents-or-evidence"));
  }

  @Test
  void postProvideDocumentsOrEvidenceNoDocumentsToSubmit()
      throws Exception {

    Notification notification = buildNotification();

    NotificationAttachmentDetails notificationAttachmentDetails =
        new NotificationAttachmentDetails();
    notificationAttachmentDetails.setContent(Collections.emptyList());

    CommonLookupValueDetail documentType = new CommonLookupValueDetail()
        .type(COMMON_VALUE_DOCUMENT_TYPES).code("TST_DOC").description(
            "Test Document");

    CommonLookupDetail documentTypes = new CommonLookupDetail();
    documentTypes
        .addContentItem(documentType);

    when(lookupService.getCommonValues(COMMON_VALUE_DOCUMENT_TYPES))
        .thenReturn(Mono.just(documentTypes));

    when(notificationService.getDraftNotificationAttachments(notification.getNotificationId(),
        userDetails.getUserId())).thenReturn(Mono.just(notificationAttachmentDetails));
    when(notificationAttachmentMapper.toBaseNotificationAttachmentDetail(
        any(uk.gov.laa.ccms.soa.gateway.model.Document.class), eq("Test Document")))
        .thenReturn(new BaseNotificationAttachmentDetail());

    Map<String, Object> flashMap = new HashMap<>();
    flashMap.put("user", userDetails);

    mockMvc.perform(post("/notifications/234/provide-documents-or-evidence")
            .sessionAttr("notification", notification)
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(model().attributeExists("errorMessage"))
        .andExpect(view().name("notifications/provide-documents-or-evidence"));

    verify(notificationService,
        times(2)).getDraftNotificationAttachments(notification.getNotificationId(),
        userDetails.getUserId());
  }

  @Test
  void getNotificationPopulatesDocumentUrls() throws Exception {
    NotificationSearchCriteria criteria = buildNotificationSearchCritieria();
    Notifications notificationsMock = getNotificationsMock();
    Notification notification = buildNotification();
    when(notificationService.getNotification("234", userDetails.getUserId(), userDetails.getProvider().getId()))
        .thenReturn(Mono.just(notification));
    Map<String, Object> flashMap = new HashMap<>();
    flashMap.put("user", userDetails);
    flashMap.put("notificationSearchCriteria", criteria);
    flashMap.put("notificationsSearchResults", notificationsMock);

    List<Document> documents = buildAttachedDocuments();

    when(notificationService.getDocumentLinks(documents)).thenReturn(Map.of("567", "doc-url"));

    mockMvc.perform(get("/notifications/234")
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(
            model().attribute("documentLinks", hasEntry("567", "doc-url")))
        .andExpect(model().attributeExists("notification"));
  }

  @Test
  void submitNotificationResponseSuccess() throws Exception {
    Map<String, Object> flashMap = new HashMap<>();

    NotificationResponseFormData formData = new NotificationResponseFormData();
    formData.setAction("action");
    formData.setMessage("message");

    flashMap.put("user", userDetails);
    flashMap.put("notificationResponseFormData", formData);

    Notification notification = buildNotification();

    String notificationId = "12345";

    when(notificationService.submitNotificationResponse(notificationId, formData.getAction(),
        formData.getMessage(), userDetails.getLoginId(), userDetails.getUserType()))
        .thenReturn(Mono.just(new ClientTransactionResponse()));

    mockMvc.perform(post("/notifications/{notification-id}", notificationId)
            .sessionAttr("user", userDetails)
            .sessionAttr("notification", notification)
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("notifications/notification"));

    verify(notificationService).submitNotificationResponse(notificationId, formData.getAction(),
        formData.getMessage(), userDetails.getLoginId(), userDetails.getUserType());
  }

  @Test
  void submitNotificationResponseThrowsExceptionWhenSubmitNotificationFails() {
    Map<String, Object> flashMap = new HashMap<>();

    NotificationResponseFormData formData = new NotificationResponseFormData();
    formData.setAction("action");
    formData.setMessage("message");

    flashMap.put("user", userDetails);
    flashMap.put("notificationResponseFormData", formData);

    Notification notification = buildNotification();

    String notificationId = "12345";

    when(notificationService.submitNotificationResponse(notificationId, formData.getAction(),
        formData.getMessage(), userDetails.getLoginId(), userDetails.getUserType()))
        .thenReturn(Mono.empty());

    Exception exception = assertThrows(Exception.class, () ->
        mockMvc.perform(post("/notifications/{notification-id}", notificationId)
            .sessionAttr("user", userDetails)
            .sessionAttr("notification", notification)
            .flashAttrs(flashMap))
          .andExpect(status().isInternalServerError()));

    assertInstanceOf(CaabApplicationException.class, exception.getCause());
    assertEquals("Failed to submit notification response", exception.getCause().getMessage());
  }

  @Test
  void submitNotificationResponseHandlesValidationError() throws Exception {
    Map<String, Object> flashMap = new HashMap<>();

    NotificationResponseFormData formData = new NotificationResponseFormData();

    flashMap.put("user", userDetails);
    flashMap.put("notificationResponseFormData", formData);

    Notification notification = buildNotification();

    String notificationId = "12345";

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];

      errors.rejectValue("action", "required.action",
          "Please complete 'Notification response action'.");
      return null;
    }).when(notificationResponseValidator).validate(any(), any());

    mockMvc.perform(post("/notifications/{notification-id}", notificationId)
            .sessionAttr("user", userDetails)
            .sessionAttr("notification", notification)
            .flashAttrs(flashMap))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("notificationResponseFormData", "action"))
        .andExpect(view().name("notifications/notification"));

    verify(notificationService, never()).submitNotificationResponse(
        any(), any(), any(), any(), any());
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
