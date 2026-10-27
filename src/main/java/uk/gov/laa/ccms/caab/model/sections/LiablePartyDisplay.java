package uk.gov.laa.ccms.caab.model.sections;

/** A party linked to the selected award, with resolved display values. */
public record LiablePartyDisplay(
    Integer id,
    Integer opponentId,
    String name,
    String partyType,
    String relationshipToCase,
    String address,
    boolean editable,
    boolean organisation) {}
