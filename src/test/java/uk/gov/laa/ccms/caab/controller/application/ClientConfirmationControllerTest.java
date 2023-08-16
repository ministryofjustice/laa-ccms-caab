package uk.gov.laa.ccms.caab.controller.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.mapper.ClientResultDisplayMapper;
import uk.gov.laa.ccms.caab.service.CaabApiService;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.caab.service.SoaGatewayService;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailRecordHistory;
import uk.gov.laa.ccms.soa.gateway.model.ClientNameDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetails;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class ClientConfirmationControllerTest {

  @Mock
  private HttpSession httpSession;

  @Mock
  private SoaGatewayService soaGatewayService;

  @Mock
  private DataService dataService;
  
  @Mock
  private CaabApiService caabApiService;

  @Mock
  private ClientResultDisplayMapper clientResultDisplayMapper;

  @InjectMocks
  private ClientConfirmationController clientConfirmationController;

  private MockMvc mockMvc;

  private UserDetail user;

  @Autowired
  private WebApplicationContext webApplicationContext;

  private String clientReferenceNumber = "12345";

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(clientConfirmationController).build();
    this.user = buildUser();
  }

  @Test
  public void testClientConfirm() throws Exception {
    String clientReferenceNumber = "123456";
    UserDetail user = buildUser(); // Assuming buildUser() method creates a UserDetail object
    ClientDetail clientInformation = new ClientDetail(); // Assuming proper initialization

    when(soaGatewayService.getClient(clientReferenceNumber, user.getLoginId(),
        user.getUserType())).thenReturn(Mono.just(clientInformation));

    this.mockMvc.perform(get("/application/client/" + clientReferenceNumber + "/confirm")
            .sessionAttr(USER_DETAILS,
                user)) // using the constant USER_DETAILS for the session attribute name
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/application-client-confirmation"))
        .andExpect(request().sessionAttribute("clientInformation", clientInformation))
        .andExpect(model().attribute("clientReferenceNumber", clientReferenceNumber));
  }

  @Test
  public void testClientConfirmedSuccess() throws Exception {
    String confirmedClientReference = "12345"; //must match client reference above
    ApplicationDetails applicationDetails = buildApplicationDetails();
    ClientDetail clientInformation = buildClientInformation();
    UserDetail user = buildUser();

    // Mocking dependencies
    CaseReferenceSummary caseReferenceSummary =
        new CaseReferenceSummary().caseReferenceNumber("REF123");
    CommonLookupDetail categoryOfLawLookupDetail = new CommonLookupDetail();
    ContractDetails contractDetails = new ContractDetails();

    AmendmentTypeLookupValueDetail amendmentType = new AmendmentTypeLookupValueDetail()
        .applicationTypeCode("TEST")
        .applicationTypeDescription("TEST")
        .defaultLarScopeFlag("Y");

    AmendmentTypeLookupDetail amendmentTypes =
        new AmendmentTypeLookupDetail().addContentItem(amendmentType);

    when(soaGatewayService.getCaseReference(user.getLoginId(), user.getUserType())).thenReturn(
        Mono.just(caseReferenceSummary));
    when(dataService.getCommonValues(anyString(), any(), any())).thenReturn(
        Mono.just(categoryOfLawLookupDetail));
    when(soaGatewayService.getContractDetails(anyInt(), anyInt(), anyString(),
        anyString())).thenReturn(Mono.just(contractDetails));
    when(dataService.getAmendmentTypes(any())).thenReturn(Mono.just(amendmentTypes));
    when(caabApiService.createApplication(anyString(), any())).thenReturn(Mono.empty());

    this.mockMvc.perform(post("/application/client/confirmed")
            .param("confirmedClientReference", confirmedClientReference)
            .sessionAttr(APPLICATION_DETAILS, applicationDetails)
            .sessionAttr("clientInformation", clientInformation)
            .sessionAttr(USER_DETAILS, user))
        .andReturn();

    verify(soaGatewayService).getCaseReference(user.getLoginId(), user.getUserType());
    verify(dataService).getCommonValues(anyString(), any(), any());
    verify(soaGatewayService).getContractDetails(anyInt(), anyInt(), anyString(), anyString());
    verify(dataService).getAmendmentTypes(any());
    verify(caabApiService).createApplication(anyString(), any());
  }

  @Test
  public void testClientConfirmedNoCaseReferenceNumber() throws Exception {
    String confirmedClientReference = "12345"; // Must match client reference above
    ApplicationDetails applicationDetails = buildApplicationDetails();
    ClientDetail clientInformation = buildClientInformation();
    UserDetail user = buildUser();

    // Mocking dependencies
    CommonLookupDetail categoryOfLawLookupDetail = new CommonLookupDetail();
    ContractDetails contractDetails = new ContractDetails();
    AmendmentTypeLookupDetail amendmentTypes = new AmendmentTypeLookupDetail();

    // Mocking getCaseReference to throw a RuntimeException
    when(soaGatewayService.getCaseReference(user.getLoginId(), user.getUserType())).thenThrow(
        new RuntimeException("No case reference number was created, unable to continue"));

    // Mocking dependencies
    when(dataService.getCommonValues(anyString(), any(), any())).thenReturn(
        Mono.just(categoryOfLawLookupDetail));
    when(soaGatewayService.getContractDetails(anyInt(), anyInt(), anyString(),
        anyString())).thenReturn(Mono.just(contractDetails));
    when(dataService.getAmendmentTypes(any())).thenReturn(Mono.just(amendmentTypes));
    when(caabApiService.createApplication(anyString(), any())).thenReturn(Mono.empty());

    Exception exception = null;

    try {
      this.mockMvc.perform(post("/application/client/confirmed")
          .param("confirmedClientReference", confirmedClientReference)
          .sessionAttr(APPLICATION_DETAILS, applicationDetails)
          .sessionAttr("clientInformation", clientInformation)
          .sessionAttr(USER_DETAILS, user));
    } catch (Exception e) {
      exception = e;
    }

    assertNotNull(exception);
    assertTrue(exception.getCause() instanceof RuntimeException);
    assertEquals("No case reference number was created, unable to continue",
        exception.getCause().getMessage());
  }

  @Test
  public void testClientConfirmedClientMismatch() throws Exception {
    String wrongReference = "wrongReference";
    ApplicationDetails applicationDetails = buildApplicationDetails();
    ClientDetail clientInformation = buildClientInformation();
    UserDetail user = buildUser();

    Exception exception = null;

    try {
      this.mockMvc.perform(post("/application/client/confirmed")
          .param("confirmedClientReference", wrongReference)
          .sessionAttr(APPLICATION_DETAILS, applicationDetails)
          .sessionAttr("clientInformation", clientInformation)
          .sessionAttr(USER_DETAILS, user));
    } catch (Exception e) {
      exception = e;
    }

    assertNotNull(exception);
    assertTrue(exception.getCause() instanceof RuntimeException);
    assertEquals("Client information does not match", exception.getCause().getMessage());
  }

  private UserDetail buildUser() {
    return new UserDetail()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId")
        .provider(buildProvider());
  }

  private ProviderDetail buildProvider() {
    return new ProviderDetail()
        .id(123)
        .addOfficesItem(
            new OfficeDetail()
                .id(1)
                .name("Office 1"));
  }

  private ApplicationDetails buildApplicationDetails() {
    ApplicationDetails applicationDetails = new ApplicationDetails();
    applicationDetails.setOfficeId(1);
    applicationDetails.setCategoryOfLawId("COL");
    applicationDetails.setExceptionalFunding(false);
    applicationDetails.setApplicationTypeCategory(APP_TYPE_SUBSTANTIVE);
    applicationDetails.setDelegatedFunctions(true);
    applicationDetails.setDelegatedFunctionUsedDay("01");
    applicationDetails.setDelegatedFunctionUsedMonth("01");
    applicationDetails.setDelegatedFunctionUsedYear("2022");
    return applicationDetails;
  }

  public ClientDetail buildClientInformation() {
    return new ClientDetail()
        .clientReferenceNumber(clientReferenceNumber)
        .details(new ClientDetailDetails()
            .name(new ClientNameDetail()))
        .recordHistory(new ClientDetailRecordHistory());
  }
}
