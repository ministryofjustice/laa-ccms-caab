package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;
import static uk.gov.laa.ccms.caab.util.EbsModelUtils.buildUserDetail;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.exception.CaabApplicationException;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.PriorAuthorityDetail;
import uk.gov.laa.ccms.caab.model.sections.ApplicationSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.IndividualAddressContactDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.IndividualDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.IndividualEmploymentDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.IndividualGeneralDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.OrganisationAddressDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.OrganisationDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.model.sections.OrganisationOrganisationDetailsSectionDisplay;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
class CaseControllerTest {

  @Mock
  private ApplicationService applicationService;

  @InjectMocks
  private CaseController caseController;

  private MockMvcTester mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcTester.create(MockMvcBuilders.standaloneSetup(caseController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build());
  }

  @Nested
  @DisplayName("/cases/details tests")
  class CaseDetails {

    @Test
    @DisplayName("Should return view and model when case details exist")
    void caseDetailsReturnsViewAndModelWhenCaseDetailsExist() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      ApplicationSectionDisplay display = ApplicationSectionDisplay.builder().build();
      when(applicationService.getCaseDetailsDisplay(ebsCase)).thenReturn(display);

      assertThat(mockMvc.perform(get("/cases/details").sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/case-details")
          .model()
          .containsEntry("summary", display);
    }

    @Test
    @DisplayName("Should throw exception when case details missing")
    void caseDetailsThrowsExceptionWhenCaseDetailsMissing() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      when(applicationService.getCaseDetailsDisplay(ebsCase)).thenReturn(null);

      assertThat(mockMvc.perform(get("/cases/details").sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("error");
    }

  }

  @Nested
  @DisplayName("/cases/details/other-party/{index} tests")
  class CaseDetailsOtherParty {

    @Test
    @DisplayName("Should return view and model when case details exist")
    void caseDetailsOtherPartyReturnsViewAndModelWhenCaseDetailsExist() {
      OpponentDetail opponent = new OpponentDetail();
      opponent.setType("Individual");
      ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setOpponents(Collections.singletonList(opponent));
      IndividualDetailsSectionDisplay otherParty = new IndividualDetailsSectionDisplay(
          new IndividualGeneralDetailsSectionDisplay(),
          new IndividualAddressContactDetailsSectionDisplay(),
          new IndividualEmploymentDetailsSectionDisplay());
      when(applicationService.getIndividualDetailsSectionDisplay(any())).thenReturn(
          otherParty);
      assertThat(mockMvc.perform(get("/cases/details/other-party/0")
          .sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/case-details-other-party")
          .model()
          .containsEntry("otherParty", otherParty);
    }

    @Test
    @DisplayName("Should throw exception when other party list null")
    void caseDetailsOtherPartyThrowsExceptionWhenOtherPartyListNull() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setOpponents(null);

      assertThat(mockMvc.perform(get("/cases/details/other-party/0")
          .sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("error");
    }

    @Test
    @DisplayName("Should throw exception when other party doesn't exist")
    void caseDetailsOtherPartyThrowsExceptionWhenOtherPartyDoesNotExist() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setOpponents(Collections.emptyList());

      assertThat(mockMvc.perform(get("/cases/details/other-party/0")
          .sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("error");
    }

    @Test
    @DisplayName("Should return view and model when case details exist for an organisation")
    void caseDetailsOtherPartyOrganisationReturnsViewAndModelWhenCaseDetailsExist() {
      OpponentDetail opponent = new OpponentDetail();
      opponent.setType("Organisation");
      ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setOpponents(Collections.singletonList(opponent));
      ApplicationSectionDisplay display = ApplicationSectionDisplay.builder().build();
      OrganisationDetailsSectionDisplay otherParty = new OrganisationDetailsSectionDisplay(
          new OrganisationOrganisationDetailsSectionDisplay(),
          new OrganisationAddressDetailsSectionDisplay());

      when(applicationService.getOrganisationDetailsSectionDisplay(any())).thenReturn(
          otherParty);
      assertThat(mockMvc.perform(get("/cases/details/other-party/0")
          .sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/case-details-other-party-organisation")
          .model()
          .containsEntry("otherPartyOrganisation", otherParty);
    }
  }

  @Test
  void handleAbandonGetReturnsCorrectView() {
    ApplicationDetail ebsCase = new ApplicationDetail();
    assertThat(mockMvc.perform(get("/cases/amendment/abandon")
        .sessionAttr(CASE, ebsCase)))
        .hasStatusOk()
        .hasViewName("application/amendment-remove");
  }

  @Test
  void handleAbandonPostCallsServiceAndReturnsCorrectView() {
    ApplicationDetail ebsCase = new ApplicationDetail();
    final UserDetail user = buildUserDetail();
    doNothing().when(applicationService).abandonApplication(ebsCase, user);

    assertThat(mockMvc.perform(post("/cases/amendment/abandon")
        .sessionAttr(CASE, ebsCase)
        .sessionAttr(USER_DETAILS, user)))
        .hasStatusOk()
        .hasViewName("home");

    verify(applicationService, times(1)).abandonApplication(ebsCase, user);
  }

  @Test
  void handleAbandonPostCallsServiceAndReturnsErrorView() {
    ApplicationDetail ebsCase = new ApplicationDetail();
    final UserDetail user = buildUserDetail();

    doThrow(new CaabApplicationException("Something went wrong")).when(applicationService)
        .abandonApplication(ebsCase, user);

    assertThat(mockMvc.perform(post("/cases/amendment/abandon")
        .sessionAttr(CASE, ebsCase)
        .sessionAttr(USER_DETAILS, user)))
        .hasStatusOk()
        .hasViewName("error");

    verify(applicationService, times(1)).abandonApplication(ebsCase, user);
  }

  @Test
  void getCaseDetailsViewReturnsViewAndModelForValidIndex() {
    ApplicationDetail ebsCase = new ApplicationDetail();
    PriorAuthorityDetail priorAuthority = new PriorAuthorityDetail();
    ebsCase.setPriorAuthorities(List.of(priorAuthority));

    assertThat(mockMvc.perform(get("/cases/details/prior-authority/0")
        .sessionAttr(CASE, ebsCase)))
        .hasStatusOk()
        .hasViewName("application/prior-authority-review")
        .model()
        .containsEntry("priorAuthority", priorAuthority);
  }

  @Test
  void getCaseDetailsViewThrowsExceptionWhenPriorAuthoritiesAreEmpty() {
    ApplicationDetail ebsCase = new ApplicationDetail();
    ebsCase.setPriorAuthorities(List.of());

    assertThat(mockMvc.perform(get("/cases/details/prior-authority/0")
        .sessionAttr(CASE, ebsCase)))
        .hasStatusOk()
        .hasViewName("error");
  }

  @Test
  void getCaseDetailsViewThrowsExceptionForInvalidIndex() {
    ApplicationDetail ebsCase = new ApplicationDetail();
    ebsCase.setPriorAuthorities(List.of(new PriorAuthorityDetail()));

    assertThat(mockMvc.perform(get("/cases/details/prior-authority/1")
        .sessionAttr(CASE, ebsCase)))
        .hasStatusOk()
        .hasViewName("error");
  }
}
