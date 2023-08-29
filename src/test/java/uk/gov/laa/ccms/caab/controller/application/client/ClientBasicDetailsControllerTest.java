package uk.gov.laa.ccms.caab.controller.application.client;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_GENDER;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_MARITAL_STATUS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.caab.bean.ClientDetailsValidator;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

@ExtendWith(MockitoExtension.class)
public class ClientBasicDetailsControllerTest {

  @Mock
  private DataService dataService;

  @Mock
  private ClientDetailsValidator clientDetailsValidator;

  @InjectMocks
  private ClientBasicDetailsController clientBasicDetailsController;

  private MockMvc mockMvc;

  private CommonLookupDetail titleLookupDetail;
  private CommonLookupDetail countryLookupDetail;
  private CommonLookupDetail genderLookupDetail;
  private CommonLookupDetail maritalStatusLookupDetail;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(clientBasicDetailsController).build();

    titleLookupDetail = new CommonLookupDetail();
    countryLookupDetail = new CommonLookupDetail();
    genderLookupDetail = new CommonLookupDetail();
    maritalStatusLookupDetail = new CommonLookupDetail();
  }

  @Test
  void testClientDetailsBasic() throws Exception {
    ClientSearchCriteria clientSearchCriteria = new ClientSearchCriteria();
    ClientDetails clientDetails = new ClientDetails();

    when(dataService.getCommonValues(eq(COMMON_VALUE_CONTACT_TITLE))).thenReturn(
        Mono.just(titleLookupDetail));
    when(dataService.getCountries()).thenReturn(
        Mono.just(countryLookupDetail));
    when(dataService.getCommonValues(eq(COMMON_VALUE_GENDER))).thenReturn(
        Mono.just(genderLookupDetail));
    when(dataService.getCommonValues(eq(COMMON_VALUE_MARITAL_STATUS))).thenReturn(
        Mono.just(maritalStatusLookupDetail));

    this.mockMvc.perform(get("/application/client/details/basic")
            .sessionAttr(CLIENT_SEARCH_CRITERIA, clientSearchCriteria)
            .flashAttr(CLIENT_DETAILS, clientDetails)
            .flashAttr("genders", Collections.emptyList())
            .flashAttr("maritalStatusList", Collections.emptyList()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/basic-client-details"))
        .andExpect(model().attributeExists("titles", "countries", "genders", "maritalStatusList"));

  }

  @Test
  void testClientDetailsBasicGetWithPopulatedFields() throws Exception {
    ClientSearchCriteria clientSearchCriteria = buildClientSearchCriteria();

    when(dataService.getCommonValues(eq(COMMON_VALUE_CONTACT_TITLE))).thenReturn(
        Mono.just(titleLookupDetail));
    when(dataService.getCountries()).thenReturn(
        Mono.just(countryLookupDetail));
    when(dataService.getCommonValues(eq(COMMON_VALUE_GENDER))).thenReturn(
        Mono.just(genderLookupDetail));
    when(dataService.getCommonValues(eq(COMMON_VALUE_MARITAL_STATUS))).thenReturn(
        Mono.just(maritalStatusLookupDetail));

    mockMvc.perform(get("/application/client/details/basic")
            .sessionAttr(CLIENT_SEARCH_CRITERIA, clientSearchCriteria)
            .flashAttr(CLIENT_DETAILS, new ClientDetails())
            .flashAttr("genders", Collections.emptyList())
            .flashAttr("maritalStatusList", Collections.emptyList()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/basic-client-details"))
        .andExpect(model().attributeExists("titles", "countries", "genders", "maritalStatusList"))
        .andExpect(model().attribute("clientDetails", notNullValue()));
  }

  @Test
  void testClientDetailsBasicGetCountries() throws Exception {
    ClientSearchCriteria clientSearchCriteria = new ClientSearchCriteria();
    ClientDetails clientDetails = new ClientDetails();

    countryLookupDetail.addContentItem(
        new CommonLookupValueDetail().code("USA").description("United States"));
    countryLookupDetail.addContentItem(
        new CommonLookupValueDetail().code("UK").description("United Kingdom"));

    when(dataService.getCommonValues(eq(COMMON_VALUE_CONTACT_TITLE))).thenReturn(
        Mono.just(titleLookupDetail));
    when(dataService.getCountries()).thenReturn(
        Mono.just(countryLookupDetail));
    when(dataService.getCommonValues(eq(COMMON_VALUE_GENDER))).thenReturn(
        Mono.just(genderLookupDetail));
    when(dataService.getCommonValues(eq(COMMON_VALUE_MARITAL_STATUS))).thenReturn(
        Mono.just(maritalStatusLookupDetail));

    this.mockMvc.perform(get("/application/client/details/basic")
            .sessionAttr(CLIENT_SEARCH_CRITERIA, clientSearchCriteria)
            .flashAttr(CLIENT_DETAILS, clientDetails)
            .flashAttr("genders", Collections.emptyList())
            .flashAttr("maritalStatusList", Collections.emptyList()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/basic-client-details"))
        .andExpect(model().attributeExists("titles", "countries", "genders", "maritalStatusList"));

  }

  @Test
  void testClientDetailsBasicPost() throws Exception {
    ClientSearchCriteria clientSearchCriteria = new ClientSearchCriteria();
    ClientDetails clientDetails = new ClientDetails();

    mockMvc.perform(post("/application/client/details/basic")
            .sessionAttr(CLIENT_SEARCH_CRITERIA, clientSearchCriteria)
            .flashAttr(CLIENT_DETAILS, clientDetails)
            .flashAttr("genders", Collections.emptyList())
            .flashAttr("maritalStatusList", Collections.emptyList()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/contact"));
  }

  @Test
  void testClientDetailsBasicPostValidationError() throws Exception {
    ClientSearchCriteria clientSearchCriteria = new ClientSearchCriteria();
    ClientDetails clientDetails = new ClientDetails();

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("title", "required.title", "Please complete 'Title'.");
      return null;
    }).when(clientDetailsValidator).validate(any(), any());

    when(dataService.getCommonValues(eq(COMMON_VALUE_CONTACT_TITLE))).thenReturn(
        Mono.just(titleLookupDetail));
    when(dataService.getCountries()).thenReturn(
        Mono.just(countryLookupDetail));
    when(dataService.getCommonValues(eq(COMMON_VALUE_GENDER))).thenReturn(
        Mono.just(genderLookupDetail));
    when(dataService.getCommonValues(eq(COMMON_VALUE_MARITAL_STATUS))).thenReturn(
        Mono.just(maritalStatusLookupDetail));

    mockMvc.perform(post("/application/client/details/basic")
            .sessionAttr(CLIENT_SEARCH_CRITERIA, clientSearchCriteria)
            .flashAttr(CLIENT_DETAILS, clientDetails)
            .flashAttr("genders", Collections.emptyList())
            .flashAttr("maritalStatusList", Collections.emptyList()))
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/basic-client-details"))
        .andExpect(model().attributeExists("titles", "countries", "genders", "maritalStatusList",
            "clientDetails"));
  }

  private ClientSearchCriteria buildClientSearchCriteria() {
    ClientSearchCriteria clientSearchCriteria = new ClientSearchCriteria();
    clientSearchCriteria.setForename("John");
    clientSearchCriteria.setSurname("Doe");
    clientSearchCriteria.setDobYear("1990");
    clientSearchCriteria.setDobMonth("02");
    clientSearchCriteria.setDobDay("01");
    clientSearchCriteria.setUniqueIdentifierType(UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER);
    clientSearchCriteria.setUniqueIdentifierValue("AA111111A");
    clientSearchCriteria.setGender("MALE");
    return clientSearchCriteria;
  }
}
