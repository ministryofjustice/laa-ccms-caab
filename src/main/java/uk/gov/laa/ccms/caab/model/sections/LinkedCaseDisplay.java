package uk.gov.laa.ccms.caab.model.sections;

/**
 * Represents a linked case display model.
 *
 * @param lscCaseReference The LSC case reference.
 * @param relationToCase The relation to the case.
 */
public record LinkedCaseDisplay(String lscCaseReference, String relationToCase) {
}
