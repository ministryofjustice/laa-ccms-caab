package uk.gov.laa.ccms.caab.mapper.context;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
import uk.gov.laa.ccms.data.model.ContactDetail;
import uk.gov.laa.ccms.data.model.OfficeDetail;
import uk.gov.laa.ccms.data.model.ProviderDetail;
import uk.gov.laa.ccms.soa.gateway.model.AssessmentResult;
import uk.gov.laa.ccms.soa.gateway.model.CaseDetail;

/**
 * Class to hold a SOA CaseDetail together with all additional data needed to perform a mapping to a
 * CAAB ApplicationDetail.
 */
@Builder
@Getter
public class SoaApplicationMappingContext {

  /** The CaseDetail which will be mapped to an ApplicationDetail. */
  CaseDetail soaCaseDetail;

  /** Lookup of additional application type data for this Case. */
  CommonLookupValueDetail applicationType;

  /** Lookup of certificate display value. */
  CommonLookupValueDetail certificate;

  /** Lookup of additional provider data for this Case. */
  ProviderDetail providerDetail;

  /** Lookup of provider Office. */
  OfficeDetail providerOffice;

  /** Lookup of the contact details for the Supervisor. */
  ContactDetail supervisorContact;

  /** Lookup of the contact details for the Fee Earner. */
  ContactDetail feeEarnerContact;

  /**
   * A subset of the Proceedings from the Case, wrapped by a SoaProceedingMappingContext with
   * further lookup data. A Proceeding is added to this list if its status is DRAFT, AND all
   * Proceedings in the Case are not at status DRAFT.
   */
  List<SoaProceedingMappingContext> amendmentProceedingsInEbs;

  /**
   * A subset of the Proceedings from the Case, wrapped by a SoaProceedingMappingContext containing
   * all required lookup data for mapping. A Proceeding is added to this list if either its status
   * is not DRAFT, OR all Proceedings in the Case are at status DRAFT.
   */
  List<SoaProceedingMappingContext> proceedings;

  /**
   * The prior authorities for the Case, wrapped by a SoaPriorAuthorityMappingContext containing all
   * required lookup data for mapping.
   */
  List<SoaPriorAuthorityMappingContext> priorAuthorities;

  /** All the required Case data to map to a CAAB CaseOutcome. */
  SoaCaseOutcomeMappingContext caseOutcome;

  /** Flag to indicate if this Case only has Proceedings at status DRAFT. */
  Boolean caseWithOnlyDraftProceedings;

  /**
   * A flag to indicate whether devolved powers are in use for this Case, together with the Date
   * that the powers were used.
   */
  Pair<Boolean, Date> devolvedPowers;

  /** A calculation of currentProviderBilledAmount - totalProviderAmount. */
  BigDecimal currentProviderBilledAmount;

  /** The most recent means Assessment for the Case. */
  AssessmentResult meansAssessment;

  /** The most recent merits Assessment for the Case. */
  AssessmentResult meritsAssessment;
}
