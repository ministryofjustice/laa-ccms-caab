package uk.gov.laa.ccms.caab.controller.application.section;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_PROCEEDING_ORDER_TYPE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_COSTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_PRIOR_AUTHORITIES;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_PROCEEDINGS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CURRENT_PROCEEDING;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CURRENT_SCOPE_LIMITATION;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PRIOR_AUTHORITY_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROCEEDING_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROCEEDING_FLOW_FORM_DATA_OLD;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.PROCEEDING_SCOPE_LIMITATIONS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.SCOPE_LIMITATION_FLOW_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import jakarta.servlet.ServletException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.bean.costs.CostsFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityDetailsFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityFlowFormData;
import uk.gov.laa.ccms.caab.bean.priorauthority.PriorAuthorityTypeFormData;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFlowFormData;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataFurtherDetails;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataMatterTypeDetails;
import uk.gov.laa.ccms.caab.bean.proceeding.ProceedingFormDataProceedingDetails;
import uk.gov.laa.ccms.caab.bean.scopelimitation.ScopeLimitationFlowFormData;
import uk.gov.laa.ccms.caab.bean.scopelimitation.ScopeLimitationFormDataDetails;
import uk.gov.laa.ccms.caab.bean.validators.costs.CostDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.priorauthority.PriorAuthorityDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.priorauthority.PriorAuthorityTypeDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.proceedings.ProceedingDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.proceedings.ProceedingFurtherDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.proceedings.ProceedingMatterTypeDetailsValidator;
import uk.gov.laa.ccms.caab.bean.validators.scopelimitation.ScopeLimitationDetailsValidator;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.mapper.ProceedingAndCostsMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.data.model.ClientInvolvementTypeLookupDetail;
import uk.gov.laa.ccms.data.model.ClientInvolvementTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.LevelOfServiceLookupDetail;
import uk.gov.laa.ccms.data.model.LevelOfServiceLookupValueDetail;
import uk.gov.laa.ccms.data.model.MatterTypeLookupDetail;
import uk.gov.laa.ccms.data.model.MatterTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetail;
import uk.gov.laa.ccms.data.model.PriorAuthorityTypeDetails;
import uk.gov.laa.ccms.data.model.ProceedingDetails;
import uk.gov.laa.ccms.data.model.ScopeLimitationDetails;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration
@WebAppConfiguration
class EditProceedingsAndCostsSectionControllerTest {

    @Mock
    private ApplicationService applicationService;
    @Mock
    private LookupService lookupService;
    @Mock
    private ProceedingMatterTypeDetailsValidator matterTypeValidator;
    @Mock
    private ProceedingDetailsValidator proceedingTypeValidator;
    @Mock
    private ProceedingFurtherDetailsValidator furtherDetailsValidator;

    @Mock
    private ScopeLimitationDetailsValidator scopeLimitationDetailsValidator;
    @Mock
    private CostDetailsValidator costDetailsValidator;

    @Mock
    private PriorAuthorityTypeDetailsValidator priorAuthorityTypeDetailsValidator;

    @Mock
    private PriorAuthorityDetailsValidator  priorAuthorityDetailsValidator;

    @Mock
    private ProceedingAndCostsMapper proceedingAndCostsMapper;

    @Mock
    private Model model;

    @InjectMocks
    private EditProceedingsAndCostsSectionController controller;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private static final UserDetail user = new UserDetail()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId");

    @Test
    public void testProceedingsAndCosts() throws Exception {
        final String applicationId = "testApplicationId";
        final ApplicationDetail application = new ApplicationDetail();
        // Mock the applicationService to return a Mono of ApplicationDetail
        when(applicationService.getApplication(applicationId)).thenReturn(Mono.just(application));

        mockMvc.perform(get("/application/proceedings-and-costs")
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(USER_DETAILS, user))
            .andExpect(status().isOk())
            .andExpect(view().name("application/proceedings-and-costs-section"))
            .andExpect(model().attribute(APPLICATION, application));

        verify(applicationService, times(1)).getApplication(applicationId);
        verify(applicationService, times(1)).prepareProceedingSummary(applicationId, application, user);
    }

    @Test
    public void testProceedingsMakeLead() throws Exception {
        final String applicationId = "testApplicationId";
        final Integer proceedingId = 1;
        final List<ProceedingDetail> proceedings = Collections.singletonList(new ProceedingDetail().id(proceedingId));

        mockMvc.perform(get("/application/proceedings/{proceeding-id}/make-lead", proceedingId)
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(APPLICATION_PROCEEDINGS, proceedings)
                .sessionAttr(USER_DETAILS, user))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/proceedings-and-costs"));

        verify(applicationService, times(1)).makeLeadProceeding(applicationId, proceedingId, user);
    }

    @Test
    void testProceedingsRemoveGet() throws Exception {
        final Integer proceedingId = 1;

        mockMvc.perform(get("/application/proceedings/{proceeding-id}/remove", proceedingId))
            .andExpect(status().isOk())
            .andExpect(view().name("application/proceedings-remove"))
            .andExpect(model().attribute("proceedingId", proceedingId));
    }

    @Test
    void testProceedingsRemovePost() throws Exception {
        final String applicationId = "testApplicationId";
        final Integer proceedingId = 1;
        final List<ProceedingDetail> proceedings = Collections.singletonList(new ProceedingDetail().id(proceedingId));
        final UserDetail userDetail = new UserDetail().userId(1).userType("testUserType").loginId("testLoginId");

        doNothing().when(applicationService).deleteProceeding(eq(applicationId), anyInt(), any(UserDetail.class));

        mockMvc.perform(post("/application/proceedings/{proceeding-id}/remove", proceedingId)
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(APPLICATION_PROCEEDINGS, proceedings)
                .sessionAttr(USER_DETAILS, userDetail))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/proceedings-and-costs"));

        verify(applicationService, times(1)).deleteProceeding(eq(applicationId), eq(proceedingId), eq(userDetail));
    }

    @Test
    public void testProceedingsSummary() throws Exception {
        final String applicationId = "testApplicationId";
        final Integer proceedingId = 1;
        final ApplicationDetail application = new ApplicationDetail();
        ApplicationType applicationType = new ApplicationType();
        applicationType.setId("SUBDP");
        application.setApplicationType(applicationType);
        final List<ProceedingDetail> proceedings = Collections.singletonList(new ProceedingDetail()
            .id(proceedingId)
            .typeOfOrder(new StringDisplayValue().id("orderType")));

        when(lookupService.getOrderTypeDescription(any())).thenReturn(Mono.just("orderTypeDisplayValue"));

        mockMvc.perform(get("/application/proceedings/{proceeding-id}/summary", proceedingId)
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(APPLICATION, application)
                .sessionAttr(APPLICATION_PROCEEDINGS, proceedings)
                .sessionAttr(USER_DETAILS, user))
            .andExpect(status().isOk())
            .andExpect(view().name("application/proceedings-summary"))
            .andExpect(model().attributeExists("orderTypeDisplayValue"))
            .andExpect(model().attributeExists(CURRENT_PROCEEDING));

        verify(applicationService, times(1)).prepareProceedingSummary(applicationId, application, user);
        verify(lookupService, times(1)).getOrderTypeDescription(any());
    }

    @Test
    public void testProceedingsActionMatterType_Add() throws Exception {
        final ApplicationDetail application = new ApplicationDetail();
        application.setCategoryOfLaw(new StringDisplayValue().id("categoryOfLawId"));
        final List<MatterTypeLookupValueDetail> matterTypes =
            List.of(new MatterTypeLookupValueDetail());

        // Mock the lookupService to return matter types
        when(lookupService.getMatterTypes(anyString())).thenReturn(Mono.just(new MatterTypeLookupDetail().content(matterTypes)));

        mockMvc.perform(get("/application/proceedings/add/matter-type")
                .sessionAttr(APPLICATION, application))
            .andExpect(status().isOk())
            .andExpect(view().name("application/proceedings-matter-type"))
            .andExpect(model().attributeExists("matterTypes"))
            .andExpect(model().attributeExists(PROCEEDING_FLOW_FORM_DATA));

        verify(lookupService, times(1)).getMatterTypes(application.getCategoryOfLaw().getId());
    }

    @Test
    public void testProceedingsActionMatterType_Edit() throws Exception {
        final ApplicationDetail application = new ApplicationDetail();
        application.setCategoryOfLaw(new StringDisplayValue().id("categoryOfLawId"));
        final ProceedingDetail proceeding = new ProceedingDetail().typeOfOrder(new StringDisplayValue().id("typeOfOrderId"));
        proceeding.setScopeLimitations(List.of(new ScopeLimitationDetail()));
        final List<MatterTypeLookupValueDetail> matterTypes =
            List.of(new MatterTypeLookupValueDetail());

        // Mock the lookupService to return matter types
        when(lookupService.getMatterTypes(anyString())).thenReturn(Mono.just(new MatterTypeLookupDetail().content(matterTypes)));
        when(proceedingAndCostsMapper.toProceedingFlow(any(), any())).thenReturn(new ProceedingFlowFormData("edit"));

        mockMvc.perform(get("/application/proceedings/edit/matter-type")
                .sessionAttr(APPLICATION, application)
                .sessionAttr(CURRENT_PROCEEDING, proceeding))
            .andExpect(status().isOk())
            .andExpect(view().name("application/proceedings-matter-type"))
            .andExpect(model().attributeExists("matterTypes"))
            .andExpect(model().attributeExists(PROCEEDING_FLOW_FORM_DATA))
            .andExpect(model().attributeExists(PROCEEDING_SCOPE_LIMITATIONS));

        verify(lookupService, times(1)).getMatterTypes(application.getCategoryOfLaw().getId());
    }

