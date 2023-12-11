package uk.gov.laa.ccms.caab.builders;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE_DISPLAY;

import java.util.List;
import java.util.Optional;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.Client;
import uk.gov.laa.ccms.caab.model.DevolvedPowers;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.BaseOffice;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetail;

/**
 * Helper class for constructing an {@link ApplicationDetail} instance using a builder pattern.
 */
public class ApplicationBuilder {

  private final ApplicationDetail application;

  public ApplicationBuilder() {
    this.application = new ApplicationDetail(null, null, null, null);
  }

  public ApplicationBuilder(ApplicationDetail application) {
    this.application = application;
  }

  /**
   * Sets the application type.
   *
   * @param applicationType The application type of the application.
   * @return The builder instance.
   */
  public ApplicationBuilder applicationType(
          final ApplicationType applicationType) {
    application.setApplicationType(applicationType);
    return this;
  }

  /**
   * Sets the case reference.
   *
   * @param caseReferenceSummary The case reference summary.
   * @return The builder instance.
   */
  public ApplicationBuilder caseReference(final CaseReferenceSummary caseReferenceSummary) {
    String caseReference = Optional.ofNullable(caseReferenceSummary.getCaseReferenceNumber())
            .orElseThrow(() -> new RuntimeException(
                    "No case reference number was created, unable to continue"));
    application.setCaseReferenceNumber(caseReference);
    return this;
  }


  /**
   * Sets the provider details.
   *
   * @param user The user detail.
   * @return The builder instance.
   */
  public ApplicationBuilder provider(final UserDetail user) {
    IntDisplayValue provider = new IntDisplayValue()
        .id(user.getProvider().getId())
        .displayValue(user.getProvider().getName());
    application.setProvider(provider);
    return this;
  }

  /**
   * Sets the client details.
   *
   * @param clientInformation The client information.
   * @return The builder instance.
   */
  public ApplicationBuilder client(final ClientDetail clientInformation) {
    Client client = new Client()
            .firstName(clientInformation.getDetails().getName().getFirstName())
            .surname(clientInformation.getDetails().getName().getSurname())
            .reference(clientInformation.getClientReferenceNumber());
    application.setClient(client);
    return this;
  }

  /**
   * Sets the category of law.
   *
   * @param categoryOfLawId           The category of law ID.
   * @param categoryOfLawLookup The category of law lookup value.
   * @return The builder instance.
   */
  public ApplicationBuilder categoryOfLaw(
          final String categoryOfLawId,
          final CategoryOfLawLookupValueDetail categoryOfLawLookup) {
    String categoryOfLawDisplayValue = Optional.ofNullable(categoryOfLawLookup)
        .map(CategoryOfLawLookupValueDetail::getMatterTypeDescription)
        .orElse(categoryOfLawId);

    StringDisplayValue categoryOfLaw = new StringDisplayValue()
            .id(categoryOfLawId)
            .displayValue(categoryOfLawDisplayValue);
    application.setCategoryOfLaw(categoryOfLaw);
    return this;
  }

  /**
   * Sets the office details.
   *
   * @param officeId The office ID.
   * @param offices  The list of office details.
   * @return The builder instance.
   */
  public ApplicationBuilder office(final Integer officeId, final List<BaseOffice> offices) {
    String officeDisplayValue = offices.stream()
            .filter(office -> officeId.equals(office.getId()))
            .map(BaseOffice::getName)
            .findFirst()
            .orElse(null);

    IntDisplayValue office = new IntDisplayValue()
            .id(officeId)
            .displayValue(officeDisplayValue);
    application.setOffice(office);
    return this;
  }

  /**
   * Sets the contractual Devolved Power flag.
   *
   * @param contractDetails a list of contract details.
   * @param categoryOfLawId the id of the category of law type.
   * @return The builder instance.
   */
  public ApplicationBuilder contractualDevolvedPower(
      final List<ContractDetail> contractDetails,
      final String categoryOfLawId) {

    String contractualDevolvedPower = contractDetails != null ? contractDetails.stream()
        .filter(contract -> categoryOfLawId
            .equals(contract.getCategoryofLaw()))
        .map(ContractDetail::getContractualDevolvedPowers)
        .findFirst()
        .orElse(null)
        : null;

    if (application.getApplicationType() == null) {
      application.setApplicationType(new ApplicationType());
    }
    if (application.getApplicationType().getDevolvedPowers() == null) {
      application.getApplicationType().setDevolvedPowers(new DevolvedPowers());
    }

    // Set the contractFlag
    application.getApplicationType().getDevolvedPowers().setContractFlag(contractualDevolvedPower);

    return this;
  }

  /**
   * Sets the LAR scope flag.
   *
   * @param amendmentTypes The amendment type details.
   * @return The builder instance.
   */
  public ApplicationBuilder larScopeFlag(final AmendmentTypeLookupDetail amendmentTypes) {

    String defaultLarScopeFlag = Optional.ofNullable(amendmentTypes.getContent())
            .filter(content -> !content.isEmpty())
            .map(content -> content.get(0))
            .map(AmendmentTypeLookupValueDetail::getDefaultLarScopeFlag)
            .orElseThrow(() -> new RuntimeException(
                    "No amendment type available, unable to continue"));

    application.setLarScopeFlag("Y".equalsIgnoreCase(defaultLarScopeFlag));
    return this;
  }

  /**
   * Sets the status details.
   *
   * @return The builder instance.
   */
  public ApplicationBuilder status() {
    StringDisplayValue status = new StringDisplayValue()
            .id(STATUS_UNSUBMITTED_ACTUAL_VALUE)
            .displayValue(STATUS_UNSUBMITTED_ACTUAL_VALUE_DISPLAY);
    application.setStatus(status);
    return this;
  }

  /**
   * Finalizes and returns the constructed ApplicationDetail instance.
   *
   * @return The constructed ApplicationDetail.
   */
  public ApplicationDetail build() {
    return application;
  }

}
