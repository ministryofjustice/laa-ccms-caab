package uk.gov.laa.ccms.caab.mapper.context;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import uk.gov.laa.ccms.caab.assessment.model.AssessmentDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.BaseEvidenceDocumentDetail;
import uk.gov.laa.ccms.data.model.UserDetail;

/**
 * Context for mapping case-related details, including application, assessments, documents,
 * and user information.
 */
@Builder
@Getter
public class CaseMappingContext {

  ApplicationDetail tdsApplication;

  AssessmentDetail meansAssessment;

  AssessmentDetail meritsAssessment;

  List<BaseEvidenceDocumentDetail> caseDocs;

  UserDetail user;

}
