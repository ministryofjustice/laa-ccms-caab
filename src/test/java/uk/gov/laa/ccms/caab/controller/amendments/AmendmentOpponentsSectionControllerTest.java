package uk.gov.laa.ccms.caab.controller.amendments;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_ORGANISATION_TYPES;
import static uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_RELATIONSHIP_TO_CLIENT;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.APPLICATION_ID;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Mono;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.bean.opponent.IndividualOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationOpponentFormData;
import uk.gov.laa.ccms.caab.bean.opponent.OrganisationSearchCriteria;
import uk.gov.laa.ccms.caab.bean.validators.opponent.IndividualOpponentValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.OrganisationOpponentValidator;
import uk.gov.laa.ccms.caab.bean.validators.opponent.OrganisationSearchCriteriaValidator;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.OrganisationResultRowDisplay;
import uk.gov.laa.ccms.caab.model.ResultsDisplay;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.OpponentSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.OpponentsSectionDisplay;
import uk.gov.laa.ccms.caab.service.AmendmentService;
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
class AmendmentOpponentsSectionControllerTest {

  @Mock private ApplicationService applicationService;
  @Mock private AmendmentService amendmentService;
  @Mock private LookupService lookupService;
  @Mock private OpponentService opponentService;
  @Mock private IndividualOpponentValidator individualOpponentValidator;
  @Mock private OrganisationOpponentValidator organisationOpponentValidator;
  @Mock private OrganisationSearchCriteriaValidator organisationSearchCriteriaValidator;

  @InjectMocks private AmendmentOpponentsSectionController controller;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    // Default behavior for validators: do nothing (pass validation)
    lenient().doAnswer(invocation -> null).when(individualOpponentValidator).validate(any(), any());
    lenient()
        .doAnswer(invocation -> null)
        .when(organisationOpponentValidator)
        .validate(any(), any());
    lenient()
        .doAnswer(invocation -> null)
        .when(organisationSearchCriteriaValidator)
        .validate(any(), any());

