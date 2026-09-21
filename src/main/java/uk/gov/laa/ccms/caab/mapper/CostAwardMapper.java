package uk.gov.laa.ccms.caab.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.util.StringUtils;
import uk.gov.laa.ccms.caab.bean.award.CostAwardFormData;
import uk.gov.laa.ccms.caab.model.CostAwardDetail;
import uk.gov.laa.ccms.caab.util.DateUtils;

/** Maps cost award form values to and from the CAAB API model. */
@Mapper(componentModel = "spring")
public interface CostAwardMapper {

  @Mapping(
      target = "preCertificateLscCost",
      source = "laaFundedLegalCosts",
      qualifiedByName = "toBigDecimalOrZero")
  @Mapping(
      target = "preCertificateOtherCost",
      source = "otherPreCertificateCosts",
      qualifiedByName = "toBigDecimalOrZero")
  @Mapping(
      target = "certificateCostLsc",
      source = "laaRate",
      qualifiedByName = "toBigDecimalOrZero")
  @Mapping(
      target = "certificateCostMarket",
      source = "marketRate",
      qualifiedByName = "toBigDecimalOrZero")
  @Mapping(
      target = "interestAwardedRate",
      source = "interestRate",
      qualifiedByName = "toBigDecimal")
  @Mapping(
      target = "totalCertCostsAwarded",
      source = ".",
      qualifiedByName = "toTotalCertificateCosts")
  @Mapping(target = "awardAmount", source = ".", qualifiedByName = "toTotalAwardAmount")
  @Mapping(target = "updateAllowed", ignore = true)
  @Mapping(target = "deleteAllowed", ignore = true)
  @Mapping(target = "ebsId", ignore = true)
  @Mapping(target = "effectiveDate", ignore = true)
  @Mapping(target = "costOrFinancial", ignore = true)
  @Mapping(target = "opponentsToSelect", ignore = true)
  @Mapping(target = "liableParties", ignore = true)
  @Mapping(target = "auditTrail", ignore = true)
  @Mapping(target = "recovery", ignore = true)
  CostAwardDetail toCostAward(CostAwardFormData formData);

  @Mapping(target = "dateOfOrder", source = "dateOfOrder", qualifiedByName = "toComponentDate")
  @Mapping(
      target = "laaFundedLegalCosts",
      source = "preCertificateLscCost",
      qualifiedByName = "toMonetaryComponent")
  @Mapping(
      target = "otherPreCertificateCosts",
      source = "preCertificateOtherCost",
      qualifiedByName = "toMonetaryComponent")
  @Mapping(
      target = "laaRate",
      source = "certificateCostLsc",
      qualifiedByName = "toMonetaryComponent")
  @Mapping(
      target = "marketRate",
      source = "certificateCostMarket",
      qualifiedByName = "toMonetaryComponent")
  @Mapping(
      target = "interestRate",
      source = "interestAwardedRate",
      qualifiedByName = "toComponentAmount")
  @Mapping(
      target = "interestStartDate",
      source = "interestStartDate",
      qualifiedByName = "toComponentDate")
  @Mapping(
      target = "orderServedDate",
      source = "orderServedDate",
      qualifiedByName = "toComponentDate")
  CostAwardFormData toCostAwardFormData(CostAwardDetail costAwardDetail);

  default Date toDate(final String value) {
    return StringUtils.hasText(value) ? DateUtils.convertToDate(value) : null;
  }

  @Named("toBigDecimal")
  default BigDecimal toBigDecimal(final String value) {
    return StringUtils.hasText(value) ? new BigDecimal(value) : null;
  }

  @Named("toBigDecimalOrZero")
  default BigDecimal toBigDecimalOrZero(final String value) {
    return StringUtils.hasText(value)
        ? new BigDecimal(value).setScale(2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
  }

  @Named("toComponentDate")
  default String toComponentDate(final Date value) {
    return value == null ? null : DateUtils.convertToComponentDate(value);
  }

  @Named("toMonetaryComponent")
  default String toMonetaryComponent(final BigDecimal value) {
    return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  @Named("toComponentAmount")
  default String toComponentAmount(final BigDecimal value) {
    return value == null ? null : value.stripTrailingZeros().toPlainString();
  }

  @Named("toTotalCertificateCosts")
  default BigDecimal toTotalCertificateCosts(final CostAwardFormData formData) {
    return toBigDecimalOrZero(formData.getLaaRate())
        .add(toBigDecimalOrZero(formData.getMarketRate()));
  }

  @Named("toTotalAwardAmount")
  default BigDecimal toTotalAwardAmount(final CostAwardFormData formData) {
    return toBigDecimalOrZero(formData.getLaaFundedLegalCosts())
        .add(toBigDecimalOrZero(formData.getOtherPreCertificateCosts()))
        .add(toBigDecimalOrZero(formData.getLaaRate()))
        .add(toBigDecimalOrZero(formData.getMarketRate()));
  }
}
