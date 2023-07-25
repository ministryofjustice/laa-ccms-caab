package uk.gov.laa.ccms.caab.controller.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

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
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ApplicationSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.FeeEarnerDetail;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class ApplicationSearchControllerTest {
    @Mock
    private DataService dataService;

    @Mock
    private ApplicationSearchCriteriaValidator validator;

    @InjectMocks
    private ApplicationSearchController applicationSearchController;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    public void setup() {
        mockMvc = standaloneSetup(applicationSearchController).build();
    }

    @Test
    public void testGetApplicationSearchAddsFeeEarnersToModel() throws Exception {
        final UserDetail user = buildUser();

        final FeeEarnerDetail feeEarnerDetail = new FeeEarnerDetail().addContentItem(
            new ContactDetail().id(123).name("A Fee Earner"));

        when(dataService.getFeeEarners(user.getProvider().getId())).thenReturn(Mono.just(feeEarnerDetail));

        this.mockMvc.perform(get("/application/search")
                .sessionAttr("user", user))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("/application/application-search"))
                .andExpect(model().attribute("feeEarners", feeEarnerDetail.getContent()))
                .andExpect(model().attribute("offices", user.getProvider().getOffices()));

        verify(dataService, times(1)).getFeeEarners(user.getProvider().getId());
    }

    @Test
    public void testPostApplicationSearchHandlesValidationFailure() throws Exception {
        final UserDetail user = buildUser();

        final FeeEarnerDetail feeEarnerDetail = new FeeEarnerDetail().addContentItem(
            new ContactDetail().id(123).name("A Fee Earner"));

        doAnswer(invocation -> {
            Errors errors = (Errors) invocation.getArguments()[1];
            errors.rejectValue(null, "required.atLeastOneSearchCriteria",
                "You must provide at least one search criteria below. Please amend your entry.");
            return null;
        }).when(validator).validate(any(), any());

        when(dataService.getFeeEarners(user.getProvider().getId())).thenReturn(Mono.just(feeEarnerDetail));

        this.mockMvc.perform(post("/application/search")
                .sessionAttr("user", user))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(view().name("/application/application-search"))
            .andExpect(model().attribute("feeEarners", feeEarnerDetail.getContent()))
            .andExpect(model().attribute("offices", user.getProvider().getOffices()));

        verify(dataService, times(1)).getFeeEarners(user.getProvider().getId());
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