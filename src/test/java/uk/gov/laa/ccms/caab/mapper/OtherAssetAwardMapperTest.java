package uk.gov.laa.ccms.caab.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.gov.laa.ccms.caab.bean.award.OtherAssetAwardFormData;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardDetail;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardRequest;
import uk.gov.laa.ccms.caab.util.DateUtils;

class OtherAssetAwardMapperTest {

  private final OtherAssetAwardMapper mapper = Mappers.getMapper(OtherAssetAwardMapper.class);

  @Test
  void mapsFormDataToGeneratedApiRequest() {
    final OtherAssetAwardFormData form = validForm();

    final OtherAssetAwardRequest request = mapper.toOtherAssetAwardRequest(form);

    assertThat(request.getAwardType()).isEqualTo("ASSET");
    assertThat(request.getDescription()).isEqualTo("Antique jewellery");
    assertThat(request.getAwardCode()).isEqualTo("OTH_ASSET");
    assertThat(request.getDateOfOrder().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
        .isEqualTo(LocalDate.of(2025, 2, 1));
    assertThat(request.getAwardedBy()).isEqualTo("COURT");
    assertThat(request.getValuationAmount()).isEqualByComparingTo("1000.50");
    assertThat(request.getValuationCriteria()).isNull();
    assertThat(request.getValuationDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
        .isEqualTo(LocalDate.of(2025, 2, 2));
    assertThat(request.getAwardedPercentage()).isEqualByComparingTo("75.25");
    assertThat(request.getRecoveredAmount()).isEqualByComparingTo("100.00");
    assertThat(request.getRecoveredPercentage()).isEqualByComparingTo("10.00");
    assertThat(request.getDisputedAmount()).isEqualByComparingTo("200.00");
    assertThat(request.getAwardedAmount()).isEqualByComparingTo("750.38");
    assertThat(request.getDisputedPercentage()).isEqualByComparingTo("20.00");
    assertThat(request.getRecovery()).isEqualTo("Recovery details");
    assertThat(request.getNoRecoveryDetails()).isEqualTo("No recovery details");
    assertThat(request.getStatutoryChargeExemptReason()).isEqualTo("Exemption reason");
    assertThat(request.getRecoveryOfAwardTimeRelated()).isTrue();
  }

  @Test
  void mapsOtherAssetAwardDetailToFormData() {
    final OtherAssetAwardDetail award =
        new OtherAssetAwardDetail()
            .id(7)
            .awardType("ASSET")
            .description("Antique jewellery")
            .awardCode("OTH_ASSET")
            .dateOfOrder(DateUtils.convertToDate("01/02/2025"))
            .awardedBy("COURT")
            .valuationAmount(new BigDecimal("1000.50"))
            .valuationDate(DateUtils.convertToDate("02/02/2025"))
            .awardedPercentage(new BigDecimal("75.25"))
            .recoveredAmount(new BigDecimal("100.00"))
            .recoveredPercentage(new BigDecimal("10.00"))
            .disputedAmount(new BigDecimal("200.00"))
            .awardedAmount(new BigDecimal("750.38"))
            .disputedPercentage(new BigDecimal("20.00"))
            .recovery("Recovery details")
            .noRecoveryDetails("No recovery details")
            .statutoryChargeExemptReason("Exemption reason")
            .recoveryOfAwardTimeRelated(false);

    final OtherAssetAwardFormData form = mapper.toOtherAssetAwardFormData(award);

    assertThat(form.getId()).isEqualTo(7);
    assertThat(form.getAwardType()).isEqualTo("ASSET");
    assertThat(form.getDescription()).isEqualTo("Antique jewellery");
    assertThat(form.getAwardCode()).isEqualTo("OTH_ASSET");
    assertThat(form.getDateOfOrder()).isEqualTo("01/02/2025");
    assertThat(form.getAwardedBy()).isEqualTo("COURT");
    assertThat(form.getValuationAmount()).isEqualTo("1000.50");
    assertThat(form.getValuationDate()).isEqualTo("02/02/2025");
    assertThat(form.getAwardedPercentage()).isEqualTo("75.25");
    assertThat(form.getRecoveredAmount()).isEqualTo("100.00");
    assertThat(form.getRecoveredPercentage()).isEqualTo("10.00");
    assertThat(form.getDisputedAmount()).isEqualTo("200.00");
    assertThat(form.getAwardedAmount()).isEqualTo("750.38");
    assertThat(form.getDisputedPercentage()).isEqualTo("20.00");
    assertThat(form.getRecovery()).isEqualTo("Recovery details");
    assertThat(form.getNoRecoveryDetails()).isEqualTo("No recovery details");
    assertThat(form.getStatutoryChargeExemptReason()).isEqualTo("Exemption reason");
    assertThat(form.getRecoveryOfAwardTimeRelated()).isFalse();
  }

  @Test
  void mapsBlankOptionalValuesToNull() {
    final OtherAssetAwardFormData form = new OtherAssetAwardFormData();
    form.setAwardType("ASSET");
    form.setDateOfOrder("");
    form.setValuationAmount(" ");
    form.setValuationDate(null);

    final OtherAssetAwardRequest request = mapper.toOtherAssetAwardRequest(form);

    assertThat(request.getDateOfOrder()).isNull();
    assertThat(request.getValuationAmount()).isNull();
    assertThat(request.getValuationDate()).isNull();
  }

  private OtherAssetAwardFormData validForm() {
    final OtherAssetAwardFormData form = new OtherAssetAwardFormData();
    form.setAwardType("ASSET");
    form.setDescription("Antique jewellery");
    form.setAwardCode("OTH_ASSET");
    form.setDateOfOrder("01/02/2025");
    form.setAwardedBy("COURT");
    form.setValuationAmount("1000.50");
    form.setValuationDate("02/02/2025");
    form.setAwardedPercentage("75.25");
    form.setRecoveredAmount("100.00");
    form.setRecoveredPercentage("10.00");
    form.setDisputedAmount("200.00");
    form.setAwardedAmount("750.38");
    form.setDisputedPercentage("20.00");
    form.setRecovery("Recovery details");
    form.setNoRecoveryDetails("No recovery details");
    form.setStatutoryChargeExemptReason("Exemption reason");
    form.setRecoveryOfAwardTimeRelated(true);
    return form;
  }
}
