package uk.gov.laa.ccms.caab.controller.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.ClientActionConstants.ACTION_CREATE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_LANGUAGE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CORRESPONDENCE_METHOD;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CREATE_CLIENT_ADDRESS_FLOW;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.AddressLookupFlowData;
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.AddressSearchValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientContactDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.FindAddressValidator;
import uk.gov.laa.ccms.caab.client.OrdinanceSurveyApiClient;
import uk.gov.laa.ccms.caab.config.CreateClientRegistrationInterceptor;
import uk.gov.laa.ccms.caab.config.SessionStateLockFilter;
import uk.gov.laa.ccms.caab.mapper.AddressFormDataMapper;
import uk.gov.laa.ccms.caab.mapper.ClientAddressResultDisplayMapper;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.os.DeliveryPointAddress;
import uk.gov.laa.ccms.caab.model.os.OrdinanceSurveyResponse;
import uk.gov.laa.ccms.caab.model.os.OrdinanceSurveyResult;
import uk.gov.laa.ccms.caab.service.AddressService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.springboot.dialect.GovUkDialect;
import uk.gov.laa.springboot.dialect.MojCustomDialect;

/** Exercises the client address journey with real MVC handlers and HTML templates. */
@ExtendWith(MockitoExtension.class)
class ClientAddressJourneyTest {

  private static final String ADDRESS_PATH = "/application/client/details/address";

  @Mock private OrdinanceSurveyApiClient ordinanceSurveyApiClient;
  @Mock private LookupService lookupService;

  private MockMvc mockMvc;
  private MockHttpSession session;
  private ClientFlowFormData clientFlowFormData;
  private boolean supplyDefaultIdentity = true;

  @BeforeEach
  void setup() {
    final AddressService addressService =
        new AddressService(
            ordinanceSurveyApiClient,
            Mappers.getMapper(ClientAddressResultDisplayMapper.class),
            Mappers.getMapper(AddressFormDataMapper.class));
    mockMvc =
        standaloneSetup(
                new ClientAddressDetailsController(
                    addressService,
                    lookupService,
                    new ClientAddressDetailsValidator(),
                    new FindAddressValidator()),
                new ClientAddressDetailsSearchController(
                    addressService, new AddressSearchValidator()),
                new ClientContactDetailsController(
                    lookupService, new ClientContactDetailsValidator()))
            .addInterceptors(new CreateClientRegistrationInterceptor())
            .addFilters(new SessionStateLockFilter())
            .defaultRequest(
                get("/")
                    .with(
                        request -> {
                          if (!supplyDefaultIdentity) {
                            return request;
                          }
                          if (request.getParameterValues("registrationId") == null) {
                            request.addParameter(
                                "registrationId", clientFlowFormData.getRegistrationId());
                          }
                          if (session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW)
                                  instanceof AddressLookupFlowData<?> flow
                              && request.getParameterValues("flowId") == null) {
                            request.addParameter("flowId", flow.getFlowId());
                            request.addParameter("revision", Long.toString(flow.getRevision()));
                          }
                          return request;
                        }))
            .setViewResolvers(templateViewResolver())
            .build();

