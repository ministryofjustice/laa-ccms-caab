package uk.gov.laa.ccms.caab.controller.application.summary;

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
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.Collections;
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
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.ApplicationFormData;
import uk.gov.laa.ccms.caab.bean.validators.application.ProviderDetailsValidator;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.ProviderService;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class ProviderDetailsSectionControllerTest {

  @Mock
  private ApplicationService applicationService;

  @Mock
  private ProviderService providerService;

  @Mock
  private ProviderDetailsValidator providerDetailsValidator;

  @InjectMocks
  private ProviderDetailsSectionController providerDetailsController;

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  public void setup() {
    mockMvc = standaloneSetup(providerDetailsController).build();
  }

  @Test
  public void testApplicationSummaryProviderDetailsGet() throws Exception {
    final String applicationId = "123";
    final ActiveCase activeCase = ActiveCase.builder().build();
    final UserDetail user = new UserDetail();
    final ApplicationFormData applicationFormData = new ApplicationFormData();

    when(applicationService.getProviderDetailsFormData(applicationId)).thenReturn(applicationFormData);

    this.mockMvc.perform(get("/application/sections/provider-details")
            .sessionAttr(APPLICATION_ID, applicationId)
            .sessionAttr(ACTIVE_CASE, activeCase)
            .sessionAttr(USER_DETAILS, user))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/summary/provider-details-section"))
        .andExpect(model().attribute(APPLICATION_FORM_DATA, applicationFormData))
        .andExpect(model().attribute(ACTIVE_CASE, activeCase));

    verify(applicationService, times(1)).getProviderDetailsFormData(applicationId);
    verifyNoInteractions(providerService);
  }

  @Test
  public void testApplicationSummaryProviderDetailsPost_ValidationError() throws Exception {
    final String applicationId = "123";
    final ActiveCase activeCase = ActiveCase.builder().build();
    final UserDetail user = new UserDetail().provider(new BaseProvider().id(987));
    final ApplicationFormData applicationFormData = new ApplicationFormData();
    ProviderDetail providerDetail = new ProviderDetail().id(987);

    doAnswer(invocation -> {
      Errors errors = (Errors) invocation.getArguments()[1];
      errors.rejectValue("contactNameId", "required.contactNameId", "Please select a contact name.");
      return null;
    }).when(providerDetailsValidator).validate(any(), any());

    when(providerService.getProvider(any()))
        .thenReturn(Mono.just(providerDetail));
    when(providerService.getFeeEarnersByOffice(any(), any()))
        .thenReturn(Collections.emptyList());

    this.mockMvc.perform(post("/application/sections/provider-details")
            .sessionAttr(APPLICATION_ID, applicationId)
            .sessionAttr(ACTIVE_CASE, activeCase)
            .sessionAttr(USER_DETAILS, user)
            .sessionAttr(APPLICATION_FORM_DATA, applicationFormData))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(view().name("application/summary/provider-details-section"));

    verify(providerDetailsValidator, times(1)).validate(any(), any());
    verify(providerService, times(1)).getProvider(providerDetail.getId());
    verify(providerService, times(1)).getFeeEarnersByOffice(providerDetail, applicationFormData.getOfficeId());
    verifyNoInteractions(applicationService);
  }

  @Test
  public void testApplicationSummaryProviderDetailsPost_Successful() throws Exception {
    final String applicationId = "123";
    final ActiveCase activeCase = ActiveCase.builder().build();
    final UserDetail user = new UserDetail();
    final ApplicationFormData applicationFormData = new ApplicationFormData();
    applicationFormData.setContactNameId("John Doe");

    when(applicationService.getProviderDetailsFormData(applicationId)).thenReturn(applicationFormData);

    this.mockMvc.perform(post("/application/sections/provider-details")
            .sessionAttr(APPLICATION_ID, applicationId)
            .sessionAttr(ACTIVE_CASE, activeCase)
            .sessionAttr(USER_DETAILS, user)
            .sessionAttr(APPLICATION_FORM_DATA, applicationFormData))
        .andDo(print())
        .andExpect(redirectedUrl("/application/sections"));

    verify(providerDetailsValidator, times(1)).validate(any(), any());
    verify(applicationService, times(1)).updateProviderDetails(any(), any(), any());
    verifyNoInteractions(providerService);
  }
}
