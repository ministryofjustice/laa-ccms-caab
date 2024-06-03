package uk.gov.laa.ccms.caab.mapper.context;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

/**
 * Context class for mapping assessment details.
 */
@Builder
@Getter
@Setter
public class AssessmentMappingContext {

  private AssessmentDetail assessment;
  private ApplicationDetail application;
  private ClientDetail client;
  private UserDetail user;
  private List<AssessmentOpponentMappingContext> opponentContext;

}
