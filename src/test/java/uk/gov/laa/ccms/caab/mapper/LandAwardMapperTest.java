package uk.gov.laa.ccms.caab.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.gov.laa.ccms.caab.bean.award.LandAwardFormData;
import uk.gov.laa.ccms.caab.model.LandAwardDetail;
import uk.gov.laa.ccms.caab.model.LandAwardRequest;
import uk.gov.laa.ccms.caab.util.DateUtils;

class LandAwardMapperTest {

  private final LandAwardMapper mapper = Mappers.getMapper(LandAwardMapper.class);

  @Test
  void mapsFormDataToGeneratedApiRequest() {
    final LandAwardFormData form = formData();

    final LandAwardRequest request = mapper.toLandAwardRequest(form);

    assertThat(request.getAwardType()).isEqualTo("LAND");
    assertThat(request.getAwardCode()).isEqualTo("LAND");
    assertThat(request.getDescription()).isEqualTo("Land award");
    assertThat(request.getDateOfOrder().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
        .isEqualTo(LocalDate.of(2025, 2, 1));
    assertThat(request.getValuationAmount()).isEqualByComparingTo("250000.00");
    assertThat(request.getValuationDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
        .isEqualTo(LocalDate.of(2025, 2, 2));
    assertThat(request.getDisputedPercentage()).isEqualByComparingTo("50.00");
    assertThat(request.getAwardedPercentage()).isEqualByComparingTo("25.00");
    assertThat(request.getMortgageAmountDue()).isEqualByComparingTo("100000.00");
    assertThat(request.getRecoveryOfAwardTimeRelated()).isTrue();
  }

  @Test
  void mapsLandAwardDetailToFormData() {
    final LandAwardDetail award =
        new LandAwardDetail()
            .id(7)
            .awardType("LAND")
            .awardCode("LAND")
            .description("Land award")
            .dateOfOrder(DateUtils.convertToDate("01/02/2025"))
            .valuationAmount(new BigDecimal("250000.00"))
            .valuationDate(DateUtils.convertToDate("02/02/2025"))
            .disputedPercentage(new BigDecimal("50.00"))
            .awardedPercentage(new BigDecimal("25.00"))
            .mortgageAmountDue(new BigDecimal("100000.00"))
            .equity(new BigDecimal("150000.00"))
            .recoveryOfAwardTimeRelated(false);

    final LandAwardFormData form = mapper.toLandAwardFormData(award);

    assertThat(form.getId()).isEqualTo(7);
    assertThat(form.getDateOfOrder()).isEqualTo("01/02/2025");
    assertThat(form.getValuationAmount()).isEqualTo("250000.00");
    assertThat(form.getValuationDate()).isEqualTo("02/02/2025");
    assertThat(form.getDisputedPercentage()).isEqualTo("50.00");
    assertThat(form.getAwardedPercentage()).isEqualTo("25.00");
    assertThat(form.getMortgageAmountDue()).isEqualTo("100000.00");
    assertThat(form.getEquity()).isEqualTo("150000.00");
    assertThat(form.getRecoveryOfAwardTimeRelated()).isEqualTo("N");
  }

  private LandAwardFormData formData() {
    final LandAwardFormData form = new LandAwardFormData();
    form.setAwardType("LAND");
    form.setAwardCode("LAND");
    form.setDescription("Land award");
    form.setDateOfOrder("01/02/2025");
    form.setValuationAmount("250000.00");
    form.setValuationDate("02/02/2025");
    form.setDisputedPercentage("50.00");
    form.setAwardedPercentage("25.00");
    form.setMortgageAmountDue("100000.00");
    form.setEquity("150000.00");
    form.setRecoveryOfAwardTimeRelated("Y");
    return form;
  }
}
