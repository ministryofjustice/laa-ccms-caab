package uk.gov.laa.ccms.caab.mapper;

import java.math.BigDecimal;
import java.util.Date;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.util.StringUtils;
import uk.gov.laa.ccms.caab.bean.award.LandAwardFormData;
import uk.gov.laa.ccms.caab.model.LandAwardDetail;
import uk.gov.laa.ccms.caab.model.LandAwardRequest;
import uk.gov.laa.ccms.caab.util.DateUtils;

/** Maps land award form values to and from CAAB API models. */
@Mapper(componentModel = "spring")
public interface LandAwardMapper {

  @Mapping(
      target = "recoveryOfAwardTimeRelated",
      source = "recoveryOfAwardTimeRelated",
      qualifiedByName = "toBoolean")
  LandAwardRequest toLandAwardRequest(LandAwardFormData formData);

  @Mapping(target = "dateOfOrder", source = "dateOfOrder", qualifiedByName = "toComponentDate")
  @Mapping(target = "valuationDate", source = "valuationDate", qualifiedByName = "toComponentDate")
  @Mapping(
      target = "recoveryOfAwardTimeRelated",
      source = "recoveryOfAwardTimeRelated",
      qualifiedByName = "toYesNo")
  LandAwardFormData toLandAwardFormData(LandAwardDetail landAwardDetail);

  default Date toDate(final String value) {
    return StringUtils.hasText(value) ? DateUtils.convertToDate(value) : null;
  }

  default BigDecimal toBigDecimal(final String value) {
    return StringUtils.hasText(value) ? new BigDecimal(value) : null;
  }

  default String toString(final BigDecimal value) {
    return value == null ? null : value.toPlainString();
  }

  @Named("toComponentDate")
  default String toComponentDate(final Date value) {
    return value == null ? null : DateUtils.convertToComponentDate(value);
  }

  @Named("toBoolean")
  default Boolean toBoolean(final String value) {
    return StringUtils.hasText(value) ? "Y".equalsIgnoreCase(value) : null;
  }

  @Named("toYesNo")
  default String toYesNo(final Boolean value) {
    return value == null ? null : Boolean.TRUE.equals(value) ? "Y" : "N";
  }
}
