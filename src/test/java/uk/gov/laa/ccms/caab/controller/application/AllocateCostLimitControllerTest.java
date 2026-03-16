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
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.USER_DETAILS;

import java.math.BigDecimal;
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
import uk.gov.laa.ccms.caab.bean.costs.AllocateCostsFormData;
import uk.gov.laa.ccms.caab.mapper.ProceedingAndCostsMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
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

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcTester.create(
            MockMvcBuilders.standaloneSetup(allocateCostLimitController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build());
  }

  @Nested
  @DisplayName("GET: /allocate-cost-limit/start")
  class StartAllocateCostLimitTests {

    @Test
    @DisplayName("Should fetch fresh case from DB and redirect to allocation page")
    void shouldFetchFromDbAndRedirect() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setId(1);
      ebsCase.setCaseReferenceNumber("123");

      UserDetail user = new UserDetail();
      user.setUsername("user");
      user.setProvider(new BaseProvider());
      user.getProvider().setId(123);
      ApplicationDetail freshCase = new ApplicationDetail();
      freshCase.setId(2);
      freshCase.setCaseReferenceNumber("123");
      freshCase.setCosts(new CostStructureDetail());

      when(applicationService.getCase(anyString(), anyLong(), anyString())).thenReturn(freshCase);

      final var result =
          mockMvc.perform(
              get("/allocate-cost-limit/start")
                  .sessionAttr(CASE, ebsCase)
                  .sessionAttr(USER_DETAILS, user));

      assertThat(result)
          .hasStatus3xxRedirection()
          .matches(
              mvcResult -> {
                ApplicationDetail sessionCase =
                    (ApplicationDetail) mvcResult.getRequest().getSession().getAttribute(CASE);
                assertThat(sessionCase).isEqualTo(freshCase);
                assertThat(sessionCase).isNotSameAs(ebsCase);
              })
          .hasRedirectedUrl("/allocate-cost-limit");

      verify(applicationService).getCase("123", 123, "user");
    }
  }

  @Nested
  @DisplayName("GET: /allocate-cost-limit")
  class GetAllocateCostLimitTests {

    @Test
    @DisplayName("Should use session data")
    void shouldUseSessionData() {
      ApplicationDetail ebsCase = new ApplicationDetail();
      ebsCase.setCaseReferenceNumber("123");
      ebsCase.costs(new CostStructureDetail());

      UserDetail user = new UserDetail();
      user.setUsername("user");
      user.setProvider(new BaseProvider());
      user.getProvider().setId(123);

      final AllocateCostsFormData allocateCostsFormData = new AllocateCostsFormData();

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
          .containsEntry("case", ebsCase)
          .containsEntry("costDetails", allocateCostsFormData);

      verify(applicationService, never()).getCase(anyString(), anyLong(), anyString());
    }
  }

  @Nested
  @DisplayName("POST: /allocate-cost-limit")
  class PostAllocateCostLimitTests {

    @Test
    @DisplayName("Should redirect to cost allocation view when calculate button clicked")
    void shouldRedirectToViewWithCalculateAction() {
      ApplicationDetail ebsCase = new ApplicationDetail();
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

      when(copyApplicationMapper.copyApplication(
              any(ApplicationDetail.class), any(ApplicationDetail.class)))
          .thenReturn(ebsCase);

      assertThat(
              mockMvc.perform(
                  post("/allocate-cost-limit")
                      .param("action", "calculate")
                      .sessionAttr(CASE, ebsCase)
                      .flashAttr("costDetails", allocateCostsFormData)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/allocate-cost-limit");
    }

    @Test
    @DisplayName("Should redirect to review page when next button clicked with valid data")
    void shouldRedirectToReviewPageWithNextAction() {
      ApplicationDetail ebsCase = new ApplicationDetail();
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

      when(copyApplicationMapper.copyApplication(
              any(ApplicationDetail.class), any(ApplicationDetail.class)))
          .thenReturn(ebsCase);

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
    @DisplayName("Should return cost allocation view when next clicked with validation errors")
    void shouldReturnViewWhenNextActionHasValidationErrors() {
      ApplicationDetail ebsCase = new ApplicationDetail();
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

      when(copyApplicationMapper.copyApplication(
              any(ApplicationDetail.class), any(ApplicationDetail.class)))
          .thenReturn(ebsCase);

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
          .hasViewName("application/cost-allocation");
    }
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
    @DisplayName("Should redirect to case overview after successful submission")
    void shouldRedirectToCaseOverviewAfterSubmission() {
      ApplicationDetail ebsCase = new ApplicationDetail();
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
      ebsCase.costs(costs);
      ebsCase.providerDetails(
          new ApplicationProviderDetails()
              .provider(new IntDisplayValue().displayValue("provider")));

      assertThat(mockMvc.perform(post("/allocate-cost-limit/review").sessionAttr(CASE, ebsCase)))
          .hasStatus3xxRedirection()
          .hasRedirectedUrl("/case/overview");
    }
  }
}
