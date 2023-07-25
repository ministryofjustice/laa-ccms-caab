package uk.gov.laa.ccms.caab.bean;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class ApplicationSearchCriteriaValidator implements Validator{

    @Override
    public boolean supports(Class<?> clazz) {
        return ApplicationSearchCriteria.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        validateAtLeastOneSearchCriteria(target, errors);
    }

    public void validateAtLeastOneSearchCriteria(Object target, Errors errors) {
        ApplicationSearchCriteria searchCriteria = (ApplicationSearchCriteria) target;

        if (StringUtils.isBlank(searchCriteria.getCaseReference()) &&
            StringUtils.isBlank(searchCriteria.getClientSurname()) &&
            StringUtils.isBlank(searchCriteria.getProviderReference()) &&
            searchCriteria.getFeeEarnerId() == null &&
            searchCriteria.getOfficeId() == null
            // Old PUI also checks actualStatus, but I don't think this would be intialised until after validation.
            // && StringUtils.isBlank(searchCriteria.getActualStatus())
            ) {
            errors.rejectValue(null, "required.atLeastOneSearchCriteria",
                "You must provide at least one search criteria below. Please amend your entry.");
        }

    }
}
