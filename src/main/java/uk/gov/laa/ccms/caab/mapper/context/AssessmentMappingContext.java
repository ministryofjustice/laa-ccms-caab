package uk.gov.laa.ccms.caab.mapper.context;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
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
}
