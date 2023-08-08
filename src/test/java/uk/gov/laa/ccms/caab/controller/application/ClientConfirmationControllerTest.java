package uk.gov.laa.ccms.caab.controller.application;

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
import uk.gov.laa.ccms.caab.service.CaabApiService;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.caab.service.SoaGatewayService;
import uk.gov.laa.ccms.data.model.*;
import uk.gov.laa.ccms.soa.gateway.model.*;

import java.util.ArrayList;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_DETAILS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

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

        when(soaGatewayService.getClient(clientReferenceNumber, user.getLoginId(), user.getUserType())).thenReturn(Mono.just(clientInformation));

        this.mockMvc.perform(get("/application/client/" + clientReferenceNumber + "/confirm")
                        .sessionAttr("user", user))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("/application/application-client-confirmation"))
                .andExpect(model().attribute("clientInformation", clientInformation))
                .andExpect(model().attribute("clientReferenceNumber", clientReferenceNumber));
    }

    @Test
    public void testClientConfirmedSuccess() throws Exception {
        String confirmedClientReference = "12345"; //must match client reference above
        ApplicationDetails applicationDetails = buildApplicationDetails();
        ClientDetail clientInformation = buildClientInformation();
        UserDetail user = buildUser();

        // Mocking dependencies
        CaseReferenceSummary caseReferenceSummary = new CaseReferenceSummary();
        caseReferenceSummary.setCaseReferenceNumber("case123");
        when(soaGatewayService.getCaseReference(user.getLoginId(), user.getUserType())).thenReturn(Mono.just(caseReferenceSummary));
        when(soaGatewayService.getContractualDevolvedPowers(any(), any(), any(), any(), any())).thenReturn("YES");
        AmendmentTypeLookupDetail amendmentTypes = new AmendmentTypeLookupDetail();
        amendmentTypes.setContent(new ArrayList<>());
        AmendmentTypeLookupValueDetail amendmentType = new AmendmentTypeLookupValueDetail();
        amendmentType.setDefaultLarScopeFlag("Y");
        amendmentTypes.getContent().add(amendmentType);
        when(dataService.getAmendmentTypes(any())).thenReturn(Mono.just(amendmentTypes));
        when(caabApiService.createApplication(any(), any())).thenReturn(Mono.empty());

        this.mockMvc.perform(post("/application/client/confirmed")
                        .param("confirmedClientReference", confirmedClientReference)
                        .sessionAttr(APPLICATION_DETAILS, applicationDetails)
                        .sessionAttr("clientInformation", clientInformation)
                        .sessionAttr(USER_DETAILS, user))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("TODO"));
    }

    @Test
    public void testClientConfirmedNoCaseReferenceNumber() throws Exception {
        String confirmedClientReference = "12345"; // Must match client reference above
        ApplicationDetails applicationDetails = buildApplicationDetails();
        ClientDetail clientInformation = buildClientInformation();
        UserDetail user = buildUser();

        // Mocking dependencies
        when(soaGatewayService.getCaseReference(user.getLoginId(), user.getUserType())).thenReturn(Mono.just(new CaseReferenceSummary()));
        when(soaGatewayService.getContractualDevolvedPowers(any(), any(), any(), any(), any())).thenReturn("YES");

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
        assertEquals("No case reference number was created, unable to continue", exception.getCause().getMessage());
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
        applicationDetails.setOfficeDisplayValue("Office Display");
        applicationDetails.setCategoryOfLawId("CategoryOfLawID");
        applicationDetails.setCategoryOfLawDisplayValue("CategoryOfLaw Display");
        applicationDetails.setExceptionalFunding(false);
        applicationDetails.setApplicationTypeCategory(APP_TYPE_SUBSTANTIVE);
        applicationDetails.setDelegatedFunctions(true);
        applicationDetails.setDelegatedFunctionUsedDay("01");
        applicationDetails.setDelegatedFunctionUsedMonth("01");
        applicationDetails.setDelegatedFunctionUsedYear("2022");
        applicationDetails.setApplicationTypeAndDisplayValues();
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
