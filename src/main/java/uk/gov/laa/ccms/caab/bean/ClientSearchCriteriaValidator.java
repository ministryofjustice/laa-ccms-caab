package uk.gov.laa.ccms.caab.bean;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import static uk.gov.laa.ccms.caab.constants.UniqueIdentifierTypeConstants.*;
import static uk.gov.laa.ccms.caab.constants.ValidationPatternConstants.*;

@Component
public class ClientSearchCriteriaValidator implements Validator{

    private static final String GENERIC_UNIQUE_IDENTIFIER_ERROR = "Your input for 'Unique Identifier Value' " +
            "is in an incorrect format. Please amend your entry.";

    @Override
    public boolean supports(Class<?> clazz) {
        return ClientSearchCriteria.class.isAssignableFrom(clazz);
    }

    public void validateForename(Object target, Errors errors){
        ValidationUtils.rejectIfEmpty(errors, "forename",
                "required.forename", "Please complete 'First name'.");
    }

    public void validateSurnameAtBirth(Object target, Errors errors){
        ValidationUtils.rejectIfEmpty(errors, "surname",
                "required.surname", "Please complete 'Surname at birth'.");
    }

    public void validateDateOfBirth(Object target, Errors errors){
        ValidationUtils.rejectIfEmpty(errors, "dobDay",
                "required.dob-day", "Please complete 'Date of birth' with a day.");
        ValidationUtils.rejectIfEmpty(errors, "dobMonth",
                "required.dob-month", "Please complete 'Date of birth' with a month.");
        ValidationUtils.rejectIfEmpty(errors, "dobYear",
                "required.dob-year", "Please complete 'Date of birth' with a year.");

        ClientSearchCriteria clientSearchCriteria = (ClientSearchCriteria) target;

        if (!clientSearchCriteria.getDobDay().isBlank()){
            if (!clientSearchCriteria.getDobDay().matches(NUMERIC_PATTERN)) {
                errors.rejectValue("dobDay", "invalid.numeric",
                        "Please enter a numeric value for the day.");
            }
        }

        if (!clientSearchCriteria.getDobMonth().isBlank()){
            if (!clientSearchCriteria.getDobMonth().matches(NUMERIC_PATTERN)) {
                errors.rejectValue("dobMonth", "invalid.numeric",
                        "Please enter a numeric value for the month.");
            }
        }

        if (!clientSearchCriteria.getDobYear().isBlank()){
            if (!clientSearchCriteria.getDobYear().matches(NUMERIC_PATTERN)) {
                errors.rejectValue("dobYear", "invalid.numeric",
                        "Please enter a numeric value for the year.");
            }
        }

    }

    public void validateUniqueIdentifierType(Object target, Errors errors){
        ClientSearchCriteria clientSearchCriteria = (ClientSearchCriteria) target;

        if (clientSearchCriteria.getUniqueIdentifierType() != null){
            if (clientSearchCriteria.getUniqueIdentifierType() == UNIQUE_IDENTIFIER_NATIONAL_INSURANCE_NUMBER) {
                if (!clientSearchCriteria.getUniqueIdentifierValue().matches(NATIONAL_INSURANCE_NUMBER_PATTERN)) {
                    errors.rejectValue("uniqueIdentifierValue", "invalid.uniqueIdentifierValue",
                            "Your input for 'Unique Identifier Value' is not in the correct format. " +
                                        "The format for 'Unique Identifier Value' is AANNNNNNA, where A is a letter " +
                                        " and N is a number. Please amend your entry.");
                }
            }
        }

        if (clientSearchCriteria.getUniqueIdentifierType() != null){
            if (clientSearchCriteria.getUniqueIdentifierType() == UNIQUE_IDENTIFIER_HOME_OFFICE_REFERENCE) {
                if (!clientSearchCriteria.getUniqueIdentifierValue().matches(HOME_OFFICE_NUMBER_PATTERN)) {
                    errors.rejectValue("uniqueIdentifierValue", "invalid.uniqueIdentifierValue",
                            GENERIC_UNIQUE_IDENTIFIER_ERROR);
                }
            }
        }

        if (clientSearchCriteria.getUniqueIdentifierType() != null){
            if (clientSearchCriteria.getUniqueIdentifierType() == UNIQUE_IDENTIFIER_CASE_REFERENCE_NUMBER) {
                if (!clientSearchCriteria.getUniqueIdentifierValue().matches(CASE_REFERENCE_NUMBER_PATTERN) ||
                        clientSearchCriteria.getUniqueIdentifierValue().matches(CASE_REFERENCE_NUMBER_NEGATIVE_PATTERN)) {
                    errors.rejectValue("uniqueIdentifierValue", "invalid.uniqueIdentifierValue",
                            GENERIC_UNIQUE_IDENTIFIER_ERROR);
                }
            }
        }

    }

    @Override
    public void validate(Object target, Errors errors) {
        validateForename(target,errors);
        validateSurnameAtBirth(target,errors);
        validateDateOfBirth(target,errors);
        validateUniqueIdentifierType(target, errors);
    }
}
