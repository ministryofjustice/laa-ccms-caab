package uk.gov.laa.ccms.caab.bean;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Component
public class ClientSearchDetailsValidator implements Validator{

    private static final String NUMERIC_PATTERN = "[0-9]+";

    @Override
    public boolean supports(Class<?> clazz) {
        return ApplicationDetails.class.isAssignableFrom(clazz);
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

        ClientSearchDetails clientSearchDetails = (ClientSearchDetails) target;

        if (!clientSearchDetails.getDobDay().isBlank()){
            if (!clientSearchDetails.getDobDay().matches(NUMERIC_PATTERN)) {
                errors.rejectValue("dobDay", "invalid.numeric",
                        "Please enter a numeric value for the day.");
            }
        }

        if (!clientSearchDetails.getDobMonth().isBlank()){
            if (!clientSearchDetails.getDobMonth().matches(NUMERIC_PATTERN)) {
                errors.rejectValue("dobMonth", "invalid.numeric",
                        "Please enter a numeric value for the month.");
            }
        }

        if (!clientSearchDetails.getDobYear().isBlank()){
            if (!clientSearchDetails.getDobYear().matches(NUMERIC_PATTERN)) {
                errors.rejectValue("dobYear", "invalid.numeric",
                        "Please enter a numeric value for the year.");
            }
        }

    }

    @Override
    public void validate(Object target, Errors errors) {
        validateForename(target,errors);
        validateSurnameAtBirth(target,errors);
        validateDateOfBirth(target,errors);
    }
}
