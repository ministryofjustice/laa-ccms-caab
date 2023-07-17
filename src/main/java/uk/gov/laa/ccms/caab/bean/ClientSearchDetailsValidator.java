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
                "required.dob-day", "Please complete 'Date of birth'.");
        ValidationUtils.rejectIfEmpty(errors, "dobMonth",
                "required.dob-month", "Please complete 'Date of birth'.");
        ValidationUtils.rejectIfEmpty(errors, "dobYear",
                "required.dob-year", "Please complete 'Date of birth'.");
    }

    @Override
    public void validate(Object target, Errors errors) {
        validateForename(target,errors);
        validateSurnameAtBirth(target,errors);
        validateDateOfBirth(target,errors);
    }
}
