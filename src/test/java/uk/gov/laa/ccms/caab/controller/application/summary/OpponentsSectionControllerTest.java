package uk.gov.laa.ccms.caab.controller.application.summary;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ORGANISATION_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_RELATIONSHIP_TO_CLIENT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_OPPONENTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CURRENT_OPPONENT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ORGANISATION_SEARCH_CRITERIA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ORGANISATION_SEARCH_RESULTS;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Errors;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.bean.opponent.AbstractOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.opponent.IndividualOpponentValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.OrganisationOpponentValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.OrganisationSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.exception.TooManyResultsException;
import uk.gov.laa.ccms.caab.model.OrganisationResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.caab.service.LookupService;
import uk.gov.laa.ccms.caab.service.OpponentService;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupDetail;
import uk.gov.laa.ccms.data.model.RelationshipToCaseLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration
@WebAppConfiguration
class OpponentsSectionControllerTest {

    @Mock
    private ApplicationService applicationService;

    @Mock
    private OpponentService opponentService;

    @Mock
    private LookupService lookupService;

    @Mock
    private OrganisationSearchCriteriaValidator organisationSearchCriteriaValidator;

    @Mock
    private OrganisationOpponentValidator organisationOpponentValidator;

    @Mock
    private IndividualOpponentValidator individualOpponentValidator;

    @InjectMocks
    private OpponentsSectionController controller;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    private static final UserDetail user = new UserDetail()
        .userId(1)
        .userType("testUserType")
        .loginId("testLoginId");

    @Test
    void opponents() throws Exception {
        when(applicationService.getOpponents(any())).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/application/summary/opponents")
                .sessionAttr("applicationId", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("application/summary/opponents-section"));
    }

