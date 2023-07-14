package uk.gov.laa.ccms.caab.controller.application;

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

import java.util.ArrayList;
import java.util.List;
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
import uk.gov.laa.ccms.caab.service.SoaGatewayService;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class CategoryOfLawControllerTest {
    @Mock
    private DataService dataService;

    @Mock
    private SoaGatewayService soaGatewayService;

    @Mock
    private ApplicationDetailsValidator applicationDetailsValidator;

    @InjectMocks
    private CategoryOfLawController categoryOfLawController;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private UserDetail user;

    private ApplicationDetails applicationDetails;

    private List<CommonLookupValueDetail> categoriesOfLaw;

    @BeforeEach
    public void setup() {
        mockMvc = standaloneSetup(categoryOfLawController).build();

        this.user = buildUser();

        applicationDetails = new ApplicationDetails();
        applicationDetails.setOfficeId(345);

        categoriesOfLaw = new ArrayList<>();
        categoriesOfLaw.add(new CommonLookupValueDetail().code("CAT1").description("Category 1"));
        categoriesOfLaw.add(new CommonLookupValueDetail().code("CAT2").description("Category 2"));
    }

    @Test
    public void testGetCategoryOfLawAddsCategoriesOfLawToModel() throws Exception {
        final List<String> categoryOfLawCodes = new ArrayList<>();
        categoryOfLawCodes.add("CAT1");
        categoryOfLawCodes.add("CAT2");

        when(soaGatewayService.getCategoryOfLawCodes(
            user.getProvider().getId(),
            applicationDetails.getOfficeId(),
            user.getLoginId(),
            user.getUserType(),
            Boolean.TRUE)).thenReturn(categoryOfLawCodes);

        when(dataService.getCategoriesOfLaw(categoryOfLawCodes)).thenReturn(categoriesOfLaw);

        this.mockMvc.perform(get("/application/category-of-law")
                        .flashAttr("applicationDetails", applicationDetails)
                        .sessionAttr("user", user))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("/application/select-category-of-law"))
                .andExpect(model().attribute("categoriesOfLaw", categoriesOfLaw))
                .andExpect(model().attributeExists("applicationDetails"));

        verify(soaGatewayService).getCategoryOfLawCodes(user.getProvider().getId(),
            applicationDetails.getOfficeId(),
            user.getLoginId(),
            user.getUserType(),
            Boolean.TRUE);

        verify(dataService).getCategoriesOfLaw(categoryOfLawCodes);
    }

    @Test
    public void testGetCategoryOfLaw_ExceptionFundingReturnsAllCodes() throws Exception {
        when(dataService.getAllCategoriesOfLaw()).thenReturn(categoriesOfLaw);

        this.mockMvc.perform(get("/application/category-of-law?exceptional_funding=true")
                .flashAttr("applicationDetails", applicationDetails)
                .sessionAttr("user", user))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(view().name("/application/select-category-of-law"))
            .andExpect(model().attribute("categoriesOfLaw", categoriesOfLaw))
            .andExpect(model().attributeExists("applicationDetails"));

        assertTrue(applicationDetails.isExceptionalFunding());
        verifyNoInteractions(soaGatewayService);
        verify(dataService).getAllCategoriesOfLaw();
    }


    @Test
    public void testPostCategoryOfLawHandlesValidationError() throws Exception {
        final List<String> categoryOfLawCodes = new ArrayList<>();
        categoryOfLawCodes.add("CAT1");
        categoryOfLawCodes.add("CAT2");

        when(soaGatewayService.getCategoryOfLawCodes(
            user.getProvider().getId(),
            applicationDetails.getOfficeId(),
            user.getLoginId(),
            user.getUserType(),
            Boolean.TRUE)).thenReturn(categoryOfLawCodes);

        when(dataService.getCategoriesOfLaw(categoryOfLawCodes)).thenReturn(categoriesOfLaw);

        doAnswer(invocation -> {
            Errors errors = (Errors) invocation.getArguments()[1];
            errors.rejectValue("categoryOfLawId", "required.categoryOfLawId", "Please select a category of law.");
            return null;
        }).when(applicationDetailsValidator).validateCategoryOfLaw(any(), any());

        this.mockMvc.perform(post("/application/category-of-law")
                        .flashAttr("applicationDetails", applicationDetails)
                        .sessionAttr("user", user))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("/application/select-category-of-law"))
                .andExpect(model().attribute("categoriesOfLaw", categoriesOfLaw));
    }

    @Test
    public void testPostCategoryOfLawIsSuccessful() throws Exception {
        applicationDetails.setCategoryOfLawId("CAT1");

        this.mockMvc.perform(post("/application/category-of-law")
                .flashAttr("applicationDetails", applicationDetails)
                .sessionAttr("user", user))
            .andDo(print())
            .andExpect(redirectedUrl("/application/application-type"));

        verifyNoInteractions(soaGatewayService);
        verifyNoInteractions(dataService);
    }

    @Test
    public void testPostCategoryOfLaw_HandlesExceptionalFunding() throws Exception {
        applicationDetails.setCategoryOfLawId("CAT1");
        applicationDetails.setExceptionalFunding(true);

        this.mockMvc.perform(post("/application/category-of-law")
                .flashAttr("applicationDetails", applicationDetails)
                .sessionAttr("user", user))
            .andDo(print())
            .andExpect(redirectedUrl("/application/client-search"));

        verifyNoInteractions(soaGatewayService);
        verifyNoInteractions(dataService);
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
}