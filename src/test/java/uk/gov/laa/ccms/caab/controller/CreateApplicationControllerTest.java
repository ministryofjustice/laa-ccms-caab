//package uk.gov.laa.ccms.caab.controller;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//import org.springframework.test.context.web.WebAppConfiguration;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.validation.Errors;
//import org.springframework.web.context.WebApplicationContext;
//import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
//import uk.gov.laa.ccms.caab.bean.ApplicationDetailsValidator;
//import uk.gov.laa.ccms.caab.controller.application.DelegatedFunctionsController;
//import uk.gov.laa.ccms.caab.service.DataService;
//import uk.gov.laa.ccms.data.model.CommonLookupValueDetails;
//import uk.gov.laa.ccms.data.model.OfficeDetails;
//import uk.gov.laa.ccms.data.model.ProviderDetails;
//import uk.gov.laa.ccms.data.model.UserDetails;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
//
//@ExtendWith(SpringExtension.class)
//@ContextConfiguration
//@WebAppConfiguration
//public class CreateApplicationControllerTest {
//  @Mock
//  private DataService dataService;
//
//  @Mock
//  private ApplicationDetailsValidator applicationDetailsValidator;
//
//  @InjectMocks
//  private DelegatedFunctionsController createApplicationController;
//
//  private MockMvc mockMvc;
//
//  @Autowired
//  private WebApplicationContext webApplicationContext;
//
//  @BeforeEach
//  public void setup() {
//    mockMvc = standaloneSetup(createApplicationController).build();
//  }
//
//  @Test
//  public void testGetOfficeAddsOfficesToModel() throws Exception {
//    final UserDetails userDetails = new UserDetails()
//        .userId(1)
//        .userType("testUserType")
//        .loginId("testLoginId")
//        .provider(
//            new ProviderDetails()
//                .addOfficesItem(
//                    new uk.gov.laa.ccms.data.model.OfficeDetails()
//                        .id(1)
//                        .name("Office 1")));
//
//    this.mockMvc.perform(get("/application/office").flashAttr("user", userDetails))
//        .andDo(print())
//        .andExpect(status().isOk())
//        .andExpect(view().name("/application/select-office"))
//        .andExpect(model().attribute("user", userDetails))
//        .andExpect(model().attribute("offices", userDetails.getProvider().getOffices()));
//
//  }
//
//  @Test
//  public void testPostOfficeIsSuccessful() throws Exception {
//    final UserDetails userDetails = new UserDetails()
//        .userId(1)
//        .userType("testUserType")
//        .loginId("testLoginId")
//        .provider(
//            new ProviderDetails()
//                .addOfficesItem(
//                    new uk.gov.laa.ccms.data.model.OfficeDetails()
//                        .id(1)
//                        .name("Office 1")));
//
//    final ApplicationDetails applicationDetails = new ApplicationDetails();
//    applicationDetails.setOfficeId(1);
//
//    this.mockMvc.perform(post("/application/office")
//            .flashAttr("user", userDetails)
//            .flashAttr("applicationDetails", applicationDetails))
//        .andDo(print())
//        .andExpect(redirectedUrl("/application/category-of-law"));
//
//    verifyNoInteractions(dataService);
//  }
//
//  @Test
//  public void testPostOfficeHandlesValidationError() throws Exception {
//      final UserDetails userDetails = new UserDetails()
//              .userId(1)
//              .userType("testUserType")
//              .loginId("testLoginId")
//              .provider(
//                      new ProviderDetails()
//                              .addOfficesItem(
//                                      new OfficeDetails()
//                                              .id(1)
//                                              .name("Office 1")));
//
//    final ApplicationDetails applicationDetails = new ApplicationDetails();
//
//    doAnswer(invocation -> {
//      Errors errors = (Errors) invocation.getArguments()[1];
//      errors.rejectValue("officeId", "required.officeId", "Please select an office.");
//      return null;
//    }).when(applicationDetailsValidator).validateSelectOffice(any(), any());
//
//
//    this.mockMvc.perform(post("/application/office")
//                    .flashAttr("user", userDetails)
//                    .flashAttr("applicationDetails", applicationDetails))
//            .andDo(print())
//            .andExpect(status().isOk())
//            .andExpect(view().name("/application/select-office"))
//            .andExpect(model().attribute("offices", userDetails.getProvider().getOffices()));
//  }
//
//  @Test
//  public void testGetCategoryOfLawAddsCategoriesOfLawToModel() throws Exception {
//    final UserDetails userDetails = new UserDetails()
//            .userId(1)
//            .userType("testUserType")
//            .loginId("testLoginId")
//            .provider(
//                    new ProviderDetails()
//                            .addOfficesItem(
//                                    new OfficeDetails()
//                                            .id(1)
//                                            .name("Office 1")));
//
//    this.mockMvc.perform(get("/application/category-of-law").flashAttr("user", userDetails))
//            .andDo(print())
//            .andExpect(status().isOk())
//            .andExpect(view().name("/application/select-category-of-law"))
//            .andExpect(model().attribute("user", userDetails))
//            .andExpect(model().attribute("categoriesOfLaw", userDetails.getProvider().getOffices()));
//  }
//
//  @Test
//  public void testPostCategoryOfLawHandlesValidationError() throws Exception {
//    final UserDetails userDetails = new UserDetails()
//            .userId(1)
//            .userType("testUserType")
//            .loginId("testLoginId")
//            .provider(
//                    new ProviderDetails()
//                            .addOfficesItem(
//                                    new OfficeDetails()
//                                            .id(1)
//                                            .name("Office 1")));
//
//    final ApplicationDetails applicationDetails = new ApplicationDetails();
//
//    doAnswer(invocation -> {
//      Errors errors = (Errors) invocation.getArguments()[1];
//      errors.rejectValue("categoryOfLawId", "required.categoryOfLawId", "Please select a category of law.");
//      return null;
//    }).when(applicationDetailsValidator).validateCategoryOfLaw(any(), any());
//
//    this.mockMvc.perform(post("/application/category-of-law")
//                    .flashAttr("user", userDetails)
//                    .flashAttr("applicationDetails", applicationDetails))
//            .andDo(print())
//            .andExpect(status().isOk())
//            .andExpect(view().name("/application/select-category-of-law"))
//            .andExpect(model().attribute("categoriesOfLaw", userDetails.getProvider().getOffices()));
//  }
//
//  @Test
//  public void testPostCategoryOfLawIsSuccessful() throws Exception {
//    final UserDetails userDetails = new UserDetails()
//            .userId(1)
//            .userType("testUserType")
//            .loginId("testLoginId")
//            .provider(
//                    new ProviderDetails()
//                            .addOfficesItem(
//                                    new OfficeDetails()
//                                            .id(1)
//                                            .name("Office 1")));
//
//    final ApplicationDetails applicationDetails = new ApplicationDetails();
//    applicationDetails.setCategoryOfLawId(1);
//
//    this.mockMvc.perform(post("/application/category-of-law")
//                    .flashAttr("user", userDetails)
//                    .flashAttr("applicationDetails", applicationDetails))
//            .andDo(print())
//            .andExpect(redirectedUrl("/application/application-type"));
//
//    verifyNoInteractions(dataService);
//  }
//  @Test
//  public void testGetApplicationTypeAddsApplicationTypesToModel() throws Exception {
//    final List<CommonLookupValueDetails> applicationTypes = new ArrayList<>();
//    applicationTypes.add(new CommonLookupValueDetails().type("Type 1").code("Code 1"));
//
//    when(dataService.getApplicationTypes()).thenReturn(applicationTypes);
//
//    this.mockMvc.perform(get("/application/application-type"))
//            .andDo(print())
//            .andExpect(status().isOk())
//            .andExpect(view().name("select-application-type"))
//            .andExpect(model().attribute("applicationDetails", new ApplicationDetails()))
//            .andExpect(model().attribute("applicationTypes", applicationTypes));
//
//    verify(dataService, times(1)).getApplicationTypes();
//  }
//
//  @Test
//  public void testPostApplicationTypeHandlesValidationError() throws Exception {
//    final ApplicationDetails applicationDetails = new ApplicationDetails();
//
//    doAnswer(invocation -> {
//      Errors errors = (Errors) invocation.getArguments()[1];
//      errors.rejectValue("applicationTypeId", "required.applicationTypeId", "Please select an application type.");
//      return null;
//    }).when(applicationDetailsValidator).validateApplicationType(any(), any());
//
//    this.mockMvc.perform(post("/application/application-type")
//                    .flashAttr("applicationDetails", applicationDetails))
//            .andDo(print())
//            .andExpect(status().isOk())
//            .andExpect(view().name("select-application-type"));
//  }
//
//  @Test
//  public void testPostApplicationTypeIsSuccessful() throws Exception {
//    final ApplicationDetails applicationDetails = new ApplicationDetails();
//    applicationDetails.setApplicationTypeId("test");
//
//    this.mockMvc.perform(post("/application/application-type")
//                    .flashAttr("applicationDetails", applicationDetails))
//            .andDo(print())
//            .andExpect(redirectedUrl("/application/delegate-functions"));
//
//    verifyNoInteractions(dataService);
//  }
//}
//
//
