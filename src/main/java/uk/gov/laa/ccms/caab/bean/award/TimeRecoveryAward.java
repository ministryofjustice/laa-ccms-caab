package uk.gov.laa.ccms.caab.bean.award;

import java.math.BigDecimal;
import uk.gov.laa.ccms.caab.model.TimeRecoveryDetail;

/** Parent award values used by the shared time-related recovery page. */
public record TimeRecoveryAward(
    String awardType,
    String description,
    BigDecimal awardAmount,
    Boolean recoveryOfAwardTimeRelated,
    TimeRecoveryDetail timeRecovery) {}