    @Test
    public void testProceedingsActionMatterTypePost_Add_Success() throws Exception {
        final String action = "add";
        final ApplicationDetail application = new ApplicationDetail().categoryOfLaw(new StringDisplayValue().id("categoryOfLawId"));
        final ProceedingFormDataMatterTypeDetails matterTypeDetails = new ProceedingFormDataMatterTypeDetails();
        matterTypeDetails.setMatterType("newMatterType");

        mockMvc.perform(post("/application/proceedings/{action}/matter-type", action)
                .sessionAttr(APPLICATION, application)
                .sessionAttr(PROCEEDING_FLOW_FORM_DATA, new ProceedingFlowFormData(action))
                .flashAttr("matterTypeDetails", matterTypeDetails))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/proceedings/%s/proceeding-type".formatted(action)));

        verify(matterTypeValidator, times(1)).validate(eq(matterTypeDetails), any(BindingResult.class));
    }

    @Test
    public void testProceedingsActionMatterTypePost_ValidationErrors() throws Exception {
        final String action = "add";
        final ApplicationDetail application = new ApplicationDetail().categoryOfLaw(new StringDisplayValue().id("categoryOfLawId"));
        final ProceedingFormDataMatterTypeDetails matterTypeDetails = new ProceedingFormDataMatterTypeDetails();
        final List<MatterTypeLookupValueDetail> matterTypes =
            List.of(new MatterTypeLookupValueDetail());

        // Simulate validation error
        doAnswer(invocation -> {
            BindingResult errors = invocation.getArgument(1);
            errors.rejectValue("matterType", "error.matterType", "Matter Type is required");
            return null;
        }).when(matterTypeValidator).validate(any(), any(BindingResult.class));

        when(lookupService.getMatterTypes(anyString())).thenReturn(Mono.just(new MatterTypeLookupDetail().content(matterTypes)));

        mockMvc.perform(post("/application/proceedings/{action}/matter-type", action)
                .sessionAttr(APPLICATION, application)
                .sessionAttr(PROCEEDING_FLOW_FORM_DATA, new ProceedingFlowFormData(action))
                .flashAttr("matterTypeDetails", matterTypeDetails))
            .andExpect(status().isOk())
            .andExpect(view().name("application/proceedings-matter-type"))
            .andExpect(model().attributeHasFieldErrors("matterTypeDetails", "matterType"));

        verify(matterTypeValidator, times(1)).validate(eq(matterTypeDetails), any(BindingResult.class));
    }

    @Test
    public void testProceedingsActionMatterTypePost_Edit_AmendmentCheck() throws Exception {
        final String action = "edit";
        final ApplicationDetail application = new ApplicationDetail().categoryOfLaw(new StringDisplayValue().id("categoryOfLawId"));
        final ProceedingFormDataMatterTypeDetails matterTypeDetails = new ProceedingFormDataMatterTypeDetails();
        matterTypeDetails.setMatterType("editedMatterType");
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);
        proceedingFlow.setAmended(false);
        final ProceedingFlowFormData oldProceedingFlow = new ProceedingFlowFormData(action);
        oldProceedingFlow.setMatterTypeDetails(new ProceedingFormDataMatterTypeDetails());
        oldProceedingFlow.getMatterTypeDetails().setMatterType("originalMatterType");

        mockMvc.perform(post("/application/proceedings/{action}/matter-type", action)
                .sessionAttr(APPLICATION, application)
                .sessionAttr(PROCEEDING_FLOW_FORM_DATA, proceedingFlow)
                .sessionAttr(PROCEEDING_FLOW_FORM_DATA_OLD, oldProceedingFlow)
                .flashAttr("matterTypeDetails", matterTypeDetails))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/proceedings/%s/proceeding-type".formatted(action)));

