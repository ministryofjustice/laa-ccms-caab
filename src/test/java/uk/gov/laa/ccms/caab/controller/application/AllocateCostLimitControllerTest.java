package uk.gov.laa.ccms.caab.controller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;

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
import uk.gov.laa.ccms.caab.bean.costs.CostsFormData;
import uk.gov.laa.ccms.caab.mapper.ProceedingAndCostsMapper;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;

@ExtendWith(MockitoExtension.class)
@DisplayName("Allocate cost limit controller tests")
public class AllocateCostLimitControllerTest {

  @InjectMocks AllocateCostLimitController allocateCostLimitController;

  private MockMvcTester mockMvc;

  @Mock private ProceedingAndCostsMapper proceedingAndCostsMapper;

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
    @DisplayName("Should return expected view")
    void shouldReturnExpectedView() {
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

      final CostsFormData costsFormData = new CostsFormData(new BigDecimal("25000"));
      costsFormData.setRequestedCostLimitation(String.valueOf(costs.getRequestedCostLimitation()));
      costsFormData.setCostEntries(ebsCase.getCosts().getCostEntries());
      costsFormData.setProviderName(ebsCase.getProviderDetails().getProvider().getDisplayValue());

      when(proceedingAndCostsMapper.toCostsForm(any(ApplicationDetail.class)))
          .thenReturn(costsFormData);

      assertThat(mockMvc.perform(get("/allocate-cost-limit").sessionAttr(CASE, ebsCase)))
          .hasStatusOk()
          .hasViewName("application/cost-allocation")
          .model()
          .containsEntry("case", ebsCase)
          .containsEntry("costDetails", costsFormData)
          .containsEntry("totalRemaining", new BigDecimal("24395.37"));
    }
  }
}
