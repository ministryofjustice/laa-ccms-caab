package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.COST_ALLOCATION_FORM_DATA;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.laa.ccms.caab.advice.GlobalExceptionHandler;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.bean.costs.AllocateCostsFormData;
import uk.gov.laa.ccms.caab.mapper.ProceedingAndCostsMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.service.AmendmentService;
import uk.gov.laa.ccms.caab.service.ApplicationService;
import uk.gov.laa.ccms.data.model.BaseProvider;
import uk.gov.laa.ccms.data.model.UserDetail;

@ExtendWith(MockitoExtension.class)
@DisplayName("Allocate cost limit controller tests")
public class AllocateCostLimitControllerTest {

  @InjectMocks AllocateCostLimitController allocateCostLimitController;

  private MockMvcTester mockMvc;

  @Mock private ProceedingAndCostsMapper proceedingAndCostsMapper;

  @Mock
  private uk.gov.laa.ccms.caab.bean.validators.costs.AllocateCostLimitValidator
      allocateCostLimitValidator;

  @Mock private uk.gov.laa.ccms.caab.mapper.CopyApplicationMapper copyApplicationMapper;

  @Mock private ApplicationService applicationService;

  @Mock private AmendmentService amendmentService;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcTester.create(
            MockMvcBuilders.standaloneSetup(allocateCostLimitController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build());
  }

  @Nested
  @DisplayName("GET: /allocate-cost-limit")
  class GetAllocateCostLimitTests {

    @Test
    @DisplayName("Should fetch fresh cost data on first entry and update session costs")
    void shouldFetchFreshCostDataOnFirstEntry() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setId(1);
      ebsCase.setCaseReferenceNumber("123");
      ebsCase.costs(
          new CostStructureDetail()
              .grantedCostLimitation(new BigDecimal("20000"))
              .addCostEntriesItem(new CostEntryDetail().requestedCosts(new BigDecimal("500"))));

      ApplicationDetail freshCase = new ApplicationDetail();
      freshCase.setId(2);
      freshCase.setCaseReferenceNumber("123");
      freshCase.costs(
          new CostStructureDetail()
              .grantedCostLimitation(new BigDecimal("25000"))
              .addCostEntriesItem(new CostEntryDetail().requestedCosts(new BigDecimal("1000"))));

      UserDetail user = new UserDetail();
      user.setUsername("user");
      user.setProvider(new BaseProvider());
      user.getProvider().setId(123);

      final AllocateCostsFormData allocateCostsFormData = new AllocateCostsFormData();

      when(applicationService.getCase("123", 123L, "user")).thenReturn(freshCase);
      when(proceedingAndCostsMapper.toAllocateCostsForm(any(ApplicationDetail.class)))
          .thenReturn(allocateCostsFormData);

      assertThat(
              mockMvc.perform(
                  get("/allocate-cost-limit")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)))
          .hasStatusOk()
          .hasViewName("application/cost-allocation")
          .model()
          .containsEntry("costDetails", allocateCostsFormData);

