package uk.gov.laa.ccms.caab.model.sections;

import java.math.BigDecimal;
import java.util.List;

/** Read-only award context and its liable or interested parties. */
public record LiablePartiesDisplay(
    String awardType,
    String description,
    BigDecimal awardAmount,
    boolean costOrFinancial,
    List<LiablePartyDisplay> parties) {}
