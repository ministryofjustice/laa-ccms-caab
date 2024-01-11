package uk.gov.laa.ccms.caab.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.PROCEEDING_STATUS_UNCHANGED_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_DRAFT;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildApplicationDetail;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildOpponent;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildProceeding;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildScopeLimitation;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildCaseReferenceSummary;
import static uk.gov.laa.ccms.caab.util.SoaModelUtils.buildClientDetail;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.builders.ApplicationBuilder;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.Client;
import uk.gov.laa.ccms.caab.model.CostStructure;
import uk.gov.laa.ccms.caab.model.Opponent;
import uk.gov.laa.ccms.caab.model.Proceeding;
import uk.gov.laa.ccms.caab.model.ScopeLimitation;
import uk.gov.laa.ccms.soa.gateway.model.CaseReferenceSummary;
import uk.gov.laa.ccms.soa.gateway.model.ClientDetail;

public class CopyApplicationMapperTest {

  private final CopyApplicationMapper copyApplicationMapper = new CopyApplicationMapperImpl();

  @Test
  void testCopyApplication() {
    Date sharedDate = new Date();
    BigDecimal requestedCostLimitation = BigDecimal.TEN;
    BigDecimal defaultCostLimitation = BigDecimal.ONE;

    ApplicationDetail copyApplication = buildApplicationDetail(2, true, sharedDate);

    ClientDetail soaClient = buildClientDetail();

    CaseReferenceSummary soaCaseReference = buildCaseReferenceSummary();

    // Build and update the expected application in line with the expected mappings.
    ApplicationDetail expectedApplication = new ApplicationBuilder().build();
    applyApplicationMappingUpdates(expectedApplication,
        copyApplication,
        soaCaseReference,
        soaClient,
        requestedCostLimitation,
        defaultCostLimitation);

    ApplicationDetail result = copyApplicationMapper.copyApplication(
        copyApplication,
        soaCaseReference,
        soaClient,
        requestedCostLimitation,
        defaultCostLimitation);

    assertEquals(expectedApplication, result);
  }

  @Test
  void testCopyProceeding() {
    Date date = new Date();
    Proceeding expectedProceeding = buildProceeding(date, BigDecimal.ONE);

    Proceeding result = copyApplicationMapper.copyProceeding(buildProceeding(date, BigDecimal.ONE));

    // Now update the expected proceeding to what the mapper should return
    applyProceedingMappingUpdates(expectedProceeding);

    assertEquals(expectedProceeding, result);
  }

  @Test
  void testCopyScopeLimitation() {
    ScopeLimitation expectedScopeLimitation = buildScopeLimitation();

    ScopeLimitation result = buildScopeLimitation();
    result = copyApplicationMapper.copyScopeLimitation(result);

    // Now update the expected scope limitation to what the mapper should return
    applyScopeLimitationMappingUpdates(expectedScopeLimitation);

    assertEquals(expectedScopeLimitation, result);
  }

  @Test
  void testCopyOpponent() {
    Date date = new Date();
    Opponent expectedOpponent = buildOpponent(date);

    Opponent result = copyApplicationMapper.copyOpponent(buildOpponent(date));

    // Now update the expected opponent to what the mapper should return
    applyOpponentMappingUpdates(expectedOpponent);

    assertEquals(expectedOpponent, result);
  }

  private void applyApplicationMappingUpdates(ApplicationDetail expectedApplication,
      ApplicationDetail copyApplication,
      CaseReferenceSummary caseReferenceSummary,
      ClientDetail clientDetail,
      BigDecimal requestedCostLimitation,
      BigDecimal defaultCostLimitation) {
    expectedApplication.setCaseReferenceNumber(caseReferenceSummary.getCaseReferenceNumber());
    expectedApplication.setApplicationType(copyApplication.getApplicationType());
    if (expectedApplication.getProviderDetails() == null) {
      expectedApplication.setProviderDetails(new ApplicationProviderDetails());
    }
    expectedApplication.getProviderDetails().setProvider(
        copyApplication.getProviderDetails().getProvider());
    expectedApplication.getProviderDetails().setOffice(
        copyApplication.getProviderDetails().getOffice());
    expectedApplication.getProviderDetails().setSupervisor(
        copyApplication.getProviderDetails().getSupervisor());
    expectedApplication.getProviderDetails().setFeeEarner(
        copyApplication.getProviderDetails().getFeeEarner());
    expectedApplication.getProviderDetails().setProviderContact(
        copyApplication.getProviderDetails().getProviderContact());
    expectedApplication.setCategoryOfLaw(copyApplication.getCategoryOfLaw());
    expectedApplication.setCorrespondenceAddress(copyApplication.getCorrespondenceAddress());
    expectedApplication.setLarScopeFlag(copyApplication.getLarScopeFlag());
    expectedApplication.setCosts(new CostStructure());
    expectedApplication.getCosts().setRequestedCostLimitation(requestedCostLimitation);
    expectedApplication.getCosts().setDefaultCostLimitation(defaultCostLimitation);
    expectedApplication.setProceedings(List.copyOf(copyApplication.getProceedings()));
    expectedApplication.getProceedings().forEach(this::applyProceedingMappingUpdates);
    expectedApplication.setOpponents(List.copyOf(copyApplication.getOpponents()));
    expectedApplication.setClient(new Client()
        .firstName(clientDetail.getDetails().getName().getFirstName())
        .surname(clientDetail.getDetails().getName().getSurname())
        .reference(clientDetail.getClientReferenceNumber()));
    applyOpponentMappingUpdates(expectedApplication.getOpponents().get(0));
  }

  private void applyProceedingMappingUpdates(Proceeding expectedProceeding) {
    expectedProceeding.setEbsId(null);
    expectedProceeding.getStatus().setId(STATUS_DRAFT);
    expectedProceeding.getStatus().setDisplayValue(PROCEEDING_STATUS_UNCHANGED_DISPLAY);
    applyScopeLimitationMappingUpdates(expectedProceeding.getScopeLimitations().get(0));
  }

  private void applyScopeLimitationMappingUpdates(ScopeLimitation expectedScopeLimitation) {
    expectedScopeLimitation.setEbsId(null);
    expectedScopeLimitation.setDefaultInd(null);
    expectedScopeLimitation.setNonDefaultWordingReqd(null);
    expectedScopeLimitation.setStage(null);
  }

  private void applyOpponentMappingUpdates(Opponent expectedOpponent) {
    expectedOpponent.setConfirmed(Boolean.TRUE);
    expectedOpponent.setDeleteInd(Boolean.TRUE);
    expectedOpponent.setAmendment(Boolean.FALSE);
    expectedOpponent.setAppMode(Boolean.TRUE);
    expectedOpponent.setAward(Boolean.FALSE);
  }
}