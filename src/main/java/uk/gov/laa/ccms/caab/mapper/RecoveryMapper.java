package uk.gov.laa.ccms.caab.mapper;

import java.math.BigDecimal;
import java.util.Date;
import org.mapstruct.Mapper;
import org.springframework.util.StringUtils;
import uk.gov.laa.ccms.caab.bean.award.RecoveryFormData;
import uk.gov.laa.ccms.caab.bean.award.RecoveryRequestData;
import uk.gov.laa.ccms.caab.model.RecoveryDetail;
import uk.gov.laa.ccms.caab.util.DateUtils;

/** Maps validated recovery inputs, retaining the API's historical LSC field names. */
@Mapper(componentModel = "spring")
public interface RecoveryMapper {
  RecoveryRequestData toRequest(RecoveryFormData form);

  RecoveryFormData toForm(RecoveryDetail recovery);

  default BigDecimal toAmount(final String value) {
    return StringUtils.hasText(value)
        ? new BigDecimal(value).setScale(2)
        : BigDecimal.ZERO.setScale(2);
  }

  default String toAmountString(final BigDecimal value) {
    return value == null ? "0.00" : value.toPlainString();
  }

  default Date toDate(final String value) {
    return StringUtils.hasText(value) ? DateUtils.convertToDate(value) : null;
  }

  default String toDateString(final Date value) {
    return value == null ? null : DateUtils.convertToComponentDate(value);
  }
}
