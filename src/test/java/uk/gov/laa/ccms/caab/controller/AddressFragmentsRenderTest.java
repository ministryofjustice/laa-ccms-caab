package uk.gov.laa.ccms.caab.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import uk.gov.laa.ccms.caab.bean.AddressFormData;
import uk.gov.laa.ccms.caab.bean.AddressLookupFlowData;
import uk.gov.laa.ccms.caab.bean.AddressSearchFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataAddressDetails;
import uk.gov.laa.ccms.caab.model.AddressResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.springboot.dialect.GovUkDialect;
import uk.gov.laa.springboot.dialect.MojCustomDialect;

class AddressFragmentsRenderTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
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

    mockMvc = standaloneSetup(new HarnessController()).setViewResolvers(viewResolver).build();
  }

  @Test
  void resultsReplayEscapedTermsAndRenderUnselectedLinkedRadios() throws Exception {
    mockMvc
        .perform(get("/test/address-fragments/results"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
        .andExpect(content().string(containsString("&lt;img src=x&gt;")))
        .andExpect(content().string(not(containsString("<script>"))))
        .andExpect(content().string(containsString("id=\"test-address-result-0\"")))
        .andExpect(content().string(containsString("for=\"test-address-result-0\"")))
        .andExpect(content().string(containsString("id=\"test-address-result-1\"")))
        .andExpect(content().string(not(containsString(" checked"))))
        .andExpect(content().string(containsString("href=\"/test/search\"")))
        .andExpect(content().string(containsString("href=\"/test/manual\"")))
        .andExpect(content().string(not(containsString("Next page"))));
  }

  @Test
  void searchFieldsUsePostcodeAndAddressAutocompleteTokens() throws Exception {
    mockMvc
        .perform(get("/test/address-fragments/search"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("autocomplete=\"postal-code\"")))
        .andExpect(content().string(containsString("autocomplete=\"address-line1\"")))
        .andExpect(content().string(containsString("For example, SW1A 1AA")))
        .andExpect(content().string(containsString("Enter address manually")));
  }

  @Test
  void manualFieldsRenderBlankWithAutocompleteAndLinkedErrors() throws Exception {
    mockMvc
        .perform(get("/test/address-fragments/manual"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("id=\"addressLine1\"")))
        .andExpect(content().string(containsString("name=\"addressLine1\"")))
        .andExpect(content().string(containsString("value=\"\"")))
        .andExpect(content().string(containsString("id=\"addressLine1-error\"")))
        .andExpect(content().string(containsString("aria-describedby=\"addressLine1-error\"")))
        .andExpect(content().string(containsString("Enter address line 1")))
        .andExpect(content().string(containsString("id=\"addressLine2\"")))
        .andExpect(content().string(containsString("id=\"country-error\"")))
        .andExpect(content().string(containsString("aria-describedby=\"country-error\"")))
        .andExpect(content().string(containsString("autocomplete=\"address-line1\"")))
        .andExpect(content().string(containsString("autocomplete=\"address-line2\"")))
        .andExpect(content().string(containsString("autocomplete=\"address-level2\"")))
        .andExpect(content().string(containsString("autocomplete=\"address-level1\"")))
        .andExpect(content().string(containsString("autocomplete=\"postal-code\"")))
        .andExpect(content().string(containsString("autocomplete=\"country\"")));
  }

  @Test
  void noResultsAndConfirmationUseProvidedNavigationAndEscapeAddressText() throws Exception {
    mockMvc
        .perform(get("/test/address-fragments/no-results"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No addresses matched your search.")))
        .andExpect(content().string(containsString("href=\"/test/search\"")))
        .andExpect(content().string(containsString("href=\"/test/manual\"")));

    mockMvc
        .perform(get("/test/address-fragments/confirmation"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("&lt;img src=x onerror=alert(1)&gt;")))
        .andExpect(content().string(not(containsString("<img src=x"))))
        .andExpect(content().string(containsString("action=\"/test/confirm\"")))
        .andExpect(content().string(containsString("href=\"/test/change\"")));
  }

  @Test
  void existingAddressIsDisplayedReadOnly() throws Exception {
    mockMvc
        .perform(get("/test/address-fragments/existing"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("12A")))
        .andExpect(content().string(containsString("Office")))
        .andExpect(content().string(containsString("United Kingdom")))
        .andExpect(content().string(containsString("href=\"/test/change\"")))
        .andExpect(content().string(not(containsString("name=\"addressLine1\""))));
  }

  @Test
  void clientAddressComponentRendersOnlyTheControlsForItsCurrentState() throws Exception {
    mockMvc
        .perform(get("/test/address-client-component/confirmed"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("10 Example Street")))
        .andExpect(content().string(not(containsString("name=\"postcode\""))))
        .andExpect(content().string(not(containsString("name=\"addressLine1\""))));

    mockMvc
        .perform(get("/test/address-client-component/manual"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("id=\"addressLine1\"")))
        .andExpect(content().string(containsString("name=\"addressLine1\"")))
        .andExpect(content().string(not(containsString("id=\"find-address-button\""))));

    mockMvc
        .perform(get("/test/address-client-component/search"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("name=\"postcode\"")))
        .andExpect(content().string(not(containsString("name=\"addressLine1\""))));

    mockMvc
        .perform(get("/test/address-client-component/legacy"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("name=\"postcode\"")))
        .andExpect(content().string(containsString("name=\"addressLine1\"")))
        .andExpect(content().string(not(containsString("id=\"postcode-hint\""))));
  }

  @Test
  void clientAddressHostTemplateRendersSearchManualAndConfirmedWithoutNestedForms()
      throws Exception {
    final MvcResult searchResult =
        mockMvc
            .perform(get("/test/address-client-page/search"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"postcode\"")))
            .andExpect(content().string(containsString("Enter address manually")))
            .andReturn();
    assertEquals(1, formCount(searchResult));

    final MvcResult manualResult =
        mockMvc
            .perform(get("/test/address-client-page/manual"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"addressLine1\"")))
            .andExpect(content().string(not(containsString("id=\"find-address-button\""))))
            .andReturn();
    assertEquals(1, formCount(manualResult));

    final MvcResult confirmedResult =
        mockMvc
            .perform(get("/test/address-client-page/confirmed"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("10 Example Street")))
            .andExpect(content().string(not(containsString("name=\"postcode\""))))
            .andExpect(content().string(not(containsString("name=\"addressLine1\""))))
            .andReturn();
    assertEquals(1, formCount(confirmedResult));

    final MvcResult resultsPage =
        mockMvc
            .perform(get("/test/address-client-page/results"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("10 Example Street")))
            .andExpect(content().string(containsString("Use this address")))
            .andReturn();
    assertEquals(1, formCount(resultsPage));

    final MvcResult confirmationPage =
        mockMvc
            .perform(get("/test/address-client-page/confirmation"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("10 Example Street")))
            .andExpect(content().string(containsString("Use a different address")))
            .andReturn();
    assertEquals(1, formCount(confirmationPage));
  }

  private int formCount(final MvcResult result) throws Exception {
    return result.getResponse().getContentAsString().split("<form").length - 1;
  }

  @Controller
  static class HarnessController {

    @GetMapping("/test/address-fragments/{view}")
    public String render(@PathVariable final String view, final Model model) {
      final AddressFormData addressDetails = new AddressFormData();
      if ("existing".equals(view)) {
        addressDetails.setHouseNameNumber("12A");
        addressDetails.setCareOf("Office");
        addressDetails.setAddressLine1("12 Example Street");
      }
      final AddressResultRowDisplay firstResult = new AddressResultRowDisplay();
      firstResult.setUprn("1001");
      firstResult.setFullAddress("<img src=x onerror=alert(1)>");
      final AddressResultRowDisplay secondResult = new AddressResultRowDisplay();
      secondResult.setUprn("1002");
      secondResult.setFullAddress("10 Example Street, London");

      model.addAttribute("view", view);
      model.addAttribute("addressDetails", addressDetails);
      model.addAttribute("addressSearch", new AddressSearchFormData());
      model.addAttribute(
          "addressSearchResults", new ResultsDisplay<>(List.of(firstResult, secondResult)));
      model.addAttribute("originalPostcode", "<script>alert(1)</script>");
      model.addAttribute("originalBuildingSearch", "<img src=x>");
      model.addAttribute("selectedAddress", firstResult);
      model.addAttribute("countryName", "United Kingdom");
      model.addAttribute(
          "countries",
          new CommonLookupDetail()
              .addContentItem(
                  new CommonLookupValueDetail().code("GBR").description("United Kingdom"))
              .getContent());

      if ("manual".equals(view)) {
        final BindingResult bindingResult =
            new BeanPropertyBindingResult(addressDetails, "addressDetails");
        bindingResult.rejectValue("addressLine1", "required", "Enter address line 1");
        bindingResult.rejectValue("country", "required", "Select a country");
        model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "addressDetails", bindingResult);
      }

      return "test/address-fragments-harness";
    }

    @GetMapping("/test/address-client-component/{state}")
    public String renderClientAddressComponent(
        @PathVariable final String state, final Model model) {
      final ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();
      addressDetails.setVulnerableClient(false);
      addressDetails.setNoFixedAbode(false);
      final AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow =
          new AddressLookupFlowData<>("create-client");
      final AddressResultRowDisplay selectedAddress = new AddressResultRowDisplay();
      selectedAddress.setUprn("1001");
      selectedAddress.setFullAddress("10 Example Street");

      if ("confirmed".equals(state)) {
        addressFlow.setSelectedAddress(selectedAddress);
        addressFlow.confirmSelectedAddress();
      } else if ("manual".equals(state)) {
        addressFlow.enterManualEntry(addressDetails);
      } else if ("unavailable".equals(state)) {
        addressFlow.markLookupUnavailable();
      }

      model.addAttribute("addressDetails", addressDetails);
      model.addAttribute("addressFlow", addressFlow);
      model.addAttribute("selectedAddress", selectedAddress);
      model.addAttribute("componentEnabled", !"legacy".equals(state));
      model.addAttribute("manualEntryUrl", "/test/manual");
      model.addAttribute("changeAddressUrl", "/test/search");
      model.addAttribute(
          "countries",
          List.of(new CommonLookupValueDetail().code("GBR").description("United Kingdom")));
      model.addAttribute(
          BindingResult.MODEL_KEY_PREFIX + "addressDetails",
          new BeanPropertyBindingResult(addressDetails, "addressDetails"));
      return "test/address-client-component-harness";
    }

    @GetMapping("/test/address-client-page/{state}")
    public String renderClientAddressPage(@PathVariable final String state, final Model model) {
      if ("results".equals(state)) {
        final AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow =
            new AddressLookupFlowData<>("create-client");
        final AddressResultRowDisplay result = new AddressResultRowDisplay();
        result.setUprn("1001");
        result.setFullAddress("10 Example Street");
        addressFlow.setSearchResults(new ResultsDisplay<>(List.of(result)));
        model.addAttribute("addressSearch", new AddressSearchFormData());
        model.addAttribute("addressSearchResults", addressFlow.getSearchResults());
        model.addAttribute("addressFlow", addressFlow);
        model.addAttribute("originalPostcode", "SA5 8AB");
        model.addAttribute("originalBuildingSearch", "10");
        model.addAttribute("searchAgainUrl", "/application/client/details/address/search-again");
        model.addAttribute("manualEntryUrl", "/application/client/details/address/manual");
        addLayoutAttributes(model);
        model.addAttribute(
            BindingResult.MODEL_KEY_PREFIX + "addressSearch",
            new BeanPropertyBindingResult(new AddressSearchFormData(), "addressSearch"));
        return "application/client/address-client-search-results";
      }
      if ("confirmation".equals(state)) {
        final AddressResultRowDisplay selectedAddress = new AddressResultRowDisplay();
        selectedAddress.setFullAddress("10 Example Street");
        model.addAttribute("selectedAddress", selectedAddress);
        model.addAttribute("showClientCaption", true);
        model.addAttribute("backUrl", "/application/client/details/address");
        model.addAttribute("confirmUrl", "/application/client/details/address/confirm");
        model.addAttribute("changeUrl", "/application/client/details/address/search");
        model.addAttribute("searchAgainUrl", "/application/client/details/address/search-again");
        model.addAttribute("manualEntryUrl", "/application/client/details/address/manual");
        addLayoutAttributes(model);
        return "application/sections/address-confirmation";
      }

      final ClientFormDataAddressDetails addressDetails = new ClientFormDataAddressDetails();
      addressDetails.setClientFlowFormAction("create");
      addressDetails.setVulnerableClient(false);
      addressDetails.setNoFixedAbode(false);
      final AddressLookupFlowData<ClientFormDataAddressDetails> addressFlow =
          new AddressLookupFlowData<>("create-client");
      final AddressResultRowDisplay selectedAddress = new AddressResultRowDisplay();
      selectedAddress.setUprn("1001");
      selectedAddress.setFullAddress("10 Example Street");

      if ("confirmed".equals(state)) {
        addressFlow.setSelectedAddress(selectedAddress);
        addressFlow.confirmSelectedAddress();
      } else if ("manual".equals(state)) {
        addressFlow.enterManualEntry(addressDetails);
      }

      model.addAttribute("addressDetails", addressDetails);
      model.addAttribute("addressFlow", addressFlow);
      model.addAttribute("selectedAddress", selectedAddress);
      model.addAttribute("componentEnabled", true);
      model.addAttribute("manualEntryUrl", "/application/client/details/address/manual");
      model.addAttribute("changeAddressUrl", "/application/client/details/address/search-again");
      addLayoutAttributes(model);
      model.addAttribute(
          "countries",
          List.of(new CommonLookupValueDetail().code("GBR").description("United Kingdom")));
      model.addAttribute(
          BindingResult.MODEL_KEY_PREFIX + "addressDetails",
          new BeanPropertyBindingResult(addressDetails, "addressDetails"));
      return "application/client/address-client-details";
    }

    private void addLayoutAttributes(final Model model) {
      model.addAttribute("researchPanelLink", "/research");
      model.addAttribute("cspNonce", "test-nonce");
      final UserDetail user = new UserDetail();
      user.setFunctions(Collections.emptyList());
      model.addAttribute("user", user);
    }
  }
}
