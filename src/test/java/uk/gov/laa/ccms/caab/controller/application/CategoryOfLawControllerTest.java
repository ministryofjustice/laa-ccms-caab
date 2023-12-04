package uk.gov.laa.ccms.caab.controller.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hamcrest.Matchers;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.CategoryOfLawValidator;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.data.model.BaseOffice;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupDetail;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class CategoryOfLawControllerTest {
  @Mock
  private ProviderService providerService;

  @Mock
  private LookupService lookupService;

  @Mock
  private CategoryOfLawValidator categoryOfLawValidator;

  @InjectMocks
  private CategoryOfLawController categoryOfLawController;

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext webApplicationContext;

  private UserDetail user;

  private ApplicationFormData applicationFormData;

  private CategoryOfLawLookupDetail categoriesOfLaw;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(categoryOfLawController).build();

    this.user = buildUser();

    applicationFormData = new ApplicationFormData();
    applicationFormData.setOfficeId(345);

    categoriesOfLaw = new CategoryOfLawLookupDetail()
        .addContentItem(new CategoryOfLawLookupValueDetail()
            .code("CAT1").matterTypeDescription("Category 1"))
        .addContentItem(new CategoryOfLawLookupValueDetail()
            .code("CAT2")
            .matterTypeDescription("Category 2"));
  }

  @Test
  public void testGetCategoryOfLawAddsCategoriesOfLawToModel() throws Exception {
    final List<String> categoryOfLawCodes = new ArrayList<>();
    categoryOfLawCodes.add("CAT1");
//    categoryOfLawCodes.add("CAT2");

    when(providerService.getCategoryOfLawCodes(
        user.getProvider().getId(),
        applicationFormData.getOfficeId(),
        user.getLoginId(),
        user.getUserType(),
        Boolean.TRUE)).thenReturn(categoryOfLawCodes);

    when(lookupService.getCategoriesOfLaw()).thenReturn(
        Mono.just(categoriesOfLaw));

    this.mockMvc.perform(get("/application/category-of-law")
            .flashAttr("applicationFormData", applicationFormData)
            .sessionAttr("user", user))
        .andExpect(status().isOk())
        .andExpect(model().attribute("categoriesOfLaw", Matchers.hasSize(1)))
        .andExpect(model().attributeExists(APPLICATION_FORM_DATA))
        .andReturn();

    verify(providerService).getCategoryOfLawCodes(user.getProvider().getId(),
        applicationFormData.getOfficeId(),
        user.getLoginId(),
        user.getUserType(),
        Boolean.TRUE);

    verify(lookupService).getCategoriesOfLaw();
  }

  @Test
  public void testGetCategoryOfLawNoCategoriesOfLawToModel() throws Exception {
    final List<String> categoryOfLawCodes = new ArrayList<>();

    when(providerService.getCategoryOfLawCodes(
        user.getProvider().getId(),
        applicationFormData.getOfficeId(),
        user.getLoginId(),
        user.getUserType(),
        Boolean.TRUE)).thenReturn(categoryOfLawCodes);

    when(lookupService.getCategoriesOfLaw()).thenReturn(
        Mono.just(categoriesOfLaw));

    MvcResult result = mockMvc.perform(get("/application/category-of-law")
            .flashAttr("applicationFormData", applicationFormData)
            .sessionAttr("user", user))
        .andExpect(status().isOk())
        .andExpect(model().attribute("categoriesOfLaw", Collections.emptyList()))
        .andExpect(model().attributeExists(APPLICATION_FORM_DATA))
        .andReturn();

    verify(providerService).getCategoryOfLawCodes(user.getProvider().getId(),
        applicationFormData.getOfficeId(),
        user.getLoginId(),
        user.getUserType(),
        Boolean.TRUE);

    verify(lookupService).getCategoriesOfLaw();

    BindingResult bindingResult = (BindingResult) result.getModelAndView().getModel()
        .get("org.springframework.validation.BindingResult.applicationFormData");
    assertEquals(1, bindingResult.getFieldErrors("categoryOfLawId").size());
    assertEquals("no.categoriesOfLaw", bindingResult.getFieldError("categoryOfLawId")
        .getCode());
  }

  @Test
  public void testGetCategoryOfLaw_ExceptionFundingReturnsAllCodes() throws Exception {
    when(lookupService.getCategoriesOfLaw()).thenReturn(
        Mono.just(categoriesOfLaw));

    this.mockMvc.perform(get("/application/category-of-law?exceptional_funding=true")
            .flashAttr(APPLICATION_FORM_DATA, applicationFormData)
            .sessionAttr("user", user))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/select-category-of-law"))
        .andExpect(model().attribute("categoriesOfLaw", categoriesOfLaw.getContent()))
        .andExpect(model().attributeExists(APPLICATION_FORM_DATA));

    assertTrue(applicationFormData.isExceptionalFunding());
    verifyNoInteractions(providerService);
    verify(lookupService).getCategoriesOfLaw();
  }

  @Test
  public void testPostCategoryOfLawHandlesValidationError() throws Exception {
    final List<String> categoryOfLawCodes = new ArrayList<>();
    categoryOfLawCodes.add("CAT1");
//    categoryOfLawCodes.add("CAT2");

    when(providerService.getCategoryOfLawCodes(
        user.getProvider().getId(),
        applicationFormData.getOfficeId(),
        user.getLoginId(),
        user.getUserType(),
        Boolean.TRUE)).thenReturn(categoryOfLawCodes);

    when(lookupService.getCategoriesOfLaw()).thenReturn(
        Mono.just(categoriesOfLaw));

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("categoryOfLawId", "required.categoryOfLawId",
          "Please select a category of law.");
      return null;
    }).when(categoryOfLawValidator).validate(any(), any());

    this.mockMvc.perform(post("/application/category-of-law")
            .flashAttr(APPLICATION_FORM_DATA, applicationFormData)
            .sessionAttr("user", user))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/select-category-of-law"))
        .andExpect(model().attribute("categoriesOfLaw", Matchers.hasSize(1)));
  }

  @Test
  public void testPostCategoryOfLawIsSuccessful() throws Exception {
    applicationFormData.setCategoryOfLawId("CAT1");

    this.mockMvc.perform(post("/application/category-of-law")
            .flashAttr(APPLICATION_FORM_DATA, applicationFormData)
            .sessionAttr("user", user))
        .andDo(print())
        .andExpect(redirectedUrl("/application/application-type"));

    verifyNoInteractions(providerService);
    verifyNoInteractions(lookupService);
  }

  @Test
  public void testPostCategoryOfLaw_HandlesExceptionalFunding() throws Exception {
    applicationFormData.setCategoryOfLawId("CAT1");
    applicationFormData.setExceptionalFunding(true);

    this.mockMvc.perform(post("/application/category-of-law")
            .flashAttr(APPLICATION_FORM_DATA, applicationFormData)
            .sessionAttr("user", user))
        .andDo(print())
        .andExpect(redirectedUrl("/application/client/search"));

    verifyNoInteractions(providerService);
    verifyNoInteractions(lookupService);
  }



  private UserDetail buildUser() {
    return new UserDetail()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId")
        .provider(buildBaseProvider());
  }

  private BaseProvider buildBaseProvider() {
    return new BaseProvider()
        .id(123)
        .addOfficesItem(
            new BaseOffice()
                .id(1)
                .name("Office 1"));
  }
}
