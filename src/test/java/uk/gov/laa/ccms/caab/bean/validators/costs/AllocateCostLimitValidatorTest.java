package uk.gov.laa.ccms.caab.bean.validators.costs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.gov.laa.ccms.caab.bean.costs.AllocateCostsFormData;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;

@DisplayName("Cost allocation validator test")
@ExtendWith(SpringExtension.class)
public class AllocateCostLimitValidatorTest {

  @InjectMocks private AllocateCostLimitValidator allocateCostLimitValidator;

  private AllocateCostsFormData allocateCostsFormData;
  private Errors errors;

  @BeforeEach
  void setUp() {
    ApplicationDetail app = new ApplicationDetail();
    CostStructureDetail costs =
        new CostStructureDetail()
            .addCostEntriesItem(
                new CostEntryDetail()
                    .requestedCosts(new BigDecimal("2250"))
                    .resourceName("PATRICK J BOWE")
                    .costCategory("Counsel")
                    .amountBilled(new BigDecimal("604.63"))
                    .requestedCosts(new BigDecimal("604.63")));
    app.costs(costs);

    allocateCostsFormData = new AllocateCostsFormData();
    allocateCostsFormData.setCostEntries(app.getCosts().getCostEntries());
    errors = new BeanPropertyBindingResult(allocateCostsFormData, "costsFormData");
  }

  @Nested
  @DisplayName("validate() tests")
  class ValidateTests {

    @Test
    @DisplayName("Should have errors when exceeding cost limitation")
    void validate_WithExceedingCostLimitation_HasErrors() {
      allocateCostsFormData
          .getCostEntries()
          .getFirst()
          .setRequestedCosts(new BigDecimal("100000001.00"));
      allocateCostLimitValidator.validate(allocateCostsFormData, errors);
      assertTrue(errors.hasErrors());
      assertNotNull(errors.getFieldError("costEntries[0]"));
      assertEquals("value.exceeds.max", errors.getFieldError("costEntries[0]").getCode());
    }

    @Test
    @DisplayName("Should not have errors")
    void validate_WithValidAndWithinLimitCostLimitation_NoErrors() {
      allocateCostsFormData.getCostEntries().getFirst().setRequestedCosts(new BigDecimal("9999"));
      allocateCostsFormData.setGrantedCostLimitation(new BigDecimal("999999"));
      allocateCostsFormData.setCurrentProviderBilledAmount(new BigDecimal("2"));
      allocateCostLimitValidator.validate(allocateCostsFormData, errors);
      assertFalse(errors.hasErrors());
    }

    @Test
    @DisplayName("Should have errors when below granted amount")
    void shouldHaveErrorsWhenBelowGrantedAmount() {
      allocateCostsFormData.getCostEntries().getFirst().setRequestedCosts(new BigDecimal("100.00"));
      allocateCostLimitValidator.validate(allocateCostsFormData, errors);
      assertTrue(errors.hasErrors());
      assertNotNull(errors.getFieldError("costEntries[0]"));
      assertEquals(
          "costCostAllocation.requestedAmount.belowBilledAmount",
          errors.getFieldError("costEntries[0]").getCode());
    }

    @Test
    @DisplayName("Should have error when exceeded the granted cost limitation")
    void shouldHaveErrorWhenExceededGrantedAmount() {
      allocateCostsFormData.getCostEntries().getFirst().setRequestedCosts(new BigDecimal("9999"));
      allocateCostsFormData.setGrantedCostLimitation(new BigDecimal("2250"));
      allocateCostsFormData.setCurrentProviderBilledAmount(new BigDecimal("0"));
      allocateCostLimitValidator.validate(allocateCostsFormData, errors);
      assertTrue(errors.hasErrors());
      assertNotNull(errors.getFieldError("grantedCostLimitation"));
      assertEquals(
          "costCostAllocation.exceeded.requestedCost",
          errors.getFieldError("grantedCostLimitation").getCode());
    }

    @Test
    @DisplayName("Should have error when below the granted cost limitation")
    void shouldHaveErrorWhenBelowGrantedAmount() {
      allocateCostsFormData.getCostEntries().getFirst().setRequestedCosts(new BigDecimal("9998"));
      allocateCostsFormData.setGrantedCostLimitation(new BigDecimal("9999"));
      allocateCostsFormData.setCurrentProviderBilledAmount(new BigDecimal("2"));
      allocateCostLimitValidator.validate(allocateCostsFormData, errors);
      assertTrue(errors.hasErrors());
      assertNotNull(errors.getFieldError("currentProviderBilledAmount"));
      assertEquals(
          "costCostAllocation.requestedAmount.belowBilledAmount",
          errors.getFieldError("currentProviderBilledAmount").getCode());
    }
  }
}
