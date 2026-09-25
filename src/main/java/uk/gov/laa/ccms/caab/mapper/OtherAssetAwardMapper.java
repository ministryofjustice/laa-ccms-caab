package uk.gov.laa.ccms.caab.mapper;

import java.math.BigDecimal;
import java.util.Date;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.util.StringUtils;
import uk.gov.laa.ccms.caab.bean.award.OtherAssetAwardFormData;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardDetail;
import uk.gov.laa.ccms.caab.model.OtherAssetAwardRequest;
import uk.gov.laa.ccms.caab.util.DateUtils;

/** Maps other asset award form values to and from the CAAB API models. */
@Mapper(componentModel = "spring")
public interface OtherAssetAwardMapper {

  OtherAssetAwardRequest toOtherAssetAwardRequest(OtherAssetAwardFormData formData);

  @Mapping(target = "dateOfOrder", source = "dateOfOrder", qualifiedByName = "toComponentDate")
  @Mapping(target = "valuationDate", source = "valuationDate", qualifiedByName = "toComponentDate")
  @Mapping(target = "valuationAmount", qualifiedByName = "toPlainString")
  @Mapping(target = "awardedPercentage", qualifiedByName = "toPlainString")
  @Mapping(target = "recoveredAmount", qualifiedByName = "toPlainString")
  @Mapping(target = "recoveredPercentage", qualifiedByName = "toPlainString")
  @Mapping(target = "disputedAmount", qualifiedByName = "toPlainString")
  @Mapping(target = "awardedAmount", qualifiedByName = "toPlainString")
  @Mapping(target = "disputedPercentage", qualifiedByName = "toPlainString")
  OtherAssetAwardFormData toOtherAssetAwardFormData(OtherAssetAwardDetail otherAssetAwardDetail);

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

  @Named("toPlainString")
  default String toPlainString(final BigDecimal value) {
    return value == null ? null : value.toPlainString();
  }
}
