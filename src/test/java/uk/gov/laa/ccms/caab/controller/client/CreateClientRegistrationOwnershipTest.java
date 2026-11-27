package uk.gov.laa.ccms.caab.controller.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.ClientActionConstants.ACTION_CREATE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CREATE_CLIENT_ADDRESS_FLOW;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.AddressFlowOwner;
import uk.gov.laa.ccms.caab.bean.AddressLookupFlowData;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.application.PrivacyNoticeAgreementValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.AddressSearchValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientBasicDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientContactDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientEqualOpportunitiesMonitoringDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.FindAddressValidator;
import uk.gov.laa.ccms.caab.config.CreateClientRegistrationInterceptor;
import uk.gov.laa.ccms.caab.config.SessionStateLockFilter;
import uk.gov.laa.ccms.caab.constants.SearchConstants;
import uk.gov.laa.ccms.caab.controller.application.PrivacyNoticeAgreementController;
import uk.gov.laa.ccms.caab.mapper.ClientDetailMapper;
import uk.gov.laa.ccms.caab.mapper.ResultDisplayMapper;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.security.SecurityEventLogger;
import uk.gov.laa.ccms.caab.service.AddressService;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/** Uses the production interceptor, not controller-only tests that bypass ownership checks. */
@ExtendWith(MockitoExtension.class)
class CreateClientRegistrationOwnershipTest {

  private static final String ADDRESS = "/application/client/details/address";
  @Mock private AddressService addressService;
  @Mock private LookupService lookupService;
  @Mock private ClientService clientService;
  @Mock private SecurityEventLogger securityEventLogger;
  @Mock private ClientBasicDetailsValidator basicValidator;
  @Mock private ClientContactDetailsValidator contactValidator;
  @Mock private ClientEqualOpportunitiesMonitoringDetailsValidator monitoringValidator;
  @Mock private PrivacyNoticeAgreementValidator privacyValidator;
  @Mock private ClientDetailMapper clientMapper;
  @Mock private ResultDisplayMapper resultMapper;
  @Mock private SearchConstants searchConstants;
  private MockMvc mvc;
  private MockHttpSession session;
  private ClientFlowFormData client;

  @BeforeEach
  void setup() {
    mvc =
        standaloneSetup(
                new ClientAddressDetailsController(
                    addressService,
                    lookupService,
                    new ClientAddressDetailsValidator(),
                    new FindAddressValidator()),
                new ClientAddressDetailsSearchController(
                    addressService, new AddressSearchValidator()),
                new CancelClientRegistrationController(),
                new ClientSearchResultsController(
                    clientService, securityEventLogger, resultMapper, searchConstants),
                new ClientBasicDetailsController(lookupService, basicValidator),
                new ClientContactDetailsController(lookupService, contactValidator),
                new ClientEqualOpportunitiesMonitoringDetailsController(
                    lookupService, monitoringValidator),
                new ClientSummaryController(lookupService, clientService, clientMapper),
                new PrivacyNoticeAgreementController(privacyValidator))
            .addInterceptors(new CreateClientRegistrationInterceptor())
            .addFilters(new SessionStateLockFilter())
            .build();
    session = new MockHttpSession();
    client = newClient();
    session.setAttribute(CLIENT_FLOW_FORM_DATA, client);
    session.setAttribute(CLIENT_SEARCH_CRITERIA, new ClientSearchCriteria());
    session.setAttribute(APPLICATION_FORM_DATA, new ApplicationFormData());
    session.setAttribute(USER_DETAILS, new UserDetail());
  }

