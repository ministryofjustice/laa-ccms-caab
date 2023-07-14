package uk.gov.laa.ccms.caab.controller.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.FlashMap;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.ApplicationDetailsValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class DelegatedFunctionsControllerTest {

    @Mock
    private ApplicationDetailsValidator applicationDetailsValidator;

    @InjectMocks
    private DelegatedFunctionsController delegatedFunctionsController;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private ApplicationDetails applicationDetails;

    @BeforeEach
    public void setup() {
        mockMvc = standaloneSetup(delegatedFunctionsController).build();
        applicationDetails = new ApplicationDetails();
    }

    @Test
    public void testGetDelegatedFunctions() throws Exception {
        this.mockMvc.perform(get("/application/delegated-functions")
                        .sessionAttr("applicationDetails", new ApplicationDetails()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("/application/select-delegated-functions"))
                .andExpect(model().attribute("applicationDetails", new ApplicationDetails()));
    }

    @Test
    public void testPostDelegatedFunctionsHandlesValidationError() throws Exception {
        doAnswer(invocation -> {
            Errors errors = (Errors) invocation.getArguments()[1];
            errors.rejectValue("delegatedFunctionsOption", "required.delegatedFunctionsOption", "Please complete 'Are delegated functions used'.");
            return null;
        }).when(applicationDetailsValidator).validateDelegatedFunction(any(), any());

        this.mockMvc.perform(post("/application/delegated-functions")
                    .flashAttr("applicationDetails", applicationDetails))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("/application/select-delegated-functions"));
    }

    @ParameterizedTest
    @CsvSource({"SUB, Y, SUBDP",
                "SUB, N, SUB",
                "EMER, Y, DP",
                "EMER, N, EMER"})
    public void testPostDelegatedFunctionsIsSuccessful(String category, String delegatedFunctionsOption,
                                                       String expectedApplicationType) throws Exception {
        applicationDetails.setApplicationTypeCategory(category);
        applicationDetails.setDelegatedFunctionsOption(delegatedFunctionsOption);

        this.mockMvc.perform(post("/application/delegated-functions")
                        .flashAttr("applicationDetails", applicationDetails))
                .andDo(print())
                .andExpect(redirectedUrl("/application/client-search"))
                .andReturn();

        assertEquals(expectedApplicationType, applicationDetails.getApplicationTypeId());
    }


}