      // Verify fresh data was fetched from database
      verify(applicationService).getCase("123", 123L, "user");
      assertThat(ebsCase.getCosts()).isEqualTo(freshCase.getCosts());
    }

    @Test
    @DisplayName("Should reuse session cost data after first entry")
    void shouldReuseSessionCostDataAfterFirstEntry() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setCaseReferenceNumber("123");

      UserDetail user = new UserDetail();
      user.setUsername("user");
      user.setProvider(new BaseProvider());
      user.getProvider().setId(123);

      final AllocateCostsFormData allocateCostsFormData = new AllocateCostsFormData();

      assertThat(
              mockMvc.perform(
                  get("/allocate-cost-limit")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(COST_ALLOCATION_FORM_DATA, allocateCostsFormData)))
          .hasStatusOk()
          .hasViewName("application/cost-allocation")
          .model()
          .containsEntry("costDetails", allocateCostsFormData);

      verify(applicationService, never()).getCase(anyString(), anyLong(), anyString());
    }
  }

  @Nested
  @DisplayName("POST: /allocate-cost-limit")
  class PostAllocateCostLimitTests {

    @Test
    @DisplayName("Should return view with calculated data when calculate button clicked")
    void shouldReturnViewWithCalculateAction() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setId(1);
      CostStructureDetail costs =
          new CostStructureDetail()
              .addCostEntriesItem(
                  new CostEntryDetail()
                      .requestedCosts(new BigDecimal("604.63"))
                      .resourceName("PATRICK J BOWE")
                      .costCategory("Counsel")
                      .amountBilled(new BigDecimal("604.63")))
              .grantedCostLimitation(new BigDecimal("25000"))
              .requestedCostLimitation(new BigDecimal("25000"));
      ebsCase.costs(costs);
      ebsCase.providerDetails(
          new ApplicationProviderDetails()
              .provider(new IntDisplayValue().displayValue("provider")));

      final AllocateCostsFormData allocateCostsFormData = new AllocateCostsFormData();
      allocateCostsFormData.setGrantedCostLimitation(costs.getGrantedCostLimitation());
      allocateCostsFormData.setCostEntries(ebsCase.getCosts().getCostEntries());

      ApplicationDetail appCopy = new ApplicationDetail();
      appCopy.costs(costs);

      when(copyApplicationMapper.copyApplication(
              any(ApplicationDetail.class), any(ApplicationDetail.class)))
          .thenReturn(appCopy);

      assertThat(
              mockMvc.perform(
                  post("/allocate-cost-limit")
                      .param("action", "calculate")
                      .sessionAttr(CASE, ebsCase)
                      .flashAttr("costDetails", allocateCostsFormData)))
          .hasStatusOk()
          .hasViewName("application/cost-allocation")
          .model()
          .containsEntry("case", ebsCase)
          .containsKey("costDetails");
    }

    @Test
    @DisplayName(
        "Should redirect to review page and update session when next button clicked with valid data")
    void shouldRedirectToReviewPageWithNextAction() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setId(1);
      CostStructureDetail costs =
          new CostStructureDetail()
              .addCostEntriesItem(
                  new CostEntryDetail()
                      .requestedCosts(new BigDecimal("604.63"))
                      .resourceName("PATRICK J BOWE")
                      .costCategory("Counsel")
                      .amountBilled(new BigDecimal("604.63")))
              .grantedCostLimitation(new BigDecimal("25000"))
              .requestedCostLimitation(new BigDecimal("25000"));
      ebsCase.costs(costs);
      ebsCase.providerDetails(
          new ApplicationProviderDetails()
              .provider(new IntDisplayValue().displayValue("provider")));

      final AllocateCostsFormData allocateCostsFormData = new AllocateCostsFormData();
      allocateCostsFormData.setGrantedCostLimitation(costs.getGrantedCostLimitation());
      allocateCostsFormData.setCostEntries(ebsCase.getCosts().getCostEntries());

      ApplicationDetail appCopy = new ApplicationDetail();
      appCopy.costs(
          new CostStructureDetail()
              .addCostEntriesItem(
                  new CostEntryDetail()
                      .requestedCosts(new BigDecimal("1000")) // Updated value
                      .resourceName("PATRICK J BOWE")
                      .costCategory("Counsel")
                      .amountBilled(new BigDecimal("604.63")))
              .grantedCostLimitation(new BigDecimal("25000"))
              .requestedCostLimitation(new BigDecimal("25000")));

      when(copyApplicationMapper.copyApplication(
              any(ApplicationDetail.class), any(ApplicationDetail.class)))
          .thenReturn(appCopy);

      assertThat(
              mockMvc.perform(
                  post("/allocate-cost-limit")
                      .param("action", "next")
                      .sessionAttr(CASE, ebsCase)
                      .flashAttr("costDetails", allocateCostsFormData)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/allocate-cost-limit/review");
    }

    @Test
    @DisplayName(
        "Should return cost allocation view and not update session when next clicked with validation errors")
    void shouldReturnViewWhenNextActionHasValidationErrors() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setId(1);
      CostStructureDetail costs =
          new CostStructureDetail()
              .addCostEntriesItem(
                  new CostEntryDetail()
                      .requestedCosts(new BigDecimal("30000")) // Exceeds granted cost
                      .resourceName("PATRICK J BOWE")
                      .costCategory("Counsel")
                      .amountBilled(new BigDecimal("604.63")))
              .grantedCostLimitation(new BigDecimal("25000"))
              .requestedCostLimitation(new BigDecimal("25000"));
      ebsCase.costs(costs);
      ebsCase.providerDetails(
          new ApplicationProviderDetails()
              .provider(new IntDisplayValue().displayValue("provider")));

      final AllocateCostsFormData allocateCostsFormData = new AllocateCostsFormData();
      allocateCostsFormData.setGrantedCostLimitation(costs.getGrantedCostLimitation());
      allocateCostsFormData.setCostEntries(ebsCase.getCosts().getCostEntries());

      ApplicationDetail appCopy = new ApplicationDetail();
      appCopy.costs(costs);

      when(copyApplicationMapper.copyApplication(
              any(ApplicationDetail.class), any(ApplicationDetail.class)))
          .thenReturn(appCopy);

      // Mock validator to add an error
      doAnswer(
              invocation -> {
                org.springframework.validation.Errors errors = invocation.getArgument(1);
                errors.rejectValue(
                    "costEntries[0].requestedCosts",
                    "costCostAllocation.exceeded.requestedCost",
                    "Exceeded granted cost limitation");
                return null;
              })
          .when(allocateCostLimitValidator)
          .validate(
              any(AllocateCostsFormData.class), any(org.springframework.validation.Errors.class));

      assertThat(
              mockMvc.perform(
                  post("/allocate-cost-limit")
                      .param("action", "next")
                      .sessionAttr(CASE, ebsCase)
                      .flashAttr("costDetails", allocateCostsFormData)))
          .hasStatusOk()
          .hasViewName("application/cost-allocation")
          .model()
          .containsEntry("case", ebsCase)
          .containsKey("costDetails");
    }

    @Test
    @DisplayName("Should preserve newly added counsel when next button clicked")
    void shouldPreserveNewlyAddedCounselWithNextAction() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setId(1);
      CostStructureDetail costs =
          new CostStructureDetail()
              .addCostEntriesItem(
                  new CostEntryDetail()
                      .requestedCosts(new BigDecimal("100"))
                      .resourceName("ORIGINAL COUNSEL")
                      .costCategory("Counsel")
                      .amountBilled(new BigDecimal("100")))
              .grantedCostLimitation(new BigDecimal("25000"))
              .requestedCostLimitation(new BigDecimal("25000"));
      ebsCase.costs(costs);
      ebsCase.providerDetails(
          new ApplicationProviderDetails()
              .provider(new IntDisplayValue().displayValue("provider")));

      // Form data with an additional entry (newly added counsel)
      final AllocateCostsFormData allocateCostsFormData = new AllocateCostsFormData();
      allocateCostsFormData.setGrantedCostLimitation(costs.getGrantedCostLimitation());

      List<CostEntryDetail> formEntries = new ArrayList<>();
      formEntries.add(
          new CostEntryDetail()
              .requestedCosts(new BigDecimal("100"))
              .resourceName("ORIGINAL COUNSEL")
              .costCategory("Counsel")
              .amountBilled(new BigDecimal("100")));
      formEntries.add(
          new CostEntryDetail()
              .requestedCosts(new BigDecimal("500"))
              .resourceName("NEW COUNSEL")
              .costCategory("Junior")
              .amountBilled(BigDecimal.ZERO)
              .newEntry(true));

      allocateCostsFormData.setCostEntries(formEntries);

      ApplicationDetail appCopy = new ApplicationDetail();
      appCopy.costs(costs); // original costs only

      when(copyApplicationMapper.copyApplication(
              any(ApplicationDetail.class), any(ApplicationDetail.class)))
          .thenReturn(appCopy);

      assertThat(
              mockMvc.perform(
                  post("/allocate-cost-limit")
                      .param("action", "next")
                      .sessionAttr(CASE, ebsCase)
                      .flashAttr("costDetails", allocateCostsFormData)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/allocate-cost-limit/review");

      // Verify that ebsCase now has 2 cost entries
      assertThat(ebsCase.getCosts().getCostEntries()).hasSize(2);
      assertThat(ebsCase.getCosts().getCostEntries().get(1).getResourceName())
          .isEqualTo("NEW COUNSEL");
    }

    @Test
    @DisplayName("Should ask for confirmation before removing a counsel, keeping typed amounts")
    void shouldRedirectToRemoveConfirmation() {
      ApplicationDetail ebsCase = caseWithOriginalCounsel();
      AllocateCostsFormData formData =
          formDataWith(originalCounsel(), newlyAddedCounsel("NEW COUNSEL"));

      MockHttpSession session = new MockHttpSession();

      assertThat(
              mockMvc.perform(
                  post("/allocate-cost-limit")
                      .param("removeCounsel", "1")
                      .sessionAttr(CASE, ebsCase)
                      .session(session)
                      .flashAttr("costDetails", formData)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/allocate-cost-limit/counsel/1/remove");

      // Nothing is removed yet, and the amounts typed so far are saved so they survive the trip
      // through the confirmation screen.
      AllocateCostsFormData stored =
          (AllocateCostsFormData) session.getAttribute(COST_ALLOCATION_FORM_DATA);
      assertThat(stored.getCostEntries()).hasSize(2);
    }

    private ApplicationDetail caseWithOriginalCounsel() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setId(1);
      ebsCase.costs(
          new CostStructureDetail()
              .addCostEntriesItem(originalCounsel())
              .grantedCostLimitation(new BigDecimal("25000"))
              .requestedCostLimitation(new BigDecimal("25000")));
      ebsCase.providerDetails(
          new ApplicationProviderDetails()
              .provider(new IntDisplayValue().displayValue("provider")));

      ApplicationDetail appCopy = new ApplicationDetail();
      appCopy.costs(ebsCase.getCosts());
      when(copyApplicationMapper.copyApplication(
              any(ApplicationDetail.class), any(ApplicationDetail.class)))
          .thenReturn(appCopy);

      return ebsCase;
    }
  }

  @Nested
  @DisplayName("GET/POST: /allocate-cost-limit/counsel/{index}/remove")
  class RemoveCounselTests {

    @Test
    @DisplayName("Should show the confirmation screen for a counsel added during this amendment")
    void shouldShowConfirmationForNewlyAddedCounsel() {
      CostEntryDetail newCounsel = newlyAddedCounsel("NEW COUNSEL");
      AllocateCostsFormData formData = formDataWith(originalCounsel(), newCounsel);

      assertThat(
              mockMvc.perform(
                  get("/allocate-cost-limit/counsel/1/remove")
                      .sessionAttr(COST_ALLOCATION_FORM_DATA, formData)))
          .hasStatusOk()
          .hasViewName("application/counsel-remove")
          .model()
          .containsEntry("counsel", newCounsel)
          .containsEntry("counselIndex", 1);
    }

    @Test
    @DisplayName("Should not offer to remove a counsel already held on the case")
    void shouldNotConfirmRemovalOfExistingCounsel() {
      AllocateCostsFormData formData =
          formDataWith(originalCounsel(), newlyAddedCounsel("NEW COUNSEL"));

      // Index 0 came from EBS, so the confirmation screen is refused outright.
      assertThat(
              mockMvc.perform(
                  get("/allocate-cost-limit/counsel/0/remove")
                      .sessionAttr(COST_ALLOCATION_FORM_DATA, formData)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/allocate-cost-limit");
    }

    @Test
    @DisplayName("Should remove the counsel once confirmed")
    void shouldRemoveCounselOnConfirm() {
      AllocateCostsFormData formData =
          formDataWith(originalCounsel(), newlyAddedCounsel("NEW COUNSEL"));
      MockHttpSession session = new MockHttpSession();

      assertThat(
              mockMvc.perform(
                  post("/allocate-cost-limit/counsel/1/remove")
                      .session(session)
                      .sessionAttr(COST_ALLOCATION_FORM_DATA, formData)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/allocate-cost-limit");

      AllocateCostsFormData stored =
          (AllocateCostsFormData) session.getAttribute(COST_ALLOCATION_FORM_DATA);
      assertThat(stored.getCostEntries()).hasSize(1);
      assertThat(stored.getCostEntries().getFirst().getResourceName())
          .isEqualTo("ORIGINAL COUNSEL");
    }

    @Test
    @DisplayName("Should refuse to remove a counsel already held on the case")
    void shouldRefuseToRemoveExistingCounselOnConfirm() {
      AllocateCostsFormData formData =
          formDataWith(originalCounsel(), newlyAddedCounsel("NEW COUNSEL"));
      MockHttpSession session = new MockHttpSession();

      // A hand-crafted post naming an EBS counsel is refused, not just hidden in the view.
      assertThat(
              mockMvc.perform(
                  post("/allocate-cost-limit/counsel/0/remove")
                      .session(session)
                      .sessionAttr(COST_ALLOCATION_FORM_DATA, formData)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/allocate-cost-limit");

      assertThat(formData.getCostEntries()).hasSize(2);
      assertThat(formData.getCostEntries().getFirst().getResourceName())
          .isEqualTo("ORIGINAL COUNSEL");
    }

    @Test
    @DisplayName("Should ignore an out of range counsel index")
    void shouldIgnoreOutOfRangeIndex() {
      AllocateCostsFormData formData = formDataWith(originalCounsel());

      assertThat(
              mockMvc.perform(
                  post("/allocate-cost-limit/counsel/9/remove")
                      .sessionAttr(COST_ALLOCATION_FORM_DATA, formData)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/allocate-cost-limit");

      assertThat(formData.getCostEntries()).hasSize(1);
    }
  }

  private static CostEntryDetail originalCounsel() {
    return new CostEntryDetail()
        .requestedCosts(new BigDecimal("100"))
        .resourceName("ORIGINAL COUNSEL")
        .costCategory("COUNSEL")
        .lscResourceId("11")
        .ebsId("ebs123")
        .amountBilled(new BigDecimal("100"))
        .newEntry(false);
  }

  private static CostEntryDetail newlyAddedCounsel(final String name) {
    return new CostEntryDetail()
        .requestedCosts(new BigDecimal("500"))
        .resourceName(name)
        .costCategory("COUNSEL")
        .lscResourceId("22")
        .amountBilled(BigDecimal.ZERO)
        .newEntry(true);
  }

  private static AllocateCostsFormData formDataWith(final CostEntryDetail... entries) {
    AllocateCostsFormData formData = new AllocateCostsFormData();
    formData.setGrantedCostLimitation(new BigDecimal("25000"));
    formData.setCostEntries(new ArrayList<>(List.of(entries)));
    return formData;
  }

  @Nested
  @DisplayName("GET: /allocate-cost-limit/review")
  class GetReviewCaseCostsTests {

    @Test
    @DisplayName("Should return expected view for review page")
    void shouldReturnExpectedViewForReviewPage() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      CostStructureDetail costs =
          new CostStructureDetail()
              .addCostEntriesItem(
                  new CostEntryDetail()
                      .requestedCosts(new BigDecimal("2250"))
                      .resourceName("PATRICK J BOWE")
                      .costCategory("Counsel")
                      .amountBilled(new BigDecimal("604.63"))
                      .requestedCosts(new BigDecimal("604.63")))
              .grantedCostLimitation(new BigDecimal("25000"))
              .requestedCostLimitation(new BigDecimal("25000"));
      ebsCase.costs(costs);
      ebsCase.providerDetails(
          new ApplicationProviderDetails()
              .provider(new IntDisplayValue().displayValue("provider")));

      final AllocateCostsFormData allocateCostsFormData = new AllocateCostsFormData();
      allocateCostsFormData.setRequestedCostLimitation(costs.getRequestedCostLimitation());
      allocateCostsFormData.setCostEntries(ebsCase.getCosts().getCostEntries());
      allocateCostsFormData.setProviderName(
          ebsCase.getProviderDetails().getProvider().getDisplayValue());

      when(proceedingAndCostsMapper.toAllocateCostsForm(any(ApplicationDetail.class)))
          .thenReturn(allocateCostsFormData);

      assertThat(mockMvc.perform(get("/allocate-cost-limit/review").sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/case-costs-review")
          .model()
          .containsEntry("case", ebsCase)
          .containsEntry("costDetails", allocateCostsFormData);
    }
  }

  @Nested
  @DisplayName("POST: /allocate-cost-limit/review")
  class PostSubmitCaseCostsTests {

    @Test
    @DisplayName("Should redirect to submission after successful submission")
    void shouldRedirectToSubmissionPageAfterSubmitting() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      UserDetail user = new UserDetail();
      user.setProvider(new BaseProvider());
      user.getProvider().setId(123);
      ActiveCase activeCase = ActiveCase.builder().caseReferenceNumber("CASE123").build();
      ebsCase.caseReferenceNumber("123456");
      CostStructureDetail costs =
          new CostStructureDetail()
              .addCostEntriesItem(
                  new CostEntryDetail()
                      .requestedCosts(new BigDecimal("2250"))
                      .resourceName("PATRICK J BOWE")
                      .costCategory("Counsel")
                      .amountBilled(new BigDecimal("604.63"))
                      .requestedCosts(new BigDecimal("604.63")))
              .grantedCostLimitation(new BigDecimal("25000"))
              .requestedCostLimitation(new BigDecimal("25000"));
      ebsCase.setCosts(costs);
      ebsCase.setProviderDetails(
          new ApplicationProviderDetails()
              .provider(new IntDisplayValue().displayValue("provider")));

      AllocateCostsFormData formData = new AllocateCostsFormData();
      formData.setRequestedCostLimitation(new BigDecimal("25000"));
      formData.setGrantedCostLimitation(new BigDecimal("25000"));
      formData.setCostEntries(costs.getCostEntries());

      when(amendmentService.submitQuickAmendmentCostAllocation(any(), anyString(), any()))
          .thenReturn("transactionId");

      assertThat(
              mockMvc.perform(
                  post("/allocate-cost-limit/review")
                      .sessionAttr(CASE, ebsCase)
                      .sessionAttr(USER_DETAILS, user)
                      .sessionAttr(ACTIVE_CASE, activeCase)
                      .sessionAttr(COST_ALLOCATION_FORM_DATA, formData)))
          .hasRedirectedUrl("/amendments/submit-case");

      // The costs held in session are submitted as-is, so the requested cost limitation and each
      // entry's EBS id survive the review step.
      verify(amendmentService).submitQuickAmendmentCostAllocation(formData, "CASE123", user);
    }
  }
}
