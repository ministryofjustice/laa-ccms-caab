package uk.gov.laa.ccms.caab.mapper.context;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.constants.assessment.AssessmentRulebase;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

/** Context class for mapping assessment details. */
@Builder
@Getter
@Setter
public class AssessmentMappingContext {

  /** The assessment details. */
  private AssessmentDetail assessment;

  /** The application details. */
  private ApplicationDetail application;

  /** The client details. */
  private ClientDetail client;

  /** The user details. */
  private UserDetail user;

  /**
   * The opponent context details, stores the common lookup values for the title of the opponent,
   * and the opponent object.
   */
  private List<AssessmentOpponentMappingContext> opponentContext;

  /**
   * The rulebase the assessment is being built for. It selects the value of {@code
   * POA_OR_BILL_FLAG} and decides whether the billing-only {@code ALLOCATED_COST_LIMIT} is
   * prepopulated, mirroring old PUI's per-rulebase prepopulation maps.
   */
  private AssessmentRulebase rulebase;

  /**
   * The cost limit allocated to the provider, prepopulated into {@code ALLOCATED_COST_LIMIT} for
   * the billing and POA rulebases only.
   */
  private BigDecimal allocatedCostLimit;
}