    session = new MockHttpSession();
    clientFlowFormData = new ClientFlowFormData(ACTION_CREATE);
    final ClientFormDataBasicDetails basicDetails = new ClientFormDataBasicDetails();
    basicDetails.setVulnerableClient(false);
    clientFlowFormData.setBasicDetails(basicDetails);
    session.setAttribute(CLIENT_FLOW_FORM_DATA, clientFlowFormData);
    when(lookupService.getCountries())
        .thenReturn(
            Mono.just(
                new CommonLookupDetail()
                    .addContentItem(
                        new CommonLookupValueDetail().code("GBR").description("United Kingdom"))));
  }

  @ParameterizedTest
  @ValueSource(strings = {"10", "missing"})
  void houseNumberSuffixMatchesAndFallbackResultsCanBeSelected(final String buildingSearch)
      throws Exception {
    mockMvc.perform(get(ADDRESS_PATH).session(session)).andExpect(status().isOk());
    final DeliveryPointAddress suffixedAddress = new DeliveryPointAddress();
    suffixedAddress.setUprn("1001");
    suffixedAddress.setBuildingNumber("10A");
    suffixedAddress.setThoroughfareName("Example Street");
    suffixedAddress.setPostTown("Swansea");
    suffixedAddress.setPostcode("SA5 8AB");
    suffixedAddress.setAddress("10A Example Street, Swansea, SA5 8AB");
    final DeliveryPointAddress otherAddress = new DeliveryPointAddress();
    otherAddress.setUprn("1002");
    otherAddress.setBuildingNumber("100");
    otherAddress.setThoroughfareName("Example Street");
    otherAddress.setPostTown("Swansea");
    otherAddress.setPostcode("SA5 8AB");
    otherAddress.setAddress("100 Example Street, Swansea, SA5 8AB");
    final OrdinanceSurveyResult first = new OrdinanceSurveyResult();
    first.setDeliveryPointAddress(suffixedAddress);
    final OrdinanceSurveyResult second = new OrdinanceSurveyResult();
    second.setDeliveryPointAddress(otherAddress);
    final OrdinanceSurveyResponse response = new OrdinanceSurveyResponse();
    response.setResults(List.of(first, second));
    when(ordinanceSurveyApiClient.getAddresses("SA5 8AB")).thenReturn(Mono.just(response));

    mockMvc
        .perform(
            post(ADDRESS_PATH)
                .session(session)
                .param("action", "find_address")
                .param("postcode", "sa58ab")
                .param("houseNameNumber", buildingSearch)
                .param("noFixedAbode", "false"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectTo(ADDRESS_PATH + "/search"));
    final AddressLookupFlowData<?> flow =
        (AddressLookupFlowData<?>) session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW);
    assertNotNull(flow);
    assertEquals(AddressLookupFlowData.Step.RESULTS, flow.getStep());
    assertEquals("10".equals(buildingSearch) ? 1 : 2, flow.getSearchResults().getContent().size());
    mockMvc
        .perform(get(ADDRESS_PATH + "/search").session(session))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Select an address")))
        .andExpect(content().string(containsString(suffixedAddress.getAddress())));

    mockMvc
        .perform(
            post(ADDRESS_PATH + "/search")
                .session(session)
                .param("uprn", "10".equals(buildingSearch) ? "1001" : "1002"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectTo(ADDRESS_PATH));
    assertEquals(AddressLookupFlowData.Step.CONFIRMED_ADDRESS, flow.getStep());
    assertEquals(
        "10".equals(buildingSearch) ? "10A" : "100",
        clientFlowFormData.getAddressDetails().getHouseNameNumber());
    assertEquals("GBR", clientFlowFormData.getAddressDetails().getCountry());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "/civil", "/custom-context"})
  void manualEntryCanReturnToSearchAfterValidationErrors(final String contextPath)
      throws Exception {
    final String addressUrl = contextPath + ADDRESS_PATH;
    mockMvc
        .perform(get(addressUrl).contextPath(contextPath).session(session))
        .andExpect(status().isOk());
    mockMvc
        .perform(get(addressUrl + "/manual").contextPath(contextPath).session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectTo(addressUrl));
    mockMvc
        .perform(get(addressUrl).contextPath(contextPath).session(session))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Search for an address")))
        .andExpect(
            content()
                .string(containsString("href=\"" + addressUrl + "/search-again?registrationId=")));

    final MvcResult invalidManualEntry =
        mockMvc
            .perform(
                post(addressUrl)
                    .contextPath(contextPath)
                    .session(session)
                    .param("action", "next")
                    .param("noFixedAbode", "false"))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasErrors("addressDetails"))
            .andReturn();
    final var searchLink =
        Pattern.compile("<a[^>]*href=\"([^\"]+)\"[^>]*>Search for an address</a>")
            .matcher(invalidManualEntry.getResponse().getContentAsString());
    assertTrue(searchLink.find(), "Manual entry must retain the search link after validation");
    String searchAgainUrl = HtmlUtils.htmlUnescape(searchLink.group(1));
    assertEquals(
        addressUrl + "/search-again",
        UriComponentsBuilder.fromUriString(searchAgainUrl).build().getPath());
    assertTrue(searchAgainUrl.contains("registrationId=" + clientFlowFormData.getRegistrationId()));
    mockMvc
        .perform(get(searchAgainUrl).contextPath(contextPath).session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectTo(addressUrl));
    mockMvc
        .perform(get(addressUrl).contextPath(contextPath).session(session))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Find an address")))
        .andExpect(content().string(containsString("name=\"postcode\"")));
    final AddressLookupFlowData<?> addressFlow =
        (AddressLookupFlowData<?>) session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW);
    assertNotNull(addressFlow);
    assertEquals(AddressLookupFlowData.Step.SEARCH, addressFlow.getStep());
    assertNull(addressFlow.getSelectedAddress());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "/civil", "/custom-context"})
  void searchSelectionReturnsDirectlyToPopulatedAddressFields(final String contextPath)
      throws Exception {
    final String addressUrl = contextPath + ADDRESS_PATH;
    mockMvc
        .perform(get(addressUrl).contextPath(contextPath).session(session))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("action=\"" + addressUrl + "\"")))
        .andExpect(
            content().string(containsString("href=\"" + addressUrl + "/manual?registrationId=")));

    final DeliveryPointAddress address = new DeliveryPointAddress();
    address.setUprn("1001");
    address.setBuildingNumber("10");
    address.setOrganisationName("Example Office");
    address.setThoroughfareName("Example Street");
    address.setPostTown("Swansea");
    address.setPostcode("SA5 8AB");
    address.setAddress("Example Office, 10 Example Street, Swansea, SA5 8AB");
    final OrdinanceSurveyResult result = new OrdinanceSurveyResult();
    result.setDeliveryPointAddress(address);
    final OrdinanceSurveyResponse response = new OrdinanceSurveyResponse();
    response.setResults(List.of(result));
    when(ordinanceSurveyApiClient.getAddresses("SA5 8AB")).thenReturn(Mono.just(response));

    mockMvc
        .perform(
            post(addressUrl)
                .contextPath(contextPath)
                .session(session)
                .param("action", "find_address")
                .param("postcode", "sa58ab")
                .param("houseNameNumber", "10")
                .param("noFixedAbode", "false"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectTo(addressUrl + "/search"));
    final AddressLookupFlowData<?> addressFlow =
        (AddressLookupFlowData<?>) session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW);
    assertNotNull(addressFlow);
    assertEquals(AddressLookupFlowData.Step.RESULTS, addressFlow.getStep());

    mockMvc
        .perform(get(addressUrl + "/search").contextPath(contextPath).session(session))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("action=\"" + addressUrl + "/search\"")))
        .andExpect(content().string(containsString(address.getAddress())));

    // A forged selection must not progress or populate the client address.
    mockMvc
        .perform(
            post(addressUrl + "/search")
                .contextPath(contextPath)
                .session(session)
                .param("uprn", "not-in-results"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("addressSearch", "uprn"));
    assertEquals(AddressLookupFlowData.Step.RESULTS, addressFlow.getStep());
    assertNull(clientFlowFormData.getAddressDetails());

    mockMvc
        .perform(
            post(addressUrl + "/search")
                .contextPath(contextPath)
                .session(session)
                .param("uprn", "1001"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectTo(addressUrl));
    assertEquals(AddressLookupFlowData.Step.CONFIRMED_ADDRESS, addressFlow.getStep());
    final ClientFormDataAddressDetails confirmedAddress = clientFlowFormData.getAddressDetails();
    assertNotNull(confirmedAddress);
    assertSame(confirmedAddress, addressFlow.getAddressDetails());
    assertEquals("Example Office", confirmedAddress.getAddressLine1());
    assertEquals("10,Example Street", confirmedAddress.getAddressLine2());
    assertEquals("SA5 8AB", confirmedAddress.getPostcode());

    final MvcResult addressPage =
        mockMvc
            .perform(get(addressUrl).contextPath(contextPath).session(session))
            .andExpect(status().isOk())
            .andExpect(model().attribute("addressDetails", confirmedAddress))
            .andExpect(content().string(containsString("action=\"" + addressUrl + "\"")))
            .andExpect(
                content()
                    .string(
                        containsString("href=\"" + addressUrl + "/search-again?registrationId=")))
            .andReturn();
    final String html = addressPage.getResponse().getContentAsString();
    for (final var field :
        Map.of(
                "houseNameNumber",
                "10",
                "addressLine1",
                "Example Office",
                "addressLine2",
                "10,Example Street",
                "cityTown",
                "Swansea",
                "postcode",
                "SA5 8AB")
            .entrySet()) {
      assertThat(
          html,
          matchesPattern(
              "(?s).*<fieldset[^>]*\\bdisabled\\b[^>]*>.*<input"
                  + "(?=[^>]*\\bname=\""
                  + field.getKey()
                  + "\")"
                  + "(?=[^>]*\\bvalue=\""
                  + field.getValue()
                  + "\")[^>]*>.*</fieldset>.*"));
    }
    assertThat(html, containsString("value=\"GBR\" selected=\"selected\">United Kingdom"));

    // Browsers omit disabled address fields; Next must use the trusted selection in the session.
    mockMvc
        .perform(
            post(addressUrl)
                .contextPath(contextPath)
                .session(session)
                .param("noFixedAbode", "false")
                .param("clientFlowFormAction", ACTION_CREATE))
        .andExpect(status().is3xxRedirection())
        .andExpect(
            redirectTo(contextPath + "/application/client/details/equal-opportunities-monitoring"));
    assertEquals(confirmedAddress, clientFlowFormData.getAddressDetails());
    assertSame(clientFlowFormData.getAddressDetails(), addressFlow.getAddressDetails());
    verify(ordinanceSurveyApiClient).getAddresses("SA5 8AB");
  }

  @ParameterizedTest
  @CsvSource({"'', true", "'/civil', true", "'', false", "'/civil', false"})
  void cancelFromResultsRestoresSearchAndManualLink(
      final String contextPath, final boolean hasResults) throws Exception {
    final MvcResult resultsPage = searchForNavigation(contextPath, hasResults);
    final AddressLookupFlowData<?> flow =
        (AddressLookupFlowData<?>) session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW);
    assertNotNull(flow);
    final Object draft = flow.getAddressDetails();
    final long revision = flow.getRevision();
    final String cancelUrl = link(resultsPage, "Cancel and return to address details");
    final var cancelIdentity = UriComponentsBuilder.fromUriString(cancelUrl).build();
    assertEquals(contextPath + ADDRESS_PATH, cancelIdentity.getPath());
    assertEquals(
        clientFlowFormData.getRegistrationId(),
        cancelIdentity.getQueryParams().getFirst("registrationId"));
    assertEquals(flow.getFlowId(), cancelIdentity.getQueryParams().getFirst("flowId"));

    final MvcResult addressPage =
        mockMvc
            .perform(get(cancelUrl).contextPath(contextPath).session(session))
            .andExpect(status().isOk())
            .andReturn();

    assertSearchComponent(addressPage);
    assertEquals(AddressLookupFlowData.Step.SEARCH, flow.getStep());
    assertSame(draft, flow.getAddressDetails());
    assertEquals("sa58ab", flow.getOriginalPostcode());
    assertEquals("10", flow.getOriginalBuildingSearch());
    assertEquals("SA5 8AB", flow.getNormalizedPostcode());
    assertNull(flow.getSearchResults());
    assertNull(flow.getSelectedAddress());
    assertTrue(flow.getRevision() > revision);
    assertEquals(Long.toString(flow.getRevision()), hidden(addressPage, "revision"));
  }

  @ParameterizedTest
  @CsvSource({"'', true", "'/civil', true", "'', false", "'/civil', false"})
  void backToContactThenNextRestoresAddressSearch(
      final String contextPath, final boolean hasResults) throws Exception {
    final MvcResult resultsPage = searchForNavigation(contextPath, hasResults);
    final MvcResult addressPage =
        mockMvc
            .perform(
                get(link(resultsPage, "Cancel and return to address details"))
                    .contextPath(contextPath)
                    .session(session))
            .andExpect(status().isOk())
            .andReturn();
    when(lookupService.getCommonValues(COMMON_VALUE_CORRESPONDENCE_METHOD))
        .thenReturn(Mono.just(new CommonLookupDetail()));
    when(lookupService.getCommonValues(COMMON_VALUE_CORRESPONDENCE_LANGUAGE))
        .thenReturn(Mono.just(new CommonLookupDetail()));
    final String contactUrl = link(addressPage, "Back");
    assertEquals(
        contextPath + "/application/client/details/contact",
        UriComponentsBuilder.fromUriString(contactUrl).build().getPath());
    final MvcResult contactPage =
        mockMvc
            .perform(get(contactUrl).contextPath(contextPath).session(session))
            .andExpect(status().isOk())
            .andReturn();
    final var form =
        Pattern.compile("<form[^>]*action=\"([^\"]+)\"")
            .matcher(contactPage.getResponse().getContentAsString());
    assertTrue(form.find(), "Contact page must render its Next form");
    final MvcResult next =
        mockMvc
            .perform(
                post(HtmlUtils.htmlUnescape(form.group(1)))
                    .contextPath(contextPath)
                    .session(session)
                    .param("registrationId", hidden(contactPage, "registrationId"))
                    .param("telephoneHomePresent", "true")
                    .param("telephoneHome", "01234567890")
                    .param("correspondenceMethod", "E-mail")
                    .param("emailAddress", "client@example.com")
                    .param("password", "example-password")
                    .param("passwordReminder", "example-reminder"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectTo(contextPath + ADDRESS_PATH))
            .andReturn();
    final String nextUrl = next.getResponse().getRedirectedUrl();
    assertNotNull(nextUrl);
    final MvcResult returnedAddress =
        mockMvc
            .perform(get(nextUrl).contextPath(contextPath).session(session))
            .andExpect(status().isOk())
            .andReturn();
    assertSearchComponent(returnedAddress);
    assertEquals("01234567890", clientFlowFormData.getContactDetails().getTelephoneHome());
  }

  @ParameterizedTest
  @CsvSource({
    "'', SEARCH", "'/civil', SEARCH",
    "'', MANUAL_ENTRY", "'/civil', MANUAL_ENTRY",
    "'', CONFIRMED_ADDRESS", "'/civil', CONFIRMED_ADDRESS",
    "'', LOOKUP_UNAVAILABLE", "'/civil', LOOKUP_UNAVAILABLE"
  })
  void addressGetPreservesSupportedStatesAndRevision(
      final String contextPath, final AddressLookupFlowData.Step step) throws Exception {
    supplyDefaultIdentity = false;
    final ClientFormDataAddressDetails draft = new ClientFormDataAddressDetails();
    draft.setPostcode("sa58ab");
    draft.setHouseNameNumber("10");
    draft.setAddressLine1("Draft address line");
    draft.setCityTown("Swansea");
    draft.setCountry("GBR");
    final AddressLookupFlowData<ClientFormDataAddressDetails> flow =
        new AddressLookupFlowData<>(CreateClientRegistrationInterceptor.owner(clientFlowFormData));
    flow.beginSearch("sa58ab", "10");
    flow.setAddressDetails(draft);
    switch (step) {
      case MANUAL_ENTRY -> flow.enterManualEntry(draft);
      case CONFIRMED_ADDRESS -> {
        final AddressResultRowDisplay selected = new AddressResultRowDisplay();
        selected.setUprn("1001");
        flow.setSelectedAddress(selected);
        flow.confirmSelectedAddress();
      }
      case LOOKUP_UNAVAILABLE -> flow.markLookupUnavailable();
      default -> assertEquals(AddressLookupFlowData.Step.SEARCH, step);
    }
    session.setAttribute(CREATE_CLIENT_ADDRESS_FLOW, flow);
    final long revision = flow.getRevision();
    final var source = flow.getSource();
    final var selected = flow.getSelectedAddress();

    for (int visit = 0; visit < 2; visit++) {
      final MvcResult page =
          mockMvc
              .perform(
                  get(contextPath + ADDRESS_PATH)
                      .contextPath(contextPath)
                      .session(session)
                      .param("registrationId", clientFlowFormData.getRegistrationId())
                      .param("flowId", flow.getFlowId()))
              .andExpect(status().isOk())
              .andExpect(model().attribute("addressDetails", draft))
              .andReturn();
      assertSame(flow, session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW));
      assertEquals(step, flow.getStep());
      assertEquals(source, flow.getSource());
      assertEquals(revision, flow.getRevision());
      assertEquals(Long.toString(revision), hidden(page, "revision"));
      assertSame(draft, flow.getAddressDetails());
      assertSame(selected, flow.getSelectedAddress());
      assertEquals("sa58ab", flow.getOriginalPostcode());
      assertEquals("10", flow.getOriginalBuildingSearch());
      assertEquals("SA5 8AB", flow.getNormalizedPostcode());
      if (step == AddressLookupFlowData.Step.MANUAL_ENTRY
          || step == AddressLookupFlowData.Step.CONFIRMED_ADDRESS) {
        final String html = page.getResponse().getContentAsString();
        assertThat(html, containsString("value=\"Draft address line\""));
        assertThat(html, not(containsString("value=\"find_address\"")));
        assertThat(
            html,
            step == AddressLookupFlowData.Step.CONFIRMED_ADDRESS
                ? matchesPattern("(?s).*<fieldset[^>]*\\bdisabled\\b[^>]*>.*</fieldset>.*")
                : not(matchesPattern("(?s).*<fieldset[^>]*\\bdisabled\\b[^>]*>.*</fieldset>.*")));
      } else {
        assertSearchComponent(page);
      }
    }
  }

  @ParameterizedTest
  @CsvSource({"true, true", "true, false", "false, true", "false, false"})
  void returningFromResultsRetainsExemptClientVisibility(
      final boolean vulnerable, final boolean hasResults) throws Exception {
    final MvcResult resultsPage = searchForNavigation("/civil", hasResults);
    final AddressLookupFlowData<?> flow =
        (AddressLookupFlowData<?>) session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW);
    assertNotNull(flow);
    final ClientFormDataAddressDetails draft =
        (ClientFormDataAddressDetails) flow.getAddressDetails();
    assertNotNull(draft);
    clientFlowFormData.getBasicDetails().setVulnerableClient(vulnerable);
    draft.setNoFixedAbode(!vulnerable);

    mockMvc
        .perform(
            get(link(resultsPage, "Cancel and return to address details"))
                .contextPath("/civil")
                .session(session))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("name=\"postcode\""))))
        .andExpect(content().string(not(containsString("name=\"addressLine1\""))))
        .andExpect(content().string(not(containsString("Enter address manually"))));
    assertEquals(AddressLookupFlowData.Step.SEARCH, flow.getStep());
    assertEquals(vulnerable, draft.getVulnerableClient());
    assertEquals(!vulnerable, draft.getNoFixedAbode());
    assertSame(draft, flow.getAddressDetails());
  }

  @ParameterizedTest
  @CsvSource({"'', true", "'/civil', true", "'', false", "'/civil', false"})
  void cancelInvalidatesOldResultsRevisionButFreshManualNavigationWorks(
      final String contextPath, final boolean hasResults) throws Exception {
    final MvcResult resultsPage = searchForNavigation(contextPath, hasResults);
    final String registrationId = hidden(resultsPage, "registrationId");
    final String flowId = hidden(resultsPage, "flowId");
    final String revision = hidden(resultsPage, "revision");
    final MvcResult addressPage =
        mockMvc
            .perform(
                get(link(resultsPage, "Cancel and return to address details"))
                    .contextPath(contextPath)
                    .session(session))
            .andExpect(status().isOk())
            .andReturn();
    final AddressLookupFlowData<?> flow =
        (AddressLookupFlowData<?>) session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW);
    assertNotNull(flow);
    final long currentRevision = flow.getRevision();

    mockMvc
        .perform(
            get(contextPath + ADDRESS_PATH + "/search")
                .contextPath(contextPath)
                .session(session)
                .param("registrationId", registrationId)
                .param("flowId", flowId)
                .param("revision", revision))
        .andExpect(status().isConflict());
    mockMvc
        .perform(
            post(contextPath + ADDRESS_PATH + "/search")
                .contextPath(contextPath)
                .session(session)
                .param("registrationId", registrationId)
                .param("flowId", flowId)
                .param("revision", revision)
                .param("uprn", "1001"))
        .andExpect(status().isConflict());
    assertEquals(currentRevision, flow.getRevision());
    assertEquals(AddressLookupFlowData.Step.SEARCH, flow.getStep());
    assertNull(flow.getSelectedAddress());
    assertNull(clientFlowFormData.getAddressDetails());
    mockMvc
        .perform(
            get(link(addressPage, "Enter address manually"))
                .contextPath(contextPath)
                .session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectTo(contextPath + ADDRESS_PATH));
    assertEquals(AddressLookupFlowData.Step.MANUAL_ENTRY, flow.getStep());
  }

  @ParameterizedTest
  @ValueSource(strings = {"registrationId", "flowId", "owner"})
  void invalidIdentityCannotNormalizeAnotherFlow(final String invalidIdentity) throws Exception {
    searchForNavigation("/civil", true);
    final AddressLookupFlowData<?> flow =
        (AddressLookupFlowData<?>) session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW);
    assertNotNull(flow);
    final long revision = flow.getRevision();
    final var candidates = flow.getSearchResults();
    if ("owner".equals(invalidIdentity)) {
      clientFlowFormData = new ClientFlowFormData(ACTION_CREATE);
      session.setAttribute(CLIENT_FLOW_FORM_DATA, clientFlowFormData);
    }
    mockMvc
        .perform(
            get("/civil" + ADDRESS_PATH)
                .contextPath("/civil")
                .session(session)
                .param(
                    "registrationId",
                    "registrationId".equals(invalidIdentity)
                        ? "another-registration"
                        : clientFlowFormData.getRegistrationId())
                .param(
                    "flowId", "flowId".equals(invalidIdentity) ? "another-flow" : flow.getFlowId()))
        .andExpect(status().isConflict());
    assertEquals(revision, flow.getRevision());
    assertEquals(AddressLookupFlowData.Step.RESULTS, flow.getStep());
    assertSame(candidates, flow.getSearchResults());
    assertSame(flow, session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW));
  }

  private MvcResult searchForNavigation(final String contextPath, final boolean hasResults)
      throws Exception {
    supplyDefaultIdentity = false;
    final String addressUrl = contextPath + ADDRESS_PATH;
    final MvcResult initialPage =
        mockMvc
            .perform(
                get(addressUrl)
                    .contextPath(contextPath)
                    .session(session)
                    .param("registrationId", clientFlowFormData.getRegistrationId()))
            .andExpect(status().isOk())
            .andReturn();
    final DeliveryPointAddress address = new DeliveryPointAddress();
    address.setUprn("1001");
    address.setBuildingNumber("10");
    address.setThoroughfareName("Example Street");
    address.setPostTown("Swansea");
    address.setPostcode("SA5 8AB");
    address.setAddress("10 Example Street, Swansea, SA5 8AB");
    final OrdinanceSurveyResult result = new OrdinanceSurveyResult();
    result.setDeliveryPointAddress(address);
    final OrdinanceSurveyResponse response = new OrdinanceSurveyResponse();
    response.setResults(hasResults ? List.of(result) : List.of());
    when(ordinanceSurveyApiClient.getAddresses("SA5 8AB")).thenReturn(Mono.just(response));
    final MvcResult search =
        mockMvc
            .perform(
                post(addressUrl)
                    .contextPath(contextPath)
                    .session(session)
                    .param("registrationId", hidden(initialPage, "registrationId"))
                    .param("flowId", hidden(initialPage, "flowId"))
                    .param("revision", hidden(initialPage, "revision"))
                    .param("action", "find_address")
                    .param("postcode", "sa58ab")
                    .param("houseNameNumber", "10")
                    .param("noFixedAbode", "false"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectTo(addressUrl + "/search"))
            .andReturn();
    final String resultsUrl = search.getResponse().getRedirectedUrl();
    assertNotNull(resultsUrl);
    return mockMvc
        .perform(get(resultsUrl).contextPath(contextPath).session(session))
        .andExpect(status().isOk())
        .andExpect(
            content().string(containsString(hasResults ? "Select an address" : "No address found")))
        .andReturn();
  }

  private void assertSearchComponent(final MvcResult page) throws Exception {
    final String html = page.getResponse().getContentAsString();
    assertThat(html, containsString("Find an address"));
    assertThat(html, containsString("name=\"postcode\""));
    assertThat(html, containsString("value=\"sa58ab\""));
    assertThat(html, containsString("value=\"10\""));
    assertThat(link(page, "Enter address manually"), containsString(ADDRESS_PATH + "/manual?"));
  }

  private String link(final MvcResult page, final String label) throws Exception {
    final var matcher =
        Pattern.compile("<a[^>]*href=\"([^\"]+)\"[^>]*>" + Pattern.quote(label) + "</a>")
            .matcher(page.getResponse().getContentAsString());
    assertTrue(matcher.find(), "Missing link: " + label);
    return HtmlUtils.htmlUnescape(matcher.group(1));
  }

  private String hidden(final MvcResult page, final String name) throws Exception {
    final var matcher =
        Pattern.compile("<input(?=[^>]*name=\"" + name + "\")[^>]*value=\"([^\"]*)\"")
            .matcher(page.getResponse().getContentAsString());
    assertTrue(matcher.find(), "Missing hidden field: " + name);
    return HtmlUtils.htmlUnescape(matcher.group(1));
  }

  private ResultMatcher redirectTo(String path) {
    return result -> {
      var uri = UriComponentsBuilder.fromUriString(result.getResponse().getRedirectedUrl()).build();
      assertEquals(path, uri.getPath());
      assertEquals(
          clientFlowFormData.getRegistrationId(), uri.getQueryParams().getFirst("registrationId"));
      assertEquals(1, uri.getQueryParams().get("registrationId").size());
      if (path.contains(ADDRESS_PATH)) {
        AddressLookupFlowData<?> flow =
            (AddressLookupFlowData<?>) session.getAttribute(CREATE_CLIENT_ADDRESS_FLOW);
        assertEquals(flow.getFlowId(), uri.getQueryParams().getFirst("flowId"));
      }
    };
  }

  private ThymeleafViewResolver templateViewResolver() {
    final GenericWebApplicationContext applicationContext =
        new GenericWebApplicationContext(new MockServletContext());
    applicationContext.refresh();
    final SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
    templateResolver.setApplicationContext(applicationContext);
    templateResolver.setPrefix("classpath:/templates/");
    templateResolver.setSuffix(".html");
    templateResolver.setTemplateMode(TemplateMode.HTML);
    templateResolver.setCharacterEncoding("UTF-8");
    final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("messages");
    messageSource.setDefaultEncoding("UTF-8");
    final SpringTemplateEngine templateEngine = new SpringTemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);
    templateEngine.setTemplateEngineMessageSource(messageSource);
    templateEngine.addDialect(new GovUkDialect());
    templateEngine.addDialect(new MojCustomDialect());
    final ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
    viewResolver.setTemplateEngine(templateEngine);
    viewResolver.setCharacterEncoding("UTF-8");
    final UserDetail user = new UserDetail();
    user.setFunctions(List.of());
    viewResolver.addStaticVariable("user", user);
    viewResolver.addStaticVariable("cspNonce", "test-nonce");
    viewResolver.addStaticVariable("researchPanelLink", "/research");
    return viewResolver;
  }
}
