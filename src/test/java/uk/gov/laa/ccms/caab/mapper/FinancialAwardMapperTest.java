package uk.gov.laa.ccms.caab.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.gov.laa.ccms.caab.bean.award.FinancialAwardFormData;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardRequest;
import uk.gov.laa.ccms.caab.util.DateUtils;

class FinancialAwardMapperTest {

  private final FinancialAwardMapper mapper = Mappers.getMapper(FinancialAwardMapper.class);

  @Test
  void mapsFormDataToGeneratedApiRequest() {
    final FinancialAwardFormData form = new FinancialAwardFormData();
    form.setAwardType("DAMAGE");
    form.setDescription("Financial Settlement");
    form.setAwardCode("DAMAGE_AGR");
    form.setDateOfOrder("01/02/2025");
    form.setAwardAmount("1234.56");
    form.setInterimAward("0");
    form.setAwardedBy("COURT");
    form.setAwardJustifications("Justification");
    form.setOrderServedDate("02/02/2025");
    form.setAddressLine1("1 High Street");
    form.setAddressLine2("London");
    form.setAddressLine3("England");
    form.setStatutoryChargeExemptReason("Reason");
    form.setOtherDetails("Other details");

    final FinancialAwardRequest request = mapper.toFinancialAwardRequest(form);

    assertThat(request.getAwardType()).isEqualTo("DAMAGE");
    assertThat(request.getDescription()).isEqualTo("Financial Settlement");
    assertThat(request.getAwardCode()).isEqualTo("DAMAGE_AGR");
    assertThat(request.getDateOfOrder().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
        .isEqualTo(LocalDate.of(2025, 2, 1));
    assertThat(request.getAwardAmount()).isEqualByComparingTo("1234.56");
    assertThat(request.getInterimAward()).isEqualTo("0");
    assertThat(request.getAwardedBy()).isEqualTo("COURT");
    assertThat(request.getAwardJustifications()).isEqualTo("Justification");
    assertThat(
            request.getOrderServedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
        .isEqualTo(LocalDate.of(2025, 2, 2));
    assertThat(request.getAddressLine1()).isEqualTo("1 High Street");
    assertThat(request.getAddressLine2()).isEqualTo("London");
    assertThat(request.getAddressLine3()).isEqualTo("England");
    assertThat(request.getStatutoryChargeExemptReason()).isEqualTo("Reason");
    assertThat(request.getOtherDetails()).isEqualTo("Other details");
  }

  @Test
  void mapsFinancialAwardDetailToFormData() {
    final FinancialAwardDetail award =
        new FinancialAwardDetail()
            .id(7)
            .awardType("DAMAGE")
            .description("Financial Settlement")
            .awardCode("DAMAGE_AGR")
            .dateOfOrder(DateUtils.convertToDate("01/02/2025"))
            .awardAmount(new BigDecimal("1234.50"))
            .interimAward("0")
            .awardedBy("COURT")
            .awardJustifications("Justification")
            .orderServedDate(DateUtils.convertToDate("02/02/2025"))
            .addressLine1("1 High Street")
            .addressLine2("London")
            .addressLine3("England")
            .statutoryChargeExemptReason("Reason")
            .otherDetails("Other details");

    final FinancialAwardFormData form = mapper.toFinancialAwardFormData(award);

    assertThat(form.getId()).isEqualTo(7);
    assertThat(form.getAwardType()).isEqualTo("DAMAGE");
    assertThat(form.getDescription()).isEqualTo("Financial Settlement");
    assertThat(form.getAwardCode()).isEqualTo("DAMAGE_AGR");
    assertThat(form.getDateOfOrder()).isEqualTo("01/02/2025");
    assertThat(form.getAwardAmount()).isEqualTo("1234.50");
    assertThat(form.getInterimAward()).isEqualTo("0");
    assertThat(form.getAwardedBy()).isEqualTo("COURT");
    assertThat(form.getAwardJustifications()).isEqualTo("Justification");
    assertThat(form.getOrderServedDate()).isEqualTo("02/02/2025");
    assertThat(form.getAddressLine1()).isEqualTo("1 High Street");
    assertThat(form.getAddressLine2()).isEqualTo("London");
    assertThat(form.getAddressLine3()).isEqualTo("England");
    assertThat(form.getStatutoryChargeExemptReason()).isEqualTo("Reason");
    assertThat(form.getOtherDetails()).isEqualTo("Other details");
  }

  @Test
  void mapsLegacyAmountWhenAwardAmountIsMissing() {
    final FinancialAwardDetail award = new FinancialAwardDetail().amount(new BigDecimal("99.95"));

    assertThat(mapper.toFinancialAwardFormData(award).getAwardAmount()).isEqualTo("99.95");
  }
}
