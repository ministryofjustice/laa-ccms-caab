package uk.gov.laa.ccms.caab.model.sections;

/**
 * A row on the party list used to add existing case opponents to an award.
 *
 * @param key the stable key submitted when the row is selected
 * @param name the party's display name
 * @param partyType the party type
 * @param relationshipToCase the party's relationship to the case
 * @param address the party's compact address
 * @param alreadyAdded whether the party is already linked to the award
 */
public record PartyListRow(
    String key,
    String name,
    String partyType,
    String relationshipToCase,
    String address,
    boolean alreadyAdded) {}