        assertTrue(proceedingFlow.isAmended());
    }

    @Test
    public void testProceedingsActionProceedingType() throws Exception {
        final String action = "add";
        final ApplicationDetail application = new ApplicationDetail()
            .categoryOfLaw(new StringDisplayValue().id("categoryOfLawId"))
            .applicationType(new ApplicationType().id("applicationTypeId"));
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);
        proceedingFlow.setMatterTypeDetails(new ProceedingFormDataMatterTypeDetails());
        proceedingFlow.getMatterTypeDetails().setMatterType("matterType");
        proceedingFlow.setLeadProceeding(true);
        final ProceedingFormDataProceedingDetails proceedingTypeDetails = new ProceedingFormDataProceedingDetails();
        proceedingFlow.setProceedingDetails(proceedingTypeDetails);

        final List<uk.gov.laa.ccms.data.model.ProceedingDetail> proceedingDetails = Arrays.asList(
            new uk.gov.laa.ccms.data.model.ProceedingDetail().code("1").description("Type 1"),
            new uk.gov.laa.ccms.data.model.ProceedingDetail().code("2").description("Type 2")
        );
        when(lookupService.getProceedings(any(uk.gov.laa.ccms.data.model.ProceedingDetail.class),
            eq(null), eq("applicationTypeId"), eq(true)))
            .thenReturn(Mono.just(new ProceedingDetails().content(proceedingDetails)));

        mockMvc.perform(get("/application/proceedings/{action}/proceeding-type", action)
                .sessionAttr(PROCEEDING_FLOW_FORM_DATA, proceedingFlow)
                .sessionAttr(APPLICATION, application))
            .andExpect(status().isOk())
            .andExpect(view().name("application/proceedings-proceeding-type"))
            .andExpect(model().attribute("proceedingTypes", proceedingDetails))
            .andExpect(model().attribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow))
            .andExpect(model().attribute("proceedingTypeDetails", proceedingTypeDetails));
    }

    @Test
    public void testProceedingsActionProceedingTypePostWithValidationErrors() throws Exception {
        final String action = "add";
        final ApplicationDetail application = new ApplicationDetail()
            .categoryOfLaw(new StringDisplayValue().id("categoryOfLawId"))
            .applicationType(new ApplicationType().id("applicationTypeId"));
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);
        proceedingFlow.setMatterTypeDetails(new ProceedingFormDataMatterTypeDetails());
        proceedingFlow.getMatterTypeDetails().setMatterType("matterType");
        final ProceedingFormDataProceedingDetails proceedingTypeDetails = new ProceedingFormDataProceedingDetails();
        final List<uk.gov.laa.ccms.data.model.ProceedingDetail> proceedingDetails = Arrays.asList(
            new uk.gov.laa.ccms.data.model.ProceedingDetail().code("1").description("Type 1"),
            new uk.gov.laa.ccms.data.model.ProceedingDetail().code("2").description("Type 2")
        );

        when(lookupService.getProceedings(any(uk.gov.laa.ccms.data.model.ProceedingDetail.class),
            eq(null), eq("applicationTypeId"), eq(false)))
            .thenReturn(Mono.just(new ProceedingDetails().content(proceedingDetails)));

        doAnswer(invocation -> {
            final BindingResult errors = invocation.getArgument(1);
            errors.rejectValue("proceedingType", "error.proceedingType", "ProceedingDetail Type is required");
            return null;
        }).when(proceedingTypeValidator).validate(any(), any(BindingResult.class));

        mockMvc.perform(post("/application/proceedings/{action}/proceeding-type", action)
                .sessionAttr(APPLICATION, application)
                .sessionAttr(PROCEEDING_FLOW_FORM_DATA, proceedingFlow)
                .flashAttr("proceedingTypeDetails", proceedingTypeDetails))
            .andExpect(status().isOk())
            .andExpect(view().name("application/proceedings-proceeding-type"))
            .andExpect(model().attributeHasFieldErrors("proceedingTypeDetails", "proceedingType"));

        verify(proceedingTypeValidator, times(1)).validate(eq(proceedingTypeDetails), any(BindingResult.class));
    }

    @Test
    public void testProceedingsActionProceedingTypePostSuccess() throws Exception {
        final String action = "add";
        final ApplicationDetail application = new ApplicationDetail();
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);
        proceedingFlow.setMatterTypeDetails(new ProceedingFormDataMatterTypeDetails());
        proceedingFlow.getMatterTypeDetails().setMatterType("matterType");
        final ProceedingFormDataProceedingDetails proceedingTypeDetails = new ProceedingFormDataProceedingDetails();
        proceedingTypeDetails.setProceedingType("newProceedingType");

        mockMvc.perform(post("/application/proceedings/{action}/proceeding-type", action)
                .sessionAttr(APPLICATION, application)
                .sessionAttr(PROCEEDING_FLOW_FORM_DATA, proceedingFlow)
                .flashAttr("proceedingTypeDetails", proceedingTypeDetails))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/proceedings/%s/further-details".formatted(action)));

        assertEquals(Boolean.TRUE, proceedingFlow.isAmended());
        assertEquals(proceedingTypeDetails, proceedingFlow.getProceedingDetails());
    }

    @Test
    public void testProceedingsActionProceedingTypePost_Edit_AmendmentCheck() throws Exception {
        final String action = "edit";
        final ApplicationDetail application = new ApplicationDetail();
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);
        proceedingFlow.setMatterTypeDetails(new ProceedingFormDataMatterTypeDetails());
        proceedingFlow.getMatterTypeDetails().setMatterType("matterType");
        final ProceedingFormDataProceedingDetails proceedingTypeDetails = new ProceedingFormDataProceedingDetails();
        proceedingTypeDetails.setProceedingType("editedProceedingType");

        final ProceedingFlowFormData oldProceedingFlow = new ProceedingFlowFormData(action);
        final ProceedingFormDataProceedingDetails oldProceedingTypeDetails = new ProceedingFormDataProceedingDetails();
        oldProceedingTypeDetails.setProceedingType("originalProceedingType");
        oldProceedingFlow.setProceedingDetails(oldProceedingTypeDetails);

        mockMvc.perform(post("/application/proceedings/{action}/proceeding-type", action)
                .sessionAttr(APPLICATION, application)
                .sessionAttr(PROCEEDING_FLOW_FORM_DATA, proceedingFlow)
                .sessionAttr(PROCEEDING_FLOW_FORM_DATA_OLD, oldProceedingFlow)
                .flashAttr("proceedingTypeDetails", proceedingTypeDetails))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/proceedings/%s/further-details".formatted(action)));

        assertTrue(proceedingFlow.isAmended(), "ProceedingDetail flow should be marked as amended when proceeding type is changed.");
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testProceedingsActionFurtherDetails(final boolean orderTypeRequired) throws Exception {
        final String action = "add";
        final ApplicationDetail application = new ApplicationDetail().categoryOfLaw(new StringDisplayValue().id("categoryOfLawId"));
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);
        proceedingFlow.setProceedingDetails(new ProceedingFormDataProceedingDetails());
        proceedingFlow.getProceedingDetails().setProceedingType("proceedingType");
        proceedingFlow.getProceedingDetails().setOrderTypeRequired(orderTypeRequired);
        proceedingFlow.setMatterTypeDetails(new ProceedingFormDataMatterTypeDetails());
        proceedingFlow.getMatterTypeDetails().setMatterType("matterType");
        proceedingFlow.setFurtherDetails(new ProceedingFormDataFurtherDetails());

        final List<ClientInvolvementTypeLookupValueDetail> clientInvolvementTypes = Arrays.asList(
            new ClientInvolvementTypeLookupValueDetail().clientInvolvementType("1").clientInvolvementTypeName("Type 1"),
            new ClientInvolvementTypeLookupValueDetail().clientInvolvementType("2").clientInvolvementTypeName("Type 2"));
        final List<LevelOfServiceLookupValueDetail> levelOfServiceTypes = Arrays.asList(
            new LevelOfServiceLookupValueDetail().levelOfServiceCode("A").description("Level A"),
            new LevelOfServiceLookupValueDetail().levelOfServiceCode("B").description("Level B"));
        final List<CommonLookupValueDetail> orderTypes = Arrays.asList(
            new CommonLookupValueDetail().code("OT1").description("Order Type 1"),
            new CommonLookupValueDetail().code("OT2").description("Order Type 2"));

        when(lookupService.getProceedingClientInvolvementTypes(anyString()))
            .thenReturn(Mono.just(new ClientInvolvementTypeLookupDetail().content(clientInvolvementTypes)));
        when(lookupService.getProceedingLevelOfServiceTypes(anyString(), anyString(), anyString()))
            .thenReturn(Mono.just(new LevelOfServiceLookupDetail().content(levelOfServiceTypes)));

        if (orderTypeRequired) {
            when(lookupService.getCommonValues(COMMON_VALUE_PROCEEDING_ORDER_TYPE))
                .thenReturn(Mono.just(new CommonLookupDetail().content(orderTypes)));
        }

        final ResultActions resultActions = mockMvc.perform(get("/application/proceedings/{action}/further-details", action)
                .sessionAttr(PROCEEDING_FLOW_FORM_DATA, proceedingFlow)
                .sessionAttr(APPLICATION, application))
            .andExpect(status().isOk())
            .andExpect(view().name("application/proceedings-further-details"))
            .andExpect(model().attributeExists("clientInvolvementTypes", "levelOfServiceTypes"))
            .andExpect(model().attribute("clientInvolvementTypes", clientInvolvementTypes))
            .andExpect(model().attribute("levelOfServiceTypes", levelOfServiceTypes))
            .andExpect(model().attribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow))
            .andExpect(model().attribute("furtherDetails", proceedingFlow.getFurtherDetails()));

        if (orderTypeRequired) {
            resultActions.andExpect(model().attributeExists("orderTypes"))
                .andExpect(model().attribute("orderTypes", orderTypes));
            verify(lookupService, times(1)).getCommonValues(COMMON_VALUE_PROCEEDING_ORDER_TYPE);
        } else {
            resultActions.andExpect(model().attributeDoesNotExist("orderTypes"));
        }

        verify(lookupService, times(1)).getProceedingClientInvolvementTypes("proceedingType");
        verify(lookupService, times(1)).getProceedingLevelOfServiceTypes("categoryOfLawId", "proceedingType", "matterType");

    }

    @Test
    public void testProceedingsActionFurtherDetailsPostAddSuccess() throws Exception {
        final String action = "add";
        final ApplicationDetail application = new ApplicationDetail();
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);
        final ProceedingFormDataFurtherDetails furtherDetails = new ProceedingFormDataFurtherDetails();

        furtherDetails.setClientInvolvementType("originalClientInvolvementType");
        furtherDetails.setLevelOfService("originalLevelOfService");
        furtherDetails.setTypeOfOrder("originalTypeOfOrder");

        doNothing().when(furtherDetailsValidator).validate(any(ProceedingFlowFormData.class), any(BindingResult.class));

        mockMvc.perform(post("/application/proceedings/{action}/further-details", action)
                .sessionAttr(APPLICATION, application)
                .sessionAttr(PROCEEDING_FLOW_FORM_DATA, proceedingFlow)
                .flashAttr("furtherDetails", furtherDetails))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/proceedings/%s/confirm".formatted(action)));

        assertTrue(proceedingFlow.isAmended());
        assertEquals(furtherDetails, proceedingFlow.getFurtherDetails());
    }

    @Test
    public void testProceedingsActionFurtherDetailsPostAddWithValidationErrors() throws Exception {
        final String action = "add";
        final ApplicationDetail application = new ApplicationDetail()
            .categoryOfLaw(new StringDisplayValue().id("categoryOfLawId"));
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);
        proceedingFlow.setProceedingDetails(new ProceedingFormDataProceedingDetails());
        proceedingFlow.getProceedingDetails().setProceedingType("proceedingType");
        proceedingFlow.getProceedingDetails().setOrderTypeRequired(false);
        proceedingFlow.setMatterTypeDetails(new ProceedingFormDataMatterTypeDetails());
        proceedingFlow.getMatterTypeDetails().setMatterType("matterType");
        proceedingFlow.setFurtherDetails(new ProceedingFormDataFurtherDetails());
        final ProceedingFormDataFurtherDetails furtherDetails = new ProceedingFormDataFurtherDetails();
        final List<ClientInvolvementTypeLookupValueDetail> clientInvolvementTypes = Arrays.asList(
            new ClientInvolvementTypeLookupValueDetail().clientInvolvementType("1").clientInvolvementTypeName("Type 1"),
            new ClientInvolvementTypeLookupValueDetail().clientInvolvementType("2").clientInvolvementTypeName("Type 2"));
        final List<LevelOfServiceLookupValueDetail> levelOfServiceTypes = Arrays.asList(
            new LevelOfServiceLookupValueDetail().levelOfServiceCode("A").description("Level A"),
            new LevelOfServiceLookupValueDetail().levelOfServiceCode("B").description("Level B"));

        when(lookupService.getProceedingClientInvolvementTypes(anyString()))
            .thenReturn(Mono.just(new ClientInvolvementTypeLookupDetail().content(clientInvolvementTypes)));
        when(lookupService.getProceedingLevelOfServiceTypes(anyString(), anyString(), anyString()))
            .thenReturn(Mono.just(new LevelOfServiceLookupDetail().content(levelOfServiceTypes)));

        // Simulate validation error
        doAnswer(invocation -> {
            final BindingResult errors = invocation.getArgument(1);
            errors.reject("errorKey", "Default message");
            return null;
        }).when(furtherDetailsValidator).validate(eq(proceedingFlow), any(BindingResult.class));

        mockMvc.perform(post("/application/proceedings/{action}/further-details", action)
                .sessionAttr(APPLICATION, application)
                .sessionAttr(PROCEEDING_FLOW_FORM_DATA, proceedingFlow)
                .flashAttr("furtherDetails", furtherDetails))
            .andExpect(status().isOk())
            .andExpect(view().name("application/proceedings-further-details"))
            .andExpect(model().attributeExists(PROCEEDING_FLOW_FORM_DATA))
            .andExpect(model().attributeHasErrors("furtherDetails"));

        assertFalse(proceedingFlow.isAmended());
    }

    private static Stream<Arguments> provideFurtherDetailsForEdit() {
        return Stream.of(
            Arguments.of("newClientInvolvementType", "newLevelOfService", "newTypeOfOrder", true),
            Arguments.of("originalClientInvolvementType", "newLevelOfService", "newTypeOfOrder", true),
            Arguments.of("originalClientInvolvementType", "originalLevelOfService", "newTypeOfOrder", true),
            Arguments.of("originalClientInvolvementType", "originalLevelOfService", "originalTypeOfOrder", false)
        );
    }

    @ParameterizedTest
    @MethodSource("provideFurtherDetailsForEdit")
    public void testProceedingsActionFurtherDetailsPostEditWithParameterized(
        final String clientInvolvementType,
        final String levelOfService,
        final String typeOfOrder,
        final boolean expectedAmendment) throws Exception {

        final String action = "edit";
        final ApplicationDetail application = new ApplicationDetail();
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);
        final ProceedingFormDataFurtherDetails furtherDetails = new ProceedingFormDataFurtherDetails();
        furtherDetails.setClientInvolvementType(clientInvolvementType);
        furtherDetails.setLevelOfService(levelOfService);
        furtherDetails.setTypeOfOrder(typeOfOrder);

        final ProceedingFlowFormData oldProceedingFlow = new ProceedingFlowFormData(action);
        oldProceedingFlow.setFurtherDetails(new ProceedingFormDataFurtherDetails());
        oldProceedingFlow.getFurtherDetails().setClientInvolvementType("originalClientInvolvementType");
        oldProceedingFlow.getFurtherDetails().setLevelOfService("originalLevelOfService");
        oldProceedingFlow.getFurtherDetails().setTypeOfOrder("originalTypeOfOrder");

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(PROCEEDING_FLOW_FORM_DATA_OLD, oldProceedingFlow);

        mockMvc.perform(post("/application/proceedings/{action}/further-details", action)
                .sessionAttr(APPLICATION, application)
                .sessionAttr(PROCEEDING_FLOW_FORM_DATA, proceedingFlow)
                .session(session)
                .flashAttr("furtherDetails", furtherDetails))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/proceedings/%s/confirm".formatted(action)));

        assertEquals(expectedAmendment, proceedingFlow.isAmended(), "ProceedingDetail flow amendment status does not match expected.");
    }

    @Test
    public void testProceedingsActionConfirmFromSummaryPage() throws Exception {
        final String action = "edit";
        final ApplicationDetail application = new ApplicationDetail();
        ApplicationType applicationType = new ApplicationType();
        applicationType.setId("SUBDP");
        application.setApplicationType(applicationType);
        final ProceedingDetail proceeding = new ProceedingDetail()
            .id(1)
            .typeOfOrder(new StringDisplayValue().id("orderType").displayValue("Order Type Description"));
        final String orderTypeDisplayValue = "Order Type Description";

        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);
        proceedingFlow.setEditingScopeLimitations(true);
        proceedingFlow.setAmended(false);

        when(lookupService.getOrderTypeDescription(anyString())).thenReturn(Mono.just(orderTypeDisplayValue));
        when(proceedingAndCostsMapper.toProceedingFlow(any(ProceedingDetail.class), anyString()))
            .thenReturn(proceedingFlow);

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(CURRENT_PROCEEDING, proceeding);
        session.setAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);

        mockMvc.perform(get("/application/proceedings/{action}/confirm", action)
                .sessionAttr(APPLICATION, application)
                .session(session))
            .andExpect(status().isOk())
            .andExpect(view().name("application/proceedings-confirm"))
            .andExpect(model().attributeExists(PROCEEDING_FLOW_FORM_DATA))
            .andExpect(model().attributeExists(CURRENT_PROCEEDING));

        verify(lookupService, times(1)).getOrderTypeDescription(anyString());
        verify(proceedingAndCostsMapper, times(1)).toProceedingFlow(any(ProceedingDetail.class), anyString());
    }

    @Test
    public void testProceedingsActionConfirmFromPreviousStep() throws Exception {
        final String action = "add";
        final ApplicationDetail application = new ApplicationDetail()
            .categoryOfLaw(new StringDisplayValue().id("categoryOfLawId"))
            .applicationType(new ApplicationType().id("applicationTypeId"));
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);
        proceedingFlow.setProceedingDetails(new ProceedingFormDataProceedingDetails());
        proceedingFlow.getProceedingDetails().setProceedingType("proceedingType");
        proceedingFlow.getProceedingDetails().setOrderTypeRequired(false);
        proceedingFlow.setMatterTypeDetails(new ProceedingFormDataMatterTypeDetails());
        proceedingFlow.getMatterTypeDetails().setMatterType("matterType");
        proceedingFlow.setFurtherDetails(new ProceedingFormDataFurtherDetails());
        proceedingFlow.getFurtherDetails().setClientInvolvementType("clientInvolvementType");
        proceedingFlow.getFurtherDetails().setLevelOfService("levelOfService");
        proceedingFlow.getFurtherDetails().setTypeOfOrder("typeOfOrder");
        proceedingFlow.setAmended(true);

        final ProceedingDetail proceeding = new ProceedingDetail()
            .id(1)
            .typeOfOrder(new StringDisplayValue().id("orderType").displayValue("Order Type Description"))
            .scopeLimitations(new ArrayList<>(
                Collections.singletonList(new ScopeLimitationDetail().id(1))));

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);
        session.setAttribute(CURRENT_PROCEEDING, proceeding);

        when(applicationService.getDefaultScopeLimitation(
            application.getCategoryOfLaw().getId(),
            proceedingFlow.getMatterTypeDetails().getMatterType(),
            proceedingFlow.getProceedingDetails().getProceedingType(),
            proceedingFlow.getFurtherDetails().getLevelOfService(),
            application.getApplicationType().getId()))
            .thenReturn(Mono.just(new ScopeLimitationDetails()));

        mockMvc.perform(get("/application/proceedings/{action}/confirm", action)
                .sessionAttr(APPLICATION, application)
                .session(session))
            .andExpect(status().isOk())
            .andExpect(view().name("application/proceedings-confirm"))
            .andExpect(model().attributeExists(PROCEEDING_FLOW_FORM_DATA))
            .andExpect(model().attribute(PROCEEDING_FLOW_FORM_DATA, hasProperty("amended", is(false))));

    }

    @Test
    public void testProceedingsActionConfirmPostAdd() throws Exception {
        final String action = "add";
        final ApplicationDetail application = new ApplicationDetail()
            .categoryOfLaw(new StringDisplayValue().id("categoryOfLawId"))
            .applicationType(new ApplicationType().id("applicationTypeId"))
            .amendment(false);

        final String applicationId = "testApplicationId";
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);
        proceedingFlow.setProceedingDetails(new ProceedingFormDataProceedingDetails());
        proceedingFlow.getProceedingDetails().setProceedingType("proceedingType");
        proceedingFlow.getProceedingDetails().setOrderTypeRequired(false);
        proceedingFlow.setMatterTypeDetails(new ProceedingFormDataMatterTypeDetails());
        proceedingFlow.getMatterTypeDetails().setMatterType("matterType");
        proceedingFlow.setFurtherDetails(new ProceedingFormDataFurtherDetails());
        proceedingFlow.getFurtherDetails().setClientInvolvementType("clientInvolvementType");
        proceedingFlow.getFurtherDetails().setLevelOfService("levelOfService");
        proceedingFlow.getFurtherDetails().setTypeOfOrder("typeOfOrder");
        proceedingFlow.setAmended(false);

        final List<ScopeLimitationDetail> scopeLimitations = Collections.emptyList();
        final List<ProceedingDetail> proceedings = Collections.emptyList();

        when(applicationService.getProceedingCostLimitation(anyString(), anyString(), anyString(), anyString(), anyString(), any(List.class)))
            .thenReturn(new BigDecimal("1000"));
        when(applicationService.getProceedingStage(anyString(), anyString(), anyString(), anyString(), any(List.class), anyBoolean()))
            .thenReturn(1);

        when(proceedingAndCostsMapper.toProceeding(any(), any(), any()))
            .thenReturn(new ProceedingDetail());

        mockMvc.perform(post("/application/proceedings/{action}/confirm", action)
                .sessionAttr(APPLICATION, application)
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(PROCEEDING_FLOW_FORM_DATA, proceedingFlow)
                .sessionAttr(PROCEEDING_SCOPE_LIMITATIONS, scopeLimitations)
                .sessionAttr(APPLICATION_PROCEEDINGS, proceedings)
                .sessionAttr(USER_DETAILS, user))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/proceedings-and-costs"));

        verify(applicationService, times(1)).addProceeding(eq(applicationId), any(ProceedingDetail.class), eq(user));

    }

    @Test
    void testScopeLimitationEditAddAction() throws Exception {
        final Integer scopeLimitationId = 0;
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData("add");
        proceedingFlow.setAction("add");
        final List<ScopeLimitationDetail> scopeLimitations = Collections.singletonList(new ScopeLimitationDetail().id(scopeLimitationId));
        final ScopeLimitationFlowFormData scopeLimitationFlow = new ScopeLimitationFlowFormData("add");

        when(proceedingAndCostsMapper.toScopeLimitationFlow(any(ScopeLimitationDetail.class))).thenReturn(scopeLimitationFlow);

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);
        session.setAttribute(PROCEEDING_SCOPE_LIMITATIONS, scopeLimitations);

        mockMvc.perform(get("/application/proceedings/scope-limitations/{scope-limitation-id}/edit", scopeLimitationId)
                .session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/proceedings/scope-limitations/edit/details"));

        verify(proceedingAndCostsMapper, times(1)).toScopeLimitationFlow(any(ScopeLimitationDetail.class));
    }

    @Test
    void testScopeLimitationEditEditAction() throws Exception {
        final Integer scopeLimitationId = 1;
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData("edit");
        proceedingFlow.setAction("edit");
        final ScopeLimitationDetail scopeLimitation = new ScopeLimitationDetail().id(scopeLimitationId);
        final ProceedingDetail proceeding = new ProceedingDetail();
        proceeding.setScopeLimitations(Collections.singletonList(scopeLimitation));
        final ScopeLimitationFlowFormData scopeLimitationFlow = new ScopeLimitationFlowFormData("edit");

        when(proceedingAndCostsMapper.toScopeLimitationFlow(any(ScopeLimitationDetail.class))).thenReturn(scopeLimitationFlow);

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);
        session.setAttribute(CURRENT_PROCEEDING, proceeding);

        mockMvc.perform(get("/application/proceedings/scope-limitations/{scope-limitation-id}/edit", scopeLimitationId)
                .session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/proceedings/scope-limitations/edit/details"));

        verify(proceedingAndCostsMapper, times(1)).toScopeLimitationFlow(any(ScopeLimitationDetail.class));
    }

    @Test
    void testScopeLimitationDetailsEditAction() throws Exception {
        final String action = "edit";
        final ApplicationDetail application = new ApplicationDetail()
            .categoryOfLaw(new StringDisplayValue().id("categoryOfLawId"))
            .applicationType(new ApplicationType().id("applicationTypeId"));
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);
        proceedingFlow.getMatterTypeDetails().setMatterType("matterType");
        proceedingFlow.getProceedingDetails().setProceedingType("proceedingType");
        proceedingFlow.getFurtherDetails().setLevelOfService("levelOfService");

        final ScopeLimitationFlowFormData scopeLimitationFlow = new ScopeLimitationFlowFormData(action);

        final ScopeLimitationDetails mockedScopeLimitationDetails = new ScopeLimitationDetails()
            .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail().description("Mock Detail 1"))
            .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail().description("Mock Detail 2"));

        when(lookupService.getScopeLimitationDetails(any(
            uk.gov.laa.ccms.data.model.ScopeLimitationDetail.class)))
            .thenReturn(Mono.just(mockedScopeLimitationDetails));

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(APPLICATION, application);
        session.setAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);
        session.setAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA, scopeLimitationFlow);

        mockMvc.perform(get("/application/proceedings/scope-limitations/{action}/details", action)
                .session(session))
            .andExpect(status().isOk())
            .andExpect(view().name("application/proceedings-scope-limitations-details"))
            .andExpect(model().attributeExists(SCOPE_LIMITATION_FLOW_FORM_DATA))
            .andExpect(model().attribute(SCOPE_LIMITATION_FLOW_FORM_DATA, scopeLimitationFlow));
    }

    @Test
    void testScopeLimitationDetailsAddAction() throws Exception {
        final String action = "add";
        final ApplicationDetail application = new ApplicationDetail()
            .categoryOfLaw(new StringDisplayValue().id("categoryOfLawId"))
            .applicationType(new ApplicationType().id("applicationTypeId"));
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);
        proceedingFlow.getMatterTypeDetails().setMatterType("matterType");
        proceedingFlow.getProceedingDetails().setProceedingType("proceedingType");
        proceedingFlow.getFurtherDetails().setLevelOfService("levelOfService");

        final ScopeLimitationDetails mockedScopeLimitationDetails = new ScopeLimitationDetails()
            .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail().description("Mock Detail 1"))
            .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail().description("Mock Detail 2"));

        when(lookupService.getScopeLimitationDetails(any(
            uk.gov.laa.ccms.data.model.ScopeLimitationDetail.class)))
            .thenReturn(Mono.just(mockedScopeLimitationDetails));

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(APPLICATION, application);
        session.setAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);

        mockMvc.perform(get("/application/proceedings/scope-limitations/{action}/details", action)
                .session(session))
            .andExpect(status().isOk())
            .andExpect(view().name("application/proceedings-scope-limitations-details"))
            .andExpect(model().attributeExists(SCOPE_LIMITATION_FLOW_FORM_DATA))
            .andExpect(model().attribute(SCOPE_LIMITATION_FLOW_FORM_DATA, hasProperty("action", is(action))));
    }

    @Test
    void testScopeLimitationDetailsPostWithValidationErrors() throws Exception {
        final String action = "edit";
        final ApplicationDetail application = new ApplicationDetail()
          .categoryOfLaw(new StringDisplayValue().id("categoryOfLawId"))
          .applicationType(new ApplicationType().id("applicationTypeId"));
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);
        proceedingFlow.getMatterTypeDetails().setMatterType("matterType");
        proceedingFlow.getProceedingDetails().setProceedingType("proceedingType");
        proceedingFlow.getFurtherDetails().setLevelOfService("levelOfService");

        final ScopeLimitationFlowFormData scopeLimitationFlow = new ScopeLimitationFlowFormData(action);
        final ScopeLimitationFormDataDetails scopeLimitationDetails = new ScopeLimitationFormDataDetails();

        final ScopeLimitationDetails mockedScopeLimitationDetails = new ScopeLimitationDetails()
            .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail().description("Mock Detail 1"))
            .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail().description("Mock Detail 2"));

        when(lookupService.getScopeLimitationDetails(any(
            uk.gov.laa.ccms.data.model.ScopeLimitationDetail.class)))
            .thenReturn(Mono.just(mockedScopeLimitationDetails));

        // Simulate validation error
        doAnswer(invocation -> {
            final BindingResult errors = invocation.getArgument(1);
            errors.reject("errorKey", "Default message");
            return null;
        }).when(scopeLimitationDetailsValidator).validate(eq(scopeLimitationDetails), any(BindingResult.class));

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(APPLICATION, application);
        session.setAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);
        session.setAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA, scopeLimitationFlow);

        mockMvc.perform(post("/application/proceedings/scope-limitations/{action}/details", action)
                .session(session)
                .flashAttr("scopeLimitationDetails", scopeLimitationDetails))
            .andExpect(status().isOk())
            .andExpect(view().name("application/proceedings-scope-limitations-details"))
            .andExpect(model().attributeHasErrors("scopeLimitationDetails"));
    }

    @Test
    void testScopeLimitationDetailsPostValidationPasses() throws Exception {
        final String action = "edit";
        final ApplicationDetail application = new ApplicationDetail()
            .categoryOfLaw(new StringDisplayValue().id("categoryOfLawId"))
            .applicationType(new ApplicationType().id("applicationTypeId"));
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);
        proceedingFlow.getMatterTypeDetails().setMatterType("matterType");
        proceedingFlow.getProceedingDetails().setProceedingType("proceedingType");
        proceedingFlow.getFurtherDetails().setLevelOfService("levelOfService");

        final ScopeLimitationFlowFormData scopeLimitationFlow = new ScopeLimitationFlowFormData(action);
        scopeLimitationFlow.setScopeLimitationId(1);
        final ScopeLimitationFormDataDetails scopeLimitationDetails = new ScopeLimitationFormDataDetails();
        scopeLimitationDetails.setScopeLimitation("newScopeLimitation");

        final ScopeLimitationDetails mockedScopeLimitationDetails = new ScopeLimitationDetails()
            .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail().description("Mock Detail 1"))
            .addContentItem(new uk.gov.laa.ccms.data.model.ScopeLimitationDetail().description("Mock Detail 2"));

        when(lookupService.getScopeLimitationDetails(any(
            uk.gov.laa.ccms.data.model.ScopeLimitationDetail.class)))
            .thenReturn(Mono.just(mockedScopeLimitationDetails));

        when(proceedingAndCostsMapper.toScopeLimitation(any(
            uk.gov.laa.ccms.data.model.ScopeLimitationDetail.class)))
            .thenReturn(new ScopeLimitationDetail());

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(APPLICATION, application);
        session.setAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);
        session.setAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA, scopeLimitationFlow);

        mockMvc.perform(post("/application/proceedings/scope-limitations/{action}/details", action)
                .session(session)
                .flashAttr("scopeLimitationDetails", scopeLimitationDetails))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/proceedings/scope-limitations/confirm"));
    }

    @Test
    void testScopeLimitationConfirm() throws Exception {
        final String action = "edit";
        final ScopeLimitationDetail scopeLimitation = new ScopeLimitationDetail();
        final ScopeLimitationFlowFormData scopeLimitationFlow = new ScopeLimitationFlowFormData(action);

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(CURRENT_SCOPE_LIMITATION, scopeLimitation);
        session.setAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA, scopeLimitationFlow);

        mockMvc.perform(get("/application/proceedings/scope-limitations/confirm")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(view().name("application/proceedings-scope-limitations-confirm"))
            .andExpect(model().attributeExists(CURRENT_SCOPE_LIMITATION))
            .andExpect(model().attributeExists(SCOPE_LIMITATION_FLOW_FORM_DATA))
            .andExpect(model().attribute(CURRENT_SCOPE_LIMITATION, scopeLimitation))
            .andExpect(model().attribute(SCOPE_LIMITATION_FLOW_FORM_DATA, scopeLimitationFlow));
    }

    @Test
    void testScopeLimitationConfirmPostActionAdd() throws Exception {
        final String action = "add";
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);
        proceedingFlow.getMatterTypeDetails().setMatterType("matterType");
        proceedingFlow.getProceedingDetails().setProceedingType("proceedingType");
        proceedingFlow.getFurtherDetails().setLevelOfService("levelOfService");

        final ScopeLimitationDetail scopeLimitation = new ScopeLimitationDetail();
        final ScopeLimitationFlowFormData scopeLimitationFlow = new ScopeLimitationFlowFormData(action);
        final List<ScopeLimitationDetail> scopeLimitations = new ArrayList<>();

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(CURRENT_SCOPE_LIMITATION, scopeLimitation);
        session.setAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);
        session.setAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA, scopeLimitationFlow);
        session.setAttribute(PROCEEDING_SCOPE_LIMITATIONS, scopeLimitations);
        session.setAttribute(USER_DETAILS, new UserDetail()); // Mocked user detail

        mockMvc.perform(post("/application/proceedings/scope-limitations/confirm")
                .session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/proceedings/%s/confirm".formatted(action)));
    }

    @Test
    void testScopeLimitationConfirmPostActionEdit() throws Exception {
        final String action = "edit";
        final ApplicationDetail application = new ApplicationDetail()
            .categoryOfLaw(new StringDisplayValue().id("categoryOfLawId"))
            .applicationType(new ApplicationType().id("applicationTypeId"));
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);
        proceedingFlow.getMatterTypeDetails().setMatterType("matterType");
        proceedingFlow.getProceedingDetails().setProceedingType("proceedingType");
        proceedingFlow.getFurtherDetails().setLevelOfService("levelOfService");

        final ScopeLimitationDetail scopeLimitation = new ScopeLimitationDetail();

        final ScopeLimitationFlowFormData scopeLimitationFlow = new ScopeLimitationFlowFormData(action);
        scopeLimitationFlow.setScopeLimitationIndex(0);

        final ProceedingDetail proceeding = new ProceedingDetail();
        proceeding.setId(123);
        final List<ScopeLimitationDetail> existingScopeLimitations = new ArrayList<>();
        existingScopeLimitations.add(new ScopeLimitationDetail().id(1));
        proceeding.setScopeLimitations(existingScopeLimitations);

        final UserDetail user = new UserDetail();

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(CURRENT_SCOPE_LIMITATION, scopeLimitation);
        session.setAttribute(APPLICATION, application);
        session.setAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);
        session.setAttribute(SCOPE_LIMITATION_FLOW_FORM_DATA, scopeLimitationFlow);
        session.setAttribute(CURRENT_PROCEEDING, proceeding);
        session.setAttribute(USER_DETAILS, user);

        mockMvc.perform(post("/application/proceedings/scope-limitations/confirm")
                .session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/proceedings/%s/confirm".formatted(action)));
    }

    @Test
    void testScopeLimitationRemove() throws Exception {
        final String action = "edit";
        final int scopeLimitationId = 1;
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);

        mockMvc.perform(get("/application/proceedings/scope-limitations/{scope-limitation-id}/remove", scopeLimitationId)
                .session(session))
            .andExpect(status().isOk())
            .andExpect(view().name("application/proceedings-scope-limitations-remove"))
            .andExpect(model().attributeExists("scopeLimitationId"))
            .andExpect(model().attribute("scopeLimitationId", scopeLimitationId))
            .andExpect(model().attributeExists(PROCEEDING_FLOW_FORM_DATA))
            .andExpect(model().attribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow));
    }

    @Test
    void testScopeLimitationRemovePos_ActionAdd() throws Exception {
        final String action = "add";
        final int scopeLimitationIndex = 0;
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);
        final List<ScopeLimitationDetail> scopeLimitations = new ArrayList<>();
        scopeLimitations.add(new ScopeLimitationDetail());

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);
        session.setAttribute(PROCEEDING_SCOPE_LIMITATIONS, scopeLimitations);
        session.setAttribute(USER_DETAILS, user);

        mockMvc.perform(post("/application/proceedings/scope-limitations/{scope-limitation-id}/remove", scopeLimitationIndex)
                .session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/proceedings/%s/confirm".formatted(action)));

        assertTrue(scopeLimitations.isEmpty(), "Scope limitations list should be empty after removal");
    }

    @Test
    void testScopeLimitationRemovePost_ActionEdit() throws Exception {
        final int scopeLimitationId = 1;
        final String action = "edit";
        final ProceedingFlowFormData proceedingFlow = new ProceedingFlowFormData(action);
        final UserDetail user = new UserDetail();
        final ProceedingDetail proceeding = new ProceedingDetail();
        proceeding.setScopeLimitations(new ArrayList<>(List.of(new ScopeLimitationDetail().id(scopeLimitationId))));

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(PROCEEDING_FLOW_FORM_DATA, proceedingFlow);
        session.setAttribute(USER_DETAILS, user);
        session.setAttribute(CURRENT_PROCEEDING, proceeding);

        mockMvc.perform(post("/application/proceedings/scope-limitations/{scope-limitation-id}/remove", scopeLimitationId)
                .session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/proceedings/%s/confirm".formatted(action)));

        assertTrue(proceeding.getScopeLimitations().isEmpty(), "ProceedingDetail's scope limitations should be empty after removal");
    }

    @Test
    void testCaseCosts() throws Exception {
        final ApplicationDetail application = new ApplicationDetail();
        final CostStructureDetail costs = new CostStructureDetail();
        costs.setRequestedCostLimitation(new BigDecimal("1000"));


        final CostsFormData costsFormData = new CostsFormData();
        costsFormData.setRequestedCostLimitation(String.valueOf(costs.getRequestedCostLimitation()));

        when(proceedingAndCostsMapper.toCostsFormData(any(BigDecimal.class)))
            .thenReturn(costsFormData);

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(APPLICATION, application);
        session.setAttribute(APPLICATION_COSTS, costs);

        mockMvc.perform(get("/application/case-costs")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(view().name("application/case-costs"))
            .andExpect(model().attributeExists("costDetails"))
            .andExpect(model().attribute("costDetails", costsFormData))
            .andExpect(model().attributeExists(APPLICATION_COSTS))
            .andExpect(model().attribute(APPLICATION_COSTS, costs))
            .andExpect(model().attributeExists(APPLICATION))
            .andExpect(model().attribute(APPLICATION, application));


        verify(proceedingAndCostsMapper, times(1))
            .toCostsFormData(costs.getRequestedCostLimitation());
    }


    @Test
    void testCaseCostsPost_ValidationPasses() throws Exception {
        final String applicationId = "123";
        final ApplicationDetail application = new ApplicationDetail();
        final CostStructureDetail costs = new CostStructureDetail();
        final UserDetail user = new UserDetail();
        final CostsFormData costsFormData = new CostsFormData();

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(APPLICATION_ID, applicationId);
        session.setAttribute(APPLICATION, application);
        session.setAttribute(APPLICATION_COSTS, costs);
        session.setAttribute(USER_DETAILS, user);

        mockMvc.perform(post("/application/case-costs")
                .session(session)
                .flashAttr("costDetails", costsFormData))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/proceedings-and-costs#case-costs"));
    }

    @Test
    void testCaseCostsPost_WithValidationErrors() throws Exception {
        final String applicationId = "123";
        final ApplicationDetail application = new ApplicationDetail();
        final CostStructureDetail costs = new CostStructureDetail();
        final UserDetail user = new UserDetail();
        final CostsFormData costsFormData = new CostsFormData();

        doAnswer(invocation -> {
            final BindingResult errors = invocation.getArgument(1);
            errors.reject("errorKey", "Default message");
            return null;
        }).when(costDetailsValidator).validate(eq(costsFormData), any(BindingResult.class));

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(APPLICATION_ID, applicationId);
        session.setAttribute(APPLICATION, application);
        session.setAttribute(APPLICATION_COSTS, costs);
        session.setAttribute(USER_DETAILS, user);

        mockMvc.perform(post("/application/case-costs")
                .session(session)
                .flashAttr("costDetails", costsFormData))
            .andExpect(status().isOk())
            .andExpect(view().name("application/case-costs"))
            .andExpect(model().attributeHasErrors("costDetails"))
            .andExpect(model().attribute("costDetails", costsFormData))
            .andExpect(model().attribute(APPLICATION_COSTS, costs))
            .andExpect(model().attribute(APPLICATION, application));

        verify(costDetailsValidator, times(1)).validate(eq(costsFormData), any(BindingResult.class));
    }

    @Test
    void testPriorAuthorityType() throws Exception {
        final List<PriorAuthorityTypeDetail> priorAuthorityTypes = List.of(
            new PriorAuthorityTypeDetail().code("1").description("Type 1"),
            new PriorAuthorityTypeDetail().code("2").description("Type 2")
        );

        final PriorAuthorityTypeDetails priorAuthorityTypeDetails = new PriorAuthorityTypeDetails().content(priorAuthorityTypes);
        when(lookupService.getPriorAuthorityTypes()).thenReturn(Mono.just(priorAuthorityTypeDetails));

        final PriorAuthorityFlowFormData priorAuthorityFlow = new PriorAuthorityFlowFormData("add");

        mockMvc.perform(get("/application/prior-authorities/add/type"))
            .andExpect(status().isOk())
            .andExpect(view().name("application/prior-authority-type"))
            .andExpect(model().attribute("priorAuthorityTypes", priorAuthorityTypes))
            .andExpect(model().attribute(PRIOR_AUTHORITY_FLOW_FORM_DATA, priorAuthorityFlow))
            .andExpect(model().attribute("priorAuthorityTypeDetails", priorAuthorityFlow.getPriorAuthorityTypeFormData()));

        verify(lookupService, times(1)).getPriorAuthorityTypes();
    }

    @Test
    void testPriorAuthorityTypePostWithValidationErrors() throws Exception {
        final PriorAuthorityTypeFormData
            priorAuthorityTypeFormData = new PriorAuthorityTypeFormData();
        priorAuthorityTypeFormData.setPriorAuthorityType("1");

        final List<PriorAuthorityTypeDetail> priorAuthorityTypes = List.of(
            new PriorAuthorityTypeDetail().code("1").description("Type 1"),
            new PriorAuthorityTypeDetail().code("2").description("Type 2")
        );

        final PriorAuthorityTypeDetails priorAuthorityTypeDetails = new PriorAuthorityTypeDetails().content(priorAuthorityTypes);
        when(lookupService.getPriorAuthorityTypes()).thenReturn(Mono.just(priorAuthorityTypeDetails));

        final PriorAuthorityFlowFormData priorAuthorityFlow = new PriorAuthorityFlowFormData("add");

        doAnswer(invocation -> {
            final BindingResult errors = invocation.getArgument(1);
            errors.rejectValue("priorAuthorityType", "required.priorAuthorityType", "Please complete 'Prior authority type'.");
            return null;
        }).when(priorAuthorityTypeDetailsValidator).validate(any(PriorAuthorityTypeFormData.class), any(BindingResult.class));

        mockMvc.perform(post("/application/prior-authorities/add/type")
                .sessionAttr(PRIOR_AUTHORITY_FLOW_FORM_DATA, priorAuthorityFlow)
                .flashAttr("priorAuthorityTypeDetails", priorAuthorityTypeFormData))
            .andExpect(status().isOk())
            .andExpect(view().name("application/prior-authority-type"))
            .andExpect(model().attributeHasFieldErrors("priorAuthorityTypeDetails", "priorAuthorityType"))
            .andExpect(model().attribute("priorAuthorityTypeDetails",
                priorAuthorityTypeFormData))
            .andExpect(model().attribute(PRIOR_AUTHORITY_FLOW_FORM_DATA, priorAuthorityFlow));

        verify(priorAuthorityTypeDetailsValidator, times(1)).validate(any(
            PriorAuthorityTypeFormData.class), any(BindingResult.class));
    }

    @Test
    void testPriorAuthorityTypePostValidationPasses() throws Exception {
        final PriorAuthorityTypeFormData
            priorAuthorityTypeDetails = new PriorAuthorityTypeFormData();
        priorAuthorityTypeDetails.setPriorAuthorityType("1");

        final PriorAuthorityFlowFormData priorAuthorityFlow = new PriorAuthorityFlowFormData("add");

        mockMvc.perform(post("/application/prior-authorities/add/type")
                .sessionAttr(PRIOR_AUTHORITY_FLOW_FORM_DATA, priorAuthorityFlow)
                .flashAttr("priorAuthorityTypeDetails", priorAuthorityTypeDetails))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/prior-authorities/add/details"));

        verify(priorAuthorityTypeDetailsValidator, times(1)).validate(any(
            PriorAuthorityTypeFormData.class), any(BindingResult.class));
    }

    @Test
    void testPriorAuthorityDetailsForAddAction() throws Exception {
        final String priorAuthorityAction = "add";
        final PriorAuthorityTypeFormData typeDetails = new PriorAuthorityTypeFormData();
        typeDetails.setPriorAuthorityType("1");

        final PriorAuthorityFlowFormData priorAuthorityFlow = new PriorAuthorityFlowFormData(priorAuthorityAction);
        priorAuthorityFlow.setPriorAuthorityTypeFormData(typeDetails);

        final PriorAuthorityTypeDetail priorAuthorityDynamicForm = createPriorAuthorityTypeDetail();

        when(applicationService.getPriorAuthorityTypeDetail(typeDetails.getPriorAuthorityType()))
            .thenReturn(priorAuthorityDynamicForm);

        final CommonLookupDetail commonLookupDetail = new CommonLookupDetail();
        final List<CommonLookupValueDetail> commonLookupValues = List.of(new CommonLookupValueDetail().code("1").description("Value 1"));
        commonLookupDetail.setContent(commonLookupValues);
        when(lookupService.getCommonValues("testLovCode")).thenReturn(Mono.just(commonLookupDetail));

        mockMvc.perform(get("/application/prior-authorities/{action}/details", priorAuthorityAction)
                .sessionAttr(PRIOR_AUTHORITY_FLOW_FORM_DATA, priorAuthorityFlow))
            .andExpect(status().isOk())
            .andExpect(view().name("application/prior-authority-details"))
            .andExpect(model().attributeExists("testCode"))
            .andExpect(model().attribute("testCode", commonLookupValues))
            .andExpect(model().attribute("priorAuthorityDynamicForm", priorAuthorityDynamicForm))
            .andExpect(model().attributeExists("priorAuthorityDetails"))
            .andExpect(model().attribute(PRIOR_AUTHORITY_FLOW_FORM_DATA, priorAuthorityFlow));

        verify(applicationService, times(1)).getPriorAuthorityTypeDetail(typeDetails.getPriorAuthorityType());
        verify(lookupService, times(1)).getCommonValues("testLovCode");
    }

    @Test
    void testPriorAuthorityDetailsForEditAction() throws Exception {
        final String priorAuthorityAction = "edit";
        final PriorAuthorityTypeFormData typeDetails = new PriorAuthorityTypeFormData();
        typeDetails.setPriorAuthorityType("1");

        final PriorAuthorityFlowFormData priorAuthorityFlow = new PriorAuthorityFlowFormData(priorAuthorityAction);
        priorAuthorityFlow.setPriorAuthorityTypeFormData(typeDetails);

        final PriorAuthorityTypeDetail priorAuthorityDynamicForm = createPriorAuthorityTypeDetail();

        when(applicationService.getPriorAuthorityTypeDetail(typeDetails.getPriorAuthorityType()))
            .thenReturn(priorAuthorityDynamicForm);

        final CommonLookupDetail commonLookupDetail = new CommonLookupDetail();
        final List<CommonLookupValueDetail> commonLookupValues = List.of(new CommonLookupValueDetail().code("1").description("Value 1"));
        commonLookupDetail.setContent(commonLookupValues);
        when(lookupService.getCommonValues("testLovCode")).thenReturn(Mono.just(commonLookupDetail));

        mockMvc.perform(get("/application/prior-authorities/{action}/details", priorAuthorityAction)
                .sessionAttr(PRIOR_AUTHORITY_FLOW_FORM_DATA, priorAuthorityFlow))
            .andExpect(status().isOk())
            .andExpect(view().name("application/prior-authority-details"))
            .andExpect(model().attributeExists("testCode"))
            .andExpect(model().attribute("testCode", commonLookupValues))
            .andExpect(model().attribute("priorAuthorityDynamicForm", priorAuthorityDynamicForm))
            .andExpect(model().attributeExists("priorAuthorityDetails"))
            .andExpect(model().attribute(PRIOR_AUTHORITY_FLOW_FORM_DATA, priorAuthorityFlow));

        verify(applicationService, times(1)).getPriorAuthorityTypeDetail(typeDetails.getPriorAuthorityType());
        verify(lookupService, times(1)).getCommonValues("testLovCode");
    }

    private PriorAuthorityTypeDetail createPriorAuthorityTypeDetail() {
        return new PriorAuthorityTypeDetail()
            .code("1")
            .valueRequired(true)
            .priorAuthorities(List.of(new uk.gov.laa.ccms.data.model.PriorAuthorityDetail()
                .code("testCode")
                .dataType("LOV")
                .lovCode("testLovCode")));
    }

    @ParameterizedTest
    @CsvSource({"add", "edit"})
    void testPriorAuthorityDetailsPostWithValidationErrors(final String action) throws Exception {
        final String applicationId = "app123";
        final UserDetail user = new UserDetail();

        final PriorAuthorityTypeFormData typeDetails = new PriorAuthorityTypeFormData();
        typeDetails.setPriorAuthorityType("1");

        final PriorAuthorityTypeDetail priorAuthorityDynamicForm = createPriorAuthorityTypeDetail();

        when(applicationService.getPriorAuthorityTypeDetail(typeDetails.getPriorAuthorityType()))
            .thenReturn(priorAuthorityDynamicForm);

        final CommonLookupDetail commonLookupDetail = new CommonLookupDetail();
        final List<CommonLookupValueDetail> commonLookupValues = List.of(new CommonLookupValueDetail().code("1").description("Value 1"));
        commonLookupDetail.setContent(commonLookupValues);
        when(lookupService.getCommonValues("testLovCode")).thenReturn(Mono.just(commonLookupDetail));

        final PriorAuthorityDetailsFormData
            priorAuthorityDetails = new PriorAuthorityDetailsFormData();
        final PriorAuthorityFlowFormData priorAuthorityFlow = new PriorAuthorityFlowFormData(action);
        priorAuthorityFlow.setPriorAuthorityTypeFormData(typeDetails);
        priorAuthorityFlow.setPriorAuthorityDetailsFormData(priorAuthorityDetails);

        doAnswer(invocation -> {
            final BindingResult errors = invocation.getArgument(1);
            errors.rejectValue("summary", "required.summary", "Error Message");
            return null;
        }).when(priorAuthorityDetailsValidator).validate(eq(priorAuthorityDetails), any(BindingResult.class));

        mockMvc.perform(post("/application/prior-authorities/{action}/details", action)
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(PRIOR_AUTHORITY_FLOW_FORM_DATA, priorAuthorityFlow)
                .sessionAttr(USER_DETAILS, user)
                .flashAttr("priorAuthorityDetails", priorAuthorityDetails))
            .andExpect(status().isOk())
            .andExpect(view().name("application/prior-authority-details"))
            .andExpect(model().attributeHasFieldErrors("priorAuthorityDetails", "summary"));

        verify(priorAuthorityDetailsValidator, times(1)).validate(eq(priorAuthorityDetails), any(BindingResult.class));
        verify(applicationService, times(1)).getPriorAuthorityTypeDetail(typeDetails.getPriorAuthorityType());
        verify(lookupService, times(1)).getCommonValues("testLovCode");
    }

    @ParameterizedTest
    @CsvSource({"add", "edit"})
    void testPriorAuthorityDetailsPostValidationPasses(final String action) throws Exception {
        final String applicationId = "app123";
        final UserDetail user = new UserDetail();

        final PriorAuthorityTypeFormData typeDetails = new PriorAuthorityTypeFormData();
        typeDetails.setPriorAuthorityType("1");

        final PriorAuthorityDetailsFormData
            priorAuthorityDetails = new PriorAuthorityDetailsFormData();
        final PriorAuthorityFlowFormData priorAuthorityFlow = new PriorAuthorityFlowFormData(action);
        priorAuthorityFlow.setPriorAuthorityTypeFormData(typeDetails);
        priorAuthorityFlow.setPriorAuthorityDetailsFormData(priorAuthorityDetails);

        mockMvc.perform(post("/application/prior-authorities/{action}/details", action)
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(PRIOR_AUTHORITY_FLOW_FORM_DATA, priorAuthorityFlow)
                .sessionAttr(USER_DETAILS, user)
                .flashAttr("priorAuthorityDetails", priorAuthorityDetails))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/proceedings-and-costs#prior-authority"));

        if ("add".equals(action)) {
            verify(applicationService, times(1)).addPriorAuthority(eq(applicationId), any(), eq(user));
        } else if ("edit".equals(action)) {
            verify(applicationService, times(1)).updatePriorAuthority(any(), eq(user));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"add", "edit"})
    @DisplayName("Prior Authority Details No Validation Errors Max Lengths Not Exceeded")
    void priorAuthorityDetailsPostNoValidationErrorsMaxLengthsNotExceeded(final String action) throws Exception {
        final String applicationId = "app123";
        final UserDetail user = new UserDetail();

        final PriorAuthorityTypeFormData typeDetails = new PriorAuthorityTypeFormData();
        typeDetails.setPriorAuthorityType("1");

        final PriorAuthorityDetailsFormData
            priorAuthorityDetails = new PriorAuthorityDetailsFormData();
        final PriorAuthorityFlowFormData priorAuthorityFlow = new PriorAuthorityFlowFormData(action);
        priorAuthorityFlow.setPriorAuthorityTypeFormData(typeDetails);
        priorAuthorityFlow.setPriorAuthorityDetailsFormData(priorAuthorityDetails);

        priorAuthorityDetails.setJustification(RandomStringUtils.insecure().nextAlphabetic(8000));
        priorAuthorityDetails.setSummary(RandomStringUtils.insecure().nextAlphabetic(35));

        mockMvc.perform(post("/application/prior-authorities/{action}/details", action)
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(PRIOR_AUTHORITY_FLOW_FORM_DATA, priorAuthorityFlow)
                .sessionAttr(USER_DETAILS, user)
                .flashAttr("priorAuthorityDetails", priorAuthorityDetails))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/proceedings-and-costs#prior-authority"));

        if ("add".equals(action)) {
            verify(applicationService, times(1)).addPriorAuthority(eq(applicationId), any(), eq(user));
        } else if ("edit".equals(action)) {
            verify(applicationService, times(1)).updatePriorAuthority(any(), eq(user));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"add", "edit"})
    @DisplayName("Prior Authority Details Validation Errors Max Lengths Exceeded")
    void priorAuthorityDetailsPostValidationErrorsMaxLengthsExceeded(final String action) throws Exception {
        final String applicationId = "app123";
        final UserDetail user = new UserDetail();

        final PriorAuthorityTypeFormData typeDetails = new PriorAuthorityTypeFormData();
        typeDetails.setPriorAuthorityType("1");

        final PriorAuthorityTypeDetail priorAuthorityDynamicForm = createPriorAuthorityTypeDetail();

        when(applicationService.getPriorAuthorityTypeDetail(typeDetails.getPriorAuthorityType()))
            .thenReturn(priorAuthorityDynamicForm);

        final CommonLookupDetail commonLookupDetail = new CommonLookupDetail();
        final List<CommonLookupValueDetail> commonLookupValues = List.of(new CommonLookupValueDetail().code("1").description("Value 1"));
        commonLookupDetail.setContent(commonLookupValues);
        when(lookupService.getCommonValues("testLovCode")).thenReturn(Mono.just(commonLookupDetail));

        final PriorAuthorityDetailsFormData
            priorAuthorityDetails = new PriorAuthorityDetailsFormData();
        final PriorAuthorityFlowFormData priorAuthorityFlow = new PriorAuthorityFlowFormData(action);
        priorAuthorityFlow.setPriorAuthorityTypeFormData(typeDetails);
        priorAuthorityFlow.setPriorAuthorityDetailsFormData(priorAuthorityDetails);

        priorAuthorityDetails.setJustification(RandomStringUtils.insecure().nextAlphabetic(8001));
        priorAuthorityDetails.setSummary(RandomStringUtils.insecure().nextAlphabetic(36));

        mockMvc.perform(post("/application/prior-authorities/{action}/details", action)
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(PRIOR_AUTHORITY_FLOW_FORM_DATA, priorAuthorityFlow)
                .sessionAttr(USER_DETAILS, user)
                .flashAttr("priorAuthorityDetails", priorAuthorityDetails))
            .andExpect(status().isOk())
            .andExpect(view().name("application/prior-authority-details"))
            .andExpect(model().attributeHasFieldErrors("priorAuthorityDetails", "summary"))
            .andExpect(model().attributeHasFieldErrors("priorAuthorityDetails", "justification"));

        verify(priorAuthorityDetailsValidator, times(1)).validate(eq(priorAuthorityDetails), any(BindingResult.class));
        verify(applicationService, times(1)).getPriorAuthorityTypeDetail(typeDetails.getPriorAuthorityType());
        verify(lookupService, times(1)).getCommonValues("testLovCode");
    }

    @Test
    void testPriorAuthorityConfirmFound() throws Exception {
        final int priorAuthorityId = 1;
        final PriorAuthorityDetail priorAuthority = new PriorAuthorityDetail();
        priorAuthority.setId(priorAuthorityId);

        final List<PriorAuthorityDetail> priorAuthorities = Collections.singletonList(priorAuthority);

        when(proceedingAndCostsMapper.toPriorAuthorityFlowFormData(any(PriorAuthorityDetail.class)))
            .thenReturn(new PriorAuthorityFlowFormData("edit"));

        mockMvc.perform(get("/application/prior-authorities/{prior-authority-id}/confirm", priorAuthorityId)
                .sessionAttr(APPLICATION_PRIOR_AUTHORITIES, priorAuthorities))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/prior-authorities/edit/details"));

        verify(proceedingAndCostsMapper, times(1)).toPriorAuthorityFlowFormData(priorAuthority);
    }

    @Test
    void testPriorAuthorityConfirmNotFound() {
        final int priorAuthorityId = 1;
        final List<PriorAuthorityDetail> priorAuthorities = Collections.emptyList();

        final Exception exception = assertThrows(ServletException.class, () ->
            mockMvc.perform(get("/application/prior-authorities/{prior-authority-id}/confirm", priorAuthorityId)
                .sessionAttr(APPLICATION_PRIOR_AUTHORITIES, priorAuthorities))
        );

        assertInstanceOf(CaabApplicationException.class, exception.getCause());
        assertEquals("No prior authority found with id: " + priorAuthorityId, exception.getCause().getMessage());
    }

    @Test
    void testPriorAuthorityRemoveFound() throws Exception {
        final int priorAuthorityId = 1;
        final PriorAuthorityDetail priorAuthority = new PriorAuthorityDetail();
        priorAuthority.setId(priorAuthorityId);

        final List<PriorAuthorityDetail> priorAuthorities = Collections.singletonList(priorAuthority);

        mockMvc.perform(get("/application/prior-authorities/{prior-authority-id}/remove", priorAuthorityId)
                .sessionAttr(APPLICATION_PRIOR_AUTHORITIES, priorAuthorities))
            .andExpect(status().isOk())
            .andExpect(view().name("application/prior-authority-remove"))
            .andExpect(model().attribute("priorAuthority", priorAuthority));
    }

    @Test
    void testPriorAuthorityRemoveNotFound() {
        final int priorAuthorityId = 1;
        final List<PriorAuthorityDetail> priorAuthorities = Collections.emptyList();

        final Exception exception = assertThrows(ServletException.class, () ->
            mockMvc.perform(get("/application/prior-authorities/{prior-authority-id}/remove", priorAuthorityId)
                .sessionAttr(APPLICATION_PRIOR_AUTHORITIES, priorAuthorities))
        );

        assertInstanceOf(CaabApplicationException.class, exception.getCause());
        assertEquals("No prior authority found with id: " + priorAuthorityId, exception.getCause().getMessage());
    }

    @Test
    void testPriorAuthorityRemovePostSuccess() throws Exception {
        final int priorAuthorityId = 1;
        final UserDetail user = new UserDetail();
        final PriorAuthorityDetail priorAuthority = new PriorAuthorityDetail();
        priorAuthority.setId(priorAuthorityId);

        final List<PriorAuthorityDetail> priorAuthorities = new ArrayList<>();
        priorAuthorities.add(priorAuthority);

        mockMvc.perform(post("/application/prior-authorities/{prior-authority-id}/remove", priorAuthorityId)
                .sessionAttr(APPLICATION_PRIOR_AUTHORITIES, priorAuthorities)
                .sessionAttr(USER_DETAILS, user))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/proceedings-and-costs#prior-authority"));

        verify(applicationService, times(1)).deletePriorAuthority(priorAuthorityId, user);
    }

    @Test
    void testPriorAuthorityRemovePostNotFound() {
        final int priorAuthorityId = 1;
        final UserDetail user = new UserDetail();
        final List<PriorAuthorityDetail> priorAuthorities = new ArrayList<>();

        final Exception exception = assertThrows(ServletException.class, () ->
            mockMvc.perform(post("/application/prior-authorities/{prior-authority-id}/remove", priorAuthorityId)
                .sessionAttr(APPLICATION_PRIOR_AUTHORITIES, priorAuthorities)
                .sessionAttr(USER_DETAILS, user))
        );

        assertInstanceOf(CaabApplicationException.class, exception.getCause());
        assertEquals("No prior authority found with id: " + priorAuthorityId, exception.getCause().getMessage());
    }







}
