package uk.gov.laa.ccms.caab.builders;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE_DISPLAY;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import uk.gov.laa.ccms.caab.model.AddressDetail;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.ClientDetail;
import uk.gov.laa.ccms.caab.model.CostStructureDetail;
import uk.gov.laa.ccms.caab.model.DevolvedPowersDetail;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.BaseOffice;
import uk.gov.laa.ccms.data.model.CategoryOfLawLookupValueDetail;
import uk.gov.laa.ccms.data.model.UserDetail;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.ContractDetail;

/**
 * Helper class for constructing an {@link ApplicationDetail} instance using a builder pattern.
 */
public class InitialApplicationBuilder {

  private final ApplicationDetail application;

  public InitialApplicationBuilder() {
    this.application = new ApplicationDetail().providerDetails(new ApplicationProviderDetails());
  }

  public InitialApplicationBuilder(final ApplicationDetail application) {
    this.application = application;
  }

  /**
   * Sets the application type.
   *
   * @param applicationType The application type of the application.
   * @return The builder instance.
   */
  public InitialApplicationBuilder applicationType(
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
  public InitialApplicationBuilder caseReference(final CaseReferenceSummary caseReferenceSummary) {
    final String caseReference = Optional.ofNullable(caseReferenceSummary.getCaseReferenceNumber())
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
  public InitialApplicationBuilder provider(final UserDetail user) {
    final IntDisplayValue provider = new IntDisplayValue()
        .id(user.getProvider().getId())
        .displayValue(user.getProvider().getName());
    application.getProviderDetails().setProvider(provider);
    return this;
  }

  /**
   * Sets the client details.
   *
   * @param clientInformation The client information.
   * @return The builder instance.
   */
  public InitialApplicationBuilder client(
      final uk.gov.laa.ccms.soa.gateway.model.ClientDetail clientInformation) {
    final ClientDetail client = new ClientDetail()
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
  public InitialApplicationBuilder categoryOfLaw(
          final String categoryOfLawId,
          final CategoryOfLawLookupValueDetail categoryOfLawLookup) {
    final String categoryOfLawDisplayValue = Optional.ofNullable(categoryOfLawLookup)
        .map(CategoryOfLawLookupValueDetail::getMatterTypeDescription)
        .orElse(categoryOfLawId);

    final StringDisplayValue categoryOfLaw = new StringDisplayValue()
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
  public InitialApplicationBuilder office(final Integer officeId, final List<BaseOffice> offices) {
    final String officeDisplayValue = offices.stream()
            .filter(office -> officeId.equals(office.getId()))
            .map(BaseOffice::getName)
            .findFirst()
            .orElse(null);

    final IntDisplayValue office = new IntDisplayValue()
            .id(officeId)
            .displayValue(officeDisplayValue);
    application.getProviderDetails().setOffice(office);
    return this;
  }

  /**
   * Sets the contractual Devolved Power flag.
   *
   * @param contractDetails a list of contract details.
   * @param categoryOfLawId the id of the category of law type.
   * @return The builder instance.
   */
  public InitialApplicationBuilder contractualDevolvedPower(
      final List<ContractDetail> contractDetails,
      final String categoryOfLawId) {

    final String contractualDevolvedPower = contractDetails != null ? contractDetails.stream()
        .filter(contract -> categoryOfLawId
            .equals(contract.getCategoryofLaw()))
        .map(ContractDetail::getContractualDevolvedPowers)
        .findFirst()
        .orElse("No")
        : "No";

    if (application.getApplicationType() == null) {
      application.setApplicationType(new ApplicationType());
    }
    if (application.getApplicationType().getDevolvedPowers() == null) {
      application.getApplicationType().setDevolvedPowers(new DevolvedPowersDetail());
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
  public InitialApplicationBuilder larScopeFlag(final AmendmentTypeLookupDetail amendmentTypes) {

    final String defaultLarScopeFlag = Optional.ofNullable(amendmentTypes.getContent())
            .filter(content -> !content.isEmpty())
            .map(List::getFirst)
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
  public InitialApplicationBuilder status() {
    final StringDisplayValue status = new StringDisplayValue()
            .id(STATUS_UNSUBMITTED_ACTUAL_VALUE)
            .displayValue(STATUS_UNSUBMITTED_ACTUAL_VALUE_DISPLAY);
    application.setStatus(status);
    return this;
  }

  /**
   * Sets the cost structure details.
   *
   * @return The builder instance.
   */
  public InitialApplicationBuilder costStructure() {
    application.costs(
        new CostStructureDetail()
            .grantedCostLimitation(BigDecimal.valueOf(0))
            .defaultCostLimitation(BigDecimal.valueOf(0))
            .requestedCostLimitation(BigDecimal.valueOf(0))
    );
    return this;
  }

  /**
   * Sets the cost structure details based on the provided value.
   *
   * @param costStructure - the cost structure.
   * @return The builder instance.
   */
  public InitialApplicationBuilder costStructure(final CostStructureDetail costStructure) {
    application.costs(costStructure);
    return this;
  }

  /**
   * Sets the correspondence address details.
   *
   * @return The builder instance.
   */
  public InitialApplicationBuilder correspondenceAddress() {
    application.correspondenceAddress(
        new AddressDetail()
            .noFixedAbode(false));
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
