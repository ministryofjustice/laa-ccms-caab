package uk.gov.laa.ccms.caab.mapper;

import java.math.BigDecimal;
import java.util.Date;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.util.StringUtils;
import uk.gov.laa.ccms.caab.bean.award.FinancialAwardFormData;
import uk.gov.laa.ccms.caab.model.FinancialAwardDetail;
import uk.gov.laa.ccms.caab.model.FinancialAwardRequest;
import uk.gov.laa.ccms.caab.util.DateUtils;

/** Maps financial award form values to the CAAB API request model. */
@Mapper(componentModel = "spring")
public interface FinancialAwardMapper {

  FinancialAwardRequest toFinancialAwardRequest(FinancialAwardFormData formData);

  @Mapping(target = "dateOfOrder", source = "dateOfOrder", qualifiedByName = "toComponentDate")
  @Mapping(
      target = "awardAmount",
      source = "financialAwardDetail",
      qualifiedByName = "toAwardAmount")
  @Mapping(
      target = "orderServedDate",
      source = "orderServedDate",
      qualifiedByName = "toComponentDate")
  FinancialAwardFormData toFinancialAwardFormData(FinancialAwardDetail financialAwardDetail);

  default Date toDate(final String value) {
    return StringUtils.hasText(value) ? DateUtils.convertToDate(value) : null;
  }

  default BigDecimal toBigDecimal(final String value) {
    return StringUtils.hasText(value) ? new BigDecimal(value) : null;
  }

  @Named("toComponentDate")
  default String toComponentDate(final Date value) {
    return value == null ? null : DateUtils.convertToComponentDate(value);
  }

  @Named("toAwardAmount")
  default String toAwardAmount(final FinancialAwardDetail financialAwardDetail) {
    final BigDecimal amount =
        financialAwardDetail.getAwardAmount() == null
            ? financialAwardDetail.getAmount()
            : financialAwardDetail.getAwardAmount();
    return amount == null ? null : amount.toPlainString();
  }
}
