package uk.gov.laa.ccms.caab.builders;

import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY_DEVOLVED_POWERS_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EMERGENCY_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_EXCEPTIONAL_CASE_FUNDING_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.APP_TYPE_SUBSTANTIVE_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_UNSUBMITTED_ACTUAL_VALUE_DISPLAY;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import uk.gov.laa.ccms.caab.bean.ApplicationDetails;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.Client;
import uk.gov.laa.ccms.caab.model.DevolvedPowers;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.caab.model.StringDisplayValue;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupDetail;
import uk.gov.laa.ccms.data.model.AmendmentTypeLookupValueDetail;
import uk.gov.laa.ccms.data.model.BaseOffice;
import uk.gov.laa.ccms.data.model.CommonLookupDetail;
import uk.gov.laa.ccms.data.model.CommonLookupValueDetail;
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
   * @param applicationTypeCategory The type category of the application.
   * @param isDelegatedFunctions    Flag indicating if delegated functions are enabled.
   * @return The builder instance.
   */
  public ApplicationBuilder applicationType(
          final String applicationTypeCategory,
          final boolean isDelegatedFunctions) {
    StringDisplayValue applicationType = new StringDisplayValue();
    if (APP_TYPE_SUBSTANTIVE.equals(applicationTypeCategory)) {
      applicationType.setId(isDelegatedFunctions
              ? APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS : APP_TYPE_SUBSTANTIVE);
      applicationType.setDisplayValue(isDelegatedFunctions
              ? APP_TYPE_SUBSTANTIVE_DEVOLVED_POWERS_DISPLAY : APP_TYPE_SUBSTANTIVE_DISPLAY);
    } else if (APP_TYPE_EMERGENCY.equals(applicationTypeCategory)) {
      applicationType.setId(isDelegatedFunctions
              ? APP_TYPE_EMERGENCY_DEVOLVED_POWERS : APP_TYPE_EMERGENCY);
      applicationType.setDisplayValue(isDelegatedFunctions
              ? APP_TYPE_EMERGENCY_DEVOLVED_POWERS_DISPLAY : APP_TYPE_EMERGENCY_DISPLAY);
    } else {
      applicationType.setId(APP_TYPE_EXCEPTIONAL_CASE_FUNDING);
      applicationType.setDisplayValue(APP_TYPE_EXCEPTIONAL_CASE_FUNDING_DISPLAY);
    }
    // Commented out until merge/rebase is done with Phil's stuff!
    //application.setApplicationType(applicationType);
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
   * @param categoryOfLawValues The category of law lookup values.
   * @return The builder instance.
   */
  public ApplicationBuilder categoryOfLaw(
          final String categoryOfLawId,
          final CommonLookupDetail categoryOfLawValues) {
    String categoryOfLawDisplayValue = categoryOfLawValues
            .getContent()
            .stream()
            .filter(category -> categoryOfLawId.equals(category.getCode()))
            .map(CommonLookupValueDetail::getDescription)
            .findFirst()
            .orElse(null);

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
   * Sets the devolved powers details.
   *
   * @param contractDetails     The list of contract details.
   * @param applicationDetails  The application details.
   * @return The builder instance.
   * @throws ParseException If there's an error in parsing dates.
   */
  public ApplicationBuilder devolvedPowers(
          final List<ContractDetail> contractDetails,
          final ApplicationDetails applicationDetails) throws ParseException {
    DevolvedPowers devolvedPowers = new DevolvedPowers();

    String contractualDevolvedPower = contractDetails != null ? contractDetails.stream()
            .filter(contract -> applicationDetails.getCategoryOfLawId()
                    .equals(contract.getCategoryofLaw()))
            .map(ContractDetail::getContractualDevolvedPowers)
            .findFirst()
            .orElse(null)
            : null;

    devolvedPowers.setContractFlag(contractualDevolvedPower);
    devolvedPowers.setUsed(applicationDetails.isDelegatedFunctions());

    if (applicationDetails.isDelegatedFunctions()) {
      String dateString = applicationDetails.getDelegatedFunctionUsedDay() + "-"
              + applicationDetails.getDelegatedFunctionUsedMonth() + "-"
              + applicationDetails.getDelegatedFunctionUsedYear();
      SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
      devolvedPowers.setDateUsed(sdf.parse(dateString));
    }
    //application.setDevolvedPowers(devolvedPowers);
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
