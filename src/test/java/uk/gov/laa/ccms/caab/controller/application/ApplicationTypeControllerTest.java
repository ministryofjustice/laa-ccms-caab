package uk.gov.laa.ccms.caab.controller.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
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
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_APPLICATION_TYPE;

import java.util.Collections;
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
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.bean.ApplicationDetailsValidator;
import uk.gov.laa.ccms.caab.service.DataService;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class ApplicationTypeControllerTest {
  @Mock
  private DataService dataService;

  @Mock
  private ApplicationDetailsValidator applicationDetailsValidator;

  @InjectMocks
  private ApplicationTypeController applicationTypeController;

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(applicationTypeController).build();
  }

  @Test
  public void testGetApplicationTypeAddsApplicationTypesToModel() throws Exception {
    final List<CommonLookupValueDetail> applicationTypes = Collections.singletonList(
        new CommonLookupValueDetail().type("Type 1").code("Code 1"));

    when(dataService.getCommonValues(COMMON_VALUE_APPLICATION_TYPE)).thenReturn(
        Mono.just(applicationTypes));

    this.mockMvc.perform(get("/application/application-type")
            .sessionAttr("applicationDetails", new ApplicationDetails()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/select-application-type"))
        .andExpect(model().attribute("applicationDetails", new ApplicationDetails()))
        .andExpect(model().attribute("applicationTypes", applicationTypes));

    verify(dataService, times(1)).getCommonValues(COMMON_VALUE_APPLICATION_TYPE);
  }

  @Test
  public void testGetApplicationTypeHandlesExceptionalFunding() throws Exception {
    final ApplicationDetails applicationDetails = new ApplicationDetails();
    applicationDetails.setApplicationTypeCategory("ECF");
    applicationDetails.setExceptionalFunding(true);

    this.mockMvc.perform(get("/application/application-type")
            .flashAttr("applicationDetails", applicationDetails))
        .andDo(print())
        .andExpect(redirectedUrl("/application/client/search"));

    verifyNoInteractions(dataService);
  }

  @Test
  public void testPostApplicationTypeHandlesValidationError() throws Exception {
    final ApplicationDetails applicationDetails = new ApplicationDetails();

    final List<CommonLookupValueDetail> applicationTypes = Collections.singletonList(
        new CommonLookupValueDetail().type("Type 1").code("Code 1"));

    when(dataService.getCommonValues(COMMON_VALUE_APPLICATION_TYPE)).thenReturn(
        Mono.just(applicationTypes));

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("applicationTypeCategory", "required.applicationTypeCategory",
          "Please select an application type.");
      return null;
    }).when(applicationDetailsValidator).validateApplicationTypeCategory(any(), any());

    this.mockMvc.perform(post("/application/application-type")
            .flashAttr("applicationDetails", applicationDetails))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/select-application-type"));
  }

  @Test
  public void testPostApplicationTypeIsSuccessful() throws Exception {
    final ApplicationDetails applicationDetails = new ApplicationDetails();
    applicationDetails.setApplicationTypeCategory("ECF");

    this.mockMvc.perform(post("/application/application-type")
            .flashAttr("applicationDetails", applicationDetails))
        .andDo(print())
        .andExpect(redirectedUrl("/application/delegated-functions"));

    verifyNoInteractions(dataService);
  }


}
