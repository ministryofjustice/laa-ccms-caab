package uk.gov.laa.ccms.caab.builders;

import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ClientDetail;
import uk.gov.laa.ccms.caab.model.CostLimitDetail;
import uk.gov.laa.ccms.caab.model.IntDisplayValue;
import uk.gov.laa.ccms.data.model.BaseClient;
import uk.gov.laa.ccms.data.model.CaseDetail;
import uk.gov.laa.ccms.data.model.SubmittedApplicationDetails;
import uk.gov.laa.ccms.data.model.UserDetail;

public class InitialAmendmentBuilder {

  private final ApplicationDetail application;

  public InitialAmendmentBuilder(final UserDetail user) {
    final IntDisplayValue provider = new IntDisplayValue()
        .id(user.getProvider().getId())
        .displayValue(user.getProvider().getName());
    ApplicationProviderDetails providerDetails = new ApplicationProviderDetails();
    providerDetails.setProvider(provider);
    this.application = new ApplicationDetail().providerDetails(providerDetails);
    this.application.setAmendment(true);
    CostLimitDetail costLimit = new CostLimitDetail();
    costLimit.setChanged(false);
    this.application.setCostLimit(costLimit);
  }

  public InitialAmendmentBuilder withCaseDetail(CaseDetail caseDetail) {
    application.setCaseReferenceNumber(caseDetail.getCaseReferenceNumber());
    if (Objects.nonNull(caseDetail.getApplicationDetails())) {
      application.setClient(getClientDetail(caseDetail.getApplicationDetails()));
    }
    return this;
  }

  private static ClientDetail getClientDetail(@NotNull SubmittedApplicationDetails submittedApplicationDetails) {
    final BaseClient baseClient = submittedApplicationDetails.getClient();
    return new ClientDetail()
        .firstName(baseClient.getFirstName())
        .surname(baseClient.getSurname())
        .reference(baseClient.getClientReferenceNumber());
  }


  public ApplicationDetail build() {
    return application;
  }
}