  @ParameterizedTest
  @CsvSource({
    "GET,basic",
    "POST,basic",
    "GET,contact",
    "POST,contact",
    "GET,equal-opportunities-monitoring",
    "POST,equal-opportunities-monitoring",
    "GET,summary",
    "POST,summary",
    "GET,cancel",
    "POST,cancel",
    "GET,address",
    "POST,address"
  })
  void oldRegistrationIsRejectedBeforeSessionFormBinding(String method, String step)
      throws Exception {
    var request =
        "GET".equals(method)
            ? get("/application/client/details/" + step)
            : post("/application/client/details/" + step);
    mvc.perform(
            request
                .session(session)
                .param("registrationId", newClient().getRegistrationId())
                .param("basicDetails.firstName", "Must not bind"))
        .andExpect(status().isConflict());
    assertSame(client, session.getAttribute(CLIENT_FLOW_FORM_DATA));
    assertEquals("Current client", client.getBasicDetails().getFirstName());
    assertNull(client.getAddressDetails());
    verifyNoInteractions(
        addressService,
        clientService,
        lookupService,
        basicValidator,
        contactValidator,
        monitoringValidator,
        privacyValidator);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"unknown"})
  void missingOrInvalidRegistrationCannotBeReplacedBySessionIdentity(String identity)
      throws Exception {
    var request = get(ADDRESS).session(session);
    if (identity != null) {
      request.param("registrationId", identity);
    }
    mvc.perform(request).andExpect(status().isConflict());
    assertNull(session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW));
    verifyNoInteractions(lookupService);
  }

  @Test
  void duplicateRegistrationAndFlowParametersAreRejected() throws Exception {
    mvc.perform(
            get(ADDRESS)
                .session(session)
                .param("registrationId", client.getRegistrationId(), client.getRegistrationId()))
        .andExpect(status().isConflict());
    AddressLookupFlowData<ClientFormDataAddressDetails> flow = installFlow(client);
    mvc.perform(
            get(ADDRESS + "/manual")
                .session(session)
                .param("registrationId", client.getRegistrationId())
                .param("flowId", flow.getFlowId(), flow.getFlowId()))
        .andExpect(status().isConflict());
    assertEquals(0L, flow.getRevision());
  }

  @Test
  void freshEntryDoesNotDisplayAnAddressOwnedByAnotherRegistration() throws Exception {
    AddressLookupFlowData<ClientFormDataAddressDetails> oldFlow = installFlow(newClient());
    ClientFormDataAddressDetails oldAddress = new ClientFormDataAddressDetails();
    oldAddress.setAddressLine1("Cancelled client address");
    oldFlow.setAddressDetails(oldAddress);
    when(lookupService.getCountries()).thenReturn(Mono.just(new CommonLookupDetail()));
    mvc.perform(get(ADDRESS).session(session).param("registrationId", client.getRegistrationId()))
        .andExpect(status().isOk())
        .andExpect(model().attribute("registrationId", client.getRegistrationId()));
    var flow = (AddressLookupFlowData<?>) session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW);
    assertEquals(CreateClientRegistrationInterceptor.owner(client), flow.getOwner());
    assertNotEquals(oldFlow.getFlowId(), flow.getFlowId());
    assertNull(flow.getAddressDetails());
    assertNull(client.getAddressDetails());
  }

  @ParameterizedTest
  @CsvSource({
    "GET,address",
    "POST,address",
    "GET,address/search",
    "POST,address/search",
    "GET,address/manual",
    "GET,address/search-again"
  })
  void supersededFlowCannotReadOrMutateTheReplacement(String method, String step) throws Exception {
    var oldFlow =
        new AddressLookupFlowData<ClientFormDataAddressDetails>(
            CreateClientRegistrationInterceptor.owner(client));
    var activeFlow = installFlow(client);
    var request =
        "GET".equals(method)
            ? get("/application/client/details/" + step)
            : post("/application/client/details/" + step);
    mvc.perform(
            request
                .session(session)
                .param("registrationId", client.getRegistrationId())
                .param("flowId", oldFlow.getFlowId())
                .param("action", "next"))
        .andExpect(status().isConflict());
    assertSame(activeFlow, session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW));
    assertEquals(0L, activeFlow.getRevision());
    assertNull(client.getAddressDetails());
    verifyNoInteractions(addressService, lookupService);
  }

  @Test
  void anotherAddressPurposeCannotBeUsedEvenWithTheMatchingFlowId() throws Exception {
    var flow =
        new AddressLookupFlowData<>(
            new AddressFlowOwner(client.getRegistrationId(), "correspondence-address"));
    session.setAttribute(CREATE_CLIENT_ADDRESS_FLOW, flow);
    mvc.perform(
            get(ADDRESS + "/manual")
                .session(session)
                .param("registrationId", client.getRegistrationId())
                .param("flowId", flow.getFlowId()))
        .andExpect(status().isConflict());
    assertSame(flow, session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW));
    assertEquals(0L, flow.getRevision());
  }

  @Test
  void cancellationRotatesRegistrationAndDiscardsAllAddressState() throws Exception {
    var flow = installFlow(client);
    var selected = new AddressResultRowDisplay();
    selected.setUprn("old-address");
    flow.setSelectedAddress(selected);
    flow.confirmSelectedAddress();
    mvc.perform(
            post("/application/client/details/cancel")
                .session(session)
                .param("registrationId", client.getRegistrationId()))
        .andExpect(redirectedUrl("/application/client/search"));
    var replacement = (ClientFlowFormData) session.getAttribute(CLIENT_FLOW_FORM_DATA);
    assertNotEquals(client.getRegistrationId(), replacement.getRegistrationId());
    assertNull(session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW));
    mvc.perform(
            post(ADDRESS)
                .session(session)
                .param("registrationId", client.getRegistrationId())
                .param("flowId", flow.getFlowId())
                .param("action", "next"))
        .andExpect(status().isConflict());
    assertSame(replacement, session.getAttribute(CLIENT_FLOW_FORM_DATA));
    assertNull(replacement.getAddressDetails());
    verifyNoInteractions(addressService);
  }

  @Test
  void registerNewClientClearsAddressStateAndRedirectsWithTheNewIdentity() throws Exception {
    installFlow(client);
    var result =
        mvc.perform(
                post("/application/client/results")
                    .session(session)
                    .param("registrationId", client.getRegistrationId()))
            .andExpect(status().is3xxRedirection())
            .andReturn();
    var replacement = (ClientFlowFormData) session.getAttribute(CLIENT_FLOW_FORM_DATA);
    var uri = UriComponentsBuilder.fromUriString(result.getResponse().getRedirectedUrl()).build();
    assertEquals("/application/agreement", uri.getPath());
    assertEquals(
        List.of(replacement.getRegistrationId()), uri.getQueryParams().get("registrationId"));
    assertNotEquals(client.getRegistrationId(), replacement.getRegistrationId());
    assertNull(session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW));
    mvc.perform(
            post("/application/client/results")
                .session(session)
                .param("registrationId", client.getRegistrationId()))
        .andExpect(status().isConflict());
    assertSame(replacement, session.getAttribute(CLIENT_FLOW_FORM_DATA));
  }

  @Test
  void oldCancellationCannotDiscardANewRegistrationsAddress() throws Exception {
    var activeFlow = installFlow(client);
    mvc.perform(
            post("/application/client/details/cancel")
                .session(session)
                .param("registrationId", newClient().getRegistrationId()))
        .andExpect(status().isConflict());
    assertSame(activeFlow, session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW));
    assertSame(client, session.getAttribute(CLIENT_FLOW_FORM_DATA));
  }

  @Test
  void searchResultsExposeTheIdentityOfTheRenderedRegistration() throws Exception {
    when(clientService.getClients(any(), any(), any())).thenReturn(Mono.empty());
    mvc.perform(get("/application/client/results").session(session))
        .andExpect(status().isOk())
        .andExpect(model().attribute("registrationId", client.getRegistrationId()));
  }

  @Test
  void agreementCannotBindAnOldRegistrationsSubmission() throws Exception {
    mvc.perform(
            post("/application/agreement")
                .session(session)
                .param("registrationId", newClient().getRegistrationId())
                .param("agreementAccepted", "true"))
        .andExpect(status().isConflict());
    verifyNoInteractions(privacyValidator);
  }

  @Test
  void missingAddressFlowOnPostDoesNotCreateOrSaveAnAddress() throws Exception {
    mvc.perform(
            post(ADDRESS)
                .session(session)
                .param("registrationId", client.getRegistrationId())
                .param("action", "next"))
        .andExpect(status().isConflict());
    assertNull(session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW));
    assertNull(client.getAddressDetails());
  }

  @Test
  void resultsSelectionKeepsTheConfirmedAddressStep() throws Exception {
    var flow = installFlow(client);
    flow.setAddressDetails(new ClientFormDataAddressDetails());
    var selected = new AddressResultRowDisplay();
    selected.setUprn("123");
    var results = new ResultsDisplay<AddressResultRowDisplay>();
    results.setContent(List.of(selected));
    flow.setSearchResults(results);
    when(addressService.getSelectedAddress("123", results)).thenReturn(selected);
    mvc.perform(
            post(ADDRESS + "/search")
                .session(session)
                .param("registrationId", client.getRegistrationId())
                .param("flowId", flow.getFlowId())
                .param("revision", Long.toString(flow.getRevision()))
                .param("uprn", "123"))
        .andExpect(status().is3xxRedirection());
    assertEquals(AddressLookupFlowData.Step.CONFIRMED_ADDRESS, flow.getStep());
    assertSame(selected, flow.getSelectedAddress());
    assertSame(flow.getAddressDetails(), client.getAddressDetails());
  }

  @Test
  void missingFlowIdentityDoesNotAttachToAnExistingAddressFlow() throws Exception {
    var flow = installFlow(client);
    mvc.perform(get(ADDRESS).session(session).param("registrationId", client.getRegistrationId()))
        .andExpect(status().isConflict());
    assertSame(flow, session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW));
    assertEquals(0L, flow.getRevision());
  }

  @Test
  void pathParametersCannotBypassAddressIdentityValidation() throws Exception {
    var flow = installFlow(client);
    mvc.perform(
            post(ADDRESS + ";ignored=value")
                .session(session)
                .param("registrationId", client.getRegistrationId())
                .param("action", "next"))
        .andExpect(status().isConflict());
    assertSame(flow, session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW));
    assertEquals(0L, flow.getRevision());
    verifyNoInteractions(addressService, lookupService);
  }

  @Test
  void firstRegistrationCanStartWithoutAnExistingRegistration() throws Exception {
    session.removeAttribute(CLIENT_FLOW_FORM_DATA);
    var result =
        mvc.perform(
                post("/application/client/results").session(session).param("registrationId", ""))
            .andExpect(status().is3xxRedirection())
            .andReturn();
    var replacement = (ClientFlowFormData) session.getAttribute(CLIENT_FLOW_FORM_DATA);
    var uri = UriComponentsBuilder.fromUriString(result.getResponse().getRedirectedUrl()).build();
    assertEquals("/application/agreement", uri.getPath());
    assertEquals(
        List.of(replacement.getRegistrationId()), uri.getQueryParams().get("registrationId"));
  }

  @Test
  void oldEntryCannotRecreateAnExpiredRegistration() throws Exception {
    session.removeAttribute(CLIENT_FLOW_FORM_DATA);
    mvc.perform(
            post("/application/client/results")
                .session(session)
                .param("registrationId", client.getRegistrationId()))
        .andExpect(status().isConflict());
    assertNull(session.getAttribute(CLIENT_FLOW_FORM_DATA));
  }

  private AddressLookupFlowData<ClientFormDataAddressDetails> installFlow(
      ClientFlowFormData owner) {
    var flow =
        new AddressLookupFlowData<ClientFormDataAddressDetails>(
            CreateClientRegistrationInterceptor.owner(owner));
    session.setAttribute(CREATE_CLIENT_ADDRESS_FLOW, flow);
    return flow;
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"invalid", "-1", "1", "9223372036854775808"})
  void invalidRevisionsCannotMutateTheAddressStep(String revision) throws Exception {
    var flow = installFlow(client);
    var request =
        get(ADDRESS + "/manual")
            .session(session)
            .param("registrationId", client.getRegistrationId())
            .param("flowId", flow.getFlowId());
    if (revision != null) {
      request.param("revision", revision);
    }
    mvc.perform(request).andExpect(status().isConflict());
    assertEquals(AddressLookupFlowData.Step.SEARCH, flow.getStep());
    assertEquals(0L, flow.getRevision());
  }

  @Test
  void duplicateRevisionsAreRejected() throws Exception {
    var flow = installFlow(client);
    mvc.perform(
            get(ADDRESS + "/manual")
                .session(session)
                .param("registrationId", client.getRegistrationId())
                .param("flowId", flow.getFlowId())
                .param("revision", "0", "0"))
        .andExpect(status().isConflict());
    assertEquals(0L, flow.getRevision());
  }

  @Test
  void oldResultsRevisionCannotDisplayANewerSearch() throws Exception {
    var flow = installFlow(client);
    flow.setSearchResults(new ResultsDisplay<>());
    mvc.perform(
            get(ADDRESS + "/search")
                .session(session)
                .param("registrationId", client.getRegistrationId())
                .param("flowId", flow.getFlowId())
                .param("revision", "0"))
        .andExpect(status().isConflict());
    assertEquals(1L, flow.getRevision());
    assertEquals(AddressLookupFlowData.Step.NO_RESULTS, flow.getStep());
  }

  @Test
  void onlyOneConcurrentTransitionCanConsumeTheSameRevision() throws Exception {
    var flow = installFlow(client);
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      Callable<Integer> transition =
          () -> {
            assertTrue(start.await(5, TimeUnit.SECONDS));
            return mvc.perform(
                    get(ADDRESS + "/manual")
                        .session(session)
                        .param("registrationId", client.getRegistrationId())
                        .param("flowId", flow.getFlowId())
                        .param("revision", "0"))
                .andReturn()
                .getResponse()
                .getStatus();
          };
      var first = executor.submit(transition);
      var second = executor.submit(transition);
      start.countDown();
      assertEquals(
          Set.of(302, 409),
          Set.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS)));
    }
    assertEquals(1L, flow.getRevision());
    assertEquals(AddressLookupFlowData.Step.MANUAL_ENTRY, flow.getStep());
  }

  @Test
  void cancellationCannotBeUndoneByALookupOrSpringSessionWriteBack() throws Exception {
    var flow = installFlow(client);
    var enteredLookup = new CountDownLatch(1);
    var releaseLookup = new CountDownLatch(1);
    var startedCancel = new CountDownLatch(1);
    when(addressService.getAddresses("SW1A 1AA"))
        .thenAnswer(
            invocation -> {
              enteredLookup.countDown();
              assertTrue(releaseLookup.await(5, TimeUnit.SECONDS));
              return new ResultsDisplay<AddressResultRowDisplay>();
            });
    try (var executor = Executors.newFixedThreadPool(2)) {
      var search =
          executor.submit(
              () ->
                  mvc.perform(
                          post(ADDRESS)
                              .session(session)
                              .param("registrationId", client.getRegistrationId())
                              .param("flowId", flow.getFlowId())
                              .param("revision", "0")
                              .param("action", "find_address")
                              .param("postcode", "SW1A 1AA"))
                      .andReturn());
      try {
        assertTrue(enteredLookup.await(5, TimeUnit.SECONDS));
        var cancel =
            executor.submit(
                () -> {
                  startedCancel.countDown();
                  return mvc.perform(
                          post("/application/client/details/cancel")
                              .session(session)
                              .param("registrationId", client.getRegistrationId()))
                      .andReturn();
                });
        assertTrue(startedCancel.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> cancel.get(100, TimeUnit.MILLISECONDS));
        releaseLookup.countDown();
        assertEquals(302, search.get(5, TimeUnit.SECONDS).getResponse().getStatus());
        assertEquals(302, cancel.get(5, TimeUnit.SECONDS).getResponse().getStatus());
      } finally {
        releaseLookup.countDown();
      }
    }
    var replacement = (ClientFlowFormData) session.getAttribute(CLIENT_FLOW_FORM_DATA);
    assertNotEquals(client.getRegistrationId(), replacement.getRegistrationId());
    assertNull(session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW));
    assertNull(replacement.getAddressDetails());
    mvc.perform(
            post(ADDRESS)
                .session(session)
                .param("registrationId", client.getRegistrationId())
                .param("flowId", flow.getFlowId())
                .param("revision", Long.toString(flow.getRevision()))
                .param("action", "next"))
        .andExpect(status().isConflict());
    assertSame(replacement, session.getAttribute(CLIENT_FLOW_FORM_DATA));
    assertNull(session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW));
  }

  private ClientFlowFormData newClient() {
    var data = new ClientFlowFormData(ACTION_CREATE);
    var basic = new ClientFormDataBasicDetails();
    basic.setFirstName("Current client");
    basic.setVulnerableClient(false);
    data.setBasicDetails(basic);
    return data;
  }
}
