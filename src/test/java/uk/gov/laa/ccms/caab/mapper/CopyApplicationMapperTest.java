package uk.gov.laa.ccms.caab.mapper;

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
import org.junit.jupiter.api.Test;
import uk.gov.laa.ccms.caab.model.ApplicationDetail;
import uk.gov.laa.ccms.caab.model.ApplicationProviderDetails;
import uk.gov.laa.ccms.caab.model.ApplicationType;
import uk.gov.laa.ccms.caab.model.DevolvedPowers;
import uk.gov.laa.ccms.caab.model.Opponent;
import uk.gov.laa.ccms.caab.model.Proceeding;
import uk.gov.laa.ccms.caab.model.ScopeLimitation;

public class CopyApplicationMapperTest {

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
                    new DevolvedPowers()
                        .contractFlag("should remain unchanged")));
    ApplicationDetail result = copyApplicationMapper.copyApplication(
        newApplication,
        copyApplication);

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

  private ApplicationDetail buildExpectedApplication(ApplicationDetail copyApplication) {
    return new ApplicationDetail()
        .applicationType(
            new ApplicationType()
                .id(copyApplication.getApplicationType().getId())
                .displayValue(copyApplication.getApplicationType().getDisplayValue())
                .devolvedPowers(
                    new DevolvedPowers()
                        .contractFlag("should remain unchanged")
                        .dateUsed(copyApplication.getApplicationType().getDevolvedPowers().getDateUsed())
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

  private Proceeding applyProceedingMappingUpdates(Proceeding expectedProceeding) {
    expectedProceeding.setEbsId(null);
    expectedProceeding.getStatus().setId(STATUS_DRAFT);
    expectedProceeding.getStatus().setDisplayValue(PROCEEDING_STATUS_UNCHANGED_DISPLAY);
    applyScopeLimitationMappingUpdates(expectedProceeding.getScopeLimitations().get(0));

    return expectedProceeding;
  }

  private void applyScopeLimitationMappingUpdates(ScopeLimitation expectedScopeLimitation) {
    expectedScopeLimitation.setEbsId(null);
    expectedScopeLimitation.setDefaultInd(null);
    expectedScopeLimitation.setNonDefaultWordingReqd(null);
    expectedScopeLimitation.setStage(null);
  }

  private Opponent applyOpponentMappingUpdates(Opponent expectedOpponent) {
    expectedOpponent.setConfirmed(Boolean.TRUE);
    expectedOpponent.setDeleteInd(Boolean.TRUE);
    expectedOpponent.setAmendment(Boolean.FALSE);
    expectedOpponent.setAppMode(Boolean.TRUE);
    expectedOpponent.setAward(Boolean.FALSE);

    return expectedOpponent;
  }
}