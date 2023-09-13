package uk.gov.laa.ccms.caab.controller.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.ClientService;
import uk.gov.laa.ccms.data.model.BaseOffice;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailDetails;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetailRecordHistory;
import uk.gov.laa.ccms.soa.gateway.model.NameDetail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class ClientConfirmationControllerTest {

  @Mock
  private HttpSession httpSession;

  @Mock
  private ClientService clientService;

  @Mock
  private ApplicationService applicationService;

  @Mock
  private ClientResultDisplayMapper clientResultDisplayMapper;

  @InjectMocks
  private ClientConfirmationController clientConfirmationController;

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(clientConfirmationController).build();
  }

  @Test
  public void testClientConfirm() throws Exception {
    String clientReferenceNumber = "123456";
    UserDetail user = buildUser(); // Assuming buildUser() method creates a UserDetail object
    ClientDetail clientInformation = new ClientDetail(); // Assuming proper initialization

    when(clientService.getClient(clientReferenceNumber, user.getLoginId(),
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

    when(applicationService.createApplication(applicationDetails, clientInformation, user)).thenReturn(Mono.empty());

    this.mockMvc.perform(post("/application/client/confirmed")
            .param("confirmedClientReference", confirmedClientReference)
            .sessionAttr(APPLICATION_DETAILS, applicationDetails)
            .sessionAttr("clientInformation", clientInformation)
            .sessionAttr(USER_DETAILS, user))
        .andReturn();

    verify(applicationService).createApplication(applicationDetails, clientInformation, user);
  }

  @Test
  public void testClientConfirmedClientMismatch() {
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

  private BaseProvider buildProvider() {
    return new BaseProvider()
        .id(123)
        .addOfficesItem(
            new BaseOffice()
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
    String clientReferenceNumber = "12345";
    return new ClientDetail()
        .clientReferenceNumber(clientReferenceNumber)
        .details(new ClientDetailDetails()
            .name(new NameDetail()))
        .recordHistory(new ClientDetailRecordHistory());
  }
}
