package uk.gov.laa.ccms.caab.bean.award;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/** Editable-only payload for the award-scoped recovery endpoint. */
@Data
public class RecoveryRequestData {
  @JsonProperty("solicitor_recovery_amount")
  private BigDecimal solicitorRecoveryAmount;

  @JsonProperty("solicitor_recovery_date")
  private Date solicitorRecoveryDate;

  @JsonProperty("solicitor_amount_paid_to_lsc")
  private BigDecimal solicitorAmountPaidToLsc;

  @JsonProperty("court_recovery_amount")
  private BigDecimal courtRecoveryAmount;

  @JsonProperty("court_recovery_date")
  private Date courtRecoveryDate;

  @JsonProperty("court_amount_paid_to_lsc")
  private BigDecimal courtAmountPaidToLsc;

  @JsonProperty("client_recovery_amount")
  private BigDecimal clientRecoveryAmount;

  @JsonProperty("client_recovery_date")
  private Date clientRecoveryDate;

  @JsonProperty("client_amount_paid_to_lsc")
  private BigDecimal clientAmountPaidToLsc;

  @JsonProperty("leave_of_court_required_ind")
  private Boolean leaveOfCourtRequiredInd;

  @JsonProperty("offered_amount")
  private BigDecimal offeredAmount;

  @JsonProperty("offer_details")
  private String offerDetails;
}
