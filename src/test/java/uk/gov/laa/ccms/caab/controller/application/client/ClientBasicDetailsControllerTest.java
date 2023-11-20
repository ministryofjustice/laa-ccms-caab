package uk.gov.laa.ccms.caab.controller.application.client;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
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
import static uk.gov.laa.ccms.caab.constants.ClientActionConstants.ACTION_CREATE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CLIENT_FLOW_FORM_DATA;
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
import uk.gov.laa.ccms.caab.bean.ClientFlowFormData;
import uk.gov.laa.ccms.caab.bean.ClientFormDataBasicDetails;
import uk.gov.laa.ccms.caab.bean.ClientSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientBasicDetailsValidator;
import uk.gov.laa.ccms.caab.service.CommonLookupService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

@ExtendWith(MockitoExtension.class)
public class ClientBasicDetailsControllerTest {

  @Mock
  private CommonLookupService commonLookupService;

  @Mock
  private ClientBasicDetailsValidator clientBasicDetailsValidator;

  @InjectMocks
  private ClientBasicDetailsController clientBasicDetailsController;

  private MockMvc mockMvc;

  private CommonLookupDetail titleLookupDetail;
  private CommonLookupDetail countryLookupDetail;
  private CommonLookupDetail genderLookupDetail;
  private CommonLookupDetail maritalStatusLookupDetail;

  private ClientFlowFormData clientFlowFormData;

  private ClientFormDataBasicDetails basicDetails;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(clientBasicDetailsController).build();

    clientFlowFormData = new ClientFlowFormData(ACTION_CREATE);

    basicDetails = new ClientFormDataBasicDetails();

    titleLookupDetail = new CommonLookupDetail();
    titleLookupDetail.addContentItem(new CommonLookupValueDetail());
    countryLookupDetail = new CommonLookupDetail();
    countryLookupDetail.addContentItem(new CommonLookupValueDetail());
    genderLookupDetail = new CommonLookupDetail();
    genderLookupDetail.addContentItem(new CommonLookupValueDetail());
    maritalStatusLookupDetail = new CommonLookupDetail();
    maritalStatusLookupDetail.addContentItem(new CommonLookupValueDetail());
  }

  @Test
  void testClientDetailsBasic() throws Exception {
    ClientSearchCriteria clientSearchCriteria = new ClientSearchCriteria();

    when(commonLookupService.getContactTitles()).thenReturn(
        Mono.just(titleLookupDetail));
    when(commonLookupService.getCountries()).thenReturn(
        Mono.just(countryLookupDetail));
    when(commonLookupService.getGenders()).thenReturn(
        Mono.just(genderLookupDetail));
    when(commonLookupService.getMaritalStatuses()).thenReturn(
        Mono.just(maritalStatusLookupDetail));

    this.mockMvc.perform(get("/application/client/details/basic")
            .sessionAttr(CLIENT_SEARCH_CRITERIA, clientSearchCriteria)
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("basicDetails", basicDetails)
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

    when(commonLookupService.getContactTitles()).thenReturn(
        Mono.just(titleLookupDetail));
    when(commonLookupService.getCountries()).thenReturn(
        Mono.just(countryLookupDetail));
    when(commonLookupService.getGenders()).thenReturn(
        Mono.just(genderLookupDetail));
    when(commonLookupService.getMaritalStatuses()).thenReturn(
        Mono.just(maritalStatusLookupDetail));

    mockMvc.perform(get("/application/client/details/basic")
            .sessionAttr(CLIENT_SEARCH_CRITERIA, clientSearchCriteria)
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("basicDetails", basicDetails)
            .flashAttr("genders", Collections.emptyList())
            .flashAttr("maritalStatusList", Collections.emptyList()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/basic-client-details"))
        .andExpect(model().attributeExists("titles", "countries", "genders", "maritalStatusList"))
        .andExpect(model().attribute("basicDetails", notNullValue()));
  }

  @Test
  void testClientDetailsBasicGetCountries() throws Exception {
    ClientSearchCriteria clientSearchCriteria = new ClientSearchCriteria();

    countryLookupDetail.addContentItem(
        new CommonLookupValueDetail().code("USA").description("United States"));
    countryLookupDetail.addContentItem(
        new CommonLookupValueDetail().code("UK").description("United Kingdom"));

    when(commonLookupService.getContactTitles()).thenReturn(
        Mono.just(titleLookupDetail));
    when(commonLookupService.getCountries()).thenReturn(
        Mono.just(countryLookupDetail));
    when(commonLookupService.getGenders()).thenReturn(
        Mono.just(genderLookupDetail));
    when(commonLookupService.getMaritalStatuses()).thenReturn(
        Mono.just(maritalStatusLookupDetail));

    this.mockMvc.perform(get("/application/client/details/basic")
            .sessionAttr(CLIENT_SEARCH_CRITERIA, clientSearchCriteria)
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("basicDetails", basicDetails)
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

    mockMvc.perform(post("/application/client/details/basic")
            .sessionAttr(CLIENT_SEARCH_CRITERIA, clientSearchCriteria)
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("basicDetails", basicDetails)
            .flashAttr("genders", Collections.emptyList())
            .flashAttr("maritalStatusList", Collections.emptyList()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/application/client/details/contact"));
  }

  @Test
  void testClientDetailsBasicPostValidationError() throws Exception {
    ClientSearchCriteria clientSearchCriteria = new ClientSearchCriteria();

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("title", "required.title", "Please complete 'Title'.");
      return null;
    }).when(clientBasicDetailsValidator).validate(any(), any());

    when(commonLookupService.getContactTitles()).thenReturn(
        Mono.just(titleLookupDetail));
    when(commonLookupService.getCountries()).thenReturn(
        Mono.just(countryLookupDetail));
    when(commonLookupService.getGenders()).thenReturn(
        Mono.just(genderLookupDetail));
    when(commonLookupService.getMaritalStatuses()).thenReturn(
        Mono.just(maritalStatusLookupDetail));

    mockMvc.perform(post("/application/client/details/basic")
            .sessionAttr(CLIENT_SEARCH_CRITERIA, clientSearchCriteria)
            .sessionAttr(CLIENT_FLOW_FORM_DATA, clientFlowFormData)
            .flashAttr("basicDetails", basicDetails)
            .flashAttr("genders", Collections.emptyList())
            .flashAttr("maritalStatusList", Collections.emptyList()))
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/basic-client-details"))
        .andExpect(model().attributeExists("titles", "countries", "genders", "maritalStatusList",
            "basicDetails"));
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
