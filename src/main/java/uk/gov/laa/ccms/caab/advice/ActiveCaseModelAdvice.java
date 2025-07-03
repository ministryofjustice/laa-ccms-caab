package uk.gov.laa.ccms.caab.advice;

import static uk.gov.laa.ccms.caab.constants.SessionConstants.ACTIVE_CASE;
import static uk.gov.laa.ccms.caab.constants.SessionConstants.CASE;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import uk.gov.laa.ccms.caab.bean.ActiveCase;
import uk.gov.laa.ccms.caab.controller.AssessmentController;
import uk.gov.laa.ccms.caab.controller.application.CaseController;
import uk.gov.laa.ccms.caab.controller.application.section.ApplicationSectionsController;
import uk.gov.laa.ccms.caab.controller.application.section.ApplicationSubmissionController;
import uk.gov.laa.ccms.caab.controller.application.section.ApplicationTypeSectionController;
import uk.gov.laa.ccms.caab.controller.application.section.EditGeneralDetailsSectionController;
import uk.gov.laa.ccms.caab.controller.application.section.EditProceedingsAndCostsSectionController;
import uk.gov.laa.ccms.caab.controller.application.section.EvidenceSectionController;
import uk.gov.laa.ccms.caab.controller.application.section.OpponentsSectionController;
import uk.gov.laa.ccms.caab.controller.application.section.ProviderDetailsSectionController;
import uk.gov.laa.ccms.caab.controller.client.EditClientAddressDetailsController;
import uk.gov.laa.ccms.caab.controller.client.EditClientAddressDetailsSearchController;
import uk.gov.laa.ccms.caab.controller.client.EditClientBasicDetailsController;
import uk.gov.laa.ccms.caab.controller.client.EditClientContactDetailsController;
import uk.gov.laa.ccms.caab.controller.client.EditClientDeceasedDetailsController;
import uk.gov.laa.ccms.caab.controller.client.EditClientEqualOpportunitiesMonitoringDetailsController;
import uk.gov.laa.ccms.caab.controller.client.EditClientSummaryController;
import uk.gov.laa.ccms.caab.controller.submission.CaseSubmissionController;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ClientDetail;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;

/**
 * Controller advice class responsible for adding active case to the model of selected controllers.
 * Adding it to the model will amend the header bar with case details.
 */
@Slf4j
@ControllerAdvice(
    assignableTypes = {
      ApplicationSectionsController.class,
      ApplicationSubmissionController.class,
      ApplicationTypeSectionController.class,
      ProviderDetailsSectionController.class,
      EditClientAddressDetailsController.class,
      EditClientAddressDetailsSearchController.class,
      EditClientBasicDetailsController.class,
      EditClientContactDetailsController.class,
      EditClientDeceasedDetailsController.class,
      EditClientEqualOpportunitiesMonitoringDetailsController.class,
      EditClientSummaryController.class,
      EditGeneralDetailsSectionController.class,
      EditProceedingsAndCostsSectionController.class,
      OpponentsSectionController.class,
      EvidenceSectionController.class,
      AssessmentController.class,
      CaseSubmissionController.class,
      CaseController.class
    })
public class ActiveCaseModelAdvice {

  /**
   * Controller advice method responsible for adding active case to the model of selected
   * controllers.
   *
   * @param model the model view to be updated
   * @param session the session data
   */
  @ModelAttribute
  public void addActiveCaseToModel(final Model model, final HttpSession session) {
    if (session.getAttribute(ACTIVE_CASE) != null) {
      model.addAttribute(session.getAttribute(ACTIVE_CASE));
    } else if (session.getAttribute(CASE) != null) {
      Object sessionCase = session.getAttribute(CASE);
      if (sessionCase instanceof ApplicationDetail ebsCase) {
        final ActiveCase activeCase = buildActiveCaseFromSessionCase(ebsCase);
        model.addAttribute(ACTIVE_CASE, activeCase);
        session.setAttribute(ACTIVE_CASE, activeCase);
      } else {
        log.debug(
            "Case found in session but was incorrect type: '{}'", sessionCase.getClass().getName());
      }
    }
  }

  private ActiveCase buildActiveCaseFromSessionCase(ApplicationDetail ebsCase) {
    String caseReference = ebsCase.getCaseReferenceNumber();

    ClientDetail client = ebsCase.getClient();
    String clientFullName = null;
    String clientReference = null;

    if (client == null) {
      log.debug("Unable to find client for case: '{}'", caseReference);
    } else {
      String clientSurname = client.getSurname();
      clientFullName = client.getFirstName() + (clientSurname.isEmpty() ? "" : " " + clientSurname);

      clientReference = client.getReference();
    }

    ApplicationProviderDetails providerDetails = ebsCase.getProviderDetails();
    String providerCaseReference = null;
    Integer providerId = null;

    if (providerDetails == null) {
      log.debug("Unable to find provider details for case: '{}'", caseReference);
    } else {
      providerCaseReference = providerDetails.getProviderCaseReference();

      IntDisplayValue provider = providerDetails.getProvider();
      if (provider == null) {
        log.debug("Unable to find provider ID for case: '{}'", caseReference);
      } else {
        providerId = provider.getId();
      }
    }

    return ActiveCase.builder()
        .caseReferenceNumber(caseReference)
        .providerId(providerId)
        .client(clientFullName)
        .clientReferenceNumber(clientReference)
        .providerCaseReferenceNumber(providerCaseReference)
        .build();
  }
}
