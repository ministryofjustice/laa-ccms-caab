package uk.gov.laa.ccms.caab.model;

/**
 * Represents an available action that can be performed for the case.
 *
 * @param actionCode     The code identifying the action.
 * @param actionKey      The key associated with the action.
 * @param descriptionKey The key used to retrieve the action's description.
 * @param link           The URL or path associated with the action.
 */
public record AvailableAction(String actionCode,
                              String actionKey,
                              String descriptionKey,
                              String link) {
}
