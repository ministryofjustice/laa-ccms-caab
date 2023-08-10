package uk.gov.laa.ccms.caab.bean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
public class CopyCaseSearchCriteriaValidatorTest {

    @InjectMocks
    private CopyCaseSearchCriteriaValidator validator;

    private CopyCaseSearchCriteria searchCriteria;

    private Errors errors;

    @BeforeEach
    public void setUp() {
        searchCriteria = new CopyCaseSearchCriteria(); // Assuming that the default constructor sets all fields to null.
        errors = new BeanPropertyBindingResult(searchCriteria, "applicationSearchCriteria");
    }

    @Test
    public void supports_ReturnsTrueForSupportedClass() {
        assertTrue(validator.supports(CopyCaseSearchCriteria.class));
    }

    @Test
    public void supports_ReturnsFalseForOtherClasses() {
        assertFalse(validator.supports(Object.class));
    }

    @Test
    public void validate_validatesNoSearchCriteria() {
        validator.validate(searchCriteria, errors);
        assertTrue(errors.hasErrors());
        assertEquals(1, errors.getGlobalErrorCount());
        assertEquals("required.atLeastOneSearchCriteria", errors.getGlobalErrors().get(0).getCode());
    }

    @Test
    public void validate_validatesSearchCriteriaCaseRef() {
        searchCriteria.setCaseReference("123");
        validator.validate(searchCriteria, errors);
        assertFalse(errors.hasErrors());
    }

    @Test
    public void validate_validatesSearchCriteriaSurname() {
        searchCriteria.setClientSurname("surname");
        validator.validate(searchCriteria, errors);
        assertFalse(errors.hasErrors());
    }

    @Test
    public void validate_validatesSearchCriteriaOffice() {
        searchCriteria.setOfficeId(1);
        validator.validate(searchCriteria, errors);
        assertFalse(errors.hasErrors());
    }

    @Test
    public void validate_validatesSearchCriteriaProviderRef() {
        searchCriteria.setProviderCaseReference("provref");
        validator.validate(searchCriteria, errors);
        assertFalse(errors.hasErrors());
    }

    @Test
    public void validate_validatesSearchCriteriaFeeEarner() {
        searchCriteria.setFeeEarnerId(123);
        validator.validate(searchCriteria, errors);
        assertFalse(errors.hasErrors());
    }
}