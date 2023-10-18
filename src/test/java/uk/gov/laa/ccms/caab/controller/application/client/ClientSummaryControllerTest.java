package uk.gov.laa.ccms.caab.controller.application.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Errors;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ClientDetails;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientAddressDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientBasicDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientContactDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.client.ClientEqualOpportunitiesMonitoringDetailsValidator;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.ClientDetailMapper;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientCreated;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

@ExtendWith(MockitoExtension.class)
public class ClientSummaryControllerTest {

  @Mock
  private ClientService clientService;

  @Mock
  private LookupService lookupService;

  @Mock
  private ClientBasicDetailsValidator basicValidator;

  @Mock
  private ClientContactDetailsValidator contactValidator;

  @Mock
  private ClientAddressDetailsValidator addressValidator;

  @Mock
  private ClientEqualOpportunitiesMonitoringDetailsValidator opportunitiesValidator;

  @Mock
  private ClientDetailMapper clientDetailsMapper;

  @InjectMocks
  private ClientSummaryController clientSummaryController;

  private MockMvc mockMvc;

  private CommonLookupValueDetail titleLookupValueDetail;
  private CommonLookupValueDetail countryLookupValueDetail;
  private CommonLookupValueDetail genderLookupValueDetail;
  private CommonLookupValueDetail maritalStatusLookupValueDetail;
  private CommonLookupValueDetail ethnicityLookupValueDetail;
  private CommonLookupValueDetail disabilityLookupValueDetail;
  private CommonLookupValueDetail correspondenceMethodLookupValueDetail;
  private CommonLookupValueDetail correspondenceLanguageLookupValueDetail;

  private static final UserDetail userDetails = new UserDetail()
      .userId(1)
      .userType("testUserType")
      .loginId("testLoginId");

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders.standaloneSetup(clientSummaryController).build();

    titleLookupValueDetail = new CommonLookupValueDetail();
    countryLookupValueDetail = new CommonLookupValueDetail();
    genderLookupValueDetail = new CommonLookupValueDetail();
    maritalStatusLookupValueDetail = new CommonLookupValueDetail();
    ethnicityLookupValueDetail = new CommonLookupValueDetail();
    disabilityLookupValueDetail = new CommonLookupValueDetail();
    correspondenceMethodLookupValueDetail = new CommonLookupValueDetail();
    correspondenceLanguageLookupValueDetail = new CommonLookupValueDetail();
  }

  @Test
  void testClientDetailsSummary_Get() throws Exception {
    ClientDetails clientDetails = new ClientDetails();

    when(lookupService.getContactTitle(any())).thenReturn(
        Mono.just(titleLookupValueDetail));
    when(lookupService.getCountry(any())).thenReturn(
        Mono.just(countryLookupValueDetail));
    when(lookupService.getGender(any())).thenReturn(
        Mono.just(genderLookupValueDetail));
    when(lookupService.getMaritalStatus(any())).thenReturn(
        Mono.just(maritalStatusLookupValueDetail));
    when(lookupService.getEthnicOrigin(any())).thenReturn(
        Mono.just(ethnicityLookupValueDetail));
    when(lookupService.getDisability(any())).thenReturn(
        Mono.just(disabilityLookupValueDetail));
    when(lookupService.getCorrespondenceMethod(any())).thenReturn(
        Mono.just(correspondenceMethodLookupValueDetail));

    mockMvc.perform(get("/application/client/details/summary")
            .flashAttr("clientDetails", clientDetails))
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/client-summary-details"));

    verify(lookupService, atLeastOnce()).getContactTitle(any());
    verify(lookupService, atLeastOnce()).getCountry(any());
    verify(lookupService, atLeastOnce()).getGender(any());
    verify(lookupService, atLeastOnce()).getMaritalStatus(any());
    verify(lookupService, atLeastOnce()).getEthnicOrigin(any());
    verify(lookupService, atLeastOnce()).getDisability(any());
    verify(lookupService, atLeastOnce()).getCorrespondenceMethod(any());
    verify(lookupService, never()).getCorrespondenceLanguage(any());
  }

  @Test
  void testClientDetailsSummary_Get_withCorrespondenceLanguage() throws Exception {
    ClientDetails clientDetails = new ClientDetails();
    clientDetails.setCorrespondenceLanguage("TEST");

    when(lookupService.getContactTitle(any())).thenReturn(
        Mono.just(titleLookupValueDetail));
    when(lookupService.getCountry(any())).thenReturn(
        Mono.just(countryLookupValueDetail));
    when(lookupService.getGender(any())).thenReturn(
        Mono.just(genderLookupValueDetail));
    when(lookupService.getMaritalStatus(any())).thenReturn(
        Mono.just(maritalStatusLookupValueDetail));
    when(lookupService.getEthnicOrigin(any())).thenReturn(
        Mono.just(ethnicityLookupValueDetail));
    when(lookupService.getDisability(any())).thenReturn(
        Mono.just(disabilityLookupValueDetail));
    when(lookupService.getCorrespondenceMethod(any())).thenReturn(
        Mono.just(correspondenceMethodLookupValueDetail));

    when(lookupService.getCorrespondenceLanguage(any())).thenReturn(
        Mono.just(correspondenceLanguageLookupValueDetail));

    mockMvc.perform(get("/application/client/details/summary")
            .flashAttr("clientDetails", clientDetails))
        .andExpect(status().isOk())
        .andExpect(view().name("application/client/client-summary-details"));

    verify(lookupService, atLeastOnce()).getContactTitle(any());
    verify(lookupService, atLeastOnce()).getCountry(any());
    verify(lookupService, atLeastOnce()).getGender(any());
    verify(lookupService, atLeastOnce()).getMaritalStatus(any());
    verify(lookupService, atLeastOnce()).getEthnicOrigin(any());
    verify(lookupService, atLeastOnce()).getDisability(any());
    verify(lookupService, atLeastOnce()).getCorrespondenceMethod(any());
    verify(lookupService, atLeastOnce()).getCorrespondenceLanguage(any());
  }

  @Test
  void testClientDetailsSummary_Post() throws Exception {
    ClientDetails clientDetails = new ClientDetails();

    when(clientDetailsMapper.toSoaClientDetail(clientDetails)).thenReturn(
        new ClientDetail());

    when(clientService.postClient(any(), any(), any())).thenReturn(
        Mono.just(new ClientCreated()));

    mockMvc.perform(post("/application/client/details/summary")
            .sessionAttr(USER_DETAILS, userDetails)
            .flashAttr("clientDetails", clientDetails))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/submissions/client-create"));

    verify(basicValidator).validate(any(), any());
    verify(contactValidator).validate(any(), any());
    verify(addressValidator).validate(any(), any());
    verify(opportunitiesValidator).validate(any(), any());

    verify(clientDetailsMapper).toSoaClientDetail(clientDetails);
    verify(clientService).postClient(any(), any(), any());
  }

  @Test
  void testClientDetailsSummary_PostWithErrors() throws Exception {
    ClientDetails clientDetails = new ClientDetails();

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("title", "required.title", "Please complete 'Title'.");
      return null;
    }).when(basicValidator).validate(any(), any());

    Exception exception = assertThrows(ServletException.class, () ->
        this.mockMvc.perform(post("/application/client/details/summary")
            .sessionAttr(USER_DETAILS, userDetails)
            .flashAttr("clientDetails", clientDetails)));

    assertInstanceOf(CaabApplicationException.class, exception.getCause());
    assertEquals("Client submission containing missing or invalid client details.", exception.getCause().getMessage());
  }
}
