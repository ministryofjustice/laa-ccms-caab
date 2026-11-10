package uk.gov.laa.ccms.caab.controller.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.ClientActionConstants.ACTION_CREATE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CREATE_CLIENT_ADDRESS_FLOW;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.support.GenericWebApplicationContext;
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
import uk.gov.laa.ccms.caab.bean.validators.client.FindAddressValidator;
import uk.gov.laa.ccms.caab.client.OrdinanceSurveyApiClient;
import uk.gov.laa.ccms.caab.mapper.AddressFormDataMapper;
import uk.gov.laa.ccms.caab.mapper.ClientAddressResultDisplayMapper;
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
                    addressService, new AddressSearchValidator()))
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
        .andExpect(redirectedUrl(ADDRESS_PATH + "/search"));
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
        .andExpect(redirectedUrl(ADDRESS_PATH));
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
        .andExpect(redirectedUrl(addressUrl));
    mockMvc
        .perform(get(addressUrl).contextPath(contextPath).session(session))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Search for an address")))
        .andExpect(content().string(containsString("href=\"" + addressUrl + "/search-again\"")));

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
    assertEquals(addressUrl + "/search-again", searchLink.group(1));
    mockMvc
        .perform(get(searchLink.group(1)).contextPath(contextPath).session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(addressUrl));
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
        .andExpect(content().string(containsString("href=\"" + addressUrl + "/manual\"")));

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
        .andExpect(redirectedUrl(addressUrl + "/search"));
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
        .andExpect(redirectedUrl(addressUrl));
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
            .andExpect(content().string(containsString("href=\"" + addressUrl + "/search-again\"")))
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
            redirectedUrl(
                contextPath + "/application/client/details/equal-opportunities-monitoring"));
    assertEquals(confirmedAddress, clientFlowFormData.getAddressDetails());
    assertSame(clientFlowFormData.getAddressDetails(), addressFlow.getAddressDetails());
    verify(ordinanceSurveyApiClient).getAddresses("SA5 8AB");
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
