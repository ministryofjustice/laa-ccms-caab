package uk.gov.laa.ccms.caab.controller;

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
import uk.gov.laa.ccms.caab.bean.ApplicationDetailsValidator;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.data.model.ProviderDetails;
import uk.gov.laa.ccms.data.model.UserDetails;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class CreateApplicationControllerTest {
  @Mock
  private DataService dataService;

  @Mock
  private ApplicationDetailsValidator applicationDetailsValidator;

  @InjectMocks
  private CreateApplicationController createApplicationController;

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(createApplicationController).build();
  }

  @Test
  public void testGetOfficeAddsOfficesToModel() throws Exception {
    final UserDetails userDetails = new UserDetails()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId")
        .provider(
            new ProviderDetails()
                .addOfficesItem(
                    new uk.gov.laa.ccms.data.model.OfficeDetails()
                        .id(1)
                        .name("Office 1")));

//    when(dataService.getUser(userDetails.getLoginId())).thenReturn(Mono.just(userDetails));

    this.mockMvc.perform(get("/application/office").flashAttr("user", userDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("/application/select-office"))
        .andExpect(model().attribute("user", userDetails))
        .andExpect(model().attribute("offices", userDetails.getProvider().getOffices()));

//    verify(dataService).getUser(userDetails.getLoginId());
  }

  @Test
  public void testPostOfficeIsSuccessful() throws Exception {
    final UserDetails userDetails = new UserDetails()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId")
        .provider(
            new ProviderDetails()
                .addOfficesItem(
                    new uk.gov.laa.ccms.data.model.OfficeDetails()
                        .id(1)
                        .name("Office 1")));

    final ApplicationDetails applicationDetails = new ApplicationDetails();
    applicationDetails.setOfficeId(1);

    this.mockMvc.perform(post("/application/office")
            .flashAttr("user", userDetails)
            .flashAttr("applicationDetails", applicationDetails))
        .andDo(print())
        .andExpect(redirectedUrl("/application/select-category-of-law"));

    verifyNoInteractions(dataService);
  }

  @Test
  public void testPostOfficeHandlesValidationError() throws Exception {
    final UserDetails userDetails = new UserDetails()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId")
        .provider(
            new ProviderDetails()
                .addOfficesItem(
                    new uk.gov.laa.ccms.data.model.OfficeDetails()
                        .id(1)
                        .name("Office 1")));

    when(dataService.getUser(userDetails.getLoginId())).thenReturn(Mono.just(userDetails));

    final ApplicationDetails applicationDetails = new ApplicationDetails();

    this.mockMvc.perform(post("/application/office")
            .flashAttr("user", userDetails)
            .flashAttr("applicationDetails", applicationDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("/application/select-office"));
  }
}