    // Default behavior for supports: return true
    lenient().when(individualOpponentValidator.supports(any())).thenReturn(true);
    lenient().when(organisationOpponentValidator.supports(any())).thenReturn(true);
    lenient().when(organisationSearchCriteriaValidator.supports(any())).thenReturn(true);
  }

  @Test
  void testBinderInitialisation() throws Exception {
    mockMvc
        .perform(
            get("/amendments/sections/opponents/organisation/search")
                .sessionAttr(ORGANISATION_SEARCH_CRITERIA, new OrganisationSearchCriteria()))
        .andExpect(status().isOk());
  }

  @Test
  void testOrganisationSearchBinderInitialisation() throws Exception {
    mockMvc
        .perform(
            post("/amendments/sections/opponents/organisation/search")
                .flashAttr(ORGANISATION_SEARCH_CRITERIA, new OrganisationSearchCriteria()))
        .andExpect(status().is3xxRedirection());
  }

  @Test
  void testUserDetailBinderInitialisation() throws Exception {
    mockMvc
        .perform(
            get("/amendments/sections/opponents/organisation/search/results")
                .sessionAttr(USER_DETAILS, user))
        .andExpect(status().isOk());
  }

  private static final UserDetail user =
      new UserDetail().userId(1).userType("testUserType").loginId("testLoginId");

  @Test
  void opponents() throws Exception {
    String applicationId = "123";
    ApplicationDetail application = new ApplicationDetail();
    when(applicationService.getApplication(applicationId)).thenReturn(Mono.just(application));

    ApplicationSectionDisplay amendmentSections = ApplicationSectionDisplay.builder().build();
    amendmentSections.setOpponentsAndOtherParties(new OpponentsSectionDisplay());
    amendmentSections.getOpponentsAndOtherParties().setOpponents(new ArrayList<>());

    OpponentSectionDisplay opponent1 = OpponentSectionDisplay.builder().ebsId("EBS123").build();
    OpponentSectionDisplay opponent2 = OpponentSectionDisplay.builder().ebsId(null).build();

    amendmentSections
        .getOpponentsAndOtherParties()
        .getOpponents()
        .addAll(List.of(opponent1, opponent2));

    when(amendmentService.getAmendmentSections(application, user)).thenReturn(amendmentSections);

    mockMvc
        .perform(
            get("/amendments/sections/opponents")
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(USER_DETAILS, user))
        .andExpect(status().isOk())
        .andExpect(view().name("amendments/sections/opponents-section"))
        .andExpect(model().attributeExists("opponents"));
  }

  @Test
  void organisationSearch() throws Exception {
    CommonLookupDetail orgTypes =
        new CommonLookupDetail().addContentItem(new CommonLookupValueDetail());
    when(lookupService.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES))
        .thenReturn(Mono.just(orgTypes));

    mockMvc
        .perform(
            get("/amendments/sections/opponents/organisation/search")
                .sessionAttr(ORGANISATION_SEARCH_CRITERIA, new OrganisationSearchCriteria()))
        .andExpect(status().isOk())
        .andExpect(model().attribute("amendment", true))
        .andExpect(model().attribute("organisationTypes", orgTypes.getContent()))
        .andExpect(view().name("application/opponents/opponents-organisation-search"));
  }

  @Test
  void organisationSearchPost_noValidationErrors_redirectsToResults() throws Exception {
    OrganisationSearchCriteria searchCriteria = new OrganisationSearchCriteria();
    mockMvc
        .perform(
            post("/amendments/sections/opponents/organisation/search")
                .flashAttr(ORGANISATION_SEARCH_CRITERIA, searchCriteria))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/amendments/sections/opponents/organisation/search/results"));
  }

  @Test
  void organisationSearchPost_withValidationErrors_returnsSearchView() throws Exception {
    OrganisationSearchCriteria searchCriteria = new OrganisationSearchCriteria();

    CommonLookupDetail orgTypes =
        new CommonLookupDetail().addContentItem(new CommonLookupValueDetail());
    lenient()
        .when(lookupService.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES))
        .thenReturn(Mono.just(orgTypes));

    lenient()
        .doAnswer(
            invocation -> {
              org.springframework.validation.BindingResult errors = invocation.getArgument(1);
              errors.rejectValue("organisationName", "required", "Organisation Name is required");
              return null;
            })
        .when(organisationSearchCriteriaValidator)
        .validate(any(), any());

    mockMvc
        .perform(
            post("/amendments/sections/opponents/organisation/search")
                .flashAttr(ORGANISATION_SEARCH_CRITERIA, searchCriteria))
        .andExpect(status().isOk());
  }

  @Test
  void organisationSearchResultsGet_displaysResultsView() throws Exception {
    OrganisationSearchCriteria searchCriteria = new OrganisationSearchCriteria();
    ResultsDisplay<OrganisationResultRowDisplay> resultsDisplay = new ResultsDisplay<>();
    resultsDisplay.setContent(List.of(new OrganisationResultRowDisplay()));

    when(opponentService.getOrganisations(eq(searchCriteria), any(), any(), eq(0), eq(10)))
        .thenReturn(resultsDisplay);

    mockMvc
        .perform(
            get("/amendments/sections/opponents/organisation/search/results")
                .sessionAttr(ORGANISATION_SEARCH_CRITERIA, searchCriteria)
                .sessionAttr(USER_DETAILS, user))
        .andExpect(status().isOk())
        .andExpect(model().attribute("amendment", true))
        .andExpect(model().attribute(ORGANISATION_SEARCH_RESULTS, resultsDisplay))
        .andExpect(view().name("application/opponents/opponents-organisation-search-results"));
  }

  @Test
  void organisationSearchResultsGet_noResults_displaysNoResultsView() throws Exception {
    OrganisationSearchCriteria searchCriteria = new OrganisationSearchCriteria();
    ResultsDisplay<OrganisationResultRowDisplay> resultsDisplay = new ResultsDisplay<>();
    resultsDisplay.setContent(new ArrayList<>());

    when(opponentService.getOrganisations(eq(searchCriteria), any(), any(), eq(0), eq(10)))
        .thenReturn(resultsDisplay);

    mockMvc
        .perform(
            get("/amendments/sections/opponents/organisation/search/results")
                .sessionAttr(ORGANISATION_SEARCH_CRITERIA, searchCriteria)
                .sessionAttr(USER_DETAILS, user))
        .andExpect(status().isOk())
        .andExpect(model().attribute("amendment", true))
        .andExpect(view().name("application/opponents/opponents-organisation-search-no-results"));
  }

  @Test
  void organisationSearchResultsGet_tooManyResults_displaysTooManyResultsView() throws Exception {
    OrganisationSearchCriteria searchCriteria = new OrganisationSearchCriteria();

    when(opponentService.getOrganisations(eq(searchCriteria), any(), any(), eq(0), eq(10)))
        .thenThrow(new uk.gov.laa.ccms.caab.exception.TooManyResultsException("Too many"));

    mockMvc
        .perform(
            get("/amendments/sections/opponents/organisation/search/results")
                .sessionAttr(ORGANISATION_SEARCH_CRITERIA, searchCriteria)
                .sessionAttr(USER_DETAILS, user))
        .andExpect(status().isOk())
        .andExpect(model().attribute("amendment", true))
        .andExpect(
            view().name("application/opponents/opponents-organisation-search-too-many-results"));
  }

  @Test
  void selectSharedOrganisationGet_invalidOrgId_throwsException() throws Exception {
    String selectedOrgId = "123";
    ResultsDisplay<OrganisationResultRowDisplay> resultsDisplay = new ResultsDisplay<>();
    resultsDisplay.setContent(new ArrayList<>()); // No matching ID

    mockMvc
        .perform(
            get("/amendments/sections/opponents/organisation/{id}/select", selectedOrgId)
                .sessionAttr(ORGANISATION_SEARCH_RESULTS, resultsDisplay)
                .sessionAttr(USER_DETAILS, user))
        .andExpect(
            result -> {
              Exception resolvedException = result.getResolvedException();
              org.junit.jupiter.api.Assertions.assertNotNull(resolvedException);
              org.junit.jupiter.api.Assertions.assertTrue(
                  resolvedException
                      instanceof uk.gov.laa.ccms.caab.exception.CaabApplicationException);
              org.junit.jupiter.api.Assertions.assertEquals(
                  "Invalid organisation partyId supplied", resolvedException.getMessage());
            });
  }

  @Test
  void createSharedOrganisationPost_addsOpponent() throws Exception {
    String applicationId = "123";
    OrganisationOpponentFormData formData = new OrganisationOpponentFormData();
    formData.setOrganisationName("Test Org");
    formData.setOrganisationType("TYPE1");
    formData.setRelationshipToCase("REL1");
    formData.setRelationshipToClient("REL2");
    formData.setShared(true);

    mockMvc
        .perform(
            post("/amendments/sections/opponents/organisation/shared/create")
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(USER_DETAILS, user)
                .flashAttr(CURRENT_OPPONENT, formData))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/amendments/sections/opponents"));

    verify(applicationService)
        .addOpponent(eq(applicationId), any(OrganisationOpponentFormData.class), eq(user));
  }

  @Test
  void createSharedOrganisationPost_withValidationErrors_returnsSharedCreateView()
      throws Exception {
    String applicationId = "123";
    OrganisationOpponentFormData formData = new OrganisationOpponentFormData();
    formData.setShared(true);

    RelationshipToCaseLookupDetail rels =
        new RelationshipToCaseLookupDetail()
            .addContentItem(new RelationshipToCaseLookupValueDetail());
    when(lookupService.getOrganisationToCaseRelationships()).thenReturn(Mono.just(rels));

    CommonLookupDetail relsClient =
        new CommonLookupDetail().addContentItem(new CommonLookupValueDetail());
    when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT))
        .thenReturn(Mono.just(relsClient));

    lenient()
        .doAnswer(
            invocation -> {
              org.springframework.validation.Errors errors = invocation.getArgument(1);
              errors.rejectValue("relationshipToCase", "required", "Required");
              return null;
            })
        .when(organisationOpponentValidator)
        .validate(any(), any());

    mockMvc
        .perform(
            post("/amendments/sections/opponents/organisation/shared/create")
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(USER_DETAILS, user)
                .flashAttr(CURRENT_OPPONENT, formData))
        .andExpect(status().isOk())
        .andExpect(model().attribute("amendment", true))
        .andExpect(view().name("application/opponents/opponents-organisation-shared-create"));
  }

  @Test
  void createNewOrganisationGet_displaysCorrectView() throws Exception {
    RelationshipToCaseLookupDetail rels =
        new RelationshipToCaseLookupDetail()
            .addContentItem(new RelationshipToCaseLookupValueDetail());
    when(lookupService.getOrganisationToCaseRelationships()).thenReturn(Mono.just(rels));

    CommonLookupDetail orgTypes =
        new CommonLookupDetail().addContentItem(new CommonLookupValueDetail());
    when(lookupService.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES))
        .thenReturn(Mono.just(orgTypes));

    CommonLookupDetail relsClient =
        new CommonLookupDetail().addContentItem(new CommonLookupValueDetail());
    when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT))
        .thenReturn(Mono.just(relsClient));

    CommonLookupDetail countries = new CommonLookupDetail();
    when(lookupService.getCountries()).thenReturn(Mono.just(countries));

    mockMvc
        .perform(get("/amendments/sections/opponents/organisation/create"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("amendment", true))
        .andExpect(view().name("application/opponents/opponents-organisation-create"));
  }

  @Test
  void createNewOrganisationPost_addsOpponent() throws Exception {
    String applicationId = "123";
    OrganisationOpponentFormData formData = new OrganisationOpponentFormData();
    formData.setOrganisationName("Test Org");
    formData.setOrganisationType("TYPE1");
    formData.setRelationshipToCase("REL1");
    formData.setRelationshipToClient("REL2");

    mockMvc
        .perform(
            post("/amendments/sections/opponents/organisation/create")
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(USER_DETAILS, user)
                .flashAttr(CURRENT_OPPONENT, formData))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/amendments/sections/opponents"));

    verify(applicationService)
        .addOpponent(eq(applicationId), any(OrganisationOpponentFormData.class), eq(user));
  }

  @Test
  void createNewOrganisationPost_withValidationErrors_returnsCreateView() throws Exception {
    String applicationId = "123";
    OrganisationOpponentFormData formData = new OrganisationOpponentFormData();

    RelationshipToCaseLookupDetail rels =
        new RelationshipToCaseLookupDetail()
            .addContentItem(new RelationshipToCaseLookupValueDetail());
    lenient().when(lookupService.getOrganisationToCaseRelationships()).thenReturn(Mono.just(rels));

    CommonLookupDetail orgTypes =
        new CommonLookupDetail().addContentItem(new CommonLookupValueDetail());
    lenient()
        .when(lookupService.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES))
        .thenReturn(Mono.just(orgTypes));

    CommonLookupDetail relsClient =
        new CommonLookupDetail().addContentItem(new CommonLookupValueDetail());
    lenient()
        .when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT))
        .thenReturn(Mono.just(relsClient));

    CommonLookupDetail countries = new CommonLookupDetail();
    lenient().when(lookupService.getCountries()).thenReturn(Mono.just(countries));

    lenient()
        .doAnswer(
            invocation -> {
              org.springframework.validation.Errors errors = invocation.getArgument(1);
              errors.rejectValue("organisationName", "required", "Required");
              return null;
            })
        .when(organisationOpponentValidator)
        .validate(any(), any());

    mockMvc
        .perform(
            post("/amendments/sections/opponents/organisation/create")
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(USER_DETAILS, user)
                .flashAttr(CURRENT_OPPONENT, formData))
        .andExpect(status().isOk())
        .andExpect(view().name("application/opponents/opponents-organisation-create"));
  }

  @Test
  void editOrganisationGet_displaysEditView() throws Exception {
    Integer opponentId = 1;
    String applicationId = "123";
    OrganisationOpponentFormData organisationOpponent = new OrganisationOpponentFormData();
    organisationOpponent.setId(opponentId);
    organisationOpponent.setEditable(true);
    organisationOpponent.setShared(false);

    when(applicationService.getOpponents(applicationId)).thenReturn(List.of(organisationOpponent));

    RelationshipToCaseLookupDetail rels =
        new RelationshipToCaseLookupDetail()
            .addContentItem(new RelationshipToCaseLookupValueDetail());
    when(lookupService.getOrganisationToCaseRelationships()).thenReturn(Mono.just(rels));

    CommonLookupDetail orgTypes =
        new CommonLookupDetail().addContentItem(new CommonLookupValueDetail());
    when(lookupService.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES))
        .thenReturn(Mono.just(orgTypes));

    CommonLookupDetail relsClient =
        new CommonLookupDetail().addContentItem(new CommonLookupValueDetail());
    when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT))
        .thenReturn(Mono.just(relsClient));

    CommonLookupDetail countries = new CommonLookupDetail();
    when(lookupService.getCountries()).thenReturn(Mono.just(countries));

    mockMvc
        .perform(
            get("/amendments/sections/opponents/organisation/{opponentId}/edit", opponentId)
                .sessionAttr(APPLICATION_ID, applicationId))
        .andExpect(status().isOk())
        .andExpect(model().attribute("amendment", true))
        .andExpect(view().name("application/opponents/opponents-organisation-edit"));
  }

  @Test
  void editOrganisationPost_updatesOpponent() throws Exception {
    Integer opponentId = 1;
    String applicationId = "123";
    OrganisationOpponentFormData formData = new OrganisationOpponentFormData();
    formData.setId(opponentId);
    formData.setOrganisationName("Updated Org");
    formData.setOrganisationType("TYPE1");
    formData.setRelationshipToCase("REL1");
    formData.setRelationshipToClient("REL2");

    mockMvc
        .perform(
            post("/amendments/sections/opponents/organisation/{opponentId}/edit", opponentId)
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(USER_DETAILS, user)
                .flashAttr(CURRENT_OPPONENT, formData))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/amendments/sections/opponents"));

    verify(applicationService)
        .updateOpponent(
            eq(applicationId), eq(opponentId), any(OrganisationOpponentFormData.class), eq(user));
  }

  @Test
  void editOrganisationPost_withValidationErrors_returnsEditView() throws Exception {
    Integer opponentId = 1;
    String applicationId = "123";
    OrganisationOpponentFormData formData = new OrganisationOpponentFormData();
    formData.setId(opponentId);

    RelationshipToCaseLookupDetail rels =
        new RelationshipToCaseLookupDetail()
            .addContentItem(new RelationshipToCaseLookupValueDetail());
    lenient().when(lookupService.getOrganisationToCaseRelationships()).thenReturn(Mono.just(rels));

    CommonLookupDetail orgTypes =
        new CommonLookupDetail().addContentItem(new CommonLookupValueDetail());
    lenient()
        .when(lookupService.getCommonValues(COMMON_VALUE_ORGANISATION_TYPES))
        .thenReturn(Mono.just(orgTypes));

    CommonLookupDetail relsClient =
        new CommonLookupDetail().addContentItem(new CommonLookupValueDetail());
    lenient()
        .when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT))
        .thenReturn(Mono.just(relsClient));

    CommonLookupDetail countries = new CommonLookupDetail();
    lenient().when(lookupService.getCountries()).thenReturn(Mono.just(countries));

    lenient()
        .doAnswer(
            invocation -> {
              org.springframework.validation.Errors errors = invocation.getArgument(1);
              errors.rejectValue("organisationName", "required", "Required");
              return null;
            })
        .when(organisationOpponentValidator)
        .validate(any(), any());

    mockMvc
        .perform(
            post("/amendments/sections/opponents/organisation/{opponentId}/edit", opponentId)
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(USER_DETAILS, user)
                .flashAttr(CURRENT_OPPONENT, formData))
        .andExpect(status().isOk())
        .andExpect(view().name("application/opponents/opponents-organisation-edit"));
  }

  @Test
  void addIndividualPost_addsOpponent() throws Exception {
    String applicationId = "123";
    IndividualOpponentFormData formData = new IndividualOpponentFormData();
    formData.setFirstName("John");
    formData.setSurname("Doe");
    formData.setRelationshipToCase("REL1");
    formData.setRelationshipToClient("REL2");

    when(lookupService.getPersonToCaseRelationship("REL1"))
        .thenReturn(
            Mono.just(
                java.util.Optional.of(new RelationshipToCaseLookupValueDetail().code("REL1"))));

    mockMvc
        .perform(
            post("/amendments/sections/opponents/individual/add")
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(USER_DETAILS, user)
                .flashAttr(CURRENT_OPPONENT, formData))
        .andExpect(status().isOk());
  }

  @Test
  void editIndividualGet_displaysEditView() throws Exception {
    Integer opponentId = 1;
    String applicationId = "123";
    IndividualOpponentFormData individualOpponent = new IndividualOpponentFormData();
    individualOpponent.setId(opponentId);
    individualOpponent.setEditable(true);

    when(applicationService.getOpponents(applicationId)).thenReturn(List.of(individualOpponent));

    RelationshipToCaseLookupDetail rels =
        new RelationshipToCaseLookupDetail()
            .addContentItem(new RelationshipToCaseLookupValueDetail());
    when(lookupService.getPersonToCaseRelationships()).thenReturn(Mono.just(rels));

    CommonLookupDetail titles = new CommonLookupDetail();
    when(lookupService.getCommonValues(
            uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE))
        .thenReturn(Mono.just(titles));

    CommonLookupDetail relsClient =
        new CommonLookupDetail().addContentItem(new CommonLookupValueDetail());
    when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT))
        .thenReturn(Mono.just(relsClient));

    CommonLookupDetail countries = new CommonLookupDetail();
    when(lookupService.getCountries()).thenReturn(Mono.just(countries));

    mockMvc
        .perform(
            get("/amendments/sections/opponents/individual/{opponentId}/edit", opponentId)
                .sessionAttr(APPLICATION_ID, applicationId))
        .andExpect(status().isOk())
        .andExpect(model().attribute("amendment", true))
        .andExpect(view().name("application/opponents/opponents-individual-edit"));
  }

  @Test
  void editIndividualPost_updatesOpponent() throws Exception {
    Integer opponentId = 1;
    String applicationId = "123";
    IndividualOpponentFormData formData = new IndividualOpponentFormData();
    formData.setId(opponentId);
    formData.setFirstName("John");
    formData.setSurname("Doe");
    formData.setRelationshipToCase("REL1");
    formData.setRelationshipToClient("REL2");

    when(lookupService.getPersonToCaseRelationship("REL1"))
        .thenReturn(
            Mono.just(
                java.util.Optional.of(new RelationshipToCaseLookupValueDetail().code("REL1"))));

    mockMvc
        .perform(
            post("/amendments/sections/opponents/individual/{opponentId}/edit", opponentId)
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(USER_DETAILS, user)
                .flashAttr(CURRENT_OPPONENT, formData))
        .andExpect(status().isOk());
  }

  @Test
  void editIndividualPost_withValidationErrors_returnsEditView() throws Exception {
    Integer opponentId = 1;
    String applicationId = "123";
    IndividualOpponentFormData formData = new IndividualOpponentFormData();
    formData.setId(opponentId);

    RelationshipToCaseLookupDetail rels =
        new RelationshipToCaseLookupDetail()
            .addContentItem(new RelationshipToCaseLookupValueDetail());
    when(lookupService.getPersonToCaseRelationships()).thenReturn(Mono.just(rels));

    CommonLookupDetail titles = new CommonLookupDetail();
    when(lookupService.getCommonValues(
            uk.gov.laa.ccms.caab.constants.CommonValueConstants.COMMON_VALUE_CONTACT_TITLE))
        .thenReturn(Mono.just(titles));

    CommonLookupDetail relsClient =
        new CommonLookupDetail().addContentItem(new CommonLookupValueDetail());
    when(lookupService.getCommonValues(COMMON_VALUE_RELATIONSHIP_TO_CLIENT))
        .thenReturn(Mono.just(relsClient));

    CommonLookupDetail countries = new CommonLookupDetail();
    when(lookupService.getCountries()).thenReturn(Mono.just(countries));

    lenient()
        .doAnswer(
            invocation -> {
              org.springframework.validation.Errors errors = invocation.getArgument(1);
              errors.rejectValue("firstName", "required", "Required");
              return null;
            })
        .when(individualOpponentValidator)
        .validate(any(), any());

    mockMvc
        .perform(
            post("/amendments/sections/opponents/individual/{opponentId}/edit", opponentId)
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(USER_DETAILS, user)
                .flashAttr(CURRENT_OPPONENT, formData))
        .andExpect(status().isOk())
        .andExpect(model().attribute("amendment", true))
        .andExpect(view().name("application/opponents/opponents-individual-edit"));
  }

  @Test
  void removeOpponentGet_displaysConfirmationScreen() throws Exception {
    Integer opponentId = 1;
    String applicationId = "123";
    OrganisationOpponentFormData organisationOpponent = new OrganisationOpponentFormData();
    organisationOpponent.setId(opponentId);
    organisationOpponent.setEditable(true);

    when(applicationService.getOpponents(applicationId)).thenReturn(List.of(organisationOpponent));

    mockMvc
        .perform(
            get("/amendments/sections/opponents/{opponent-id}/remove", opponentId)
                .sessionAttr(APPLICATION_ID, applicationId))
        .andExpect(status().isOk())
        .andExpect(model().attribute("amendment", true))
        .andExpect(model().attribute("opponent", organisationOpponent))
        .andExpect(view().name("application/opponents/opponents-remove"));
  }

  @Test
  void removeOpponentPost_removesOpponent() throws Exception {
    Integer opponentId = 1;
    String applicationId = "123";

    mockMvc
        .perform(
            post("/amendments/sections/opponents/{opponent-id}/remove", opponentId)
                .sessionAttr(APPLICATION_ID, applicationId)
                .sessionAttr(USER_DETAILS, user))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/amendments/sections/opponents"));

    verify(opponentService).deleteOpponent(opponentId, user);
  }
}
