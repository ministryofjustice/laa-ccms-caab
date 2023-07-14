package uk.gov.laa.ccms.caab.controller.application;

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
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.ApplicationDetailsValidator;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.data.model.OfficeDetails;
import uk.gov.laa.ccms.data.model.ProviderDetails;
import uk.gov.laa.ccms.data.model.UserDetails;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class OfficeControllerTest {
    @Mock
    private DataService dataService;

    @Mock
    private ApplicationDetailsValidator applicationDetailsValidator;

    @InjectMocks
    private OfficeController officeController;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private UserDetails user;

    @BeforeEach
    public void setup() {
        mockMvc = standaloneSetup(officeController).build();
        this.user = buildUser();
    }

    @Test
    public void testGetOfficeAddsOfficesToModel() throws Exception {
        this.mockMvc.perform(get("/application/office").flashAttr("user", user))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("/application/select-office"))
                .andExpect(model().attribute("user", user))
                .andExpect(model().attribute("offices", user.getProvider().getOffices()))
                .andExpect(model().attributeExists("applicationDetails"));

    }

    @Test
    public void testPostOfficeIsSuccessful() throws Exception {
        final ApplicationDetails applicationDetails = new ApplicationDetails();
        applicationDetails.setOfficeId(1);

        this.mockMvc.perform(post("/application/office")
                        .flashAttr("user", user)
                        .flashAttr("applicationDetails", applicationDetails))
                .andDo(print())
                .andExpect(redirectedUrl("/application/category-of-law"));

        verifyNoInteractions(dataService);
    }

    @Test
    public void testPostOfficeHandlesValidationError() throws Exception {
        final ApplicationDetails applicationDetails = new ApplicationDetails();

        doAnswer(invocation -> {
            Errors errors = (Errors) invocation.getArguments()[1];
            errors.rejectValue("officeId", "required.officeId", "Please select an office.");
            return null;
        }).when(applicationDetailsValidator).validateSelectOffice(any(), any());


        this.mockMvc.perform(post("/application/office")
                        .flashAttr("user", user)
                        .flashAttr("applicationDetails", applicationDetails))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("/application/select-office"))
                .andExpect(model().attribute("offices", user.getProvider().getOffices()));
    }

    private UserDetails buildUser() {
        return new UserDetails()
            .userId(1)
            .userType("testUserType")
            .loginId("testLoginId")
            .provider(buildProvider());
    }
    private ProviderDetails buildProvider() {
        return new ProviderDetails()
            .id(123)
            .addOfficesItem(
                new OfficeDetails()
                    .id(1)
                    .name("Office 1"));
    }


}