    @Test
    void organisationSearch() throws Exception {
        CommonLookupDetail orgTypes = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES)).thenReturn(Mono.just(orgTypes));

        mockMvc.perform(get("/application/opponents/organisation/search"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(model().attribute("organisationTypes", orgTypes.getContent()))
            .andExpect(view().name("application/opponents/opponents-organisation-search"));

    }

    @Test
    void organisationSearchPost_noValidationErrors_redirectsToResults() throws Exception {

        mockMvc.perform(post("/application/opponents/organisation/search")
                .flashAttr(ORGANISATION_SEARCH_CRITERIA, new OrganisationSearchCriteria()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/opponents/organisation/search/results"));

    }

    @Test
    void organisationSearchPost_validationErrors_returnsToSearch() throws Exception {
        doAnswer(invocation -> {
            Errors errors = (Errors) invocation.getArguments()[1];
            errors.rejectValue("name", "required.name", "Please complete 'Name'.");
            return null;
        }).when(organisationSearchCriteriaValidator).validate(any(), any());

        CommonLookupDetail orgTypes = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES)).thenReturn(Mono.just(orgTypes));

        mockMvc.perform(post("/application/opponents/organisation/search")
                .flashAttr(ORGANISATION_SEARCH_CRITERIA, new OrganisationSearchCriteria()))
            .andDo(print())
            .andExpect(model().attribute("organisationTypes", orgTypes.getContent()))
            .andExpect(status().isOk())
            .andExpect(view().name("application/opponents/opponents-organisation-search"));

    }

    @Test
    void organisationSearchResultsGet_noResults_displaysNoResultsView() throws Exception {
        int page = 0;
        int size = 20;
        OrganisationSearchCriteria searchCriteria = new OrganisationSearchCriteria();

        ResultsDisplay<OrganisationResultRowDisplay> resultsDisplay = new ResultsDisplay<>();
        resultsDisplay.setContent(new ArrayList<>());

        when(opponentService.getOrganisations(
            searchCriteria,
            user.getLoginId(),
            user.getUserType(),
            page,
            size)).thenReturn(resultsDisplay);

        mockMvc.perform(get("/application/opponents/organisation/search/results")
                .flashAttr(ORGANISATION_SEARCH_CRITERIA, searchCriteria)
                .sessionAttr(USER_DETAILS, user)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(view().name("application/opponents/opponents-organisation-search-no-results"));
    }

    @Test
    void organisationSearchResultsGet_tooMany_displaysNoResultsView() throws Exception {
        int page = 0;
        int size = 20;
        OrganisationSearchCriteria searchCriteria = new OrganisationSearchCriteria();

        when(opponentService.getOrganisations(
            searchCriteria,
            user.getLoginId(),
            user.getUserType(),
            page,
            size)).thenThrow(new TooManyResultsException("too many"));

        mockMvc.perform(get("/application/opponents/organisation/search/results")
                .flashAttr(ORGANISATION_SEARCH_CRITERIA, searchCriteria)
                .sessionAttr(USER_DETAILS, user)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(view().name("application/opponents/opponents-organisation-search-too-many-results"));
    }

    @Test
    void organisationSearchResultsGet_displaysResultsView() throws Exception {
        int page = 0;
        int size = 20;
        OrganisationSearchCriteria searchCriteria = new OrganisationSearchCriteria();

        ResultsDisplay<OrganisationResultRowDisplay> resultsDisplay = new ResultsDisplay<>();
        resultsDisplay.setContent(List.of(new OrganisationResultRowDisplay()));

        when(opponentService.getOrganisations(
            searchCriteria,
            user.getLoginId(),
            user.getUserType(),
            page,
            size)).thenReturn(resultsDisplay);

        mockMvc.perform(get("/application/opponents/organisation/search/results")
                .flashAttr(ORGANISATION_SEARCH_CRITERIA, searchCriteria)
                .sessionAttr(USER_DETAILS, user)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(model().attribute(ORGANISATION_SEARCH_RESULTS, resultsDisplay))
            .andExpect(view().name("application/opponents/opponents-organisation-search-results"));
    }

    @Test
    void selectSharedOrganisationGet_displaysConfirmationScreen() throws Exception {
        String selectedOrgId = "123";

        ResultsDisplay<OrganisationResultRowDisplay> resultsDisplay = new ResultsDisplay<>();
        OrganisationResultRowDisplay organisationResultRowDisplay = new OrganisationResultRowDisplay();
        organisationResultRowDisplay.setPartyId(selectedOrgId);
        resultsDisplay.setContent(List.of(organisationResultRowDisplay));

        OrganisationOpponentFormData opponentFormData = new OrganisationOpponentFormData();

        when(opponentService.getOrganisationOpponent(
            selectedOrgId,
            user.getLoginId(),
            user.getUserType())).thenReturn(opponentFormData);

        RelationshipToCaseLookupDetail relationshipToCaseLookupDetail =
            new RelationshipToCaseLookupDetail().addContentItem(new RelationshipToCaseLookupValueDetail());
        when(lookupService.getOrganisationToCaseRelationships()).thenReturn(Mono.just(relationshipToCaseLookupDetail));

        CommonLookupDetail relationshipToClientLookupDetail = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT)).thenReturn(Mono.just(relationshipToClientLookupDetail));

        mockMvc.perform(get("/application/opponents/organisation/{id}/select", selectedOrgId)
                .sessionAttr(ORGANISATION_SEARCH_RESULTS, resultsDisplay)
                .sessionAttr(USER_DETAILS, user))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(model().attribute(CURRENT_OPPONENT, opponentFormData))
            .andExpect(model().attribute("relationshipsToCase", relationshipToCaseLookupDetail.getContent()))
            .andExpect(model().attribute("relationshipsToClient", relationshipToClientLookupDetail.getContent()))
            .andExpect(view().name("application/opponents/opponents-organisation-shared-create"));
    }

    @Test
    void selectSharedOrganisationGet_rejectsInvalidOrgId() throws Exception {
        String selectedOrgId = "123";

        ResultsDisplay<OrganisationResultRowDisplay> resultsDisplay = new ResultsDisplay<>();
        OrganisationResultRowDisplay organisationResultRowDisplay = new OrganisationResultRowDisplay();
        organisationResultRowDisplay.setPartyId("different");
        resultsDisplay.setContent(List.of(organisationResultRowDisplay));

        mockMvc.perform(get("/application/opponents/organisation/{id}/select", selectedOrgId)
                .sessionAttr(ORGANISATION_SEARCH_RESULTS, resultsDisplay)
                .sessionAttr(USER_DETAILS, user))
            .andDo(print())
            .andExpect(result -> assertInstanceOf(CaabApplicationException.class,
                result.getResolvedException()));
    }

    @Test
    void selectSharedOrganisationPost_noValidationErrors_addsOpponent() throws Exception {
        String applicationId = "123";
        OrganisationOpponentFormData opponentFormData = new OrganisationOpponentFormData();

        mockMvc.perform(post("/application/opponents/organisation/shared/create")
                .sessionAttr(CURRENT_OPPONENT, opponentFormData)
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(USER_DETAILS, user))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/summary/opponents"));

        verify(applicationService).addOpponent(applicationId, opponentFormData, user);
    }

    @Test
    void selectSharedOrganisationPost_validationErrors_returnsToConfirmScreen() throws Exception {
        String applicationId = "123";
        OrganisationOpponentFormData opponentFormData = new OrganisationOpponentFormData();

        RelationshipToCaseLookupDetail relationshipToCaseLookupDetail =
            new RelationshipToCaseLookupDetail().addContentItem(new RelationshipToCaseLookupValueDetail());
        when(lookupService.getOrganisationToCaseRelationships()).thenReturn(Mono.just(relationshipToCaseLookupDetail));

        CommonLookupDetail relationshipToClientLookupDetail = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT)).thenReturn(Mono.just(relationshipToClientLookupDetail));

        doAnswer(invocation -> {
            Errors errors = (Errors) invocation.getArguments()[1];
            errors.rejectValue("relationshipToCase", "required.relationshipToCase", "Please complete 'Relationship to case'.");
            return null;
        }).when(organisationOpponentValidator).validate(any(), any());

        mockMvc.perform(post("/application/opponents/organisation/shared/create")
                .sessionAttr(CURRENT_OPPONENT, opponentFormData)
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(USER_DETAILS, user))
            .andDo(print())
            .andExpect(model().attribute("relationshipsToCase", relationshipToCaseLookupDetail.getContent()))
            .andExpect(model().attribute("relationshipsToClient", relationshipToClientLookupDetail.getContent()))
            .andExpect(view().name("application/opponents/opponents-organisation-shared-create"));

        verifyNoInteractions(applicationService);
    }

    @Test
    void organisationCreateGet_displaysCorrectView() throws Exception {
        CommonLookupDetail orgTypes = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES)).thenReturn(Mono.just(orgTypes));

        RelationshipToCaseLookupDetail relationshipToCaseLookupDetail =
            new RelationshipToCaseLookupDetail().addContentItem(new RelationshipToCaseLookupValueDetail());
        when(lookupService.getOrganisationToCaseRelationships()).thenReturn(Mono.just(relationshipToCaseLookupDetail));

        CommonLookupDetail relationshipToClientLookupDetail = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT)).thenReturn(Mono.just(relationshipToClientLookupDetail));

        CommonLookupDetail countriesLookupDetail = new CommonLookupDetail();
        when(lookupService.getCountries()).thenReturn(Mono.just(countriesLookupDetail));

        mockMvc.perform(get("/application/opponents/organisation/create"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(model().attribute("organisationTypes", orgTypes.getContent()))
            .andExpect(model().attribute("relationshipsToCase", relationshipToCaseLookupDetail.getContent()))
            .andExpect(model().attribute("relationshipsToClient", relationshipToClientLookupDetail.getContent()))
            .andExpect(model().attribute("countries", countriesLookupDetail.getContent()))
            .andExpect(view().name("application/opponents/opponents-organisation-create"));

    }

    @Test
    void organisationCreatePost_noValidationErrors_createsOpponent() throws Exception {
        final String applicationId = "123";
        final OrganisationOpponentFormData opponentFormData = new OrganisationOpponentFormData();

        mockMvc.perform(post("/application/opponents/organisation/create")
                .sessionAttr(CURRENT_OPPONENT, opponentFormData)
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(USER_DETAILS, user))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/summary/opponents"));

        verify(applicationService).addOpponent(applicationId, opponentFormData, user);
    }

    @Test
    void organisationCreatePost_validationErrors_returnsToCreateScreen() throws Exception {
        final String applicationId = "123";
        final OrganisationOpponentFormData opponentFormData = new OrganisationOpponentFormData();

        doAnswer(invocation -> {
            Errors errors = (Errors) invocation.getArguments()[1];
            errors.rejectValue("relationshipToCase", "required.relationshipToCase", "Please complete 'Relationship to case'.");
            return null;
        }).when(organisationOpponentValidator).validate(eq(opponentFormData), any());

        CommonLookupDetail orgTypes = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES)).thenReturn(Mono.just(orgTypes));

        RelationshipToCaseLookupDetail relationshipToCaseLookupDetail =
            new RelationshipToCaseLookupDetail().addContentItem(new RelationshipToCaseLookupValueDetail());
        when(lookupService.getOrganisationToCaseRelationships()).thenReturn(Mono.just(relationshipToCaseLookupDetail));

        CommonLookupDetail relationshipToClientLookupDetail = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT)).thenReturn(Mono.just(relationshipToClientLookupDetail));

        CommonLookupDetail countriesLookupDetail = new CommonLookupDetail();
        when(lookupService.getCountries()).thenReturn(Mono.just(countriesLookupDetail));

        mockMvc.perform(post("/application/opponents/organisation/create")
                .sessionAttr(CURRENT_OPPONENT, opponentFormData)
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(USER_DETAILS, user))
            .andExpect(model().attribute("organisationTypes", orgTypes.getContent()))
            .andExpect(model().attribute("relationshipsToCase", relationshipToCaseLookupDetail.getContent()))
            .andExpect(model().attribute("relationshipsToClient", relationshipToClientLookupDetail.getContent()))
            .andExpect(model().attribute("countries", countriesLookupDetail.getContent()))
            .andExpect(status().isOk())
            .andExpect(view().name("application/opponents/opponents-organisation-create"));

        verifyNoInteractions(applicationService);
    }

    @Test
    void individualCreateGet_displaysCorrectView() throws Exception {
        CommonLookupDetail contactTitles = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_CONTACT_TITLE)).thenReturn(Mono.just(contactTitles));

        RelationshipToCaseLookupDetail relationshipToCaseLookupDetail =
            new RelationshipToCaseLookupDetail().addContentItem(new RelationshipToCaseLookupValueDetail());
        when(lookupService.getPersonToCaseRelationships()).thenReturn(Mono.just(relationshipToCaseLookupDetail));

        CommonLookupDetail relationshipToClientLookupDetail = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT)).thenReturn(Mono.just(relationshipToClientLookupDetail));

        CommonLookupDetail countries = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCountries()).thenReturn(Mono.just(countries));


        mockMvc.perform(get("/application/opponents/individual/create"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(model().attribute("contactTitles", contactTitles.getContent()))
            .andExpect(model().attribute("relationshipsToCase", relationshipToCaseLookupDetail.getContent()))
            .andExpect(model().attribute("relationshipsToClient", relationshipToClientLookupDetail.getContent()))
            .andExpect(model().attribute("countries", countries.getContent()))
            .andExpect(model().attributeExists("legalAidedOptions"))
            .andExpect(view().name("application/opponents/opponents-individual-create"));
    }

    @Test
    void individualCreatePost_validationErrors_returnsToView() throws Exception {
        CommonLookupDetail contactTitles = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_CONTACT_TITLE)).thenReturn(Mono.just(contactTitles));

        RelationshipToCaseLookupDetail relationshipToCaseLookupDetail =
            new RelationshipToCaseLookupDetail().addContentItem(new RelationshipToCaseLookupValueDetail());
        when(lookupService.getPersonToCaseRelationships()).thenReturn(Mono.just(relationshipToCaseLookupDetail));

        CommonLookupDetail relationshipToClientLookupDetail = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT)).thenReturn(Mono.just(relationshipToClientLookupDetail));

        CommonLookupDetail countries = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCountries()).thenReturn(Mono.just(countries));

        doAnswer(invocation -> {
            Errors errors = (Errors) invocation.getArguments()[1];
            errors.rejectValue("firstName", "required.firstName", "Please complete 'First Name'.");
            return null;
        }).when(individualOpponentValidator).validate(any(), any());

        mockMvc.perform(post("/application/opponents/individual/create")
            .sessionAttr(CURRENT_OPPONENT, new IndividualOpponentFormData())
            .sessionAttr(APPLICATION_ID, "123")
            .sessionAttr(USER_DETAILS, user))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(model().attribute("contactTitles", contactTitles.getContent()))
            .andExpect(model().attribute("relationshipsToCase", relationshipToCaseLookupDetail.getContent()))
            .andExpect(model().attribute("relationshipsToClient", relationshipToClientLookupDetail.getContent()))
            .andExpect(model().attribute("countries", countries.getContent()))
            .andExpect(model().attributeExists("legalAidedOptions"))
            .andExpect(view().name("application/opponents/opponents-individual-create"));

    }

    @Test
    void individualCreatePost_noValidationErrors_createsOpponent() throws Exception {
        final String applicationId = "123";
        final IndividualOpponentFormData opponentFormData = new IndividualOpponentFormData();

        mockMvc.perform(post("/application/opponents/individual/create")
                .sessionAttr(CURRENT_OPPONENT, opponentFormData)
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(USER_DETAILS, user))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/summary/opponents"));

        verify(applicationService).addOpponent(applicationId, opponentFormData, user);

    }

    @Test
    void editSharedOrganisationOpponentGet_displaysCorrectView() throws Exception {
        IndividualOpponentFormData individualOpponent = new IndividualOpponentFormData();
        individualOpponent.setId(1);

        OrganisationOpponentFormData organisationOpponent = new OrganisationOpponentFormData();
        organisationOpponent.setId(2);
        organisationOpponent.setShared(true);

        List<AbstractOpponentFormData> applicationOpponents = List.of(
            individualOpponent,
            organisationOpponent);

        String selectedOpponentId = "2";

        RelationshipToCaseLookupDetail relationshipToCaseLookupDetail =
            new RelationshipToCaseLookupDetail().addContentItem(new RelationshipToCaseLookupValueDetail());
        when(lookupService.getOrganisationToCaseRelationships()).thenReturn(Mono.just(relationshipToCaseLookupDetail));

        CommonLookupDetail relationshipToClientLookupDetail = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT)).thenReturn(Mono.just(relationshipToClientLookupDetail));

        mockMvc.perform(get("/application/opponents/{id}/edit", selectedOpponentId)
                .sessionAttr(APPLICATION_OPPONENTS, applicationOpponents)
                .sessionAttr(USER_DETAILS, user))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(model().attribute(CURRENT_OPPONENT, organisationOpponent))
            .andExpect(model().attribute("relationshipsToCase", relationshipToCaseLookupDetail.getContent()))
            .andExpect(model().attribute("relationshipsToClient", relationshipToClientLookupDetail.getContent()))
            .andExpect(view().name("application/opponents/opponents-organisation-shared-edit"));
    }

    @Test
    void editOpponentGet_rejectsInvalidOpponentId() throws Exception {
        IndividualOpponentFormData individualOpponent = new IndividualOpponentFormData();
        individualOpponent.setId(1);

        OrganisationOpponentFormData organisationOpponent = new OrganisationOpponentFormData();
        organisationOpponent.setId(2);
        organisationOpponent.setShared(true);

        List<AbstractOpponentFormData> applicationOpponents = List.of(
            individualOpponent,
            organisationOpponent);

        String selectedOpponentId = "3";

        mockMvc.perform(get("/application/opponents/{id}/edit", selectedOpponentId)
                .sessionAttr(APPLICATION_OPPONENTS, applicationOpponents)
                .sessionAttr(USER_DETAILS, user))
            .andDo(print())
            .andExpect(result -> assertInstanceOf(CaabApplicationException.class,
                result.getResolvedException()));
    }

    @Test
    void editNonSharedOrganisationOpponentGet_displaysCorrectView() throws Exception {
        IndividualOpponentFormData individualOpponent = new IndividualOpponentFormData();
        individualOpponent.setId(1);

        OrganisationOpponentFormData organisationOpponent = new OrganisationOpponentFormData();
        organisationOpponent.setId(2);
        organisationOpponent.setShared(false);

        List<AbstractOpponentFormData> applicationOpponents = List.of(
            individualOpponent,
            organisationOpponent);

        String selectedOpponentId = "2";

        CommonLookupDetail orgTypes = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES)).thenReturn(Mono.just(orgTypes));

        RelationshipToCaseLookupDetail relationshipToCaseLookupDetail =
            new RelationshipToCaseLookupDetail().addContentItem(new RelationshipToCaseLookupValueDetail());
        when(lookupService.getOrganisationToCaseRelationships()).thenReturn(Mono.just(relationshipToCaseLookupDetail));

        CommonLookupDetail relationshipToClientLookupDetail = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT)).thenReturn(Mono.just(relationshipToClientLookupDetail));

        CommonLookupDetail countriesLookupDetail = new CommonLookupDetail();
        when(lookupService.getCountries()).thenReturn(Mono.just(countriesLookupDetail));

        mockMvc.perform(get("/application/opponents/{id}/edit", selectedOpponentId)
                .sessionAttr(APPLICATION_OPPONENTS, applicationOpponents))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(model().attribute(CURRENT_OPPONENT, organisationOpponent))
            .andExpect(model().attribute("organisationTypes", orgTypes.getContent()))
            .andExpect(model().attribute("relationshipsToCase", relationshipToCaseLookupDetail.getContent()))
            .andExpect(model().attribute("relationshipsToClient", relationshipToClientLookupDetail.getContent()))
            .andExpect(model().attribute("countries", countriesLookupDetail.getContent()))
            .andExpect(view().name("application/opponents/opponents-organisation-edit"));
    }

    @Test
    void editIndividualOpponentGet_displaysCorrectView() throws Exception {
        IndividualOpponentFormData individualOpponent = new IndividualOpponentFormData();
        individualOpponent.setId(1);

        OrganisationOpponentFormData organisationOpponent = new OrganisationOpponentFormData();
        organisationOpponent.setId(2);
        organisationOpponent.setShared(false);

        List<AbstractOpponentFormData> applicationOpponents = List.of(
            individualOpponent,
            organisationOpponent);

        String selectedOpponentId = "1";

        CommonLookupDetail contactTitles = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_CONTACT_TITLE)).thenReturn(Mono.just(contactTitles));

        RelationshipToCaseLookupDetail relationshipToCaseLookupDetail =
            new RelationshipToCaseLookupDetail().addContentItem(new RelationshipToCaseLookupValueDetail());
        when(lookupService.getPersonToCaseRelationships()).thenReturn(Mono.just(relationshipToCaseLookupDetail));

        CommonLookupDetail relationshipToClientLookupDetail = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT)).thenReturn(Mono.just(relationshipToClientLookupDetail));

        CommonLookupDetail countries = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCountries()).thenReturn(Mono.just(countries));

        mockMvc.perform(get("/application/opponents/{id}/edit", selectedOpponentId)
                .sessionAttr(APPLICATION_OPPONENTS, applicationOpponents))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(model().attribute(CURRENT_OPPONENT, individualOpponent))
            .andExpect(model().attribute("contactTitles", contactTitles.getContent()))
            .andExpect(model().attribute("relationshipsToCase", relationshipToCaseLookupDetail.getContent()))
            .andExpect(model().attribute("relationshipsToClient", relationshipToClientLookupDetail.getContent()))
            .andExpect(model().attribute("countries", countries.getContent()))
            .andExpect(model().attributeExists("legalAidedOptions"))
            .andExpect(view().name("application/opponents/opponents-individual-edit"));
    }

    @Test
    void editOpponentPost_noValidationErrors_editsOpponent() throws Exception {
        String selectedOpponentId = "123";
        OrganisationOpponentFormData opponentFormData = new OrganisationOpponentFormData();

        mockMvc.perform(post("/application/opponents/{id}/edit", selectedOpponentId)
                .sessionAttr(CURRENT_OPPONENT, opponentFormData)
                .sessionAttr(USER_DETAILS, user))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/summary/opponents"));

        verify(opponentService).updateOpponent(Integer.valueOf(selectedOpponentId),
            opponentFormData, user);
    }

    @Test
    void editSharedOrganisationPost_validationErrors_returnsToSharedEditScreen() throws Exception {
        String selectedOpponentId = "123";
        OrganisationOpponentFormData opponentFormData = new OrganisationOpponentFormData();
        opponentFormData.setShared(true);

        RelationshipToCaseLookupDetail relationshipToCaseLookupDetail =
            new RelationshipToCaseLookupDetail().addContentItem(new RelationshipToCaseLookupValueDetail());
        when(lookupService.getOrganisationToCaseRelationships()).thenReturn(Mono.just(relationshipToCaseLookupDetail));

        CommonLookupDetail relationshipToClientLookupDetail = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT)).thenReturn(Mono.just(relationshipToClientLookupDetail));

        doAnswer(invocation -> {
            Errors errors = (Errors) invocation.getArguments()[1];
            errors.rejectValue("relationshipToCase", "required.relationshipToCase", "Please complete 'Relationship to case'.");
            return null;
        }).when(organisationOpponentValidator).validate(any(), any());

        mockMvc.perform(post("/application/opponents/{id}/edit", selectedOpponentId)
                .sessionAttr(CURRENT_OPPONENT, opponentFormData)
                .sessionAttr(USER_DETAILS, user))
            .andDo(print())
            .andExpect(model().attribute("relationshipsToCase", relationshipToCaseLookupDetail.getContent()))
            .andExpect(model().attribute("relationshipsToClient", relationshipToClientLookupDetail.getContent()))
            .andExpect(view().name("application/opponents/opponents-organisation-shared-edit"));

        verifyNoInteractions(opponentService);
    }

    @Test
    void editNonSharedOrganisationPost_validationErrors_returnsToNonSharedEditScreen() throws Exception {
        String selectedOpponentId = "123";
        OrganisationOpponentFormData opponentFormData = new OrganisationOpponentFormData();
        opponentFormData.setShared(false);

        doAnswer(invocation -> {
            Errors errors = (Errors) invocation.getArguments()[1];
            errors.rejectValue("relationshipToCase", "required.relationshipToCase", "Please complete 'Relationship to case'.");
            return null;
        }).when(organisationOpponentValidator).validate(eq(opponentFormData), any());

        CommonLookupDetail orgTypes = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES)).thenReturn(Mono.just(orgTypes));

        RelationshipToCaseLookupDetail relationshipToCaseLookupDetail =
            new RelationshipToCaseLookupDetail().addContentItem(new RelationshipToCaseLookupValueDetail());
        when(lookupService.getOrganisationToCaseRelationships()).thenReturn(Mono.just(relationshipToCaseLookupDetail));

        CommonLookupDetail relationshipToClientLookupDetail = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT)).thenReturn(Mono.just(relationshipToClientLookupDetail));

        CommonLookupDetail countriesLookupDetail = new CommonLookupDetail();
        when(lookupService.getCountries()).thenReturn(Mono.just(countriesLookupDetail));

        mockMvc.perform(post("/application/opponents/{id}/edit", selectedOpponentId)
                .sessionAttr(CURRENT_OPPONENT, opponentFormData)
                .sessionAttr(USER_DETAILS, user))
            .andDo(print())
            .andExpect(model().attribute("organisationTypes", orgTypes.getContent()))
            .andExpect(model().attribute("relationshipsToCase", relationshipToCaseLookupDetail.getContent()))
            .andExpect(model().attribute("relationshipsToClient", relationshipToClientLookupDetail.getContent()))
            .andExpect(model().attribute("countries", countriesLookupDetail.getContent()))
            .andExpect(view().name("application/opponents/opponents-organisation-edit"));

        verifyNoInteractions(opponentService);
    }

    @Test
    void editIndividualPost_validationErrors_returnsToIndividualEditScreen() throws Exception {
        String selectedOpponentId = "123";
        IndividualOpponentFormData opponentFormData = new IndividualOpponentFormData();

        CommonLookupDetail contactTitles = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_CONTACT_TITLE)).thenReturn(Mono.just(contactTitles));

        RelationshipToCaseLookupDetail relationshipToCaseLookupDetail =
            new RelationshipToCaseLookupDetail().addContentItem(new RelationshipToCaseLookupValueDetail());
        when(lookupService.getPersonToCaseRelationships()).thenReturn(Mono.just(relationshipToCaseLookupDetail));

        CommonLookupDetail relationshipToClientLookupDetail = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT)).thenReturn(Mono.just(relationshipToClientLookupDetail));

        CommonLookupDetail countries = new CommonLookupDetail()
            .addContentItem(new CommonLookupValueDetail());
        when(lookupService.getCountries()).thenReturn(Mono.just(countries));

        doAnswer(invocation -> {
            Errors errors = (Errors) invocation.getArguments()[1];
            errors.rejectValue("firstName", "required.firstName", "Please complete 'First Name'.");
            return null;
        }).when(individualOpponentValidator).validate(any(), any());

        mockMvc.perform(post("/application/opponents/{id}/edit", selectedOpponentId)
                .sessionAttr(CURRENT_OPPONENT, opponentFormData)
                .sessionAttr(USER_DETAILS, user))
            .andDo(print())
            .andExpect(model().attribute("contactTitles", contactTitles.getContent()))
            .andExpect(model().attribute("relationshipsToCase", relationshipToCaseLookupDetail.getContent()))
            .andExpect(model().attribute("relationshipsToClient", relationshipToClientLookupDetail.getContent()))
            .andExpect(model().attribute("countries", countries.getContent()))
            .andExpect(model().attributeExists("legalAidedOptions"))
            .andExpect(view().name("application/opponents/opponents-individual-edit"));

        verifyNoInteractions(opponentService);
    }

    @Test
    void removeOpponentGet_displaysCorrectView() throws Exception {
        IndividualOpponentFormData individualOpponent = new IndividualOpponentFormData();
        individualOpponent.setId(1);

        OrganisationOpponentFormData organisationOpponent = new OrganisationOpponentFormData();
        organisationOpponent.setId(2);
        organisationOpponent.setDeletable(true);

        List<AbstractOpponentFormData> applicationOpponents = List.of(
            individualOpponent,
            organisationOpponent);

        Integer selectedOpponentId = 2;

        mockMvc.perform(get("/application/opponents/{id}/remove", selectedOpponentId)
                .sessionAttr(APPLICATION_OPPONENTS, applicationOpponents)
                .sessionAttr(USER_DETAILS, user))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(model().attribute(CURRENT_OPPONENT, organisationOpponent))
            .andExpect(view().name("application/opponents/opponents-remove"));
    }

    @Test
    void removeOpponentGet_rejectsInvalidOpponentId() throws Exception {
        IndividualOpponentFormData individualOpponent = new IndividualOpponentFormData();
        individualOpponent.setId(1);

        OrganisationOpponentFormData organisationOpponent = new OrganisationOpponentFormData();
        organisationOpponent.setId(2);
        organisationOpponent.setDeletable(true);

        List<AbstractOpponentFormData> applicationOpponents = List.of(
            individualOpponent,
            organisationOpponent);

        String selectedOpponentId = "3";

        mockMvc.perform(get("/application/opponents/{id}/remove", selectedOpponentId)
                .sessionAttr(APPLICATION_OPPONENTS, applicationOpponents)
                .sessionAttr(USER_DETAILS, user))
            .andDo(print())
            .andExpect(result -> assertInstanceOf(CaabApplicationException.class,
                result.getResolvedException()));
    }

    @Test
    void removeOpponentGet_rejectsNonDeletableOpponentId() throws Exception {
        IndividualOpponentFormData individualOpponent = new IndividualOpponentFormData();
        individualOpponent.setId(1);

        OrganisationOpponentFormData organisationOpponent = new OrganisationOpponentFormData();
        organisationOpponent.setId(2);
        organisationOpponent.setDeletable(false);

        List<AbstractOpponentFormData> applicationOpponents = List.of(
            individualOpponent,
            organisationOpponent);

        String selectedOpponentId = "2";

        mockMvc.perform(get("/application/opponents/{id}/remove", selectedOpponentId)
                .sessionAttr(APPLICATION_OPPONENTS, applicationOpponents)
                .sessionAttr(USER_DETAILS, user))
            .andDo(print())
            .andExpect(result -> assertInstanceOf(CaabApplicationException.class,
                result.getResolvedException()));
    }

    @Test
    void removeOpponentPost_removesOpponent() throws Exception {
        IndividualOpponentFormData individualOpponent = new IndividualOpponentFormData();
        individualOpponent.setId(1);

        OrganisationOpponentFormData organisationOpponent = new OrganisationOpponentFormData();
        organisationOpponent.setId(2);
        organisationOpponent.setDeletable(true);

        List<AbstractOpponentFormData> applicationOpponents = List.of(
            individualOpponent,
            organisationOpponent);

        Integer selectedOpponentId = 2;

        mockMvc.perform(post("/application/opponents/{id}/remove", selectedOpponentId)
                .sessionAttr(APPLICATION_OPPONENTS, applicationOpponents)
                .sessionAttr(USER_DETAILS, user))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/application/summary/opponents"));

        verify(opponentService).deleteOpponent(selectedOpponentId, user);
    }

    @Test
    void removeOpponentPost_rejectsInvalidOpponentId() throws Exception {
        IndividualOpponentFormData individualOpponent = new IndividualOpponentFormData();
        individualOpponent.setId(1);

        OrganisationOpponentFormData organisationOpponent = new OrganisationOpponentFormData();
        organisationOpponent.setId(2);
        organisationOpponent.setDeletable(true);

        List<AbstractOpponentFormData> applicationOpponents = List.of(
            individualOpponent,
            organisationOpponent);

        String selectedOpponentId = "3";

        mockMvc.perform(get("/application/opponents/{id}/remove", selectedOpponentId)
                .sessionAttr(APPLICATION_OPPONENTS, applicationOpponents)
                .sessionAttr(USER_DETAILS, user))
            .andDo(print())
            .andExpect(result -> assertInstanceOf(CaabApplicationException.class,
                result.getResolvedException()));
        verifyNoInteractions(opponentService);
    }

    @Test
    void removeOpponentPost_rejectsNonDeletableOpponentId() throws Exception {
        IndividualOpponentFormData individualOpponent = new IndividualOpponentFormData();
        individualOpponent.setId(1);

        OrganisationOpponentFormData organisationOpponent = new OrganisationOpponentFormData();
        organisationOpponent.setId(2);
        organisationOpponent.setDeletable(false);

        List<AbstractOpponentFormData> applicationOpponents = List.of(
            individualOpponent,
            organisationOpponent);

        String selectedOpponentId = "2";

        mockMvc.perform(get("/application/opponents/{id}/remove", selectedOpponentId)
                .sessionAttr(APPLICATION_OPPONENTS, applicationOpponents)
                .sessionAttr(USER_DETAILS, user))
            .andDo(print())
            .andExpect(result -> assertInstanceOf(CaabApplicationException.class,
                result.getResolvedException()));
        verifyNoInteractions(opponentService);
    }

}