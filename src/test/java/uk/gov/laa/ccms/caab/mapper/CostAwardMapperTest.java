package uk.gov.laa.ccms.caab.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.gov.laa.ccms.caab.bean.award.CostAwardFormData;
import uk.gov.laa.ccms.caab.model.CostAwardDetail;
import uk.gov.laa.ccms.caab.util.DateUtils;

class CostAwardMapperTest {

  private final CostAwardMapper mapper = Mappers.getMapper(CostAwardMapper.class);

  @Test
  void mapsFormDataToGeneratedApiModel() {
    final CostAwardFormData form = new CostAwardFormData();
    form.setAwardType("COST");
    form.setDescription("Cost");
    form.setAwardCode("COST_AGR");
    form.setDateOfOrder("01/02/2025");
    form.setCourtAssessmentStatus("ASSESSED");
    form.setLaaFundedLegalCosts("10.10");
    form.setOtherPreCertificateCosts("20.20");
    form.setLaaRate("30.30");
    form.setMarketRate("40.40");
    form.setAwardedBy("COURT");
    form.setInterestRate("8.5");
    form.setInterestStartDate("02/02/2025");
    form.setOrderServedDate("03/02/2025");
    form.setAddressLine1("1 High Street");
    form.setAddressLine2("London");
    form.setAddressLine3("England");
    form.setOtherDetails("Other details");

    final CostAwardDetail request = mapper.toCostAward(form);

    assertThat(request.getAwardType()).isEqualTo("COST");
    assertThat(request.getDescription()).isEqualTo("Cost");
    assertThat(request.getAwardCode()).isEqualTo("COST_AGR");
    assertThat(request.getDateOfOrder().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
        .isEqualTo(LocalDate.of(2025, 2, 1));
    assertThat(request.getCourtAssessmentStatus()).isEqualTo("ASSESSED");
    assertThat(request.getPreCertificateLscCost()).isEqualByComparingTo("10.10");
    assertThat(request.getPreCertificateOtherCost()).isEqualByComparingTo("20.20");
    assertThat(request.getCertificateCostLsc()).isEqualByComparingTo("30.30");
    assertThat(request.getCertificateCostMarket()).isEqualByComparingTo("40.40");
    assertThat(request.getTotalCertCostsAwarded()).isEqualByComparingTo("70.70");
    assertThat(request.getAwardAmount()).isEqualByComparingTo("101.00");
    assertThat(request.getAwardedBy()).isEqualTo("COURT");
    assertThat(request.getInterestAwardedRate()).isEqualByComparingTo("8.5");
    assertThat(
            request.getInterestStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
        .isEqualTo(LocalDate.of(2025, 2, 2));
    assertThat(
            request.getOrderServedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
        .isEqualTo(LocalDate.of(2025, 2, 3));
    assertThat(request.getAddressLine1()).isEqualTo("1 High Street");
    assertThat(request.getAddressLine2()).isEqualTo("London");
    assertThat(request.getAddressLine3()).isEqualTo("England");
    assertThat(request.getOtherDetails()).isEqualTo("Other details");
  }

  @Test
  void mapsCostAwardDetailToFormData() {
    final CostAwardDetail award =
        new CostAwardDetail()
            .id(7)
            .awardType("COST")
            .description("Cost")
            .awardCode("COST_AGR")
            .dateOfOrder(DateUtils.convertToDate("01/02/2025"))
            .courtAssessmentStatus("ASSESSED")
            .preCertificateLscCost(new BigDecimal("10.10"))
            .preCertificateOtherCost(new BigDecimal("20.20"))
            .certificateCostLsc(new BigDecimal("30.30"))
            .certificateCostMarket(new BigDecimal("40.40"))
            .awardedBy("COURT")
            .interestAwardedRate(new BigDecimal("8.5"))
            .interestStartDate(DateUtils.convertToDate("02/02/2025"))
            .orderServedDate(DateUtils.convertToDate("03/02/2025"))
            .addressLine1("1 High Street")
            .addressLine2("London")
            .addressLine3("England")
            .otherDetails("Other details");

    final CostAwardFormData form = mapper.toCostAwardFormData(award);

    assertThat(form.getId()).isEqualTo(7);
    assertThat(form.getAwardType()).isEqualTo("COST");
    assertThat(form.getDescription()).isEqualTo("Cost");
    assertThat(form.getAwardCode()).isEqualTo("COST_AGR");
    assertThat(form.getDateOfOrder()).isEqualTo("01/02/2025");
    assertThat(form.getCourtAssessmentStatus()).isEqualTo("ASSESSED");
    assertThat(form.getLaaFundedLegalCosts()).isEqualTo("10.10");
    assertThat(form.getOtherPreCertificateCosts()).isEqualTo("20.20");
    assertThat(form.getLaaRate()).isEqualTo("30.30");
    assertThat(form.getMarketRate()).isEqualTo("40.40");
    assertThat(form.getAwardedBy()).isEqualTo("COURT");
    assertThat(form.getInterestRate()).isEqualTo("8.5");
    assertThat(form.getInterestStartDate()).isEqualTo("02/02/2025");
    assertThat(form.getOrderServedDate()).isEqualTo("03/02/2025");
    assertThat(form.getAddressLine1()).isEqualTo("1 High Street");
    assertThat(form.getAddressLine2()).isEqualTo("London");
    assertThat(form.getAddressLine3()).isEqualTo("England");
    assertThat(form.getOtherDetails()).isEqualTo("Other details");
  }

  @Test
  void defaultsBlankAmountsToZeroWhenMappingForPersistence() {
    final CostAwardFormData form = new CostAwardFormData();
    form.setAwardType("COST");

    final CostAwardDetail request = mapper.toCostAward(form);

    assertThat(request.getPreCertificateLscCost()).isEqualByComparingTo("0.00");
    assertThat(request.getPreCertificateOtherCost()).isEqualByComparingTo("0.00");
    assertThat(request.getCertificateCostLsc()).isEqualByComparingTo("0.00");
    assertThat(request.getCertificateCostMarket()).isEqualByComparingTo("0.00");
    assertThat(request.getTotalCertCostsAwarded()).isEqualByComparingTo("0.00");
    assertThat(request.getAwardAmount()).isEqualByComparingTo("0.00");
  }

  @Test
  void updateCostAward_preservesNonFormFieldsOnExistingAward() {
    final CostAwardFormData form = new CostAwardFormData();
    form.setAwardType("TAMPERED");
    form.setDescription("Changed");
    form.setAwardCode("WRONG");
    form.setCourtAssessmentStatus("ASSESSED");
    form.setLaaFundedLegalCosts("10.10");
    form.setOtherPreCertificateCosts("20.20");
    form.setLaaRate("30.30");
    form.setMarketRate("40.40");

    final CostAwardDetail existingAward =
        new CostAwardDetail()
            .id(7)
            .awardType("COST")
            .description("Cost")
            .awardCode("COST_AGR")
            .ebsId("ebs-1");

    mapper.updateCostAward(form, existingAward);

    assertThat(existingAward.getId()).isEqualTo(7);
    assertThat(existingAward.getAwardType()).isEqualTo("COST");
    assertThat(existingAward.getDescription()).isEqualTo("Cost");
    assertThat(existingAward.getAwardCode()).isEqualTo("COST_AGR");
    assertThat(existingAward.getEbsId()).isEqualTo("ebs-1");
    assertThat(existingAward.getCourtAssessmentStatus()).isEqualTo("ASSESSED");
    assertThat(existingAward.getPreCertificateLscCost()).isEqualByComparingTo("10.10");
    assertThat(existingAward.getAwardAmount()).isEqualByComparingTo("101.00");
  }
}
