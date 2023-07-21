package uk.gov.laa.ccms.caab.constants;

public class ValidationPatternConstants {

    /**
     * Validation pattern to check for numerical values
     */
    public static final String NUMERIC_PATTERN = "[0-9]+";

    /**
     * Validation pattern for national insurance numbers
     */
    public static final String NATIONAL_INSURANCE_NUMBER_PATTERN = "^[A-Za-z]{2}[0-9]{6}[A-Za-z]{1}$";

    /**
     * Validation pattern for home office reference numbers
     */
    public static final String HOME_OFFICE_NUMBER_PATTERN = "^[A-Za-z0-9\s]*$";

    /**
     * Validation pattern for case reference numbers
     */
    public static final String CASE_REFERENCE_NUMBER_PATTERN = "^[A-Za-z0-9\s/]*$";

    /**
     * Neagtive validation pattern for case reference numbers
     */
    public static final String CASE_REFERENCE_NUMBER_NEGATIVE_PATTERN = "(.*)\s\s(.*)";
}
