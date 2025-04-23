package uk.gov.laa.ccms.caab.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.PROCEEDING_STATUS_UNCHANGED_DISPLAY;
import static uk.gov.laa.ccms.caab.constants.ApplicationConstants.STATUS_DRAFT;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildApplicationDetail;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildOpponent;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildProceeding;
import static uk.gov.laa.ccms.caab.util.CaabModelUtils.buildScopeLimitation;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.DevolvedPowersDetail;
import uk.gov.laa.ccms.caab.model.OpponentDetail;
import uk.gov.laa.ccms.caab.model.ProceedingDetail;
import uk.gov.laa.ccms.caab.model.ScopeLimitationDetail;

class CopyApplicationMapperTest {

  private final CopyApplicationMapper copyApplicationMapper = new CopyApplicationMapperImpl();

  @Test
  void testCopyApplication() {
    Date sharedDate = new Date();

    ApplicationDetail copyApplication = buildApplicationDetail(2, true, sharedDate);

    // Build and update the expected application in line with the expected mappings.
    ApplicationDetail expectedApplication = buildExpectedApplication(copyApplication);

    ApplicationDetail newApplication = new ApplicationDetail()
        .applicationType(
            new ApplicationType()
                .devolvedPowers(
                    new DevolvedPowersDetail()
                        .contractFlag("should remain unchanged")));
    ApplicationDetail result = copyApplicationMapper.copyApplication(
        newApplication,
        copyApplication);

    assertEquals(expectedApplication, result);
  }

  @Test
  void testCopyProceeding() {
    Date date = new Date();
    ProceedingDetail expectedProceeding = buildProceeding(date, BigDecimal.ONE);

    ProceedingDetail result =
        copyApplicationMapper.copyProceeding(buildProceeding(date, BigDecimal.ONE));

    // Now update the expected proceeding to what the mapper should return
    applyProceedingMappingUpdates(expectedProceeding);

    assertEquals(expectedProceeding, result);
  }

  @Test
  void testCopyScopeLimitation() {
    ScopeLimitationDetail expectedScopeLimitation = buildScopeLimitation();

    ScopeLimitationDetail result = buildScopeLimitation();
    result = copyApplicationMapper.copyScopeLimitation(result);

    // Now update the expected scope limitation to what the mapper should return
    applyScopeLimitationMappingUpdates(expectedScopeLimitation);

    assertEquals(expectedScopeLimitation, result);
  }

  @Test
  void testCopyOpponent() {
    Date date = new Date();
    OpponentDetail expectedOpponent = buildOpponent(date);

    OpponentDetail result = copyApplicationMapper.copyOpponent(buildOpponent(date));

    // Now update the expected opponent to what the mapper should return
    applyOpponentMappingUpdates(expectedOpponent);

    assertEquals(expectedOpponent, result);
  }

  @Test
  @DisplayName("Should copy opponent with null address as empty address")
  void testCopyOpponentIfAddressIsNull() {
    Date date = new Date();
    OpponentDetail opponentToCopy = buildOpponent(date);
    opponentToCopy.setAddress(null);
    OpponentDetail result = copyApplicationMapper.copyOpponent(opponentToCopy);
    assertThat(result.getAddress()).isNotNull().hasAllNullFieldsOrProperties();
  }

  private ApplicationDetail buildExpectedApplication(ApplicationDetail copyApplication) {
    return new ApplicationDetail()
        .applicationType(
            new ApplicationType()
                .id(copyApplication.getApplicationType().getId())
                .displayValue(copyApplication.getApplicationType().getDisplayValue())
                .devolvedPowers(
                    new DevolvedPowersDetail()
                        .contractFlag("should remain unchanged")
                        .dateUsed(
                            copyApplication.getApplicationType().getDevolvedPowers().getDateUsed())
                        .used(copyApplication.getApplicationType().getDevolvedPowers().getUsed())))
        .providerDetails(new ApplicationProviderDetails()
            .provider(copyApplication.getProviderDetails().getProvider())
            .office(copyApplication.getProviderDetails().getOffice())
            .supervisor(copyApplication.getProviderDetails().getSupervisor())
            .feeEarner(copyApplication.getProviderDetails().getFeeEarner())
            .providerContact(copyApplication.getProviderDetails().getProviderContact()))
        .categoryOfLaw(copyApplication.getCategoryOfLaw())
        .correspondenceAddress(copyApplication.getCorrespondenceAddress())
        .larScopeFlag(copyApplication.getLarScopeFlag())
        .proceedings(List.copyOf(copyApplication.getProceedings()))
        .opponents(copyApplication.getOpponents().stream()
            .map(this::applyOpponentMappingUpdates)
            .toList())
        .proceedings(
            copyApplication.getProceedings().stream()
                .map(this::applyProceedingMappingUpdates)
                .toList());
  }

  private ProceedingDetail applyProceedingMappingUpdates(ProceedingDetail expectedProceeding) {
    expectedProceeding.setEbsId(null);
    expectedProceeding.getStatus().setId(STATUS_DRAFT);
    expectedProceeding.getStatus().setDisplayValue(PROCEEDING_STATUS_UNCHANGED_DISPLAY);
    applyScopeLimitationMappingUpdates(expectedProceeding.getScopeLimitations().getFirst());

    return expectedProceeding;
  }

  private void applyScopeLimitationMappingUpdates(ScopeLimitationDetail expectedScopeLimitation) {
    expectedScopeLimitation.setEbsId(null);
    expectedScopeLimitation.setDefaultInd(null);
    expectedScopeLimitation.setNonDefaultWordingReqd(null);
    expectedScopeLimitation.setStage(null);
  }

  private OpponentDetail applyOpponentMappingUpdates(OpponentDetail expectedOpponent) {
    expectedOpponent.setConfirmed(Boolean.TRUE);
    expectedOpponent.setDeleteInd(Boolean.TRUE);
    expectedOpponent.setAmendment(Boolean.FALSE);
    expectedOpponent.setAppMode(Boolean.TRUE);
    expectedOpponent.setAward(Boolean.FALSE);

    return expectedOpponent;
  }
}