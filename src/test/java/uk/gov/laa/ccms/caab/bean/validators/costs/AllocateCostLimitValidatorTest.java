package uk.gov.laa.ccms.caab.bean.validators.costs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import uk.gov.laa.ccms.caab.bean.costs.AllocateCostsFormData;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.CostEntryDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;

@DisplayName("Cost allocation validator test")
@ExtendWith(MockitoExtension.class)
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
                    .newEntry(Boolean.TRUE)
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
      assertNotNull(errors.getFieldError("costEntries[0].requestedCosts"));
      assertEquals(
          "value.exceeds.max", errors.getFieldError("costEntries[0].requestedCosts").getCode());
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
      assertNotNull(errors.getFieldError("costEntries[0].requestedCosts"));
      assertEquals(
          "costCostAllocation.requestedAmount.belowBilledAmount",
          errors.getFieldError("costEntries[0].requestedCosts").getCode());
    }

    @Test
    @DisplayName("Should have error when exceeded the granted cost limitation")
    void shouldHaveErrorWhenExceededGrantedAmount() {
      allocateCostsFormData.getCostEntries().getFirst().setRequestedCosts(new BigDecimal("9999"));
      allocateCostsFormData.setGrantedCostLimitation(new BigDecimal("2250"));
      allocateCostsFormData.setCurrentProviderBilledAmount(new BigDecimal("0"));
      allocateCostLimitValidator.validate(allocateCostsFormData, errors);
      assertTrue(errors.hasErrors());
      assertNotNull(errors.getFieldError("costEntries[0].requestedCosts"));
      assertEquals(
          "costCostAllocation.exceeded.requestedCost",
          errors.getFieldError("costEntries[0].requestedCosts").getCode());
    }

    @Test
    @DisplayName("Should have error when below the granted cost limitation")
    void shouldHaveErrorWhenBelowGrantedAmount() {
      allocateCostsFormData.getCostEntries().getFirst().setRequestedCosts(new BigDecimal("9998"));
      allocateCostsFormData.setGrantedCostLimitation(new BigDecimal("9999"));
      allocateCostsFormData.setCurrentProviderBilledAmount(new BigDecimal("2"));
      allocateCostLimitValidator.validate(allocateCostsFormData, errors);
      assertTrue(errors.hasErrors());
      assertNotNull(errors.getFieldError("costEntries[0].requestedCosts"));
      assertEquals(
          "costCostAllocation.requestedAmount.belowBilledAmount",
          errors.getFieldError("costEntries[0].requestedCosts").getCode());
    }

    @Test
    @DisplayName("Should accept number with 2 decimals places")
    void shouldValidateNumberAllowed2dp() {
      allocateCostsFormData
          .getCostEntries()
          .getFirst()
          .setRequestedCosts(new BigDecimal("9995.12"));
      allocateCostsFormData.setGrantedCostLimitation(new BigDecimal("999999"));
      allocateCostsFormData.setCurrentProviderBilledAmount(new BigDecimal("2"));
      allocateCostLimitValidator.validate(allocateCostsFormData, errors);

      boolean hasErrors = errors.hasErrors();
      assertFalse(hasErrors);

      FieldError fieldError = errors.getFieldError("costEntries[0].requestedCosts");
      assertNull(fieldError);
    }

    @Test
    @DisplayName("Should not accept number with decimals places more than 2")
    void shouldNotValidateNumberAllowed2dp() {
      allocateCostsFormData
          .getCostEntries()
          .getFirst()
          .setRequestedCosts(new BigDecimal("9998.123"));
      allocateCostsFormData.setGrantedCostLimitation(new BigDecimal("9999"));
      allocateCostsFormData.setCurrentProviderBilledAmount(new BigDecimal("2"));
      allocateCostLimitValidator.validate(allocateCostsFormData, errors);

      boolean hasErrors = errors.hasErrors();
      assertTrue(errors.hasErrors());

      FieldError fieldError = errors.getFieldError("costEntries[0].requestedCosts");
      assertNotNull(fieldError);

      String errorCode = errors.getFieldError("costEntries[0].requestedCosts").getCode();
      assertEquals("invalid.decimal.places", errorCode);
    }
  }
}
