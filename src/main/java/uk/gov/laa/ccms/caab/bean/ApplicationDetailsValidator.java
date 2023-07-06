package uk.gov.laa.ccms.caab.bean;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Component
public class ApplicationDetailsValidator implements Validator{

    @Override
    public boolean supports(Class<?> clazz) {
        return ApplicationDetails.class.isAssignableFrom(clazz);
    }

    public void validateSelectOffice(Object target, Errors errors){
        ValidationUtils.rejectIfEmpty(errors, "officeId",
                "required.officeId", "Please select an office.");
    }

    public void validateCategoryOfLaw(Object target, Errors errors){
        ValidationUtils.rejectIfEmpty(errors, "categoryOfLawId",
                "required.categoryOfLawId", "Please select a category of law.");
    }

    public void validateApplicationType(Object target, Errors errors){
        ValidationUtils.rejectIfEmpty(errors, "applicationTypeId",
                "required.applicationTypeId", "Please select an application type.");
    }

    @Override
    public void validate(Object target, Errors errors) {
        validateSelectOffice(target, errors);
        validateCategoryOfLaw(target, errors);
        validateApplicationType(target, errors);
    }
}
