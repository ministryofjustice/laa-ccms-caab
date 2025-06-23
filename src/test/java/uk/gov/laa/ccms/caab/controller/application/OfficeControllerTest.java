package uk.gov.laa.ccms.caab.controller.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;

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
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.OfficeValidator;
import uk.gov.laa.ccms.data.model.BaseOffice;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class OfficeControllerTest {

  @Mock
  private OfficeValidator officeValidator;

  @InjectMocks
  private OfficeController officeController;

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext webApplicationContext;

  private UserDetail user;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(officeController).build();
    this.user = buildUser();
  }

  @Test
  public void testGetOfficeAddsOfficesToModel() throws Exception {
    final ApplicationFormData applicationFormData = new ApplicationFormData();
    applicationFormData.setOfficeId(1);

    this.mockMvc.perform(get("/application/office")
            .flashAttr("user", user)
            .flashAttr(APPLICATION_FORM_DATA, applicationFormData))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/select-office"))
        .andExpect(model().attribute("user", user))
        .andExpect(model().attribute("offices", user.getProvider().getOffices()))
        .andExpect(model().attributeExists(APPLICATION_FORM_DATA));

  }

  @Test
  public void testPostOfficeIsSuccessful() throws Exception {
    final ApplicationFormData applicationFormData = new ApplicationFormData();
    applicationFormData.setOfficeId(1);

    this.mockMvc.perform(post("/application/office")
            .flashAttr("user", user)
            .flashAttr(APPLICATION_FORM_DATA, applicationFormData))
        .andDo(print())
        .andExpect(redirectedUrl("/application/category-of-law"));
  }

  @Test
  public void testPostOfficeHandlesValidationError() throws Exception {
    final ApplicationFormData applicationFormData = new ApplicationFormData();

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("officeId", "required.officeId", "Please select an office.");
      return null;
    }).when(officeValidator).validate(any(), any());
    
    this.mockMvc.perform(post("/application/office")
            .flashAttr("user", user)
            .flashAttr(APPLICATION_FORM_DATA, applicationFormData))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/select-office"))
        .andExpect(model().attribute("offices", user.getProvider().getOffices()));
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


}
